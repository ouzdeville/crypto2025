package dcssi.cfc.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.Buffer;
import javax.crypto.Cipher;

import dcssi.cfc.crypto.ICrypto;

public class Emetteur extends Thread {
    private Cipher cipher = null;
    private Socket s = null;
    private ICrypto crypto;
    public Emetteur(Cipher cipher, Socket s, String name) {
        super(name);
        this.s = s;
        this.cipher = cipher;
    }


    @Override
    public void run() {
        // Lire depuis le clavier, chiffrer et envoyer au socket
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
            while (true) { 
                String line = br.readLine();
                lineEnc = cipher.doFinal(line.getBytes());
                String lineHex = crypto.bytesToHex(lineEnc);
                bw.write(lineHex);
                bw.newLine();
                bw.flush();
            }
           
        }
            
        catch (Exception e) {
            e.printStackTrace();
        }
        
        
}

}

