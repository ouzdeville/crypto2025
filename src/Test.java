import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.dcssi.cfc.crypto.CryptoImpl;

public class Test {

    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();

        SecretKey key = crypto.generateKey();

        crypto.saveHexKey(key, "mykey.key", null);
        crypto.cipherProcess(key, "testFile.text", 
        "encryptedFile.text", 
        Cipher.ENCRYPT_MODE, false);
    }
}
