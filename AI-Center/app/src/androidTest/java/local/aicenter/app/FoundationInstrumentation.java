package local.aicenter.app;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import local.aicenter.core.AgentRuntime;
import local.aicenter.core.FileVault;
import local.aicenter.core.LeaseBook;
import local.aicenter.core.StopController;
import local.aicenter.platform.CenterDatabase;
import local.aicenter.platform.SecretStore;
import org.json.JSONArray;
import org.json.JSONObject;

/** Runs only inside the test APK on a fresh CI emulator. No test entry point in the shipping APK. */
public final class FoundationInstrumentation extends Instrumentation {
    private CenterApplication app;
    private Activity screen;
    private Bundle options;
    private final JSONArray results = new JSONArray();
    private int failures;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); options=arguments==null?new Bundle():arguments; start(); }
    @Override public void onStart() {
        try {
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            screen=startActivitySync(intent);
            app=(CenterApplication)screen.getApplication();
            test("application_initialization",()->{
                until(()->app.initialized||app.startupError!=null,30_000);
                require(app.initialized&&app.startupError==null,"Application initialization failed");
            });
            if(!app.initialized)throw new IllegalStateException("Cannot test an uninitialized application");
            test("administrator_setup_via_real_ui",()->{
                require(!app.admin.configured(),"CI requires a fresh app; existing admin data will not be changed");
                until(()->hasText("建立管理员"),10_000);
                runOnMainSync(()->{
                    List<EditText> fields=new ArrayList<>(); collectFields(screen.getWindow().getDecorView(),fields);
                    require(fields.size()==2,"Expected two administrator password fields");
                    String password=UUID.randomUUID().toString();
                    fields.get(0).setText(password);fields.get(1).setText(password);
                    findText(screen.getWindow().getDecorView(),"建立管理员").performClick();
                });
                until(()->hasText("个人 AI 中枢"),30_000);
                require(app.admin.unlocked(),"Administrator did not unlock");
            });
            test("responsive_home_layout",()->{
                boolean tablet="tablet".equals(options.getString("profile","phone"));
                require(hasText("立即断开AI"),"Disconnect button is missing");
                require(hasText("我的空间")==tablet,"Sidebar does not match requested form factor");
                require((screen.getResources().getConfiguration().screenWidthDp>=840)==tablet,"Display configuration did not apply");
            });
            test("android_sqlite_and_keystore_message_persistence",()->{
                String fixture="CI-generated 中文 English message "+UUID.randomUUID();
                app.db.message("user",fixture);
                try(Cursor c=app.db.getReadableDatabase().rawQuery("SELECT body_enc FROM messages ORDER BY id DESC LIMIT 1",null)){
                    require(c.moveToFirst()&&!c.getString(0).contains(fixture),"Message is not encrypted at the storage boundary");
                }
                try(CenterDatabase reopened=new CenterDatabase(getTargetContext(),new SecretStore())){
                    List<String[]> messages=reopened.recentMessages();
                    require(messages.stream().anyMatch(m->fixture.equals(m[1])),"Encrypted message did not survive reopening the database");
                    try(Cursor c=reopened.getReadableDatabase().rawQuery("PRAGMA integrity_check",null)){
                        require(c.moveToFirst()&&"ok".equals(c.getString(0)),"SQLite integrity check failed");
                    }
                    try(Cursor c=reopened.getReadableDatabase().rawQuery("PRAGMA foreign_keys",null)){
                        require(c.moveToFirst()&&c.getInt(0)==1,"Foreign keys disabled");
                    }
                }
            });
            test("sandbox_copy_and_traversal_denial",()->{
                StopController.Token token=app.stop.begin();byte[] original="CI-only sandbox fixture".getBytes(StandardCharsets.UTF_8);
                FileVault.Imported file=app.vault.importCopy(new ByteArrayInputStream(original),"fixture.txt",1024,token);
                require(new String(original,StandardCharsets.UTF_8).equals(app.vault.readText(file.storedName,1024,token)),"Sandbox copy changed content");
                boolean denied=false;try{app.vault.resolveExisting("../center.db");}catch(SecurityException expected){denied=true;}
                require(denied,"Sandbox traversal was allowed");
            });
            test("interrupted_task_journal_recovery",()->{
                String id=UUID.randomUUID().toString();app.db.record(id,AgentRuntime.State.RUNNING,"files","started");
                app.db.recoverInterruptedTasks();
                try(Cursor c=app.db.getReadableDatabase().rawQuery("SELECT state FROM tasks WHERE id=?",new String[]{id})){
                    require(c.moveToFirst()&&"INTERRUPTED".equals(c.getString(0)),"Interrupted task was not recovered");
                }
            });
            test("disconnect_button_revokes_token_lease_and_close_hook",()->{
                StopController.Token token=app.stop.begin();AtomicBoolean closed=new AtomicBoolean();
                String uri="content://ci-lease/single-file";String lease=app.leases.grant("ci-stop",uri,token);
                try(AutoCloseable hook=app.stop.onStop(token,()->closed.set(true))){
                    runOnMainSync(()->findText(screen.getWindow().getDecorView(),"立即断开AI").performClick());
                    require(!token.valid()&&closed.get()&&app.leases.count()==0,"Disconnect did not revoke active resources");
                    boolean denied=false;try{app.leases.check(lease,uri);}catch(SecurityException expected){denied=true;}
                    require(denied,"Disconnected lease still works");
                }
                app.stop.resume();require(!token.valid(),"Resume revived an old token");
            });
            if("true".equals(options.getString("long_lease","false")))test("lease_expires_after_real_five_minutes",()->{
                String uri="content://ci-lease/single-file";String task="ci-five-minute";
                String lease=app.leases.grant(task,uri,app.stop.begin());
                app.leases.complete(task);app.leases.check(lease,uri);
                long started=SystemClock.elapsedRealtime();
                while(SystemClock.elapsedRealtime()-started<LeaseBook.GRACE_MS+100)SystemClock.sleep(500);
                boolean denied=false;try{app.leases.check(lease,uri);}catch(SecurityException expected){denied=true;}
                require(denied&&app.leases.count()==0,"Lease remained usable after five real minutes");
            });
        } catch(Throwable error) { failures++; addResult("harness",false,error.getClass().getSimpleName()); }
        Bundle output=new Bundle();
        output.putString("aicenter_results",results.toString());output.putInt("aicenter_failures",failures);
        output.putString("stream",failures==0?"OK: foundation Android checks\n":"FAIL: foundation Android checks\n");
        finish(Activity.RESULT_OK,output);
    }
    private interface Checked { void run()throws Exception; }
    private void test(String name,Checked action) {
        long started=SystemClock.elapsedRealtime();
        try{action.run();addResult(name,true,"elapsed_ms="+(SystemClock.elapsedRealtime()-started));}
        catch(Throwable error){failures++;addResult(name,false,error.getClass().getSimpleName()+": "+error.getMessage());}
    }
    private void addResult(String name,boolean passed,String detail){
        try{results.put(new JSONObject().put("test",name).put("status",passed?"PASS":"FAIL").put("detail",detail));}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void until(BooleanSupplier ready,long timeout){
        long deadline=SystemClock.elapsedRealtime()+timeout;
        while(!ready.getAsBoolean()){require(SystemClock.elapsedRealtime()<deadline,"Timed out waiting for application state");SystemClock.sleep(100);}
    }
    private boolean hasText(String value){
        AtomicBoolean found=new AtomicBoolean();runOnMainSync(()->found.set(findText(screen.getWindow().getDecorView(),value)!=null));return found.get();
    }
    private static View findText(View root,String value){
        if(root instanceof TextView&&value.contentEquals(((TextView)root).getText()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){
            View match=findText(((ViewGroup)root).getChildAt(i),value);if(match!=null)return match;
        }
        return null;
    }
    private static void collectFields(View root,List<EditText> fields){
        if(root instanceof EditText)fields.add((EditText)root);
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)collectFields(((ViewGroup)root).getChildAt(i),fields);
    }
}
