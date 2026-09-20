package local.aicenter.platform;

import android.content.Context;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import local.aicenter.core.ModelEngine;
import local.aicenter.core.StopController;
import org.json.JSONObject;

/** Real in-process llama.cpp inference. Bundled weights are verified before use. */
public final class LocalModelEngine implements ModelEngine {
    private static final Object LOCK=new Object();
    private static long sequence;
    static { System.loadLibrary("aicenter"); }
    private final Context context;
    private final StopController stop;
    private volatile File file;
    private volatile String modelId="model-initializing";
    private volatile double[] metrics=new double[4];
    public LocalModelEngine(Context context,StopController stop){this.context=context.getApplicationContext();this.stop=stop;}
    public void installBundled()throws Exception{
        JSONObject spec;
        try(InputStream in=context.getAssets().open("model-manifest.json")){
            spec=new JSONObject(new String(readManifest(in),StandardCharsets.UTF_8));
        }
        String expected=spec.getString("sha256");long size=spec.getLong("bytes");
        if(!expected.matches("[a-f0-9]{64}")||size<=0||size>8L*1024*1024*1024)throw new IOException("Invalid model manifest");
        File dir=new File(context.getFilesDir(),"models");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Model directory unavailable");
        File target=new File(dir,expected+".gguf");
        if(!target.isFile()||target.length()!=size||!hash(target).equals(expected)){
            if(dir.getUsableSpace()<size+256L*1024*1024)throw new IOException("Insufficient model storage");
            File part=File.createTempFile("model-",".part",dir);
            try{
                try(InputStream in=context.getAssets().open("base.gguf");FileOutputStream out=new FileOutputStream(part)){
                    byte[] buf=new byte[1024*1024];int n;long total=0;
                    while((n=in.read(buf))!=-1){total+=n;if(total>size)throw new IOException("Model too large");out.write(buf,0,n);}
                    out.getFD().sync();
                }
                if(part.length()!=size||!hash(part).equals(expected))throw new IOException("Model verification failed");
                Files.move(part.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }finally{Files.deleteIfExists(part.toPath());}
        }
        modelId=spec.getString("id");file=target;
    }
    private static byte[] readManifest(InputStream in)throws IOException{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[1024];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>16384)throw new IOException("Manifest too large");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    private static String hash(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new FileInputStream(file)){byte[] b=new byte[1024*1024];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
        StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
    }
    public String id(){return modelId;}
    public boolean isLocal(){return true;}
    public boolean isPaid(){return false;}
    public boolean ready(){return file!=null;}
    public double[] lastMetrics(){return metrics.clone();}
    public String generate(String prompt,StopController.Token token)throws Exception{return generate(prompt,"",384,token);}
    public String generate(String prompt,String grammar,int maxTokens,StopController.Token token)throws Exception{
        synchronized(LOCK){
            token.check();if(!ready())throw new IllegalStateException("Local model is not ready");
            long request=++sequence;nativePrepare(request);
            try(AutoCloseable hook=stop.onStop(token,()->nativeCancel(request))){
                token.check();
                byte[] result=nativeGenerate(file.getAbsolutePath().getBytes(StandardCharsets.UTF_8),prompt.getBytes(StandardCharsets.UTF_8),
                    grammar.getBytes(StandardCharsets.UTF_8),maxTokens,Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors())));
                token.check();metrics=nativeStats();
                String text=new String(result,StandardCharsets.UTF_8).trim();
                if(text.isEmpty())throw new IllegalStateException("The model returned no answer");return text;
            }catch(Exception e){token.check();throw e;}
        }
    }
    private static native void nativePrepare(long request);
    private static native void nativeCancel(long request);
    private static native byte[] nativeGenerate(byte[] path,byte[] prompt,byte[] grammar,int maxTokens,int threads);
    private static native double[] nativeStats();
}
