package com.dcssi.cfc.chat;

import com.dcssi.cfc.crypto.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.JFrame;

public class Recepteur extends Thread {
    private Cipher cipher = null;
    private Socket s = null;
    private ICrypto crypto = new CryptoImpl();
    private String password;
    private Correspondant correspondant;
    private NewMDIApplication fen;

    public Recepteur(Socket s, String name, String password, Correspondant correspondant, NewMDIApplication fen) {
        super(name);
        this.s = s;
        this.password = password;
        this.correspondant = correspondant;
        this.fen=fen;
    }

    @Override

    public void run() {
        // Lire depuis le socket, déchiffrer et afficher
        try {
            
            Key key = crypto.generatePBEKey(password);
            Cipher cipher = Cipher.getInstance(ICrypto.transform);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ICrypto.iv.getBytes()));
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            while (true) { 
                String lineHex = br.readLine();
                if (lineHex == null) {
                    break; // Fin de la connexion
                }

                

                try {
                    byte[] lineEnc = crypto.hextoBytes(lineHex);
                    String line = new String(cipher.doFinal(lineEnc));
                    System.out.println(this.getName() + "Message reçu : " + line);
                    this.correspondant.ajouterMessage(this.correspondant.id+">"+line);
                    if(this.correspondant.id.equals(this.fen.currentCorrespondant.id)){
                        this.fen.ajouterMessage(this.correspondant.id+">"+line);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur de déchiffrement (Mauvais mot de passe ou IV incorrect) : " + e.getMessage());
                }
                
            }
        } catch (Exception e) {
            System.err.println("Erreur Recepteur : " + e.getMessage());
        }
    }
    
}