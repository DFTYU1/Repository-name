package local.aicenter.platform;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import javax.net.ssl.HttpsURLConnection;
import org.json.*;
import local.aicenter.core.*;
/** Opt-in chat-completions protocol adapter. No redirects, cookies, retries or key logging. */
public final class HttpModelEngine implements ModelEngine {
 private final ApiSettings settings;private final String slot;private final StopController stop;
 public HttpModelEngine(ApiSettings settings,String slot,StopController stop){this.settings=settings;this.slot=slot;this.stop=stop;}
 public String id(){return "API "+slot;}
 public boolean isLocal(){return false;}
 public boolean isPaid(){return true;} // Cost is unknown: always require explicit cost consent.
 public boolean ready(){return settings.ready(slot);}
 public String generate(String prompt,StopController.Token token)throws Exception{
  token.check();String endpoint=settings.endpoint(slot);ApiSettings.validateEndpoint(endpoint);
  HttpsURLConnection connection=(HttpsURLConnection)new URL(endpoint).openConnection();
  connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(20000);connection.setRequestMethod("POST");connection.setDoOutput(true);
  connection.setRequestProperty("Content-Type","application/json; charset=utf-8");connection.setRequestProperty("Authorization","Bearer "+settings.key(slot));
  JSONObject payload=new JSONObject().put("model",settings.model(slot)).put("stream",false).put("max_tokens",512)
   .put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",prompt)));
  byte[] body=payload.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(body.length);
  ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
  ScheduledFuture<?> deadline=timer.schedule(connection::disconnect,30,TimeUnit.SECONDS);
  try(AutoCloseable hook=stop.onStop(token,connection::disconnect)){
   token.check();try(OutputStream output=connection.getOutputStream()){output.write(body);}token.check();
   int status=connection.getResponseCode();if(status<200||status>=300)throw new IOException("API请求未成功，HTTP "+status);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   try(InputStream input=connection.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){token.check();if(bytes.size()+n>1024*1024)throw new IOException("API响应超出限制");bytes.write(buffer,0,n);}}
   token.check();String text=new JSONObject(bytes.toString(StandardCharsets.UTF_8.name())).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
   if(text.isEmpty())throw new IOException("API返回空内容");return text;
  }catch(Exception e){token.check();throw new IOException("API连接或响应失败");}
  finally{deadline.cancel(false);timer.shutdownNow();connection.disconnect();}
 }
}
