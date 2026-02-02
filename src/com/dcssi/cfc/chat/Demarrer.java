package dcssi.cfc.chat;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Demarrer {
    public static void main(String[] args) {
        Socket s = null;
        try {
            s = new Socket("172.16.0.45", 2026);
            System.out.println("Serveur trouver !");

        } catch (IOException e) {
            try {
                ServerSocket SS = new ServerSocket(2026);
                System.out.println("Serveur démarrer!");
                s = SS.accept();
                System.out.println("Client trouver !");

            } catch (Exception ex) {
                System.getLogger(Demarrer.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }

        }
        ICrypto crypto = new CryptoImpl();
            //Cipher cipher = Cipher.getInstance(null);
            new Emetteur(s, "AD").start();
            new Recepteur(s, "AD").start();
    }

}
