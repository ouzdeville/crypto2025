import javax.crypto.SecretKey;

import dcssi.cfc.crypto.CryptoImpl;

import java.io.File;
/* 

public class BureauShield {
    public static void main(String[] args) {
        CryptoImpl crypto = new CryptoImpl();
        
        // 1. Déterminer le chemin du Bureau (Desktop) de l'utilisateur
        String userHome = System.getProperty("user.home");
        String desktopPath = userHome + File.separator + "Desktop" + File.separator + "DossierTest"; 
        // CONSEIL: Utilise un dossier "DossierTest" pour tes essais !

        try {
            // 2. Génération de la clé de session
            // On utilise ta méthode basée sur l'entropie de la souris
            System.out.println("Veuillez bouger la souris pour générer la clé...");
            //SecretKey sessionKey = crypto.generateKey();
            SecretKey sessionKey = crypto.generatePBEKey("INGENIEUR");

            if (sessionKey != null) {
                // 3. Sauvegarde de la clé (Crucial pour le déchiffrement futur)
                String keyStorage = userHome + File.separator + "session_access.key";
                crypto.saveHexKey(sessionKey, keyStorage, "");
                System.out.println("Clé sauvegardée dans : " + keyStorage);

                // 4. Lancement du chiffrement récursif sur le bureau
                System.out.println("Chiffrement du répertoire en cours : " + desktopPath);
                
                // On utilise ta méthode cipherProcessFolder
                // mode 1 = Cipher.ENCRYPT_MODE
                // deleteAfter = true (pour supprimer les originaux et ne laisser que les .enc)
                boolean success = crypto.cipherProcessFolder(
                    sessionKey, 
                    desktopPath, 
                    desktopPath, // On écrase/remplace dans le même dossier
                    1, 
                    true 
                );

                if (success) {
                    System.out.println("Opération terminée. Les fichiers originaux ont été sécurisés.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur durant le processus : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

        
*/