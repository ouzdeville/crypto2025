package com.dcssi.cfc.chat;

import com.dcssi.cfc.crypto.ICrypto;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;

/**
 * Gère les 4 protocoles de négociation de clé symétrique :
 *
 *   PSK       — mot de passe partagé hors-bande
 *   DH        — Diffie-Hellman (MODP-2048), dérivation HKDF-SHA256
 *   KEM       — RSA-OAEP : l'initiateur génère AES256, le chiffre avec la clé pub du pair
 *   DH_SIGNE  — DH + signature RSA de la clé publique DH (authentification mutuelle)
 *
 * Format des trames de négociation :
 *   __NEG__|<protocole>|<phase>|<données_base64_ou_hex>
 *
 * Côté initiateur : sendInit()  → attend la réponse dans handleFrame()
 * Côté répondeur  : handleFrame() traite la première trame et répond
 */
public class ProtocolNegotiator {

    // ── Paramètres DH MODP-2048 (RFC 3526) ────────────────────────────────────
    private static final BigInteger DH_P = new BigInteger(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
        "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
        "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
        "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
        "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
        "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
        "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
        "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
        "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
        "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    private static final BigInteger DH_G = BigInteger.valueOf(2);

    // ── Constantes protocoles ─────────────────────────────────────────────────
    public static final String PSK      = "psk";
    public static final String DH       = "dh";
    public static final String KEM      = "kem";
    public static final String DH_SIGNE = "dh_signe";

    // ── État interne (une instance par session de négociation) ─────────────────
    private final Correspondant local;   // moi (avec ma paire RSA)
    private final Correspondant remote;  // l'autre
    private final boolean       initiateurRole; // true = j'ai lancé la négo

    // Valeurs temporaires DH
    private BigInteger dhPrivate;
    private BigInteger dhPublic;

    // Callback appelé quand la clé est établie
    private final OnKeyEstablished onEstablished;

    public interface OnKeyEstablished {
        void onKey(String protocol, SecretKey key);
    }

    // ─────────────────────────────────────────────────────────────────────────
    public ProtocolNegotiator(Correspondant local, Correspondant remote,
                              boolean initiateur, OnKeyEstablished cb) {
        this.local          = local;
        this.remote         = remote;
        this.initiateurRole = initiateur;
        this.onEstablished  = cb;
    }

    // ── Point d'entrée : côté INITIATEUR ─────────────────────────────────────

    /**
     * Lance la négociation côté initiateur (Alice).
     * Envoie la première trame et attend la réponse dans handleFrame().
     */
    public void sendInit(String protocol, JFrame parent) throws Exception {
        switch (protocol) {
            case PSK:
                initPsk(parent);
                break;
            case DH:   
                initDH(false);
                break;
            case KEM:    
                initKEM();
                break;
            case DH_SIGNE: 
                initDH(true);
                break;
            default:
                throw new IllegalArgumentException("Protocole inconnu : " + protocol);
        }
    }

    // ── Point d'entrée : RÉCEPTION d'une trame de négociation ─────────────────

    /**
     * Appelé par Recepteur quand il reçoit une trame __NEG__|...
     * Gère toutes les phases pour les deux rôles.
     *
     * @return true si la négociation est terminée (clé établie)
     */
    public boolean handleFrame(String frame, JFrame parent) throws Exception {
        // __NEG__|protocole|phase|data
        String[] parts = frame.split("\\|", 4);
        if (parts.length < 3 || !parts[0].equals("__NEG__")) 
            return false;

        String proto = parts[1];
        String phase = parts[2];
        String data  = parts.length > 3 ? parts[3] : "";

        return switch (proto) {
            case PSK      -> handlePsk(phase, data, parent);
            case DH       -> handleDH(phase, data, false);
            case KEM      -> handleKEM(phase, data);
            case DH_SIGNE -> handleDH(phase, data, true);
            default -> false;
        };
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PSK
    // ═════════════════════════════════════════════════════════════════════════

    /** Alice envoie la demande PSK */
    private void initPsk(JFrame parent) throws Exception {
        String pwd = askPassword(parent,
                "Mot de passe partagé avec " + remote.son_id + " :");
        if (pwd == null || pwd.isBlank())
            throw new CancelledException("Négociation PSK annulée");

        // Initialiser localement
        SecretKey key = deriveFromPassword(pwd);
        remote.initpsk(pwd);

        // Envoyer la demande (sans le mdp bien sûr)
        sendNeg(PSK, "INIT", "");

        // PSK : la clé locale est déjà prête, on attend juste la confirmation
        onEstablished.onKey(PSK, key);
    }

    /** Bob reçoit la demande PSK et répond */
    private boolean handlePsk(String phase, String data, JFrame parent) throws Exception {
        if ("INIT".equals(phase) && !initiateurRole) {
            String pwd = askPassword(parent,
                    "Mot de passe partagé avec " + remote.son_id + " :");
            if (pwd == null || pwd.isBlank()) return false;

            SecretKey key = deriveFromPassword(pwd);
            remote.initpsk(pwd);
            sendNeg(PSK, "ACK", "");
            onEstablished.onKey(PSK, key);
            return true;
        }
        if ("ACK".equals(phase) && initiateurRole) {
            // Alice reçoit l'ACK → clé déjà prête, rien à faire
            System.out.println("[PSK] Clé confirmée avec " + remote.son_id);
            return true;
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DH (avec ou sans signature)
    // ═════════════════════════════════════════════════════════════════════════

    /** Alice génère sa clé DH et l'envoie */
    private void initDH(boolean signed) throws Exception {
        generateDHKeyPair();
        String pubHex = dhPublic.toString(16);

        if (signed) {
            // on veut Signer la clé DH publique avec la clé RSA privée
            
            // recuperer la cle prive de local 
            String sig = signHex(pubHex, local.getKeyPair().getPrivate());
            sendNeg(DH_SIGNE, "INIT", pubHex + ":" + sig);
        } else {
            sendNeg(DH, "INIT", pubHex);
        }
    }

    /** Bob reçoit la clé DH d'Alice, calcule le secret, envoie sa clé */
    private boolean handleDH(String phase, String data, boolean signed) throws Exception {
        System.out.println("handleDH");
        if ("INIT".equals(phase) && !initiateurRole) {
            // Extraire et vérifier
            String alicePubHex;
            if (signed) {
                // on recupere la cle publique DH et sa signature
                String[] parts = data.split(":", 2);
                alicePubHex    = parts[0];
                String sig     = parts[1];
                //q1 recupere la cle publique de this.remote
                //q2 appeler la methode verifyHex si faux lancer une exception
                boolean isGood = verifyHex(alicePubHex, sig, this.remote.publicKey);
                if(isGood) 
                System.out.println("[DH+Sign] Signature d'Alice vérifiée ✓");
                else  throw new UnsupportedOperationException("Signature non valide");
            } else {
                alicePubHex = data;
            }

            BigInteger alicePub = new BigInteger(alicePubHex, 16);

            // Générer la paire DH de Bob
            generateDHKeyPair();
            BigInteger shared = alicePub.modPow(dhPrivate, DH_P);
            SecretKey  aesKey = hkdf(shared.toByteArray(), 32);

            // Envoyer la clé DH de Bob (éventuellement signée)
            String bobPubHex = dhPublic.toString(16);
            if (signed && local.getKeyPair() != null) {
                String sig = signHex(bobPubHex, local.getKeyPair().getPrivate());
                sendNeg(DH_SIGNE, "RESP", bobPubHex + ":" + sig);
                
            } else {
                sendNeg(DH, "RESP", bobPubHex);
            }

            onEstablished.onKey(signed ? DH_SIGNE : DH, aesKey);
            return true;
        }

        if ("RESP".equals(phase) && initiateurRole) {
            // Alice reçoit la clé DH de Bob
            String bobPubHex;
            if (signed) {
                // on recupere la cle publique DH et sa signature
                String[] parts = data.split(":", 2);
                bobPubHex  = parts[0];
                String sig = parts[1];
                //q1 recupere la cle publique de this.remote
                //q2 appeler la methode verifyHex si faux lancer une exception
                //q3 System.out.println("[DH+Sign] Signature de Bob vérifiée ✓");
                 boolean isGood = verifyHex(bobPubHex, sig, this.remote.publicKey);
                if(isGood) 
                System.out.println("[DH+Sign] Signature de Bob vérifiée ✓");
                else  throw new UnsupportedOperationException("Signature de Bob non valide");
            } else {
                bobPubHex = data;
            }

            BigInteger bobPub = new BigInteger(bobPubHex, 16);
            BigInteger shared = bobPub.modPow(dhPrivate, DH_P);
            SecretKey  aesKey = hkdf(shared.toByteArray(), 32);

            onEstablished.onKey(signed ? DH_SIGNE : DH, aesKey);
            return true;
        }

        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // KEM (RSA-OAEP)
    // ═════════════════════════════════════════════════════════════════════════

    /** Alice génère une clé AES256, la chiffre avec la clé pub RSA de Bob, l'envoie */
    private void initKEM() throws Exception {
        // recuperer la cle public de remote 
        // genrenerer une cle AES 
        // chiffrer cette cle avec la cle publique
        // emvoie de la cle chiffree sendNeg(KEM, "INIT", Base64.getEncoder().encodeToString(wrapped));
        // callback de la cle onEstablished.onKey(KEM, aesKey);
        
        
    }

    /** Bob reçoit la clé AES chiffrée, la déchiffre avec sa clé privée */
    private boolean handleKEM(String phase, String data) throws Exception {
        // si INIT "INIT".equals(phase) && !initiateurRole
        // recupere la cle publique de this.local
        // decode data Base64.getDecoder().decode(data);
        // dechiffre avec ma cle privee
        //SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");
        //sendNeg(KEM, "ACK", "");
        //onEstablished.onKey(KEM, aesKey); et return true
        // si ("ACK".equals(phase) && initiateurRole)
        // alors System.out.println("[KEM] Bob a bien déchiffré la clé"); et return true
        // return false
        
        return false;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers crypto
    // ═════════════════════════════════════════════════════════════════════════

    private void generateDHKeyPair() {
        SecureRandom rng = new SecureRandom();
        // Exposant privé de 256 bits (suffisant pour MODP-2048)
        dhPrivate = new BigInteger(256, rng);
        dhPublic  = DH_G.modPow(dhPrivate, DH_P);
    }

    /**
     * HKDF-SHA256 simplifié (extract + expand) pour dériver une clé AES.
     * @param ikm  Input Key Material (secret DH)
     * @param len  Longueur désirée en octets (16 = AES128, 32 = AES256)
     */
    private static SecretKey hkdf(byte[] ikm, int len) throws Exception {
        // Extract
        Mac hmac = Mac.getInstance("HmacSHA256");
        byte[] salt = new byte[32]; // sel nul => HKDF extract
        hmac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = hmac.doFinal(ikm);

        // Expand (une seule itération suffit pour 32 octets)
        hmac.init(new SecretKeySpec(prk, "HmacSHA256"));
        hmac.update(new byte[]{0x01});  // info vide, counter = 1
        byte[] okm = Arrays.copyOf(hmac.doFinal(), len);

        return new SecretKeySpec(okm, "AES");
    }

    private static SecretKey deriveFromPassword(String pwd) throws Exception {
        // PBKDF2-SHA256 : compatible avec ICrypto.generatePBEKey si tu veux
        javax.crypto.SecretKeyFactory f = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec(pwd.toCharArray(),
                "cfc_chat_salt".getBytes(), 65536, 256);
        byte[] raw = f.generateSecret(spec).getEncoded();
        return new SecretKeySpec(raw, "AES");
    }

    /** Signe une chaîne hex avec RSA-SHA256. */
    private static String signHex(String hex, PrivateKey priv) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(priv);
        sig.update(hex.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    /** Vérifie une signature RSA-SHA256. */
    private static boolean verifyHex(String hex, String sigB64, PublicKey pub) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(pub);
        sig.update(hex.getBytes("UTF-8"));
        return sig.verify(Base64.getDecoder().decode(sigB64));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers UI & réseau
    // ═════════════════════════════════════════════════════════════════════════

    private void sendNeg(String proto, String phase, String data) throws Exception {
        String frame = "__NEG__|" + proto + "|" + phase + "|" + data;
        System.out.println(frame);
        remote.sendClearMessage(remote.son_id, frame);
    }

    private static String askPassword(JFrame parent, String prompt) {
        JPasswordField pf = new JPasswordField(20);
        int res = JOptionPane.showConfirmDialog(parent,
                new Object[]{prompt, pf}, "Négociation PSK",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return null;
        return new String(pf.getPassword());
    }

    // Exception interne pour annulation user
    public static class CancelledException extends RuntimeException {
        public CancelledException(String msg) { super(msg); }
    }
}