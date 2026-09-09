package local.aicenter.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import local.aicenter.core.AgentRuntime;
import local.aicenter.core.StopController;
import local.aicenter.platform.StorageManager;

/** Native, adaptive foundation screen. Unimplemented capabilities are described honestly. */
public final class MainActivity extends Activity {
    private static final int PICK_TEXT=41;
    private static final int BG=0xfff4f6f3, INK=0xff1e302c, MUTED=0xff61716c, ACCENT=0xff246f61;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private CenterApplication app;
    private LinearLayout conversation;
    private EditText input;
    private TextView activityStatus;
    private ScrollView chatScroll;
    private boolean selectingFile;
    private boolean foreground;
    private CancellationSignal biometricSignal;
    private String draft="";
    private final Runnable statusTick=new Runnable(){
        public void run(){
            if(isFinishing()||isDestroyed())return;
            if(foreground&&app.initialized&&activityStatus!=null&&!app.admin.unlocked()&&!selectingFile){app.disconnect();showLogin();}
            if(activityStatus!=null)activityStatus.setText(app.busy.get()?"正在处理…":app.stop.enabled()?"本地资料就绪 · 模型待接入":"AI 已断开");
            handler.postDelayed(this,500);
        }
    };
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);app=(CenterApplication)getApplication();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if(saved!=null){draft=saved.getString("draft","");selectingFile=saved.getBoolean("selecting_file",false);}
        showLoading();waitForInitialization();
    }
    private void waitForInitialization(){
        if(isFinishing()||isDestroyed())return;
        if(app.startupError!=null){showFatal(app.startupError);return;}
        if(!app.initialized){handler.postDelayed(this::waitForInitialization,100);return;}
        if(app.admin.unlocked())showHome();else showLogin();
    }
    @Override protected void onResume(){
        super.onResume();foreground=true;handler.removeCallbacks(statusTick);handler.post(statusTick);
        if(app!=null&&app.initialized&&!selectingFile&&!app.admin.unlocked())showLogin();
    }
    @Override protected void onStop(){
        super.onStop();foreground=false;handler.removeCallbacks(statusTick);
        if(!isChangingConfigurations()&&!selectingFile){
            if(biometricSignal!=null)biometricSignal.cancel();
            app.disconnect();app.admin.lock();
        }
    }
    @Override protected void onDestroy(){super.onDestroy();handler.removeCallbacksAndMessages(null);}
    @Override protected void onSaveInstanceState(Bundle out){
        if(input!=null)out.putString("draft",input.getText().toString());
        out.putBoolean("selecting_file",selectingFile);super.onSaveInstanceState(out);
    }
    private void showLoading(){LinearLayout root=column();root.setGravity(Gravity.CENTER);root.addView(label("正在打开本地资料…",20,INK));setContentView(root);}
    private void showFatal(String message){LinearLayout root=column();root.setPadding(dp(24),dp(48),dp(24),dp(24));root.addView(label(message,18,INK));setContentView(root);}
    private void showLogin(){
        input=null;activityStatus=null;conversation=null;chatScroll=null;
        LinearLayout root=column();root.setGravity(Gravity.CENTER);root.setPadding(dp(28),dp(32),dp(28),dp(32));
        LinearLayout card=column();card.setPadding(dp(24),dp(24),dp(24),dp(24));card.setBackground(round(Color.WHITE,24));
        boolean setup=!app.admin.configured();
        card.addView(label(setup?"建立你的本地中枢":"欢迎回来",28,INK));
        card.addView(label(setup?"唯一管理员：你。设置至少8位密码，数据保存在本机。":"请输入管理员密码。",15,MUTED));
        EditText password=field("管理员密码");password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);card.addView(password);
        EditText repeat=field("再次输入密码");repeat.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);if(setup)card.addView(repeat);
        TextView feedback=label("",14,MUTED);card.addView(feedback);
        Button enter=button(setup?"建立管理员":"解锁",true,()->{});
        enter.setOnClickListener(v->{
            char[] value=password.getText().toString().toCharArray();
            char[] confirmed=repeat.getText().toString().toCharArray();
            if(setup&&(value.length<8||!Arrays.equals(value,confirmed))){Arrays.fill(value,'\0');Arrays.fill(confirmed,'\0');feedback.setText("密码至少8位，两次输入需一致。");return;}
            Arrays.fill(confirmed,'\0');password.setText("");repeat.setText("");enter.setEnabled(false);feedback.setText("正在安全验证…");
            app.worker.execute(()->{
                String error=null;
                try{if(setup)app.admin.setup(value);else if(!app.admin.unlock(value))error="密码不正确。";}
                catch(Exception e){error="暂时无法解锁；若连续输错，请稍后再试。已有数据保留。";}
                final String result=error;
                runOnUiThread(()->{if(!foreground||isFinishing()||isDestroyed()){app.admin.lock();app.disconnect();return;}enter.setEnabled(true);if(result==null){app.stop.resume();showHome();}else feedback.setText(result);});
            });
        });card.addView(enter);
        if(!setup&&app.admin.biometricEnabled())card.addView(button("使用已启用的生物识别",false,this::biometricUnlock));
        LinearLayout.LayoutParams width=new LinearLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels-dp(56),dp(500)),ViewGroup.LayoutParams.WRAP_CONTENT);
        root.addView(card,width);setContentView(root);insets(root);
    }
    private void showHome(){
        if(!app.admin.unlocked()){showLogin();return;}
        LinearLayout root=column();root.setPadding(dp(18),dp(8),dp(18),dp(12));
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading=column();heading.addView(label("个人 AI 中枢",25,INK));
        activityStatus=label("本地资料就绪 · 模型待接入",13,MUTED);heading.addView(activityStatus);
        header.addView(heading,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        header.addView(button("立即断开AI",false,()->{app.disconnect();append("assistant","AI 已断开，当前任务和临时文件访问已停止。");}));root.addView(header);
        LinearLayout body=row();boolean wide=getResources().getConfiguration().screenWidthDp>=840;
        if(wide){
            LinearLayout sidebar=column();sidebar.setPadding(0,dp(24),dp(20),0);
            sidebar.addView(label("我的空间",15,MUTED));
            sidebar.addView(button("导入资料",true,this::selectText));
            sidebar.addView(button("本地文件",false,()->submit("查看文件")));
            sidebar.addView(button("任务记录",false,()->submit("查看任务")));
            sidebar.addView(button("存储与管理",false,this::showManagement));
            sidebar.addView(label("离线优先\n原文件保留\n访问可随时撤销",14,MUTED));
            body.addView(sidebar,new LinearLayout.LayoutParams(dp(200),ViewGroup.LayoutParams.MATCH_PARENT));
        }
        LinearLayout center=column();chatScroll=new ScrollView(this);chatScroll.setFillViewport(true);
        conversation=column();conversation.setPadding(0,dp(16),0,dp(16));chatScroll.addView(conversation);
        center.addView(chatScroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout composer=row();composer.setGravity(Gravity.CENTER_VERTICAL);
        input=field("输入目标，或“查找：扭矩”");input.setMaxLines(4);input.setText(draft);draft="";
        composer.addView(input,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        composer.addView(button("发送",true,()->{String text=input.getText().toString().trim();if(!text.isEmpty()){input.setText("");submit(text);}}));center.addView(composer);
        if(!wide){LinearLayout actions=row();actions.addView(button("导入资料",false,this::selectText));actions.addView(button("本地文件",false,()->submit("查看文件")));actions.addView(button("管理",false,this::showManagement));center.addView(actions);}
        body.addView(center,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));root.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);insets(root);
        app.worker.execute(()->{
            try{List<String[]> messages=app.db.recentMessages();runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||conversation==null)return;
                conversation.removeAllViews();
                if(messages.isEmpty())append("assistant","这是开发中的基础版本，可导入 TXT / Markdown、检索资料、查看文件、存储及任务记录。\n\n本地模型尚未接入，AI 对话暂未开放。");
                else for(String[] message:messages)append(message[0],message[1]);
            });}catch(Exception e){runOnUiThread(()->showFatal("历史记录暂时无法读取，原数据保留。"));}
        });
    }
    private void submit(String text){
        if(!app.admin.unlocked()){showLogin();return;}
        if(text.length()>2000){showInfo("当前问题最多2000个字符。");return;}
        if(!app.stop.enabled()){
            new AlertDialog.Builder(this).setTitle("恢复 AI 工作？").setMessage("会开始一个新任务。之前撤销的文件授权仍然无效。")
                .setPositiveButton("恢复并继续",(d,w)->{app.stop.resume();submit(text);}).setNegativeButton("取消",null).show();return;
        }
        if(!app.busy.compareAndSet(false,true)){showInfo("当前任务还在处理，可等待或点击“立即断开AI”。");return;}
        StopController.Token token=app.stop.begin();append("user",text);
        app.worker.execute(()->{
            String answer;
            String stage="保存输入";
            try{
                app.admin.require();token.check();app.db.message("user",text);
                String tool="chat",arg=text;
                if(text.equals("查看文件"))tool="files";
                else if(text.equals("查看空间")||text.equals("查看存储"))tool="storage";
                else if(text.equals("查看任务"))tool="tasks";
                else if(text.startsWith("查找：")||text.startsWith("查找:")){tool="search";arg=text.substring(3).trim();}
                stage="执行任务";answer=app.agent.execute(Collections.singletonList(new AgentRuntime.Step(tool,arg)),token).output;
                stage="保存结果";token.check();app.db.message("assistant",answer);
            }catch(StopController.Stopped e){answer="任务已停止，已有内容保留。";}
            catch(Exception e){answer="“"+stage+"”这一步未完成。请检查管理员是否已解锁及存储空间；已有文件保留。";}
            finally{app.busy.set(false);}
            final String result=answer;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())append("assistant",result);});
        });
    }
    private void selectText(){
        if(!app.admin.unlocked()){showLogin();return;}
        if(app.busy.get()){showInfo("请等待当前任务结束，或先断开AI。");return;}
        if(!app.stop.enabled()){showInfo("请先发送目标并确认恢复AI，再导入文件。");return;}
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("text/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);selectingFile=true;
        try{startActivityForResult(intent,PICK_TEXT);}catch(Exception e){selectingFile=false;showInfo("系统文件选择器暂时不可用。");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=PICK_TEXT)return;selectingFile=false;
        if(result!=RESULT_OK||data==null||data.getData()==null)return;
        if(!app.admin.unlocked()){showLogin();return;}
        if(!app.stop.enabled()||!app.busy.compareAndSet(false,true))return;
        Uri uri=data.getData();StopController.Token token=app.stop.begin();
        app.worker.execute(()->{
            String answer;
            try{app.admin.require();answer=app.importer.importText(uri,token);token.check();app.db.message("assistant",answer);}
            catch(StopController.Stopped e){answer="导入已停止；原文件未修改。";}
            catch(Exception e){answer="导入或索引未完成。请确认文件为 UTF-8 TXT / Markdown、大小不超过2MB，并检查存储空间。原文件未修改；已完成的副本保留。";}
            finally{app.busy.set(false);}
            final String reply=answer;runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())append("assistant",reply);});
        });
    }
    private void showManagement(){
        if(!app.admin.unlocked()){showLogin();return;}
        new AlertDialog.Builder(this).setTitle("本地管理").setItems(new String[]{"查看存储空间","查看任务记录",app.admin.biometricEnabled()?"关闭生物识别解锁":"启用生物识别解锁","锁定管理员"},(d,which)->{
            if(which==0)submit("查看空间");else if(which==1)submit("查看任务");
            else if(which==2){
                if(app.admin.biometricEnabled()){app.admin.setBiometricEnabled(false);showInfo("生物识别解锁已关闭。");}
                else{BiometricManager manager=getSystemService(BiometricManager.class);if(manager==null||manager.canAuthenticate()!=BiometricManager.BIOMETRIC_SUCCESS)showInfo("设备未配置可用生物识别。请先在系统设置中配置指纹或面部识别。");else{app.admin.setBiometricEnabled(true);showInfo("已启用设备支持的生物识别解锁，密码仍可使用。");}}
            }else{app.disconnect();app.admin.lock();showLogin();}
        }).show();
    }
    private void biometricUnlock(){
        if(!app.admin.biometricEnabled())return;
        biometricSignal=new CancellationSignal();
        new BiometricPrompt.Builder(this).setTitle("解锁个人AI中枢").setSubtitle("使用本机已配置的生物识别")
            .setNegativeButton("使用密码",getMainExecutor(),(d,w)->{})
            .build().authenticate(biometricSignal,getMainExecutor(),new BiometricPrompt.AuthenticationCallback(){
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result){if(!foreground||isFinishing()||isDestroyed())return;app.admin.biometricSucceeded();app.stop.resume();showHome();}
                @Override public void onAuthenticationError(int code,CharSequence text){showInfo("生物识别未完成，请使用密码解锁。");}
            });
    }
    private void append(String role,String text){
        if(conversation==null)return;LinearLayout card=column();card.setPadding(dp(16),dp(12),dp(16),dp(14));
        card.setBackground(round(role.equals("user")?0xffe2eee8:Color.WHITE,18));card.addView(label(role.equals("user")?"你":"个人AI中枢",12,MUTED));
        TextView message=label(text,16,INK);message.setTextIsSelectable(true);message.setLineSpacing(dp(3),1);card.addView(message);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);params.setMargins(0,0,0,dp(12));conversation.addView(card,params);
        if(chatScroll!=null)chatScroll.post(()->chatScroll.fullScroll(View.FOCUS_DOWN));
    }
    private void showInfo(String message){if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this).setMessage(message).setPositiveButton("知道了",null).show();}
    private LinearLayout column(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.VERTICAL);view.setBackgroundColor(BG);return view;}
    private LinearLayout row(){LinearLayout view=new LinearLayout(this);view.setOrientation(LinearLayout.HORIZONTAL);return view;}
    private TextView label(String text,int sp,int color){TextView view=new TextView(this);view.setText(text);view.setTextSize(sp);view.setTextColor(color);view.setPadding(0,dp(5),0,dp(5));if(sp>=24)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return view;}
    private EditText field(String hint){EditText view=new EditText(this);view.setHint(hint);view.setTextSize(16);view.setTextColor(INK);view.setPadding(dp(12),dp(14),dp(12),dp(14));view.setMinHeight(dp(52));return view;}
    private Button button(String text,boolean primary,Runnable action){Button view=new Button(this);view.setText(text);view.setTextSize(14);view.setAllCaps(false);view.setMinHeight(dp(48));view.setTextColor(primary?Color.WHITE:ACCENT);view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary?ACCENT:0xffe5eee8));view.setOnClickListener(v->action.run());return view;}
    private GradientDrawable round(int color,int radius){GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);drawable.setCornerRadius(dp(radius));return drawable;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void insets(View root){final int l=root.getPaddingLeft(),t=root.getPaddingTop(),r=root.getPaddingRight(),b=root.getPaddingBottom();root.setOnApplyWindowInsetsListener((view,insets)->{view.setPadding(l+insets.getSystemWindowInsetLeft(),t+insets.getSystemWindowInsetTop(),r+insets.getSystemWindowInsetRight(),b+insets.getSystemWindowInsetBottom());return insets;});root.requestApplyInsets();}
}
