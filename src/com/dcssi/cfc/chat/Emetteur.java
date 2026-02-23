package com.dcssi.cfc.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.Socket;

import javax.crypto.Cipher;

import com.dcssi.cfc.crypto.ICrypto;

public class Emetteur extends Thread{
    private Cipher cipher = null;
    private Socket s = null;
    private ICrypto crypto;
    public Emetteur(ICrypto crypto, Cipher cipher, Socket s, String name){
        super(name);
        this.cipher = cipher;
        this.s = s;
        this.crypto = crypto;


    }


    @Override
    public void run() {
        // lire depuis le clavier, chiffrer  et envoyer au socket
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));

            while(true){
                String line = br.readLine();
                byte[] lineEnc = cipher.doFinal(line.getBytes());
                String lineHex = this.crypto.bytesToHex(lineEnc);
                bw.write(lineHex + "\n");

            }

        } catch (Exception e) {
            e.printStackTrace();
            // TODO: handle exception
        }

        
        
    }

}
