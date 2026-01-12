import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.dcssi.cfc.crypto.CryptoImpl;
import com.dcssi.cfc.crypto.ICrypto;

public class Test {

    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();

        SecretKey key;

        //crypto.saveHexKey(key, "mykey.key", null);
        key=(SecretKey) crypto.loadHexKey(
            "mykey.key", 
            null,
            ICrypto.SECRET_KEY);
        crypto.cipherProcessFolder(key,
             "dossier",
              "dossier",
               Cipher.DECRYPT_MODE,
                true);
    }
}
