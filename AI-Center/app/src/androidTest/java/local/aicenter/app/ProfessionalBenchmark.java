package local.aicenter.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Build;
import android.util.AtomicFile;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import local.aicenter.core.AgentRuntime;
import local.aicenter.core.StopController;
import org.json.*;

/** Synthetic CI fixtures only. Never packaged in the shipping application. */
final class ProfessionalBenchmark {
    private final Instrumentation host;
    private final Bundle options;
    private CenterApplication app;
    private final JSONArray results=new JSONArray();
    private JSONObject report;
    private String password;
    private volatile boolean unsafeNativeWorker;
    ProfessionalBenchmark(Instrumentation host,Bundle options){this.host=host;this.options=options;}
    void run(){
        Bundle end=new Bundle();
        try{
            if(!host.getTargetContext().getPackageName().equals("local.aicenter.app.dev") ||
               !(Build.FINGERPRINT.contains("generic")||Build.FINGERPRINT.contains("emulator")||Build.MODEL.startsWith("sdk_gphone")))
                throw new SecurityException("Benchmark is restricted to isolated emulator fixtures");
            app=(CenterApplication)host.getTargetContext().getApplicationContext();
            long deadline=SystemClock.elapsedRealtime()+180000;
            while(!app.initialized&&app.startupError==null&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(100);
            require(app.initialized&&app.localModel!=null&&app.localModel.ready(),"Verified model not ready");
            require(app.localModel.id().equals("Qwen3.5-2B-Q4_K_M"),"Wrong model");
            require(host.getTargetContext().checkSelfPermission("android.permission.INTERNET")!=0,"Network permission present");
            require(android.provider.Settings.Global.getInt(host.getTargetContext().getContentResolver(),"airplane_mode_on",0)==1,"Not offline");
            password=options.getString("fixture_password","");require(password.length()>=20,"Fixture credential missing");
            if(!app.admin.configured()){
                require("true".equals(options.getString("fresh_fixture")),"Refuse to create administrator outside a fresh fixture");
                app.admin.setup(password.toCharArray());
            }else require(!"true".equals(options.getString("fresh_fixture")),"Existing installation left unchanged");
            byte[] source;
            try(InputStream in=host.getContext().getAssets().open("professional-100.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);source=out.toByteArray();
            }
            JSONObject suite=new JSONObject(new String(source,StandardCharsets.UTF_8));JSONArray cases=suite.getJSONArray("cases");
            int start=Integer.parseInt(options.getString("case_start","0"));int count=Integer.parseInt(options.getString("case_count","5"));
            require(start>=0&&count>0&&count<=5&&start+count<=cases.length(),"Invalid batch range");
            StringBuilder digest=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(source))digest.append(String.format(Locale.ROOT,"%02x",b&255));
            JSONObject manifest;
            try(InputStream in=host.getTargetContext().getAssets().open("model-manifest.json")){
                ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                manifest=new JSONObject(out.toString("UTF-8"));
            }
            report=new JSONObject().put("suite",suite.getString("id")).put("suite_sha256",digest.toString())
                .put("commit",options.getString("commit", "UNKNOWN")).put("model",manifest)
                .put("device",new JSONObject().put("model",Build.MODEL).put("api",Build.VERSION.SDK_INT).put("abis",new JSONArray(Build.SUPPORTED_ABIS)))
                .put("engine_revision",options.getString("engine_revision","UNKNOWN")).put("offline",true).put("physical_devices","NOT_RUN").put("start",start).put("count",count)
                .put("started_utc_ms",System.currentTimeMillis()).put("status","RUNNING").put("results",results);
            save();
            for(int i=start;i<start+count;i++)execute(cases.getJSONObject(i));
            report.put("status","EXECUTED").put("finished_utc_ms",System.currentTimeMillis());save();
            end.putString("benchmark_status","EXECUTED");
        }catch(Throwable error){
            try{if(report!=null){report.put("status","ERROR").put("harness_error",error.getClass().getSimpleName());save();}}catch(Exception ignored){}
            end.putString("benchmark_status","ERROR");end.putString("benchmark_error",error.getClass().getSimpleName());
        }finally{password=null;}
        host.finish(Activity.RESULT_OK,end);
    }
    private void execute(JSONObject question)throws Exception{
        require(app.admin.unlock(password.toCharArray()),"Fixture authentication failed");
        app.stop.resume();
        long begin=SystemClock.elapsedRealtime();AtomicBoolean sampling=new AtomicBoolean(true);
        AtomicLong peak=new AtomicLong(Debug.getPss());
        Thread sampler=new Thread(()->{while(sampling.get()){peak.accumulateAndGet(Debug.getPss(),Math::max);SystemClock.sleep(250);}},"benchmark-pss");
        sampler.start();
        JSONObject item=new JSONObject().put("id",question.getString("id")).put("category",question.getString("category"))
            .put("difficulty",question.optString("difficulty","UNRATED"))
            .put("provenance",new JSONObject().put("commit",report.getString("commit")).put("model",report.getJSONObject("model")).put("device",report.getJSONObject("device")).put("engine_revision",report.getString("engine_revision")))
            .put("expected_rule",question)
            .put("input",question.getString("prompt")).put("rubric",question.getString("rubric"))
            .put("output","").put("status","RUNNING").put("crash",JSONObject.NULL).put("timeout",false).put("cancelled",false)
            .put("ttft_ms",JSONObject.NULL).put("tokens_per_second",JSONObject.NULL);
        results.put(item);save();
        FutureTask<String> work=new FutureTask<>(()->answer(question,item));Thread inference=new Thread(work,"benchmark-inference");inference.start();
        try{
            String answer=work.get(question.getLong("timeout_ms"),TimeUnit.MILLISECONDS);
            item.put("output",answer);
            double[] m=app.localModel.lastMetrics();
            item.put("ttft_ms",m[0]).put("generated_tokens",m[1]).put("native_total_ms",m[2]).put("tokens_per_second",m[3]);
            String kind=question.getString("kind");String status=grade(question,answer);
            if((kind.equals("manual")||kind.equals("formula")||kind.equals("number"))&&m[1]>=question.getInt("max_tokens")){
                status="FAIL";item.put("reason","generation_budget_exhausted; answer may be truncated");
            }
            item.put("status",status).put("crash",false);
            if(!item.has("reason"))item.put("reason",status.equals("FAIL")?"deterministic_rule_mismatch":status.equals("MANUAL_REVIEW")?"requires_human_rubric_review":"deterministic_rule_satisfied");
        }catch(TimeoutException error){app.disconnect();item.put("status","FAIL").put("cancelled",true).put("timeout",true).put("reason","question_timeout");}
        catch(ExecutionException error){item.put("status","FAIL").put("reason",error.getCause().getClass().getSimpleName()+": "+error.getCause().getMessage());}
        finally{
            sampling.set(false);sampler.join(2000);
            if(inference.isAlive()){app.disconnect();inference.join(10000);}
            item.put("elapsed_ms",SystemClock.elapsedRealtime()-begin).put("peak_pss_kb",peak.get()).put("pss_sample_interval_ms",250);
            save();
        }
        require(!unsafeNativeWorker&&!inference.isAlive(),"Native worker failed to stop; aborting batch without concurrent inference");
    }
    private String answer(JSONObject q,JSONObject item)throws Exception{
        String kind=q.getString("kind"),prompt=q.getString("prompt");
        StopController.Token token=app.stop.begin();
        if(kind.equals("tool")){
            String selected=app.planTool(prompt,token);item.put("selected_tool",selected);
            AgentRuntime.Result result=app.agent.execute(Collections.singletonList(new AgentRuntime.Step(selected,prompt)),token);
            item.put("tool_state",result.state.name());require(selected.equals(q.getString("expected")),"Wrong selected tool");
            require(result.state==AgentRuntime.State.SUCCEEDED,"Selected tool failed");return result.output;
        }
        if(kind.equals("cancel")){
            AtomicReference<Throwable> outcome=new AtomicReference<>();
            Thread t=new Thread(()->{try{app.localModel.generate(prompt,"",1024,token);}catch(Throwable error){outcome.set(error);}});
            t.start();SystemClock.sleep(500);long at=SystemClock.elapsedRealtime();app.disconnect();t.join(10000);
            item.put("cancel_latency_ms",SystemClock.elapsedRealtime()-at).put("cancelled",true);
            unsafeNativeWorker=t.isAlive();
            require(!t.isAlive()&&outcome.get() instanceof StopController.Stopped&&!token.valid(),"Cancel failed");
            app.stop.resume();return app.localModel.generate("3加4等于多少？仅输出数字。","",16,app.stop.begin());
        }
        if(kind.equals("conversation")){
            app.db.message("user",prompt);
            AgentRuntime.Result first=app.agent.execute(Collections.singletonList(new AgentRuntime.Step("chat",prompt)),token);
            require(first.state==AgentRuntime.State.SUCCEEDED,"First conversation turn failed");
            app.db.message("assistant",first.output);item.put("first_output",first.output);
            String next=q.getString("followup");app.db.message("user",next);item.put("followup",next);
            AgentRuntime.Result second=app.agent.execute(Collections.singletonList(new AgentRuntime.Step("chat",next)),app.stop.begin());
            require(second.state==AgentRuntime.State.SUCCEEDED,"Second conversation turn failed");
            app.db.message("assistant",second.output);return second.output;
        }
        return app.localModel.generate(prompt,"",q.getInt("max_tokens"),token);
    }
    private static String grade(JSONObject q,String answer)throws Exception{
        String kind=q.getString("kind");String s=answer.trim();
        if(kind.equals("manual"))return "MANUAL_REVIEW";
        if(kind.equals("tool"))return s.isEmpty()?"FAIL":"PASS";
        if(kind.equals("conversation"))return s.contains("K73")&&s.matches("(?s).*\\b42\\b.*")?"PASS":"FAIL";
        if(kind.equals("cancel"))return s.equals("7")?"PASS":"FAIL";
        if(kind.equals("number")){
            if(!s.matches("[+-]?[0-9]+(?:\\.[0-9]+)?"))return "FAIL";
            return Math.abs(Double.parseDouble(s)-q.getDouble("expected"))<=q.getDouble("tolerance")?"PASS":"FAIL";
        }
        if(kind.equals("formula")){
            // Whitespace and letter case outside string literals are cosmetic only.
            JSONArray accepted=q.getJSONArray("expected");for(int i=0;i<accepted.length();i++)if(formula(s).equals(formula(accepted.getString(i))))return "PASS";
            return "FAIL";
        }
        throw new IllegalArgumentException("Unknown grading contract");
    }
    private static String formula(String value){
        StringBuilder b=new StringBuilder();boolean quoted=false;
        for(char c:value.toCharArray()){if(c=='"')quoted=!quoted;if(quoted||!Character.isWhitespace(c))b.append(quoted?c:Character.toUpperCase(c));}
        return b.toString();
    }
    private void save()throws Exception{
        File file=new File(host.getTargetContext().getFilesDir(),"ci-professional-benchmark.json");
        AtomicFile journal=new AtomicFile(file);
        FileOutputStream out=journal.startWrite();
        try{out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));journal.finishWrite(out);}
        catch(Exception error){journal.failWrite(out);throw error;}
    }
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalStateException(reason);}
}
