package local.aicenter.platform;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import local.aicenter.core.AgentRuntime;
import local.aicenter.core.FileVault;
import local.aicenter.core.KnowledgeIndex;
import local.aicenter.core.StopController;

public final class CenterDatabase extends SQLiteOpenHelper implements AgentRuntime.Journal {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_CHUNKS = 5000;
    private final Context context;
    private final SecretStore secret;
    public CenterDatabase(Context context, SecretStore secret) {
        super(context,"center.db",null,SCHEMA_VERSION); this.context=context.getApplicationContext(); this.secret=secret;
        setWriteAheadLoggingEnabled(true);
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onCreate(SQLiteDatabase db) {
        try (InputStream input=context.getAssets().open("schema.sql"); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int size;
            while ((size=input.read(buffer))!=-1) output.write(buffer,0,size);
            String sql=new String(output.toByteArray(),StandardCharsets.UTF_8);
            for (String statement:sql.split(";")) if (!statement.trim().isEmpty()) db.execSQL(statement);
        } catch (Exception e) { throw new IllegalStateException("数据库初始化失败",e); }
    }
    @Override public void onUpgrade(SQLiteDatabase db,int from,int to) {
        throw new IllegalStateException("需要显式数据库迁移；禁止删除用户数据重建");
    }
    public synchronized void recoverInterruptedTasks() {
        List<String> ids=new ArrayList<>();
        try (Cursor c=getReadableDatabase().rawQuery("SELECT id FROM tasks WHERE state IN ('PLANNED','RUNNING')",null)) {
            while(c.moveToNext()) ids.add(c.getString(0));
        }
        for(String id:ids) record(id,AgentRuntime.State.INTERRUPTED,"","process_restarted");
    }
    @Override public synchronized void record(String taskId,AgentRuntime.State state,String toolId,String code) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            ContentValues task=new ContentValues(); task.put("state",state.name()); task.put("tool_id",toolId); task.put("code",code); task.put("updated_utc",System.currentTimeMillis());
            if(db.update("tasks",task,"id=?",new String[]{taskId})==0){task.put("id",taskId);db.insertOrThrow("tasks",null,task);}
            ContentValues event=new ContentValues();event.put("task_id",taskId);event.put("state",state.name());event.put("tool_id",toolId);event.put("code",code);event.put("created_utc",System.currentTimeMillis());
            db.insertOrThrow("task_events",null,event);db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    public synchronized void message(String role,String text) {
        ContentValues value=new ContentValues();value.put("role",role);value.put("body_enc",secret.encrypt("message:"+role,text));value.put("created_utc",System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("messages",null,value);
    }
    public List<String[]> recentMessages() {
        List<String[]> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT role,body_enc FROM (SELECT id,role,body_enc FROM messages ORDER BY id DESC LIMIT 30) ORDER BY id",null)) {
            while(c.moveToNext()) result.add(new String[]{c.getString(0),secret.decrypt("message:"+c.getString(0),c.getString(1))});
        }
        return result;
    }
    public synchronized String addDocument(String title,FileVault.Imported file) {
        String id=UUID.randomUUID().toString();ContentValues value=new ContentValues();
        value.put("id",id);value.put("title_enc",secret.encrypt("title:"+id,title));value.put("stored_name",file.storedName);value.put("sha256",file.sha256);value.put("bytes",file.size);
        value.put("added_utc",System.currentTimeMillis());value.put("kind","USER_UPLOAD");value.put("index_state","IMPORTED");
        getWritableDatabase().insertOrThrow("documents",null,value);return id;
    }
    public synchronized void index(String documentId,List<KnowledgeIndex.Chunk> chunks,StopController.Token token) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            long existing;
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM chunks WHERE document_id<>?",new String[]{documentId})) {c.moveToFirst();existing=c.getLong(0);}
            if(existing+chunks.size()>MAX_CHUNKS) throw new IllegalStateException("当前知识索引达到 5000 分块上限，文件副本已经保留");
            db.delete("chunks","document_id=?",new String[]{documentId});
            int ordinal=0;
            for(KnowledgeIndex.Chunk chunk:chunks){token.check();ContentValues value=new ContentValues();value.put("document_id",documentId);value.put("ordinal",ordinal);value.put("source_offset",chunk.offset);value.put("body_enc",secret.encrypt("chunk:"+documentId+":"+ordinal,chunk.text));db.insertOrThrow("chunks",null,value);ordinal++;}
            token.check();ContentValues ready=new ContentValues();ready.put("index_state","READY");db.update("documents",ready,"id=?",new String[]{documentId});db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    public List<KnowledgeIndex.Chunk> chunks(StopController.Token token) {
        List<KnowledgeIndex.Chunk> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT c.document_id,d.title_enc,c.body_enc,c.source_offset,c.ordinal FROM chunks c JOIN documents d ON d.id=c.document_id WHERE d.index_state='READY' ORDER BY c.document_id,c.ordinal",null)) {
            while(c.moveToNext()){token.check();String id=c.getString(0);result.add(new KnowledgeIndex.Chunk(id,secret.decrypt("title:"+id,c.getString(1)),secret.decrypt("chunk:"+id+":"+c.getInt(4),c.getString(2)),c.getInt(3)));}
        }
        return result;
    }
    public String documentSummary() {
        StringBuilder result=new StringBuilder();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,title_enc,bytes,index_state FROM documents ORDER BY added_utc DESC",null)) {
            while(c.moveToNext()) result.append(secret.decrypt("title:"+c.getString(0),c.getString(1))).append(" · ").append(c.getLong(2)).append(" 字节 · ").append(c.getString(3).equals("READY")?"已索引":"待完成索引").append('\n');
        }
        return result.length()==0?"还没有导入文件。可以选择 TXT 或 Markdown 文件，原文件保留。":result.toString();
    }
    public String taskSummary() {
        StringBuilder result=new StringBuilder();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT state,tool_id,code FROM tasks ORDER BY updated_utc DESC LIMIT 20",null)) {
            while(c.moveToNext())result.append(stateLabel(c.getString(0))).append(" · ").append(toolLabel(c.getString(1))).append(" · ").append(codeLabel(c.getString(2))).append('\n');
        }
        return result.length()==0?"还没有任务记录。":result.toString();
    }
    private static String stateLabel(String state){
        switch(state){case "PLANNED":return "已安排";case "RUNNING":return "进行中";case "SUCCEEDED":return "已完成";case "CANCELLED":return "已停止";case "INTERRUPTED":return "上次中断";default:return "未完成";}
    }
    private static String toolLabel(String tool){
        switch(tool){case "file.import":return "导入资料";case "files":return "查看文件";case "storage":return "查看存储";case "search":return "资料检索";case "chat":return "AI对话";case "tasks":return "任务记录";default:return "本地任务";}
    }
    private static String codeLabel(String code){
        switch(code){case "created":return "已记录";case "started":case "copy_started":return "正在处理";case "finished":case "indexed":return "结果已保存";case "model_unavailable":return "模型尚未就绪";case "process_restarted":return "应用重启后保留记录";case "stopped":return "已断开";case "approval_required":return "需要授权";default:return "此步骤失败，已有内容保留";}
    }
}
