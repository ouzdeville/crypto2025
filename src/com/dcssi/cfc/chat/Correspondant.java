package com.dcssi.cfc.chat;

import com.dcssi.cfc.crypto.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;

public class Correspondant {

    public String nom;
    public String prenom;
    public String mon_id;
    public String son_id;
    public Socket socket;
    public SecretKey secretKey;
    public PublicKey publicKey;
    public Certificate certificate;
    public boolean isServer;
    public String password;
    public List<String> messages  = new ArrayList<>();
    /** Protocoles annoncés par ce correspondant (ex: psk, dh, dh_signe, kem). */
    public java.util.Set<String> protocols = new java.util.LinkedHashSet<>();
    /** Paire de clés RSA de cet utilisateur (null si pas encore chargée/générée). */
    private KeyPair keyPair = null;

    public KeyPair getKeyPair()          { return keyPair; }
    public void    setKeyPair(KeyPair kp){ this.keyPair = kp; }

    // ── Chiffrement ──────────────────────────────────────────────────────────
    private static final ICrypto crypto = new CryptoImpl();
    public Cipher encryptor = null;
    public Cipher decryptor = null;

    /** Writer partagé pour toute communication avec ce correspondant. */
    public BufferedWriter bw = null;

    // ── Constructeurs ────────────────────────────────────────────────────────

    public Correspondant() {}

    public Correspondant(String mon_id, String son_id, Socket socket,
                         boolean isServer, String password) throws Exception {
        this.mon_id   = mon_id;
        this.son_id   = son_id;
        this.socket   = socket;
        this.isServer = isServer;
        this.password = password;
        // bw sera initialisé après connexion (joinServeur)
    }

    // ── Envoi de messages ────────────────────────────────────────────────────

    /**
     * Envoie un message chiffré.  Lance une exception explicite si PSK
     * n'est pas encore initialisé.
     */
    public void sendMessage(String destId, String message) throws Exception {
        if (encryptor == null || bw == null) {
            throw new IllegalStateException(
                "PSK non initialisé avec " + son_id + " — négociez d'abord la clé.");
        }
        byte[] enc = encryptor.doFinal(message.getBytes("UTF-8"));
        String payload = crypto.bytesToHex(enc);
        bw.write(destId + "|" + payload);
        bw.newLine();
        bw.flush();
    }

    /**
     * Envoie un message en clair via le BW partagé.
     * FIX : n'ouvre plus un nouveau flux — utilise this.bw.
     */
    public void sendClearMessage(String destId, String message) throws Exception {
        if (bw == null) {
            // Initialisation paresseuse du writer si nécessaire
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        }
        bw.write(destId + "|" + message);
        bw.newLine();
        bw.flush();
    }

    /** Demande la liste des clients au serveur. */
    public void demanderListeClients() throws Exception {
        sendClearMessage("__LIST__", "");
    }

    // ── Initialisation PSK ───────────────────────────────────────────────────

    /**
     * Initialise le chiffrement symétrique par mot de passe partagé.
     * Appelé des deux côtés une fois le mot de passe échangé hors-bande.
     */
    public void initpsk(String pwd) {
        try {
            this.password  = pwd;
            this.secretKey = crypto.generatePBEKey(pwd);

            encryptor = Cipher.getInstance(ICrypto.transform);
            decryptor = Cipher.getInstance(ICrypto.transform);
            IvParameterSpec ivSpec = new IvParameterSpec(ICrypto.iv.getBytes("UTF-8"));
            encryptor.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            decryptor.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            // Assurer que le writer est prêt
            if (bw == null) {
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            }
            System.out.println("[PSK] Clé établie avec " + son_id);

        } catch (Exception ex) {
            Logger.getLogger(Correspondant.class.getName())
                  .log(Level.SEVERE, "Erreur initpsk", ex);
        }
    }

    /** Indique si la session est prête à chiffrer/déchiffrer. */
    public boolean isPskReady() {
        return encryptor != null && decryptor != null;
    }

    /**
     * Initialise le chiffrement AES avec une clé fournie directement
     * (issue d'un échange DH, KEM, etc.)
     */
    public void initAesKey(javax.crypto.SecretKey key) {
        try {
            this.secretKey = key;
            encryptor = Cipher.getInstance(ICrypto.transform);
            decryptor = Cipher.getInstance(ICrypto.transform);
            IvParameterSpec ivSpec = new IvParameterSpec(ICrypto.iv.getBytes("UTF-8"));
            encryptor.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            decryptor.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            if (bw == null)
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            System.out.println("[AES] Clé établie avec " + son_id);
        } catch (Exception ex) {
            Logger.getLogger(Correspondant.class.getName()).log(Level.SEVERE, "initAesKey", ex);
        }
    }

    // ── Historique ───────────────────────────────────────────────────────────

    public void ajouterMessage(String msg) {
        messages.add(msg);
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return son_id + "|" + (nom != null ? nom : "") + "|" + (prenom != null ? prenom : "");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Correspondant)) return false;
        Correspondant cor = (Correspondant) obj;
        return Objects.equals(this.son_id, cor.son_id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(son_id);
    }
}