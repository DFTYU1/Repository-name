#include <jni.h>
#include "llama.h"
#include <atomic>
#include <algorithm>
#include <chrono>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

// All generation calls are serialized by LocalModelEngine.LOCK. Cancellation is
// the only concurrent operation and touches no model/context pointer.
static std::atomic<int64_t> active_request{0};
static std::unique_ptr<llama_model, decltype(&llama_model_free)> model(nullptr, llama_model_free);
static std::string model_path;
static double stats[4]{};
static bool abort_decode(void *) { return (active_request.load()==0); }
static bool loading(float, void *) { return !(active_request.load()==0); }
static void check() { if((active_request.load()==0)) throw std::runtime_error("Inference cancelled"); }
static std::string bytes(JNIEnv *env, jbyteArray input) {
    auto n=env->GetArrayLength(input); std::string s(n,'\0');
    env->GetByteArrayRegion(input,0,n,reinterpret_cast<jbyte *>(s.data())); return s;
}
static double milliseconds(std::chrono::steady_clock::time_point start) {
    return std::chrono::duration<double,std::milli>(std::chrono::steady_clock::now()-start).count();
}
extern "C" JNIEXPORT void JNICALL Java_local_aicenter_platform_LocalModelEngine_nativePrepare(JNIEnv *,jclass,jlong request) { active_request=request; }
extern "C" JNIEXPORT void JNICALL Java_local_aicenter_platform_LocalModelEngine_nativeCancel(JNIEnv *,jclass,jlong request) { int64_t expected=request; active_request.compare_exchange_strong(expected,0); }
extern "C" JNIEXPORT jdoubleArray JNICALL Java_local_aicenter_platform_LocalModelEngine_nativeStats(JNIEnv *env,jclass) {
    auto result=env->NewDoubleArray(4); env->SetDoubleArrayRegion(result,0,4,stats); return result;
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_local_aicenter_platform_LocalModelEngine_nativeGenerate(
    JNIEnv *env,jclass,jbyteArray path_bytes,jbyteArray prompt_bytes,jbyteArray grammar_bytes,jint limit,jint threads) {
    try {
        check(); auto started=std::chrono::steady_clock::now();
        auto path=bytes(env,path_bytes), prompt=bytes(env,prompt_bytes), grammar=bytes(env,grammar_bytes);
        if(prompt.size()>64000 || limit<1 || limit>1024) throw std::runtime_error("Input exceeds inference budget");
        if(!model || path!=model_path) {
            model.reset(); llama_log_set([](ggml_log_level,const char *,void *){},nullptr); llama_backend_init();
            auto mp=llama_model_default_params(); mp.n_gpu_layers=0;
            mp.progress_callback=loading; mp.progress_callback_user_data=nullptr;
            model.reset(llama_model_load_from_file(path.c_str(),mp));
            if(!model) throw std::runtime_error("Unable to load verified local GGUF");
            model_path=path;
        }
        check(); const auto *vocab=llama_model_get_vocab(model.get());
        // Fixed ChatML template is the verified text-only, thinking-disabled
        // Qwen3.5 format. No external server, network or shell is involved.
        const std::string system=
            "You are a helpful local assistant. Answer accurately and concisely in the user's language. "
            "Use exact canonical terminology for named standards, methods, and acronym expansions; do not substitute a related synonym. "
            "For spreadsheet formula requests, prefer the appropriate built-in function and a compact range reference over enumerating cells. "
            "Silently verify that every requested component is present before answering. "
            "Never claim to execute a tool unless a tool result is provided.";
        llama_chat_message messages[]={{"system",system.c_str()},{"user",prompt.c_str()}};
        int size=llama_chat_apply_template("chatml",messages,2,true,nullptr,0);
        if(size<0) throw std::runtime_error("Chat template failed");
        std::vector<char> buffer(size+1);
        llama_chat_apply_template("chatml",messages,2,true,buffer.data(),buffer.size());
        std::string formatted(buffer.data(),size); formatted+="<think>\n\n</think>\n\n";
        int count=-llama_tokenize(vocab,formatted.data(),formatted.size(),nullptr,0,true,true);
        if(count<=0 || count+limit>2048) throw std::runtime_error("Prompt exceeds local context budget");
        std::vector<llama_token> tokens(count);
        if(llama_tokenize(vocab,formatted.data(),formatted.size(),tokens.data(),count,true,true)!=count)
            throw std::runtime_error("Tokenization failed");
        auto cp=llama_context_default_params(); cp.n_ctx=2048; cp.n_batch=256; cp.n_ubatch=256;
        cp.n_threads=threads; cp.n_threads_batch=threads;
        cp.abort_callback=abort_decode; cp.abort_callback_data=nullptr;
        std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model.get(),cp),llama_free);
        if(!ctx) throw std::runtime_error("Insufficient memory for context");
        std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(
            llama_sampler_chain_init(llama_sampler_chain_default_params()),llama_sampler_free);
        if(!grammar.empty()) {
            auto *g=llama_sampler_init_grammar(vocab,grammar.c_str(),"root");
            if(!g) throw std::runtime_error("Invalid tool grammar");
            llama_sampler_chain_add(sampler.get(),g);
        }
        llama_sampler_chain_add(sampler.get(),llama_sampler_init_greedy());
        for(int offset=0;offset<count;offset+=256) {
            check(); auto b=llama_batch_get_one(tokens.data()+offset,std::min(256,count-offset));
            if(llama_decode(ctx.get(),b)!=0) throw std::runtime_error("Prompt evaluation failed");
        }
        std::string answer; int generated=0; double first=0;
        auto decode_started=std::chrono::steady_clock::now();
        for(int i=0;i<limit;i++) {
            check(); auto token=llama_sampler_sample(sampler.get(),ctx.get(),-1);
            if(llama_vocab_is_eog(vocab,token)) break;
            std::vector<char> piece(256);
            int n=llama_token_to_piece(vocab,token,piece.data(),piece.size(),0,false);
            if(n<0) {piece.resize(-n); n=llama_token_to_piece(vocab,token,piece.data(),piece.size(),0,false);}
            if(n<0) throw std::runtime_error("Token decoding failed");
            answer.append(piece.data(),n); if(generated++==0) first=milliseconds(started);
            if(i+1<limit) {
                auto b=llama_batch_get_one(&token,1);
                if(llama_decode(ctx.get(),b)!=0) throw std::runtime_error("Token evaluation failed");
            }
        }
        check(); stats[0]=first; stats[1]=generated; stats[2]=milliseconds(started);
        stats[3]=generated*1000.0/std::max(1.0,milliseconds(decode_started));
        auto result=env->NewByteArray(answer.size());
        env->SetByteArrayRegion(result,0,answer.size(),reinterpret_cast<const jbyte *>(answer.data())); return result;
    } catch(const std::exception &e) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),e.what()); return nullptr;
    } catch(...) {env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),"Native inference failure");return nullptr;}
}
