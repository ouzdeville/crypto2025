package dcssi.cfc.chat;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Demarrer {

    public static List<Correspondant> correspondantsList = new ArrayList<>();                                                                                                       

    public static void main(String[] args) throws Exception {
        Socket s = null;
        try {
            s = new Socket("127.0.0.1", 2026);

            System.out.println("Serveur trouver !");
            //correspondantsList.add(new Correspondant(s, false));
            ICrypto crypto = new CryptoImpl();
            //Cipher cipher = Cipher.getInstance(null);
            System.out.println("Taper votre mot de passe");
            Scanner sc = new Scanner(System.in);
            String mdp = sc.nextLine();
            new Emetteur(s, "AD", mdp, new Correspondant(s, false)).start();
            new Recepteur(s, "AD", mdp, new Fenetre()).start();

        } catch (IOException e) {
            try {
                ServerSocket SS = new ServerSocket(2026);
                System.out.println("Serveur démarrer!");
                while (true) {
                    s = SS.accept();
                    //correspondantsList.add(new Correspondant(s, true));
                    ICrypto crypto = new CryptoImpl();
                    //Cipher cipher = Cipher.getInstance(null);
                    System.out.println("Taper votre mot de passe");
                    Scanner sc = new Scanner(System.in);
                    String mdp = sc.nextLine();
                    Correspondant correspondant = new Correspondant(s, true);
                    new Emetteur(s, "AD", mdp, correspondant).start();
                    new Recepteur(s, "AD", mdp, new Fenetre()).start();
                }
                //System.out.println("Client trouver !"); 

            } catch (Exception ex) {
                System.getLogger(Demarrer.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }

        }

    }

}
