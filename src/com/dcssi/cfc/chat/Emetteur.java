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
    private String password;
    private Correspondant correspondant;
    public Emetteur(Socket s, String name, String password, Correspondant correspondant) {
        super(name);
        this.s = s;
        this.password = password;
        this.correspondant = correspondant;

    }



    @Override
    public void run() {
        // Lire depuis le clavier, chiffrer et envoyer au socket
        try {
            
            Key key = crypto.generatePBEKey(password);
            Cipher cipher = Cipher.getInstance(ICrypto.transform);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(ICrypto.iv.getBytes()));
            
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            
            while (true) { 
                String line = br.readLine();//id : message

                
                if (line.equalsIgnoreCase("exit")) {
                    break;
                }
                String[] parts = line.split(":", 2);
                if (parts.length < 2) {
                    System.out.println("Format incorrect. Veuillez entrer 'id:message'");
                    continue;
                }
                String id = parts[0];
                String message = parts[1];
                if (!id.equals(correspondant.id)) {
                    System.out.println("ID du destinataire inconnu : " + id);
                    continue;
                }
                byte[] lineEnc = cipher.doFinal(message.getBytes());
                String lineHex = crypto.bytesToHex(lineEnc);
                bw.write(lineHex);
                bw.newLine();
                bw.flush();
            }
           
        }          
        catch (Exception e) {
            System.err.println("Erreur Emetteur : " + e.getMessage());
        }
        
        
}

}

