package dcssi.cfc.chat;

import java.io.BufferedReader;
import java.net.Socket;

import javax.crypto.Cipher;

public class Recepteur extends Thread {
    private Cipher cipher = null;
    private Socket s = null;

    public Recepteur(Cipher cipher, Socket s, String name) {
        super(name);
        this.s = s;
        this.cipher = cipher;
    }

    @Override
    public void run() {
        // Lire depuis le socket, déchiffrer et afficher
        try {
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(s.getInputStream()));
            String lineHex = br.readLine();
            byte[] lineEnc = crypto.hexToBytes(lineHex);
            String line = new String(cipher.doFinal(lineEnc));
            System.out.println(this.getName() + "Message reçu : " + line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}