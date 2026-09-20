package local.aicenter.app;

import android.app.Application;
import android.os.SystemClock;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import local.aicenter.core.*;
import local.aicenter.platform.*;

public final class CenterApplication extends Application {
    public final StopController stop=new StopController();
    public final LeaseBook leases=new LeaseBook(SystemClock::elapsedRealtime);
    public final ExecutorService worker=Executors.newSingleThreadExecutor();
    public final AtomicBoolean busy=new AtomicBoolean();
    public SecretStore secrets;
    public AdminStore admin;
    public CenterDatabase db;
    public FileVault vault;
    public SafImporter importer;
    public AgentRuntime agent;
    public ModelRouter models;
    public LocalModelEngine localModel;
    public volatile String modelError;
    public volatile boolean initialized;
    public volatile String startupError;
    @Override public void onCreate(){
        super.onCreate();
        secrets=new SecretStore();admin=new AdminStore(this,secrets);db=new CenterDatabase(this,secrets);
        worker.execute(()->{
            try{
                vault=new FileVault(new File(getFilesDir(),"vault").toPath());
                importer=new SafImporter(this,vault,leases,stop,db);
                db.getWritableDatabase();db.recoverInterruptedTasks();
                try {
                    localModel=new LocalModelEngine(this,stop);
                    localModel.installBundled();
                    models=new ModelRouter(Collections.singletonList(localModel));
                } catch(Exception | LinkageError error) {
                    modelError="本地模型初始化失败，请检查可用空间或安装包完整性。";
                }
                agent=new AgentRuntime(db,ApprovalPolicy.DENY);
                register("storage",input->StorageManager.summary(this,activeToken.get()));
                register("files",input->db.documentSummary());
                register("tasks",input->db.taskSummary());
                register("chat",input->{
                    if(models==null)throw new IllegalStateException(modelError);
                    return models.answer(input,false,false,(provider,paid,privateContent)->false,activeToken.get()).text;
                });
                register("search",input->{
                    StopController.Token token=activeToken.get();
                    java.util.List<KnowledgeIndex.Hit> hits=new KnowledgeIndex().search(input,db.chunks(token),5);
                    if(hits.isEmpty())return "导入的文本中没有找到相关内容。可以换个关键词，或先导入资料。";
                    StringBuilder text=new StringBuilder("资料检索结果\n");int n=0;
                    for(KnowledgeIndex.Hit hit:hits)text.append("\n[").append(++n).append("] ").append(hit.chunk.title).append(" · 字符位置 ").append(hit.chunk.offset).append("\n").append(hit.chunk.text).append('\n');
                    return text.toString();
                });
                initialized=true;
            }catch(Exception e){startupError="本地数据初始化失败，已有文件保留。请继续修复工程，勿清除应用数据。";}
        });
    }
    private final ThreadLocal<StopController.Token> activeToken=new ThreadLocal<>();
    private interface Action {String run(String input)throws Exception;}
    private void register(String id,Action action){
        agent.register(new AgentRuntime.Tool(){
            public String id(){return id;}
            public java.util.Set<ApprovalPolicy.Risk> risks(){return Collections.emptySet();}
            public String execute(String input,StopController.Token token)throws Exception{
                admin.require();token.check();activeToken.set(token);
                try{return action.run(input);}finally{activeToken.remove();}
            }
        });
    }
    /** A real model selects one bounded, read-only tool; arguments remain the user's original input. */
    public String planTool(String input,StopController.Token token)throws Exception{
        admin.require();token.check();
        if(localModel==null||!localModel.ready())throw new IllegalStateException("Local model unavailable");
        String selected=localModel.generate("Select exactly one tool for this user request. "
            +"storage: inspect device disk usage and free space; files: list imported documents; "
            +"tasks: inspect task history; search: find text in imported documents; "
            +"chat: answer all other questions without executing actions. Output only the tool name. Request: "+input,
            "root ::= \"storage\" | \"files\" | \"tasks\" | \"search\" | \"chat\"",16,token);
        if(!agent.availableTools().contains(selected))throw new IllegalStateException("Invalid model tool selection");
        return selected;
    }
    public void disconnect(){stop.stop();leases.revokeAll();}
}
