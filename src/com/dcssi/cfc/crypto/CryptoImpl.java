package com.dcssi.cfc.crypto;

import java.security.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.crypto.*;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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
            FileInputStream fis = new FileInputStream(inputFile);
            FileOutputStream fos = new FileOutputStream(outputFile);
            Cipher chiffreur = Cipher.getInstance(ICrypto.transform);
            chiffreur.init(mode, k, new IvParameterSpec(ICrypto.iv.getBytes()));

            CipherInputStream cis = new CipherInputStream(fis, chiffreur);

            // cis pour lire et fos pour écrire
            byte[] buffer = new byte[4096];
            int nbreBytesLus;
            while ((nbreBytesLus = cis.read(buffer)) != -1) {
                fos.write(buffer, 0, nbreBytesLus);
            }
            cis.close();
            fis.close();
            fos.close();
            if (deleteAfter) {
                java.io.File f = new java.io.File(inputFile);
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
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        // on chiffre
                        String outputFile = "";
                        if (mode == Cipher.ENCRYPT_MODE)
                            outputFile = outputFolder + "/" + file.getName() + ".enc";
                        else if (mode == Cipher.DECRYPT_MODE)
                            outputFile = outputFolder + "/" +file.getName().substring(0, file.getName().length()-4);
                        if(mode == Cipher.DECRYPT_MODE && !file.getName().endsWith(".enc")) continue;
                        cipherProcess(k, file.getAbsolutePath(), outputFile, mode, deleteAfter);

                    } else if (file.isDirectory()) {
                        String outputSubFolder = outputFolder + "/" + file.getName();
                        cipherProcessFolder(k, file.getAbsolutePath(), outputSubFolder, mode, deleteAfter);

                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
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
        // TODO: handle exception
       }
        return null;
    }

    @Override
    public KeyPair generateKeyPair(byte[] seed) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'generateKeyPair'");
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
            byte[] encoded = hextoBytes(parts[2]);
            
            if (typeKey.equals("PrivateKey") && type == ICrypto.PRIVATE_KEY) {
                KeyFactory kf = KeyFactory.getInstance(algo);
                return kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(encoded));
            } else if (typeKey.equals("PublicKey") && type == ICrypto.PUBLIC_KEY) {
                KeyFactory kf = KeyFactory.getInstance(algo);
                return kf.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
            } else if (typeKey.equals("SecretKey") && type == ICrypto.SECRET_KEY) {

                return new javax.crypto.spec.SecretKeySpec(encoded, algo);
            } else {
                throw new IllegalArgumentException("Key type mismatch or unsupported key type");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean saveHexKey(Key k, String chemin, String password) {
        // algo;typeKey;encoded en hex selon le type de la clé instanceof (PrivateKey,
        // PublicKey, SecretKey )
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

    @Override
    public boolean HybridEnCrypt(PublicKey k, String fileToencrypt, String encryptedFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'HybridEnCrypt'");
    }

    @Override
    public boolean HybridDeCrypt(PrivateKey k, String fileToencrypt, String encreptedFile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'HybridDeCrypt'");
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
