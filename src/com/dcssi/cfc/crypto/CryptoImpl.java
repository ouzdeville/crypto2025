package com.dcssi.cfc.crypto;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.*;
import java.security.spec.DSAGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoImpl implements ICrypto {

    @Override
    public byte[] generateSeedTrullyRandom() {
        // je veux recuperer des coordonnees de la souris pour avoir une bonne entropie
        // en do while jusqu'a avoir ICrypto.keysize/8 bytes
        byte[] seed = new byte[ICrypto.keysize / 8];
        java.awt.Point precedent = new Point();
        java.awt.Point current = new Point();

        int i = 0;
        do {
            current = java.awt.MouseInfo.getPointerInfo().getLocation();
            if (!current.equals(precedent)) {
                // on a un nouveau point
                seed[i] = (byte) (current.x);
                i += 1;
                precedent = current;
            }
        } while (i < seed.length);
        return seed;
    }

    @Override
    public SecretKey generateKey() {
        try { 
               byte[] seed = generateSeedTrullyRandom();
               java.security.SecureRandom sr = java.security.SecureRandom.getInstance("SHA1PRNG");
               sr.setSeed(seed);
                javax.crypto.KeyGenerator kg = javax.crypto.KeyGenerator.getInstance(ICrypto.algo);
                kg.init(ICrypto.keysize, sr);
                SecretKey sk = kg.generateKey();
                return sk;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }

   

    @Override
    public String bytesToHex(byte[] tab) {
        // convertir un tableau de byte en chaine hexadécimale
        StringBuilder sb = new StringBuilder();
        for (byte b : tab) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public byte[] hextoBytes(String chaine) {
        int len = chaine.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(chaine.charAt(i), 16) << 4)
                    + Character.digit(chaine.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public boolean cipherProcess(SecretKey k, String inputFile, String outputFile, int mode, boolean deleteAfter) {
       
        try {
            // (lire + chiffrer) + écrire
            FileInputStream fis= new FileInputStream(inputFile);
            FileOutputStream fos= new FileOutputStream(outputFile);
            Cipher chiffreur=Cipher.getInstance(ICrypto.transform);
            chiffreur.init(mode, k, new IvParameterSpec(ICrypto.iv.getBytes()));

            CipherInputStream cis=new CipherInputStream(fis, chiffreur);

            // cis pour lire et fos pour écrire
            byte[] buffer=new byte[4096];
            int nbreBytesLus;
            while((nbreBytesLus=cis.read(buffer))!=-1){
                fos.write(buffer, 0, nbreBytesLus);
            }
            
            
            cis.close();
            fis.close();
            fos.close();    
            if(deleteAfter){
                java.io.File f= new java.io.File (inputFile);
                f.delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean cipherProcessFolder(SecretKey k, String inputFolder, String outputFolder, int mode,
            boolean deleteAfter) {
                try {
        File folder = new File(inputFolder);
        File destination = new File(outputFolder);
        
        // Créer le dossier de destination s'il n'existe pas
        if (!destination.exists()) {
            destination.mkdirs();
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    // Calcul du nom de fichier de sortie
                    String outputFileName;
                    if (mode == javax.crypto.Cipher.ENCRYPT_MODE) {
                        outputFileName = file.getName() + ".enc";
                    } else {
                        // Retire .enc s'il existe
                        outputFileName = file.getName().endsWith(".enc") 
                            ? file.getName().substring(0, file.getName().length() - 4) 
                            : file.getName();
                    }
                    
                    String outputPath = outputFolder + File.separator + outputFileName;
                    cipherProcess(k, file.getAbsolutePath(), outputPath, mode, deleteAfter);
                    
                } else if (file.isDirectory()) {
                    // RÉCURSIVITÉ : On appelle la même méthode pour le sous-dossier
                    String subFolderPath = outputFolder + File.separator + file.getName();
                    cipherProcessFolder(k, file.getAbsolutePath(), subFolderPath, mode, deleteAfter);
                }
            }
        }
        
        // Optionnel : supprimer le dossier source une fois vide
        if (deleteAfter) {
            folder.delete();
        }
        return true;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
    }

     @Override
    public SecretKey generatePBEKey(String password) {
        // TODO Auto-generated method stub
       try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(),ICrypto.salt,
            ICrypto.iteration, ICrypto.keysize);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(ICrypto.kdf);
            SecretKey k = keyFactory.generateSecret(pbeKeySpec);

            return new SecretKeySpec(k.getEncoded(), ICrypto.algo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public KeyPair generateKeyPair(byte[] seed) {
        try {
            KeyPairGenerator kg=KeyPairGenerator.getInstance(ICrypto.algoAsym);
            SecureRandom sc=new SecureRandom(seed);
            kg.initialize(ICrypto.keysizeAsym, sc);
            return kg.genKeyPair();
            
        } catch (Exception ex) {
            Logger.getLogger(CryptoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Key loadHexKey(String chemin, String password, int type) {
        // algo;typeKey;encoded en hex selon le type de la clé instanceof (PrivateKey,
        // PublicKey, SecretKey )

        try {
            FileInputStream fis = new FileInputStream(chemin);
            byte[] data = fis.readAllBytes();
            fis.close();
            String content = new String(data);
            String[] parts = content.split(";");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid key file format");
            }
            String algo = parts[0];
            String typeKey = parts[1];
            byte[] encodedKey = hextoBytes(parts[2]);
            KeyFactory keyFactory = KeyFactory.getInstance(algo);
            if (typeKey.equals("PrivateKey") && type == ICrypto.PRIVATE_KEY) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
                return keyFactory.generatePrivate(keySpec);
            } else if (typeKey.equals("PublicKey") && type == ICrypto.PUBLIC_KEY) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
                return keyFactory.generatePublic(keySpec);
            } else if (typeKey.equals("SecretKey") && type == ICrypto.SECRET_KEY) {
                return new javax.crypto.spec.SecretKeySpec(encodedKey, algo);
            } else {
                throw new IllegalArgumentException("Mismatched key type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean saveHexKey(Key k, String chemin, String password) {
        // algo;typeKey;encoded en hex selon le type de la clé instanceof (PrivateKey, PublicKey, SecretKey )
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(k.getAlgorithm());
            sb.append(";");
            if (k instanceof PrivateKey) {
                sb.append("PrivateKey");
            } else if (k instanceof PublicKey) {
                sb.append("PublicKey");
            } else if (k instanceof SecretKey) {
                sb.append("SecretKey");
            } else {
                throw new IllegalArgumentException("Unsupported key type");
            }
            sb.append(";");
            sb.append(bytesToHex(k.getEncoded()));
            // écrire dans le fichier
            FileOutputStream fos = new FileOutputStream(chemin);
            fos.write(sb.toString().getBytes());
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

   public boolean HybridEnCrypt(PublicKey k, String fileToencrypt, String encryptedFile) {
        try {
            SecretKey secretKey = generateKey();
            
            System.out.println("Secret Key:");
            System.out.println(bytesToHex(secretKey.getEncoded()));
            IvParameterSpec IvParam = new IvParameterSpec(iv.getBytes());
            System.out.println("IV:");
            System.out.println(bytesToHex(IvParam.getIV()));
            byte[] keypack = packKeyAndIv(secretKey, IvParam);
            Cipher pubCipher=Cipher.getInstance(algoAsym);
            pubCipher.init(Cipher.ENCRYPT_MODE, k);
            byte[] encryptedPack = pubCipher.doFinal(keypack);
            String encryptedPackHex = bytesToHex(encryptedPack);
            
            //chiffrement symetrique
            Cipher symCipher=Cipher.getInstance(transform);
            symCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParam);
            FileInputStream fis=new FileInputStream(fileToencrypt);
            
            System.out.println("TextZise:"+fis.available());
            CipherInputStream cis=new CipherInputStream(fis, symCipher);
            
            byte[] buffer = new byte[1024 * 1024];
            byte[] buffer1 = new byte[0];
            int nombrebytes = 0;
            while ((nombrebytes = cis.read(buffer)) != -1) {
                buffer1=concat(buffer1, buffer, nombrebytes);
            }
            
            String encryptedFileHex = bytesToHex(buffer1);
            System.out.println("Secret Message:");
            System.out.println(encryptedFileHex);
            
            FileOutputStream fos=new FileOutputStream(encryptedFile);
            PrintWriter pw=new PrintWriter(fos, true);
            pw.println("-----ENCRYPTED KEY-----");
            pw.println(encryptedPackHex);
            pw.println("-----END ENCRYPTED KEY-----");
            
            pw.println("-----ENCRYPTED MESSAGE-----");
            pw.println(encryptedFileHex);
            pw.println("-----END ENCRYPTED MESSAGE-----");
            
            fis.close();
            pw.close();
            fos.close();
            return true;
        } catch (Exception ex) {
            Logger.getLogger(CryptoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    @Override
    public boolean HybridDeCrypt(PrivateKey k, String fileTodecrypt, String decryptedFile) {
        try {
            //FileInputStream fis=new FileInputStream(fileTodecrypt);
            FileReader fr=new FileReader(fileTodecrypt);
            // comme cest du text on peut utiliser BufferedReader
            BufferedReader br=new BufferedReader(fr);
            br.readLine();//-----ENCRYPTED KEY-----
            String encryptedPackHex = br.readLine();
            br.readLine();//-----END ENCRYPTED KEY-----
            br.readLine();//-----ENCRYPTED MESSAGE-----
            String encryptedFileHex = br.readLine();
            System.out.println("Secret Message:");
            System.out.println(encryptedFileHex);
            br.close();
            fr.close();
            
            //dechiffrement de la cle et IV par la cle privee
            byte[] encryptedPack = hextoBytes(encryptedPackHex);
            Cipher pubCipher=Cipher.getInstance(algoAsym);
            pubCipher.init(Cipher.DECRYPT_MODE, k);
            byte[] keypack = pubCipher.doFinal(encryptedPack);
            Object[] keyAndIV = unpackKeyAndIV(keypack);
            SecretKeySpec keySym = (SecretKeySpec) keyAndIV[0];
            System.out.println("Secret Key:");
            System.out.println(bytesToHex(keySym.getEncoded()));
            IvParameterSpec ivParam=(IvParameterSpec) keyAndIV[1];
            System.out.println("IV:");
            System.out.println(bytesToHex(ivParam.getIV()));
            //System.out.println(new String(ivParam.getIV()));
            
            // dechiffrement du document
            byte[] encryptedFile = hextoBytes(encryptedFileHex);
            Cipher symCipher=Cipher.getInstance(transform);
            symCipher.init(Cipher.DECRYPT_MODE, keySym, ivParam);
            FileOutputStream fos=new FileOutputStream(decryptedFile);
            CipherOutputStream cos=new CipherOutputStream(fos, symCipher);
            cos.write(encryptedFile);
            cos.close();
            fos.close();
            
            
        } catch (Exception ex) {
            Logger.getLogger(CryptoImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return false;
    }
    
    private static byte[] packKeyAndIv(Key key, IvParameterSpec ivSpec) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        bOut.write(ivSpec.getIV());
        bOut.write(key.getEncoded());

        return bOut.toByteArray();
    }

    private static Object[] unpackKeyAndIV(byte[] data) {
        byte[] keyD = new byte[keysize / 8];
        byte[] iv = new byte[data.length - keyD.length];

        return new Object[]{
            new SecretKeySpec(data, iv.length, keyD.length, algo),
            new IvParameterSpec(data, 0, iv.length)
        };
    }
    private static byte[] concat(byte[] a, byte[] b, int nbrLu) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        bOut.write(a);
        bOut.write(b,0,nbrLu);

        return bOut.toByteArray();
    }

    @Override
    public byte[] processData(byte[] claire, SecretKey secretKey, int mode, IvParameterSpec ivParam) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processData'");
    }

    @Override
    public byte[] processData(byte[] claire, Key key, int mode, IvParameterSpec ivParam) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processData'");
    }

}