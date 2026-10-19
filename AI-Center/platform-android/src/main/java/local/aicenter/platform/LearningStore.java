package local.aicenter.platform;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import local.aicenter.core.LearningPolicy;

/** Local explicit corrections and measured practice. Never uploads or trains weights. */
public final class LearningStore {
 private final SharedPreferences prefs;private final SecretStore secret;
 public LearningStore(Context context,SecretStore secret){prefs=context.getSharedPreferences("learning",Context.MODE_PRIVATE);this.secret=secret;}
 public synchronized boolean enabled(){return prefs.getBoolean("enabled",true);}
 public synchronized void enabled(boolean value){if(!prefs.edit().putBoolean("enabled",value).commit())throw new IllegalStateException("学习设置保存失败");}
 private JSONObject load(){String v=prefs.getString("records",null);try{return v==null?new JSONObject():new JSONObject(secret.decrypt("learning:records",v));}catch(Exception e){throw new IllegalStateException("学习记录无法读取，原数据保留",e);}}
 private void save(JSONObject data){if(!prefs.edit().putString("records",secret.encrypt("learning:records",data.toString())).commit())throw new IllegalStateException("学习记录保存失败");}
 public synchronized List<String[]> corrections(){try{JSONArray rows=load().optJSONArray("corrections");List<String[]> result=new ArrayList<>();if(rows!=null)for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);result.add(new String[]{row.getString("question"),row.getString("answer")});}return result;}catch(Exception e){throw new IllegalStateException("纠错记录读取失败",e);}}
 public synchronized void correct(String question,String answer){question=question.trim();answer=answer.trim();if(question.isEmpty()||answer.isEmpty()||question.length()>500||answer.length()>2000)throw new IllegalArgumentException("问题1–500字，确认答案1–2000字");try{JSONObject data=load();JSONArray next=new JSONArray();for(String[] row:corrections())if(!row[0].equals(question))next.put(new JSONObject().put("question",row[0]).put("answer",row[1]));if(next.length()>=50)throw new IllegalArgumentException("最多50条，请先删除旧记录");next.put(new JSONObject().put("question",question).put("answer",answer));data.put("corrections",next);save(data);}catch(IllegalArgumentException e){throw e;}catch(Exception e){throw new IllegalStateException("纠错记录保存失败",e);}}
 public synchronized void remove(String question){try{JSONObject data=load();JSONArray next=new JSONArray();for(String[] row:corrections())if(!row[0].equals(question))next.put(new JSONObject().put("question",row[0]).put("answer",row[1]));data.put("corrections",next);save(data);}catch(Exception e){throw new IllegalStateException("删除失败，原记录保留",e);}}
 public synchronized String answer(String question){if(!enabled())return null;for(String[] row:corrections())if(row[0].equals(question.trim()))return "[你确认的纠错记录；未独立核实]\n"+row[1];return null;}
 public synchronized void practice(boolean chinese,int typed,int errors){if(!enabled())return;double accuracy=LearningPolicy.accuracy(typed,errors);try{JSONObject data=load();String key=chinese?"zh":"en";JSONObject stats=data.optJSONObject(key);if(stats==null)stats=new JSONObject();int sessions=stats.optInt("sessions");stats.put("sessions",Math.min(1000000,sessions+1));stats.put("accuracy",accuracy);data.put(key,stats);save(data);}catch(Exception e){throw new IllegalStateException("练习学习记录保存失败",e);}}
 public synchronized String target(boolean chinese){JSONObject stats=enabled()?load().optJSONObject(chinese?"zh":"en"):null;return LearningPolicy.target(chinese,stats==null?0:stats.optInt("sessions"),stats==null?0:stats.optDouble("accuracy"));}
 public synchronized String summary(){JSONObject data=load();StringBuilder s=new StringBuilder(enabled()?"学习已开启":"学习已关闭");for(String key:new String[]{"zh","en"}){JSONObject v=data.optJSONObject(key);s.append("\n").append(key.equals("zh")?"中文":"英文").append("：").append(v==null?0:v.optInt("sessions")).append("次有效练习");}return s.append("\n纠错记录：").append(corrections().size()).append("条；仅精确匹配提问，不作为工具指令执行。\n练习满3次且最近一次过程正确率≥97%时增加句子难度，否则保持基础练习。关闭后不应用或新增学习记录。").toString();}
 public synchronized void clear(){if(!prefs.edit().remove("records").commit())throw new IllegalStateException("清除学习记录失败");}
}
