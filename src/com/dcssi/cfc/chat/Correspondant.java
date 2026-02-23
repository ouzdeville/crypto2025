/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dcssi.cfc.chat;

import com.dcssi.cfc.crypto.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.security.PublicKey;
import java.security.cert.Certificate;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

class Correspondant {

    public String nom;
    public String prenom;
    public String id;
    public String mon_id;
    public Socket socket;
    public SecretKey secretKey;
    public PublicKey publicKey;
    public Certificate certificate;
    public boolean isServer;
    private static ICrypto crypto = new CryptoImpl();
    Cipher cipher = null;
    BufferedWriter bw = null;
    private String password;
    public List<String> messages = new ArrayList<>();

    // constructeur, getters, setters, etc.
    public Correspondant(String mon_id, Socket socket, boolean isServer, String password) throws Exception {
        cipher = Cipher.getInstance(ICrypto.transform);
        this.isServer = isServer;
        this.socket = socket;
        this.mon_id = mon_id;
        this.password = password;
        this.negocierCle();

    }

    public void sendMessage(String message) throws Exception {
        // Implémenter l'envoi de message chiffré
        byte[] lineEnc = cipher.doFinal(message.getBytes());
        String lineHex = crypto.bytesToHex(lineEnc);
        bw.write(lineHex);
        bw.newLine();
        bw.flush();
    }

    /**
     * Negocier la cle synetrique a utiliser plusieurs options a implementer 1-
     * Cle partagee 2- Diffie-Hellman 3- Chiffrement Asymetrique 4- Signature
     * seule 5- Diffie-Hellman signee
     *
     */
    public void negocierCle() throws InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        // Implémenter la négociation de clé (ex: Diffie-Hellman, RSA,PSK, etc.)
        // System.out.println("Donner votre mot de passe pour négocier la clé :");
        // sc = new Scanner(System.in);
        // String password = sc.nextLine();
        String protocole = "psk";

        if (this.isServer) {
            // Lire l'ID du client depuis le socket
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String idClient = br.readLine();
                this.id = idClient;
                protocole = br.readLine();
                
                this.secretKey = crypto.generatePBEKey(password);
                cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new IvParameterSpec(ICrypto.iv.getBytes()));
                this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));

                // Envoyer l'ID du serveur au client
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                bw.write(this.mon_id);
                bw.newLine();
                bw.flush();

                

            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture de l'ID du client : " + e.getMessage());
                // this.mon_id = this.mon_id + "_S"; // fallback en cas d'erreur
            }

        } else {
            // Envoyer pui lire l'ID du serveur depuis le socket
            try {
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                bw.write(this.mon_id);
                bw.newLine();
                bw.flush();
                protocole = "psk";
                bw.write(protocole);
                bw.newLine();
                bw.flush();
                BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String idServeur = br.readLine();
                this.id = idServeur;
            } catch (IOException e) {
                System.err.println("Erreur lors de la lecture de l'ID du serveur : " + e.getMessage());
                // this.id = idSource + "_C"; // fallback en cas d'erreur
            }
        }
        
        switch (protocole) {
                    case "psk":
                        this.secretKey = crypto.generatePBEKey(password);
                        cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new IvParameterSpec(ICrypto.iv.getBytes()));
                        this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                        break;
                    case "enc":
                        break;
                    case "dh":
                        break;
                    case "signature":
                        break;
                    case "dh_signe":
                        break;

                    default:
                        this.secretKey = crypto.generatePBEKey(password);
                        cipher.init(Cipher.ENCRYPT_MODE, this.secretKey, new IvParameterSpec(ICrypto.iv.getBytes()));
                        this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                        break;
                }

    }

    public String getPassword() {
        return password;
    }

    void ajouterMessage(String msg) {
        this.messages.add(msg);
    }

    @Override
    public String toString() {
        return id;
    }

}
