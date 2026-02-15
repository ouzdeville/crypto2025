package dcssi.cfc.chat;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

public class Recepteur extends Thread {
    private Cipher cipher = null;
    private Socket s = null;
    private ICrypto crypto = new CryptoImpl();
    private String password;

    public Recepteur(Socket s, String name, String password) {
        super(name);
        this.s = s;
        this.password = password;
    }

    @Override
    public void run() {
        // Lire depuis le socket, déchiffrer et afficher
        try {
            Cipher cipher = Cipher.getInstance(ICrypto.transform);
            Key key = crypto.generatePBEKey(password);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ICrypto.iv.getBytes()));
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while (true) { 
                String lineHex = br.readLine();
                byte[] lineEnc = crypto.hextoBytes(lineHex);
                String line = new String(cipher.doFinal(lineEnc));
                System.out.println(this.getName() + "Message reçu : " + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}