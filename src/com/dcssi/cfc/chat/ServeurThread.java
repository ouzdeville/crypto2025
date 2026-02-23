/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dcssi.cfc.chat;

import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

/**
 *
 * @author ousmane3ndiaye
 */
class ServeurThread extends Thread {

    private ServerSocket ss;
    private String login;
    private NewMDIApplication fen;

    public ServeurThread(ServerSocket ss, String login, NewMDIApplication fen) {
        this.ss = ss;
        this.login = login;
        this.fen=fen;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket s = ss.accept();
                System.out.println("Client connecté");

                JPasswordField passwordField = new JPasswordField();

                int passOption = JOptionPane.showConfirmDialog(
                        null,
                        passwordField,
                        "Entrez votre mot de passe",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE
                );

                if (passOption == JOptionPane.OK_OPTION) {
                    char[] pwd = passwordField.getPassword();
                    String password = new String(pwd);

                    Correspondant correspondant = new Correspondant(login, s, true, password);
                    NewMDIApplication.addCorrespondant(correspondant);
                    new Recepteur(s, correspondant.id, correspondant.getPassword(), correspondant,this.fen).start();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
