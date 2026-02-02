import javax.crypto.SecretKey;

import dcssi.cfc.crypto.CryptoImpl;

import java.io.File;
import java.security.Key;
/* 
public class BureauRestorer {
    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();
        
        // 1. Déterminer les chemins
        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop" + File.separator + "DossierTest";
        String keyStorage = userHome + File.separator + "session_access.key";

        try {
            System.out.println("Recherche de la clé de déchiffrement...");
            File keyFile = new File(keyStorage);

            if (!keyFile.exists()) {
                System.err.println("Erreur : Fichier de clé introuvable ! Impossible de restaurer.");
                return;
            }

            // 2. Charger la clé sauvegardée
            // On utilise le type SECRET_KEY (valeur 3 dans votre interface ICrypto)
            Key savedKey = crypto.loadHexKey(keyStorage, "", 3);

            if (savedKey instanceof SecretKey) {
                SecretKey secretKey = (SecretKey) savedKey;
                System.out.println("Clé chargée avec succès. Restauration en cours...");

                // 3. Lancement du déchiffrement récursif
                // mode 0 = Cipher.DECRYPT_MODE (selon l'usage standard)
                // deleteAfter = true (pour supprimer les fichiers .enc une fois restaurés)
                boolean success = crypto.cipherProcessFolder(
                    secretKey, 
                    desktopPath, 
                    desktopPath, 
                    0, 
                    true 
                );

                if (success) {
                    System.out.println("Restauration terminée ! Vos fichiers sont à nouveau accessibles.");
                    // Optionnel : supprimer le fichier de clé après usage
                    // keyFile.delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Échec de la restauration : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
*/