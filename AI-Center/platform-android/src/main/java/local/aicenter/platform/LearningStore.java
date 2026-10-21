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
 public synchronized String answer(String question){if(!enabled())return null;for(String[] row:corrections())if(row[0].equals(question.trim()))return LearningPolicy.CORRECTION_PREFIX+row[1];return null;}
 public synchronized void practice(boolean chinese,int typed,int errors){if(!enabled())return;double accuracy=LearningPolicy.accuracy(typed,errors);try{JSONObject data=load();String key=chinese?"zh":"en";JSONObject stats=data.optJSONObject(key);if(stats==null)stats=new JSONObject();int sessions=stats.optInt("sessions");stats.put("sessions",Math.min(1000000,sessions+1));stats.put("accuracy",accuracy);data.put(key,stats);save(data);}catch(Exception e){throw new IllegalStateException("练习学习记录保存失败",e);}}
 public synchronized String target(boolean chinese){JSONObject stats=enabled()?load().optJSONObject(chinese?"zh":"en"):null;return LearningPolicy.target(chinese,stats==null?0:stats.optInt("sessions"),stats==null?0:stats.optDouble("accuracy"));}
 public synchronized String summary(){JSONObject data=load();StringBuilder s=new StringBuilder(enabled()?"学习已开启":"学习已关闭");for(String key:new String[]{"zh","en"}){JSONObject v=data.optJSONObject(key);s.append("\n").append(key.equals("zh")?"中文":"英文").append("：").append(v==null?0:v.optInt("sessions")).append("次有效练习");}return s.append("\n纠错记录：").append(corrections().size()).append("条；仅精确匹配提问，不作为工具指令执行。\n练习满3次且最近一次过程正确率≥97%时增加句子难度，否则保持基础练习。关闭后不应用纠错或自动记录练习；可显式管理纠错。").toString();}
 public synchronized void courseResult(String id,int correct,int total){
  local.aicenter.core.CapabilityCourse.Lesson lesson=local.aicenter.core.CapabilityCourse.lesson(id);
  if(total!=lesson.questions.length||correct<0||correct>total)throw new IllegalArgumentException("无效课程成绩");
  if(!enabled())return;
  try{JSONObject data=load();JSONObject courses=data.optJSONObject("courses");if(courses==null)courses=new JSONObject();JSONObject old=courses.optJSONObject(id);JSONObject row=new JSONObject().put("attempts",Math.min(1000000,old==null?1:old.optInt("attempts")+1)).put("correct",correct).put("total",total).put("updated",System.currentTimeMillis());courses.put(id,row);data.put("courses",courses);save(data);}catch(Exception e){throw new IllegalStateException("课程成绩保存失败",e);}
 }
 public synchronized void courseAnswers(String id,int[] selections){
  local.aicenter.core.CapabilityCourse.Lesson lesson=local.aicenter.core.CapabilityCourse.lesson(id);int correct=local.aicenter.core.CapabilityCourse.grade(lesson,selections);if(!enabled())return;
  try{JSONObject data=load(),courses=data.optJSONObject("courses");if(courses==null)courses=new JSONObject();JSONObject old=courses.optJSONObject(id);long now=System.currentTimeMillis();courses.put(id,new JSONObject().put("attempts",Math.min(1000000,old==null?1:old.optInt("attempts")+1)).put("correct",correct).put("total",selections.length).put("updated",now));data.put("courses",courses);for(int i=0;i<selections.length;i++)updateReview(data,local.aicenter.core.CapabilityCourse.questionId(lesson,i),selections[i]==lesson.questions[i].answer,now);save(data);}catch(Exception e){throw new IllegalStateException("逐题成绩保存失败，原数据保留",e);}
 }
 private local.aicenter.core.ReviewSchedule.State reviewState(JSONObject row){return row==null?null:new local.aicenter.core.ReviewSchedule.State(row.optInt("attempts"),row.optInt("errors"),row.optInt("stage"),row.optLong("updated"),row.optLong("due"),row.optBoolean("correct"));}
 private void updateReview(JSONObject data,String id,boolean correct,long now)throws org.json.JSONException{
  JSONObject reviews=data.optJSONObject("reviews");if(reviews==null)reviews=new JSONObject();local.aicenter.core.ReviewSchedule.State next=local.aicenter.core.ReviewSchedule.answer(reviewState(reviews.optJSONObject(id)),correct,now);reviews.put(id,new JSONObject().put("attempts",next.attempts).put("errors",next.errors).put("stage",next.stage).put("updated",next.updated).put("due",next.due).put("correct",next.lastCorrect));data.put("reviews",reviews);
 }
 public synchronized local.aicenter.core.ReviewSchedule.State review(String id){local.aicenter.core.CapabilityCourse.question(id);JSONObject reviews=load().optJSONObject("reviews");return reviewState(reviews==null?null:reviews.optJSONObject(id));}
 public synchronized void reviewAnswer(String id,int choice){local.aicenter.core.CapabilityCourse.Question q=local.aicenter.core.CapabilityCourse.question(id);if(choice<0||choice>=q.options.length)throw new IllegalArgumentException("请先选择答案");if(!enabled())return;try{JSONObject data=load();updateReview(data,id,choice==q.answer,System.currentTimeMillis());save(data);}catch(Exception e){throw new IllegalStateException("复习保存失败，原数据保留",e);}}
 public synchronized List<String> reviewIds(boolean onlyWrong,boolean onlyDue){List<String> result=new ArrayList<>();long now=System.currentTimeMillis();for(local.aicenter.core.CapabilityCourse.Lesson lesson:local.aicenter.core.CapabilityCourse.LESSONS)for(int i=0;i<lesson.questions.length;i++){String id=local.aicenter.core.CapabilityCourse.questionId(lesson,i);local.aicenter.core.ReviewSchedule.State row=review(id);if(row!=null&&(!onlyWrong||!row.lastCorrect)&&(!onlyDue||local.aicenter.core.ReviewSchedule.due(row,now)))result.add(id);}result.sort((a,b)->Long.compare(review(a).due,review(b).due));return result;}
 public synchronized void clearReviews(){try{JSONObject data=load();data.remove("reviews");save(data);}catch(Exception e){throw new IllegalStateException("复习记录清除失败",e);}}
 public synchronized String courseSummary(String id){local.aicenter.core.CapabilityCourse.lesson(id);JSONObject courses=load().optJSONObject("courses"),row=courses==null?null:courses.optJSONObject(id);return row==null?"尚未测验":"已测"+row.optInt("attempts")+"次，最近"+row.optInt("correct")+"/"+row.optInt("total");}
 public synchronized void clear(){if(!prefs.edit().remove("records").commit())throw new IllegalStateException("清除学习记录失败");}
}
