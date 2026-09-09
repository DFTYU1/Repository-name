package local.aicenter.platform;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** App-private ciphertext; encryption keys never leave Android Keystore. */
public final class SecretStore {
    private static final String ALIAS = "local.aicenter.v1.data";
    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (!store.containsAlias(ALIAS)) {
            KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build());
            generator.generateKey();
        }
        return (SecretKey)store.getKey(ALIAS, null);
    }
    public String encrypt(String context, String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return "v1:"+Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)+":"
                +Base64.encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (Exception e) { throw new IllegalStateException("无法安全保存数据", e); }
    }
    public String decrypt(String context, String encoded) {
        try {
            String[] parts = encoded.split(":", -1);
            if (parts.length != 3 || !parts[0].equals("v1")) throw new GeneralSecurityException("Ciphertext format");
            byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
            if (iv.length != 12) throw new GeneralSecurityException("Invalid GCM IV");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.decode(parts[2], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("加密数据无法读取；请勿清除原数据", e); }
    }
}
