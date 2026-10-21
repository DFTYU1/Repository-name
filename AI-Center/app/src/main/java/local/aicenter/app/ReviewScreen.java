package local.aicenter.app;
import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import local.aicenter.core.CapabilityCourse;
import local.aicenter.core.ReviewSchedule;

/** Local Cpk review queue with transparent rule-based scheduling; no generated grading. */
final class ReviewScreen extends LinearLayout {
 private final Activity activity;private final CenterApplication app;private final Runnable login;private final LinearLayout body;private boolean wrongOnly=false,dueOnly=false;
 ReviewScreen(Activity a,CenterApplication app,Runnable courses,Runnable home,Runnable login){super(a);activity=a;this.app=app;this.login=login;setOrientation(VERTICAL);LinearLayout bar=new LinearLayout(a);bar.addView(button("立即断开AI",()->{app.disconnect();home.run();}),new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));bar.addView(button("返回课程目录",courses),new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));addView(bar);body=new LinearLayout(a);body.setOrientation(VERTICAL);body.setPadding(dp(18),dp(12),dp(18),dp(18));ScrollView scroll=new ScrollView(a);scroll.addView(body);addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,0,1));list();}
 private boolean allowed(){if(!app.admin.unlocked()){login.run();return false;}if(!app.stop.enabled())return false;return true;}
 private void list(){body.removeAllViews();body.addView(label("Cpk错题与复习",24));body.addView(label("仅覆盖现有3节入门课的6题。按每题作答结果安排：错误10分钟后；到期答对依次1/3/7/14/30/60天。提前重复答对不推迟到期时间、不提升阶段。阶段不是掌握度证明，不是模型训练。",15));if(!app.learning.enabled())body.addView(label("学习已关闭：可查看和练习，作答不保存、不调整安排。",15));
  CheckBox wrong=new CheckBox(activity);wrong.setText("只看最近答错");wrong.setChecked(wrongOnly);body.addView(wrong);CheckBox due=new CheckBox(activity);due.setText("只看到期题目");due.setChecked(dueOnly);body.addView(due);body.addView(button("应用复习筛选",()->{if(!allowed())return;wrongOnly=wrong.isChecked();dueOnly=due.isChecked();list();}));
  try{List<String> ids=app.learning.reviewIds(wrongOnly,dueOnly);body.addView(label("当前列表："+ids.size()+"题",17));if(ids.isEmpty())body.addView(label("没有符合筛选的记录；先完成课程测验，也可取消筛选查看其他记录。",15));for(String id:ids){ReviewSchedule.State s=app.learning.review(id);body.addView(button(CapabilityCourse.question(id).text,()->{if(allowed())question(id);}));body.addView(label("作答"+s.attempts+"次；累计错误"+s.errors+"次；最近"+(s.lastCorrect?"答对":"答错")+"；阶段"+s.stage+"\n下次："+DateFormat.getDateTimeInstance().format(new Date(s.due))+(ReviewSchedule.due(s,System.currentTimeMillis())?"（已到期）":""),14));}}catch(Exception e){body.addView(label("复习记录读取失败，原数据保留。",16));}
  body.addView(button("清除本模块复习记录",()->{if(!app.admin.unlocked()){login.run();return;}new AlertDialog.Builder(activity).setTitle("清除本模块复习记录？").setMessage("删除逐题错题和复习安排，不能撤销；课程汇总成绩、纠错和打字记录保留。").setNegativeButton("取消",null).setPositiveButton("清除",(d,w)->{if(!app.admin.unlocked())return;try{app.learning.clearReviews();list();}catch(Exception e){new AlertDialog.Builder(activity).setMessage("清除未完成，原数据保留。").setPositiveButton("关闭",null).show();}}).show();}));
 }
 private void question(String id){body.removeAllViews();CapabilityCourse.Question q=CapabilityCourse.question(id);body.addView(label("逐题复习",24));body.addView(label(q.text,19));RadioGroup choices=new RadioGroup(activity);for(String value:q.options){RadioButton b=new RadioButton(activity);b.setId(View.generateViewId());b.setText(value);choices.addView(b);}body.addView(choices);TextView feedback=label("选择后提交；同一页面每次只记录一次。",16);body.addView(feedback);final boolean[] answered={false};Button submit=button("提交复习答案",()->{});submit.setOnClickListener(v->{if(answered[0])return;if(!allowed()){feedback.setText("AI已断开或未解锁，未记录。");return;}int selected=choices.indexOfChild(choices.findViewById(choices.getCheckedRadioButtonId()));if(selected<0){feedback.setText("请先选择答案");return;}try{app.learning.reviewAnswer(id,selected);answered[0]=true;feedback.setText((selected==q.answer?"答对":"答错")+"\n正确答案："+q.options[q.answer]+"\n解释："+q.explanation+(app.learning.enabled()?"\n复习记录已加密保存。":"\n学习已关闭，本次不保存。"));submit.setEnabled(false);for(int i=0;i<choices.getChildCount();i++)choices.getChildAt(i).setEnabled(false);}catch(Exception e){feedback.setText("作答记录保存失败，请重试；原数据保留。");}});body.addView(submit);body.addView(label("参考："+CapabilityCourse.questionLesson(id).source,13));body.addView(button("返回复习列表",()->{if(allowed())list();}));}
 private TextView label(String text,int size){TextView v=new TextView(activity);v.setText(text);v.setTextSize(size);v.setTextColor(0xff1e302c);v.setPadding(0,dp(5),0,dp(5));return v;}
 private Button button(String text,Runnable r){Button b=new Button(activity);b.setText(text);b.setAllCaps(false);b.setOnClickListener(v->r.run());return b;}
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
