package local.aicenter.platform;
import android.content.Context;
import android.content.SharedPreferences;
import java.net.URI;
/** User-selected HTTPS endpoints. Keys are ciphertext bound to slot identity. */
public final class ApiSettings {
 private final SharedPreferences preferences;private final SecretStore secrets;
 public ApiSettings(Context context,SecretStore secrets){preferences=context.getSharedPreferences("api_settings",Context.MODE_PRIVATE);this.secrets=secrets;}
 public void save(String slot,String endpoint,String model,String key){
  validateSlot(slot);endpoint=endpoint.trim();model=model.trim();
  if(endpoint.isEmpty()&&model.isEmpty()&&key.isEmpty()){preferences.edit().remove(slot+".endpoint").remove(slot+".model").remove(slot+".key").apply();return;}
  validateEndpoint(endpoint);if(model.isEmpty()||model.length()>200||key.length()>4096||key.contains("\n")||key.contains("\r"))throw new IllegalArgumentException("模型名或Key格式错误");
  if(key.isEmpty()&&!endpoint.equals(endpoint(slot)))throw new IllegalArgumentException("更换接口地址需重新输入Key");
  String encrypted=key.isEmpty()?preferences.getString(slot+".key",""):secrets.encrypt("api."+slot,key);
  if(encrypted.isEmpty())throw new IllegalArgumentException("请输入API Key");
  preferences.edit().putString(slot+".endpoint",endpoint).putString(slot+".model",model).putString(slot+".key",encrypted).apply();
 }
 public String endpoint(String slot){validateSlot(slot);return preferences.getString(slot+".endpoint","");}
 public String model(String slot){validateSlot(slot);return preferences.getString(slot+".model","");}
 public String key(String slot){validateSlot(slot);String value=preferences.getString(slot+".key","");return value.isEmpty()?"":secrets.decrypt("api."+slot,value);}
 public boolean ready(String slot){return !endpoint(slot).isEmpty()&&!model(slot).isEmpty()&&!preferences.getString(slot+".key","").isEmpty();}
 private static void validateSlot(String slot){if(!slot.equals("A")&&!slot.equals("B"))throw new IllegalArgumentException("未知API配置");}
 public static void validateEndpoint(String endpoint){
  try{URI uri=new URI(endpoint);if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)throw new IllegalArgumentException("仅支持无内嵌凭据、查询串或片段的HTTPS完整接口地址");}
  catch(java.net.URISyntaxException e){throw new IllegalArgumentException("接口地址格式错误");}
 }
}
