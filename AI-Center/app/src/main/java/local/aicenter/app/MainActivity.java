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
import local.aicenter.core.FeatureCatalog;
import local.aicenter.core.TypingSession;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.SystemClock;
import local.aicenter.core.ReadOnlyToolIntent;
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
    private boolean protectedScreen;
    private AlertDialog apiDialog;
    private CancellationSignal biometricSignal;
    private String draft="";
    private final Runnable statusTick=new Runnable(){
        public void run(){
            if(isFinishing()||isDestroyed())return;
            if(foreground&&app.initialized&&protectedScreen&&!app.admin.unlocked()&&!selectingFile){app.disconnect();showLogin();}
            if(activityStatus!=null)activityStatus.setText(app.busy.get()?"正在处理…":app.stop.enabled()?(app.localModel!=null&&app.localModel.ready()?"本地 AI 就绪 · 离线运行":"本地资料就绪 · 模型不可用"):"AI 已断开");
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
            app.disconnect();app.admin.lock();if(apiDialog!=null){apiDialog.dismiss();apiDialog=null;}
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
        protectedScreen=false;if(apiDialog!=null){apiDialog.dismiss();apiDialog=null;}
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
        protectedScreen=true;
        LinearLayout root=column();root.setPadding(dp(18),dp(8),dp(18),dp(12));
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout heading=column();heading.addView(label("个人 AI 中枢",25,INK));
        activityStatus=label((app.localModel!=null&&app.localModel.ready()?"本地 AI 就绪 · 离线运行":"本地资料就绪 · 模型不可用"),13,MUTED);heading.addView(activityStatus);
        header.addView(heading,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        header.addView(button("立即断开AI",false,()->{app.disconnect();append("assistant","AI 已断开，当前任务和临时文件访问已停止。");}));root.addView(header);
        LinearLayout body=row();boolean wide=getResources().getConfiguration().screenWidthDp>=840;
        if(wide){
            LinearLayout sidebar=column();sidebar.setPadding(0,dp(24),dp(20),0);
            sidebar.addView(label("我的空间",15,MUTED));
            sidebar.addView(button("导入资料",true,this::selectText));
            sidebar.addView(button("中英文打字",false,this::showTyping));
            sidebar.addView(button("学习与纠错",false,this::showLearning));
            sidebar.addView(button("功能清单",false,()->showInfo(FeatureCatalog.summary())));
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
        if(wide)center.addView(button("本次使用API增强",false,this::confirmCloud));
        else {
            LinearLayout modules=row();
            modules.addView(button("中英文打字",false,this::showTyping),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
            modules.addView(button("学习与纠错",false,this::showLearning),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));center.addView(modules);
            LinearLayout actions=row();
            actions.addView(button("本次使用API增强",false,this::confirmCloud),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
            actions.addView(button("更多功能",false,this::showFunctions),new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));center.addView(actions);
        }
        body.addView(center,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.MATCH_PARENT,1));root.addView(body,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));setContentView(root);insets(root);
        final LinearLayout historyView=conversation;
        app.worker.execute(()->{
            try{List<String[]> messages=app.db.recentMessages();runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||conversation!=historyView||app.busy.get()||conversation.getChildCount()>0)return;
                conversation.removeAllViews();
                if(messages.isEmpty())append("assistant","这是开发中的基础版本，可导入 TXT / Markdown、检索资料、查看文件、存储及任务记录。\n\n内置本地模型可在断网时对话。当前为小模型验证版本，回答需核实。");
                else for(String[] message:messages)append(message[0],message[1]);
            });}catch(Exception e){runOnUiThread(()->showFatal("历史记录暂时无法读取，原数据保留。"));}
        });
    }
    private void submit(String text){submit(text,false);}
    private void submit(String text,boolean cloud){
        if(!app.admin.unlocked()){showLogin();return;}
        if(text.length()>2000){showInfo("当前问题最多2000个字符。");return;}
        if(!app.stop.enabled()){
            new AlertDialog.Builder(this).setTitle("恢复 AI 工作？").setMessage("会开始一个新任务。之前撤销的文件授权仍然无效。")
                .setPositiveButton("恢复并继续",(d,w)->{app.stop.resume();submit(text,cloud);}).setNegativeButton("取消",null).show();return;
        }
        if(!app.busy.compareAndSet(false,true)){showInfo("当前任务还在处理，可等待或点击“立即断开AI”。");return;}
        if(FeatureCatalog.typingRequested(text)){app.busy.set(false);showTyping();return;}
        if(FeatureCatalog.learningRequested(text)){app.busy.set(false);showLearning();return;}
        StopController.Token token=app.stop.begin();append("user",text);
        final LinearLayout targetConversation=conversation;
        app.worker.execute(()->{
            String answer;
            String stage="保存输入";
            try{
                app.admin.require();token.check();if(cloud)app.authorizeCloudRequest(token);app.db.message("user",text);
                String tool=cloud?"cloud_chat":"chat",arg=text;
                if(text.equals("查看文件"))tool="files";
                else if(text.equals("查看空间")||text.equals("查看存储"))tool="storage";
                else if(text.equals("查看任务"))tool="tasks";
                else if(text.startsWith("查找：")||text.startsWith("查找:")){tool="search";arg=text.substring(3).trim();}
                else {
                    String candidate=ReadOnlyToolIntent.candidate(text);
                    if(!candidate.equals("chat")){
                        String selected=app.planTool(text,token);
                        tool=candidate.equals(selected)?selected:"chat";
                    }
                }
                stage="执行任务";answer=app.agent.execute(Collections.singletonList(new AgentRuntime.Step(tool,arg)),token).output;
                stage="保存结果";token.check();app.db.message("assistant",answer);
            }catch(StopController.Stopped e){answer="任务已停止，已有内容保留。";}
            catch(Exception e){answer="“"+stage+"”这一步未完成。请检查管理员是否已解锁及存储空间；已有文件保留。";}
            finally{app.revokeCloudRequest(token);app.busy.set(false);}
            final String result=answer;runOnUiThread(()->{if(token.valid()&&foreground&&conversation==targetConversation&&!isFinishing()&&!isDestroyed())append("assistant",result);});
        });
    }
    private void showTyping(){
        if(!app.admin.unlocked()){showLogin();return;}
        if(app.busy.get()){showInfo("请先等待当前任务结束，或点击立即断开AI。");return;}
        protectedScreen=true;input=null;conversation=null;chatScroll=null;activityStatus=null;
        LinearLayout root=column();root.setPadding(dp(20),dp(20),dp(20),dp(20));
        root.addView(label("中英文打字练习",26,INK));
        root.addView(label("离线练习 · 首次输入计时 · 中文字/分钟，英文5字符/词。少于1秒完成不计入难度学习；粘贴不能代表真实速度。",14,MUTED));
        final boolean[] chinesePractice={false};
        final String[] target={app.learning.target(false)};
        final int[] typedTotal={0},errorTotal={0};
        final String[] previous={""};
        final TypingSession[] session={null};final boolean[] saved={false};
        TextView reference=label(target[0],22,INK);root.addView(reference);
        EditText practice=field("在此输入上方文本");practice.setSingleLine(false);practice.setMinLines(3);
        practice.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        TextView score=label("尚未开始",17,MUTED);
        android.content.SharedPreferences progress=getSharedPreferences("typing_progress",MODE_PRIVATE);
        TextView record=label("已完成练习："+progress.getInt("completed",0)+"次",14,MUTED);
        LinearLayout languages=row();
        Runnable reset=()->{typedTotal[0]=0;errorTotal[0]=0;previous[0]="";target[0]=app.learning.target(chinesePractice[0]);session[0]=null;saved[0]=false;reference.setText(target[0]);practice.setText("");score.setText("尚未开始");practice.setEnabled(true);};
        languages.addView(button("英文练习",false,()->{chinesePractice[0]=false;reset.run();}));
        languages.addView(button("中文练习",false,()->{chinesePractice[0]=true;reset.run();}));root.addView(languages);
        practice.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){}
            public void afterTextChanged(Editable text){
                if(text.length()==0&&session[0]==null)return;
                int[] old=previous[0].codePoints().toArray(),now=text.toString().codePoints().toArray(),expected=target[0].codePoints().toArray();
                int prefix=0;while(prefix<old.length&&prefix<now.length&&old[prefix]==now[prefix])prefix++;
                for(int i=prefix;i<now.length;i++){typedTotal[0]++;if(i>=expected.length||now[i]!=expected[i])errorTotal[0]++;}previous[0]=text.toString();
                if(session[0]==null)session[0]=new TypingSession(target[0],SystemClock.elapsedRealtime());
                TypingSession.Result result=session[0].score(text.toString(),SystemClock.elapsedRealtime());
                boolean chinese=target[0].codePoints().anyMatch(c->c>127);
                score.setText(String.format(java.util.Locale.ROOT,"%s · 正确率 %.1f%% · 错误字符 %d · %.1f %s · %d秒",
                    result.complete?"已完成":"练习中",result.accuracy,result.errors,chinese?result.charactersPerMinute:result.wpm,chinese?"字/分钟":"WPM",result.elapsedMs/1000));
                if(result.complete&&!saved[0]){saved[0]=true;try{if(result.elapsedMs>=1000)app.learning.practice(chinesePractice[0],typedTotal[0],errorTotal[0]);}catch(Exception e){showInfo("本次学习记录保存失败，原记录保留。");}int total=progress.getInt("completed",0)+1;progress.edit().putInt("completed",total).putFloat("last_accuracy",(float)result.accuracy).apply();record.setText("已完成练习："+total+"次");practice.setEnabled(false);}
            }
        });
        root.addView(practice);root.addView(score);root.addView(record);
        root.addView(button("重新练习",false,reset));root.addView(button("返回AI首页",true,this::showHome));
        ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);insets(root);
    }
    private void showFunctions(){
        if(!app.admin.unlocked()){showLogin();return;}
        apiDialog=new AlertDialog.Builder(this).setTitle("功能与管理").setItems(new String[]{"功能清单","导入资料","本地文件","任务记录","管理"},(d,n)->{
            if(!app.admin.unlocked()){showLogin();return;}
            if(n==0)showInfo(FeatureCatalog.summary());else if(n==1)selectText();else if(n==2)submit("查看文件");else if(n==3)submit("查看任务");else showManagement();
        }).setNegativeButton("关闭",null).show();
    }
    private void showLearning(){
        if(!app.admin.unlocked()){showLogin();return;}if(app.busy.get()){showInfo("请先等待当前任务结束。");return;}
        protectedScreen=true;input=null;conversation=null;chatScroll=null;activityStatus=null;
        LinearLayout root=column();root.setPadding(dp(20),dp(20),dp(20),dp(20));root.addView(label("学习与纠错",26,INK));
        TextView status=label("",15,MUTED);root.addView(status);
        EditText question=field("需要纠正的原问题");EditText answer=field("你确认的正确答案");answer.setSingleLine(false);root.addView(question);root.addView(answer);
        Runnable refresh=()->{try{status.setText(app.learning.summary());}catch(Exception e){status.setText("学习记录读取失败，原数据保留。");}};refresh.run();
        root.addView(button("保存确认答案",true,()->{if(!app.admin.unlocked()){showLogin();return;}try{app.learning.correct(question.getText().toString(),answer.getText().toString());question.setText("");answer.setText("");refresh.run();}catch(Exception e){showInfo(e instanceof IllegalArgumentException?e.getMessage():"保存失败，原记录保留。");}}));
        root.addView(button("查看／修改／删除纠错",false,()->{try{List<String[]> rows=app.learning.corrections();if(rows.isEmpty()){showInfo("还没有纠错记录。");return;}String[] names=new String[rows.size()];for(int i=0;i<names.length;i++)names[i]=rows.get(i)[0];apiDialog=new AlertDialog.Builder(this).setTitle("选择纠错记录").setItems(names,(d,i)->{String[] item=rows.get(i);apiDialog=new AlertDialog.Builder(this).setTitle(item[0]).setMessage(item[1]).setPositiveButton("修改",(dialog,w)->{if(app.admin.unlocked()){question.setText(item[0]);answer.setText(item[1]);}}).setNegativeButton("删除",(dialog,w)->{if(app.admin.unlocked()){try{app.learning.remove(item[0]);refresh.run();}catch(Exception e){showInfo("删除失败，原记录保留。");}}}).setNeutralButton("关闭",null).show();}).show();}catch(Exception e){showInfo("读取失败，原数据保留。");}}));
        root.addView(button("开启／关闭学习",false,()->{if(!app.admin.unlocked()){showLogin();return;}try{app.learning.enabled(!app.learning.enabled());refresh.run();}catch(Exception e){showInfo("设置保存失败。");}}));
        root.addView(button("清除全部学习记录",false,()->{apiDialog=new AlertDialog.Builder(this).setTitle("清除学习记录？").setMessage("将删除纠错与适配记录，不能撤销；原文件与聊天记录保留。").setPositiveButton("清除",(d,w)->{if(app.admin.unlocked()){try{app.learning.clear();refresh.run();}catch(Exception e){showInfo("清除失败，原数据保留。");}}}).setNegativeButton("取消",null).show();}));
        root.addView(button("返回AI首页",true,this::showHome));ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);insets(root);
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
    private void confirmCloud(){
        if(!app.admin.unlocked()){showLogin();return;}
        if(input==null)return;final String text=input.getText().toString().trim();if(text.isEmpty()){showInfo("请先输入问题。");return;}
        if(!app.apiSettings.ready("A")&&!app.apiSettings.ready("B")){showInfo("请先在管理中配置API A或B。默认发送仍使用本地模型。");return;}
        String destinations="";
        for(String slot:new String[]{"A","B"})if(app.apiSettings.ready(slot))destinations+="API "+slot+"："+app.apiSettings.endpoint(slot)+"\n";
        new AlertDialog.Builder(this).setTitle("仅授权本次API请求")
            .setMessage("将发送本次输入，不附带历史聊天或已导入文件。服务可能收费，费用由你的API账号承担。失败按已配置的A→B→本地回退。\n"+destinations+"\n确认发送这次输入吗？")
            .setPositiveButton("确认本次发送",(d,w)->{if(!app.admin.unlocked()||input==null)return;input.setText("");submit(text,true);}).setNegativeButton("取消",null).show();
    }
    private void showApiSettings(String slot){
        if(!app.admin.unlocked()){showLogin();return;}
        LinearLayout form=column();form.setPadding(dp(20),dp(10),dp(20),dp(10));
        form.addView(label("兼容Chat Completions协议的完整HTTPS接口；保存不会联网。Key使用本机密钥库加密。留空Key仅保留同一地址原Key。",14,MUTED));
        EditText endpoint=field("完整HTTPS接口地址");endpoint.setText(app.apiSettings.endpoint(slot));form.addView(endpoint);
        EditText model=field("模型标识");model.setText(app.apiSettings.model(slot));form.addView(model);
        EditText key=field("API Key（不回显已保存值）");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);form.addView(key);
        TextView feedback=label("",14,MUTED);form.addView(feedback);
        Button save=button("加密保存配置",true,()->{
            if(!app.admin.unlocked())return;
            try{app.apiSettings.save(slot,endpoint.getText().toString(),model.getText().toString(),key.getText().toString());key.setText("");feedback.setText("配置已保存；尚未证明连接成功。可在首页输入测试问题并授权本次API增强。");}
            catch(Exception e){feedback.setText("保存失败：请检查HTTPS地址、模型名和Key；更换地址须重新输入Key。");}
        });form.addView(save);
        form.addView(button("删除本槽位配置",false,()->{if(app.admin.unlocked()){app.apiSettings.save(slot,"","","");key.setText("");endpoint.setText("");model.setText("");feedback.setText("该配置已移除。");}}));
        ScrollView scroll=new ScrollView(this);scroll.addView(form);
        apiDialog=new AlertDialog.Builder(this).setTitle("API "+slot).setView(scroll).setNegativeButton("关闭",(d,w)->key.setText("")).show();
    }
    private void showManagement(){
        if(!app.admin.unlocked()){showLogin();return;}
        new AlertDialog.Builder(this).setTitle("本地管理").setItems(new String[]{"查看存储空间","查看任务记录",app.admin.biometricEnabled()?"关闭生物识别解锁":"启用生物识别解锁","锁定管理员","配置API A","配置API B"},(d,which)->{
            if(which==0)submit("查看空间");else if(which==1)submit("查看任务");
            else if(which==2){
                if(app.admin.biometricEnabled()){app.admin.setBiometricEnabled(false);showInfo("生物识别解锁已关闭。");}
                else{BiometricManager manager=getSystemService(BiometricManager.class);if(manager==null||manager.canAuthenticate()!=BiometricManager.BIOMETRIC_SUCCESS)showInfo("设备未配置可用生物识别。请先在系统设置中配置指纹或面部识别。");else{app.admin.setBiometricEnabled(true);showInfo("已启用设备支持的生物识别解锁，密码仍可使用。");}}
            }else if(which==4||which==5){showApiSettings(which==4?"A":"B");}
            else{app.disconnect();app.admin.lock();showLogin();}
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
