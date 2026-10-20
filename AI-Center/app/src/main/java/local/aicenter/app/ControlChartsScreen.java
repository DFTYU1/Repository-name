package local.aicenter.app;
import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import local.aicenter.core.ControlCharts;
import local.aicenter.core.AgentRuntime;

/** Six further chart families; fixed stop/home controls and no model-generated numerical answers. */
final class ControlChartsScreen extends LinearLayout {
 private final Activity activity;private final CenterApplication app;private final Runnable login;
 private static final String[] NAMES={"X̄-R（均值/极差）","X̄-S（均值/标准差）","p（不合格件比例）","np（不合格件数）","c（固定机会量缺陷数）","u（单位机会缺陷数）"};
 static final String[] EXAMPLES={"1,3\n3,5\n50,52","1,2,3\n2,3,4\n40,41,42","1,100\n2,100\n40,100","1,100\n2,100\n40,100","1\n2\n30","1,100\n2,200\n30,50"};
 ControlChartsScreen(Activity activity,CenterApplication app,Runnable home,Runnable login){super(activity);this.activity=activity;this.app=app;this.login=login;setOrientation(VERTICAL);setBackgroundColor(0xfff4f6f3);
  LinearLayout bar=new LinearLayout(activity);bar.setPadding(dp(8),dp(5),dp(8),dp(5));bar.addView(button("立即断开AI",()->{app.disconnect();home.run();}),new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));bar.addView(button("返回AI首页",home),new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));addView(bar);
  LinearLayout body=new LinearLayout(activity);body.setOrientation(VERTICAL);body.setPadding(dp(20),dp(10),dp(20),dp(20));body.addView(label("SPC子组与计数图",24));body.addView(label("按采样时间排列各组；每行或分号一组，组内逗号/Tab分隔。X̄图输入每组测量值；p/np输入不合格件数、样本量；c输入固定机会量的缺陷数；u输入缺陷数、机会量。缺陷数可多于件数，不要混用。",15));
  Spinner kind=new Spinner(activity);kind.setAdapter(new ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,NAMES));body.addView(kind);
  EditText data=new EditText(activity);data.setHint("按所选图种输入分组数据");data.setMinLines(5);data.setTextSize(16);data.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);body.addView(data);
  CheckBox confirmed=new CheckBox(activity);confirmed.setText("已核对时间顺序、合理分组、同单位及所选图种的适用条件");body.addView(confirmed);TextView result=label("等待数据；本工具不自动判断过程稳定。",15);body.addView(result);LinearLayout plots=new LinearLayout(activity);plots.setOrientation(VERTICAL);body.addView(plots);
  kind.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){confirmed.setChecked(false);plots.removeAllViews();result.setText("已切换图种，请重新核对输入格式与条件。");}});
  body.addView(button("填入所选SPC示例",()->{if(!allowed())return;data.setText(EXAMPLES[kind.getSelectedItemPosition()]);confirmed.setChecked(false);plots.removeAllViews();result.setText("示例用于演示计算与越界，不是有效稳定基线。");}));
  body.addView(button("计算并绘制SPC",()->{
   if(!allowed())return;if(!app.stop.enabled()){result.setText("AI已断开，请回首页恢复后再计算。");return;}String id=java.util.UUID.randomUUID().toString();
   try{ControlCharts.Result calculated=ControlCharts.calculate(ControlCharts.KINDS[kind.getSelectedItemPosition()],data.getText().toString(),confirmed.isChecked());result.setText(calculated.report());plots.removeAllViews();plots.addView(new ControlChartsView(activity,calculated),new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(240*calculated.series.size())));app.db.record(id,AgentRuntime.State.SUCCEEDED,"spc.charts","computed");}
   catch(IllegalArgumentException e){result.setText(e.getMessage());plots.removeAllViews();}
   catch(Exception e){result.setText("计算记录未完成，请检查本地存储；原文件未修改。");plots.removeAllViews();}
  }));body.addView(label("仅单点3σ近似判异；样本不足、相关性、低计数等可能使结果不适用。p/u允许变化样本量/机会量，np必须固定样本量，c须固定检查机会。\n依据：NIST工程统计手册\nhttps://www.itl.nist.gov/div898/handbook/pmc/section3/pmc321.htm\nhttps://www.itl.nist.gov/div898/handbook/pmc/section3/pmc331.htm\nhttps://www.itl.nist.gov/div898/handbook/pmc/section3/pmc332.htm",13));ScrollView scroll=new ScrollView(activity);scroll.addView(body);addView(scroll,new LayoutParams(LayoutParams.MATCH_PARENT,0,1));
 }
 private boolean allowed(){if(!app.admin.unlocked()){login.run();return false;}return true;}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView label(String text,int sp){TextView v=new TextView(activity);v.setText(text);v.setTextSize(sp);v.setTextColor(0xff1e302c);v.setPadding(0,dp(5),0,dp(5));if(sp>=24)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
 private Button button(String text,Runnable action){Button b=new Button(activity);b.setText(text);b.setAllCaps(false);b.setTextColor(0xff1e302c);b.setMinHeight(dp(48));GradientDrawable bg=new GradientDrawable();bg.setColor(0xffe2eee8);bg.setCornerRadius(dp(12));b.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);p.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(p);b.setOnClickListener(v->action.run());return b;}
}
