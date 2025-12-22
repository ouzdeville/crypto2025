package com.dcssi.cfc.crypto;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

/**
 *
 * @author Ousmane NDIAYE door
 */
public interface ICrypto {

    public static final String algo = "AES";
    public static final int keysize = 256;
    public static final String transform = "AES/CBC/PKCS5Padding";
    public static final String iv = "16carat_res01251";
    public final String kdf = "PBKDF2WithHmacSHA256";
    public final int iteration = 1000;
    public final byte[] salt = "MO5-°HG3YEH255367gdsjhgd".getBytes();
    // Faire un programme pour recupérer un seed avec une bonne entropie
    public static final String algoAsym = "RSA";
    public static final String algoSign = "SHA256withECDSA";
    public static final int keysizeAsym = 1024;
    public static final String transformAsym = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * Recupérer des coordonnees différentes de la souris de l'utilisateur pour
     * avoir 256 bits aléatoires.
     *
     * @return
     */
    public byte[] generateSeedTrullyRandom();

    /**
     * la méthode generateKey permet de générer une clé à partir des paramètres
     * spécifiés.
     *
     * @return
     */
    public SecretKey generateKey();

    public SecretKey generatePBEKey(String password);

    public String bytesToHex(byte[] tab);

    public byte[] hextoBytes(String chaine);

    /**
     * Chiffrement ou déchiffrement d'un fichier avec une clé symétrique
     * 
     * @param k
     * @param inputFile   les fichier à chiffrer ou déchiffrer
     * @param outputFile  le fichier chiffré ou déchiffré
     * @param mode        Cipher.ENCRYPT_MODE ou Cipher.DECRYPT_MODE pour
     *                    chiffrement ou déchiffrement
     * @param deleteAfter si true supprime le fichier source après traitement
     * @return
     */
    public boolean cipherProcess(SecretKey k, String inputFile, String outputFile, int mode, boolean deleteAfter);

    /**
     * Chiffrement ou déchiffrement d'un dossier avec une clé symétrique
     * 
     * @param k
     * @param inputFolder le dossier à chiffrer ou déchiffrer
     * @param outputFolder le dossier chiffré ou déchiffré
     * @param mode            Cipher.ENCRYPT_MODE ou Cipher.DECRYPT_MODE pour
     *                        chiffrement ou déchiffrement
     * @param deleteAfter     si true supprime le dossier source après traitement
     * @return
     */
    public boolean cipherProcessFolder(SecretKey k, String inputFolder, String outputFolder, int mode,
            boolean deleteAfter);




    
    public KeyPair generateKeyPair(byte[] seed);

    public Key loadHexKey(String chemin, String password, int type);

    public boolean saveHexKey(Key k, String chemin, String password);

    public boolean HybridEnCrypt(PublicKey k, String fileToencrypt, String encryptedFile);

    public boolean HybridDeCrypt(PrivateKey k, String fileToencrypt, String encreptedFile);

    public byte[] processData(byte[] claire, SecretKey secretKey, int mode, IvParameterSpec ivParam);

    public byte[] processData(byte[] claire, Key key, int mode, IvParameterSpec ivParam);

}
