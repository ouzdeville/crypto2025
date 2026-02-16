/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package dcssi.cfc.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;
import java.io.IOException;
import java.util.Scanner;


class Correspondant {
    public String nom;
    public String prenom;
    public String id;
    public Socket socket;
    public SecretKey secretKey;
    public PublicKey publicKey;
    public Certificate certificate;
    public boolean isServer;
    private static  ICrypto crypto = new CryptoImpl();
    Cipher cipher = null;
    BufferedWriter bw = null;

    public void negocierCle() {
        // Implémenter la négociation de clé (ex: Diffie-Hellman, RSA,PSK, etc.)
        System.out.println("Donner votre mot de passe pour négocier la clé :");
        Scanner sc = new Scanner(System.in);
        String password = sc.nextLine();
        this.secretKey = crypto.generatePBEKey(password);
        System.out.println("Donner votre ID :");
        String idSource = sc.nextLine();
        System.out.println("Donner votre nom");
        String nomSource = sc.nextLine();
        if (this.isServer) {
            //Lire l'ID du client depuis le socket
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String idClient = br.readLine();
                this.id = idClient;
                //Envoyer l'ID du serveur au client
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                bw.write(idSource);
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture de l'ID du client : " + e.getMessage());
                this.id = idSource + "_S"; // fallback en cas d'erreur
            }

        } else {
            //Envoyer pui lire l'ID du serveur depuis le socket
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                bw.write(idSource);
                bw.newLine();
                bw.flush();
                BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String idServeur = br.readLine();
                this.id = idServeur;
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture de l'ID du serveur : " + e.getMessage());
                this.id = idSource + "_C"; // fallback en cas d'erreur
            }
        }
    }
    //constructeur, getters, setters, etc.
    public Correspondant(Socket socket, boolean isServer) throws Exception{
        cipher = Cipher.getInstance(ICrypto.transform);
        this.isServer = isServer;
        this.socket = socket;
        this.negocierCle();        
        cipher.init(Cipher.ENCRYPT_MODE,this.secretKey, new IvParameterSpec(ICrypto.iv.getBytes()));        
        bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
    }
    
    public void sendMessage(String message) throws Exception {
        // Implémenter l'envoi de message chiffré     
                byte[] lineEnc = cipher.doFinal(message.getBytes());
                String lineHex = crypto.bytesToHex(lineEnc);
                bw.write(lineHex);
                bw.newLine();
                bw.flush();
            }


}
