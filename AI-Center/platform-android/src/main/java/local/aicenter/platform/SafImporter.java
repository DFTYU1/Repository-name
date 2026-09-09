package local.aicenter.platform;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import local.aicenter.core.AgentRuntime;
import local.aicenter.core.FileVault;
import local.aicenter.core.KnowledgeIndex;
import local.aicenter.core.LeaseBook;
import local.aicenter.core.StopController;
import local.aicenter.core.StorageBudget;

/** Only explicitly selected content URIs. No persisted directory-wide grant is taken. */
public final class SafImporter {
    public static final int MAX_TEXT_BYTES=2*1024*1024;
    private final Context context;
    private final FileVault vault;
    private final LeaseBook leases;
    private final StopController stop;
    private final CenterDatabase db;
    public SafImporter(Context context,FileVault vault,LeaseBook leases,StopController stop,CenterDatabase db) {
        this.context=context;this.vault=vault;this.leases=leases;this.stop=stop;this.db=db;
    }
    public String importText(Uri uri,StopController.Token token) throws Exception {
        if(uri==null||!"content".equals(uri.getScheme())) throw new SecurityException("只能导入明确选择的文件");
        String taskId=UUID.randomUUID().toString();String lease=leases.grant(taskId,uri.toString(),token);
        ContentResolver resolver=context.getContentResolver();CancellationSignal signal=new CancellationSignal();
        try(AutoCloseable queryStop=stop.onStop(token,signal::cancel)) {
            db.record(taskId,AgentRuntime.State.PLANNED,"file.import","created");
            leases.check(lease,uri.toString());
            String title="";long advertised=-1;
            try(Cursor c=resolver.query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null,signal)) {
                if(c!=null&&c.moveToFirst()){title=c.getString(0);if(!c.isNull(1))advertised=c.getLong(1);}
            }
            if(title==null||title.length()>512||!title.toLowerCase(Locale.ROOT).matches(".*\\.(txt|md)")) throw new IllegalArgumentException("当前可导入 UTF-8 编码的 TXT 或 Markdown 文件");
            if(advertised>MAX_TEXT_BYTES) throw new IOException("当前文本导入上限为 2MB，原文件未修改");
            StorageBudget.check(StorageManager.appBytes(context),MAX_TEXT_BYTES,context.getFilesDir().getUsableSpace());
            leases.check(lease,uri.toString());
            db.record(taskId,AgentRuntime.State.RUNNING,"file.import","copy_started");
            FileVault.Imported file;
            ParcelFileDescriptor descriptor=resolver.openFileDescriptor(uri,"r",signal);
            if(descriptor==null) throw new IOException("无法读取所选文件");
            try(InputStream original=new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                AutoCloseable streamStop=stop.onStop(token,()->{try{original.close();}catch(IOException ignored){}});
                InputStream checked=new FilterInputStream(original) {
                    @Override public int read(byte[] b,int off,int length)throws IOException {leases.check(lease,uri.toString());return super.read(b,off,length);}
                    @Override public int read()throws IOException {leases.check(lease,uri.toString());return super.read();}
                }) {file=vault.importCopy(checked,title,MAX_TEXT_BYTES,token);}
            // Record the complete owned copy before indexing, so failed indexing never destroys a file.
            String documentId=db.addDocument(title,file);
            token.check();String text=vault.readText(file.storedName,MAX_TEXT_BYTES,token);
            List<KnowledgeIndex.Chunk> chunks=KnowledgeIndex.chunk(documentId,title,text,512,64);
            db.index(documentId,chunks,token);token.check();
            db.record(taskId,AgentRuntime.State.SUCCEEDED,"file.import","indexed");
            return "已保存「"+title+"」的本地副本，并建立 "+chunks.size()+" 个带来源的文本分块。原文件保留。";
        } catch(Exception e) {
            try{db.record(taskId,token.valid()?AgentRuntime.State.FAILED:AgentRuntime.State.CANCELLED,"file.import",token.valid()?"import_failed":"stopped");}catch(RuntimeException ignored){}
            throw e;
        } finally {
            leases.complete(taskId);
            new Handler(Looper.getMainLooper()).postDelayed(()->leases.revoke(lease),LeaseBook.GRACE_MS);
        }
    }
}
