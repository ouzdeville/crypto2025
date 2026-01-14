package com.dcssi.cfc.chat;

import java.net.Socket;

import javax.crypto.Cipher;

import com.dcssi.cfc.crypto.ICrypto;

public class Recepteur extends Thread{
    private Cipher c = null;
    private Socket s = null;
    private ICrypto crypto;

    public Recepteur(Cipher c, Socket s, ICrypto crypto, String name){
        super(name);
        this.c = c;
        this.s = s;
        this.crypto = crypto;
    }
    @Override
    public void run() {
        // lire depuis le socket, déchiffrer et afficher
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(s.getInputStream()));
            while(true){
                String lineHex = br.readLine();
                byte[] lineEnc = this.crypto.hextoBytes(lineHex);
                byte[] lineDec = c.doFinal(lineEnc);
                String line = new String (lineDec);
                System.out.println(this.getName() + ">" + line);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Erreur de lecture du socket");
        }
    }
}