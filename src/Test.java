import dcssi.cfc.crypto.CryptoImpl;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;



public class Test {

    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();

        SecretKey key = crypto.generateKey();

        crypto.saveHexKey(key, "mykey.key", null);

        SecretKey key1 = crypto.generatePBEKey("INGENIEUR");
        //crypto.cipherProcessFolder(key1, "dossier", "dossier", Cipher.DECRYPT_MODE, true);
        /*
        System.out.println("Chiffrement en cours...");
        boolean result = crypto.cipherProcess(key1, "testFile.text", "testFile.text.enc", Cipher.ENCRYPT_MODE, true);
        if (result) {
            System.out.println("Chiffrement réussi. L'original a été supprimé.");
        } else {
            System.out.println("Échec du chiffrement.");
        }
        */
       //-------Déchiffrement-------
         /*
       System.out.println("Déchiffrement en cours...");
         boolean result = crypto.cipherProcess(key1, "testFile.text.enc", "testFile_decrypted.text", Cipher.DECRYPT_MODE, false);
        if (result) {
            System.out.println("Déchiffrement réussi.");
        } else {
            System.out.println("Échec du déchiffrement.");
        }
        */
       /* 
        //-------Chiffrement de dossier-------
        System.out.println("Chiffrement de dossier en cours...");
        boolean dossierEnc = crypto.cipherProcessFolder(key, "dossier", "dossier_enc", Cipher.ENCRYPT_MODE, false);
        if (dossierEnc) {
            System.out.println("Chiffrement de dossier réussi. L'original n'a pas été supprimé.");
        } else {
            System.out.println("Échec du chiffrement de dossier.");
        }
        */

        //-------chiffrement de dossier-------
        /* 
        System.out.println("chiffrement de dossier en cours..."); 
        boolean dossierDec = crypto.cipherProcessFolder(key, "dossier", "dossierCible", Cipher.ENCRYPT_MODE, false);
        if (dossierDec) {
            System.out.println("Chiffrement de dossier réussi.");
        } else {
            System.out.println("Échec du chiffrement de dossier.");
        }
        */
       //new java.io.File("dossierCible").mkdirs(); 
       
       //----------Déchiffrement de dossier----------
         System.out.println("Déchiffrement de dossier en cours...");
          boolean dossierDec = crypto.cipherProcessFolder(key1, "dossier", "dossierCible", Cipher.ENCRYPT_MODE, false);
          if (dossierDec) {
                System.out.println("chiffrement de dossier réussi.");
          } else {
                System.out.println("Échec du chiffrement de dossier.");
          }


    }
}
