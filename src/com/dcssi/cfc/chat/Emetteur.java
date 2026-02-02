package dcssi.cfc.chat;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

public class Emetteur extends Thread {
    private Socket s = null;
    private ICrypto crypto = new CryptoImpl();
    public Emetteur(Socket s, String name) {
        super(name);
        this.s = s;
    }


    @Override
    public void run() {
        // Lire depuis le clavier, chiffrer et envoyer au socket
        try {
            Cipher cipher = Cipher.getInstance(ICrypto.transform);
            Key key = crypto.generatePBEKey("INGENIEUR");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ICrypto.iv.getBytes()));
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
            while (true) { 
                String line = br.readLine();
                byte[] lineEnc = cipher.doFinal(line.getBytes());
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

