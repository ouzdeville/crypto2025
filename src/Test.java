import dcssi.cfc.crypto.CryptoImpl;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;



public class Test {

    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();

        SecretKey key = crypto.generateKey();

        crypto.saveHexKey(key, "mykey.key", null);

        SecretKey key1 = crypto.generatePBEKey("INGENIEUR");
        crypto.cipherProcessFolder(key1, "dossier", "dossier", Cipher.DECRYPT_MODE, true);
    }
}
