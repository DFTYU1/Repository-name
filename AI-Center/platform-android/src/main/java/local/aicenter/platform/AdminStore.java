package local.aicenter.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import local.aicenter.core.PasswordCredential;

/** Exactly one local administrator. No default password and no remote identity provider. */
public final class AdminStore {
    private final SharedPreferences prefs;
    private final SecretStore secret;
    private volatile long sessionDeadline;
    public AdminStore(Context context, SecretStore secret) {
        this.prefs = context.getSharedPreferences("admin",Context.MODE_PRIVATE); this.secret = secret;
    }
    public boolean configured() { return prefs.contains("verifier"); }
    public synchronized void setup(char[] password) throws GeneralSecurityException {
        try {
            if (configured()) throw new IllegalStateException("管理员已经建立");
            String credential = PasswordCredential.create(password);
            if (!prefs.edit().putString("verifier",secret.encrypt("admin-verifier",credential)).commit()) throw new IllegalStateException("管理员保存失败");
            renew();
        } finally { Arrays.fill(password,'\0'); }
    }
    public synchronized boolean unlock(char[] password) throws GeneralSecurityException {
        try {
            if (!configured()) return false;
            long blockedUntil = prefs.getLong("blocked_until",0);
            if (System.currentTimeMillis()<blockedUntil) throw new IllegalStateException("连续错误过多，请稍后再试");
            boolean correct = PasswordCredential.verify(password,secret.decrypt("admin-verifier",prefs.getString("verifier","")));
            if (correct) { if(!prefs.edit().putInt("failures",0).putLong("blocked_until",0).commit())throw new IllegalStateException("认证状态保存失败"); renew(); }
            else {
                int failures = Math.min(20,prefs.getInt("failures",0)+1);
                long delay = failures < 5 ? 0 : Math.min(300_000,30_000L*(failures-4));
                if(!prefs.edit().putInt("failures",failures).putLong("blocked_until",System.currentTimeMillis()+delay).commit())throw new IllegalStateException("认证状态保存失败");
            }
            return correct;
        } finally { Arrays.fill(password,'\0'); }
    }
    public boolean unlocked() { return configured() && SystemClock.elapsedRealtime()<sessionDeadline; }
    public void require() { if (!unlocked()) throw new SecurityException("请先解锁管理员"); }
    public void renew() { sessionDeadline = SystemClock.elapsedRealtime()+15*60*1000L; }
    public void lock() { sessionDeadline = 0; }
    public boolean biometricEnabled() { return prefs.getBoolean("biometric",false); }
    public void setBiometricEnabled(boolean enabled) { require(); prefs.edit().putBoolean("biometric",enabled).commit(); }
    public void biometricSucceeded() {
        if (!configured() || !biometricEnabled()) throw new SecurityException("生物识别未启用");
        renew();
    }
}
