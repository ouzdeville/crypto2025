package com.dcssi.cfc.chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class PlateformServer {

    public static final List<Correspondant> correspondantList =
            Collections.synchronizedList(new ArrayList<>());

    private static final int PORT = 2026;

    public static void main(String[] args) {
        System.out.println("=== Serveur central démarré sur le port " + PORT + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[+] Nouveau client : " + socket.getInetAddress());
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[ERREUR] Serveur : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    private static class ClientHandler implements Runnable {

        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private Correspondant correspondant;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                // ── 1. Enregistrement : "id|nom|prenom"
                String line = in.readLine();
                if (line == null) return;

                String[] parts = line.split("\\|", -1);
                correspondant = new Correspondant();
                correspondant.son_id = parts[0];
                correspondant.nom    = parts.length > 1 ? parts[1] : "";
                correspondant.prenom = parts.length > 2 ? parts[2] : "";
                correspondant.socket = socket;
                correspondant.bw     = out;
                correspondant.isServer = true;

                correspondantList.add(correspondant);
                System.out.println("[+] Enregistré : " + correspondant.son_id);
                broadcastListe();

                // ── 2. Boucle de routage
                while ((line = in.readLine()) != null) {
                    int sep = line.indexOf('|');
                    if (sep == -1) continue;

                    String dest    = line.substring(0, sep);
                    String payload = line.substring(sep + 1);

                    System.out.printf("[%s → %s] %s%n",
                            correspondant.son_id, dest, payload);

                    if ("__LIST__".equals(dest)) {
                        broadcastListe();
                        continue;
                    }

                    // ── Broadcast général (ex: annonce de protocoles) ─────────
                    if ("__BROADCAST__".equals(dest)) {
                        // Préfixer avec l'émetteur et rediffuser à tous sauf lui
                        broadcastSauf(correspondant.son_id,
                                correspondant.son_id + "|" + payload);
                        continue;
                    }

                    router(dest, correspondant.son_id + "|" + payload);
                }

            } catch (IOException e) {
                System.out.println("[-] Déconnexion : " +
                        (correspondant != null ? correspondant.son_id : "inconnu"));
            } finally {
                if (correspondant != null) correspondantList.remove(correspondant);
                broadcastListe();
            }
        }

        /** Envoie une trame au destinataire identifié par son id. */
        private void router(String destId, String frame) {
            synchronized (correspondantList) {
                for (Correspondant c : correspondantList) {
                    if (c.son_id.equals(destId)) {
                        try { c.bw.write(frame); c.bw.newLine(); c.bw.flush(); }
                        catch (IOException ignored) {}
                        return;
                    }
                }
            }
            System.out.println("[!] Destinataire introuvable : " + destId);
        }

        /** Diffuse à tous sauf l'émetteur (pour __BROADCAST__). */
        private void broadcastSauf(String exceptId, String frame) {
            synchronized (correspondantList) {
                for (Correspondant c : correspondantList) {
                    if (c.son_id.equals(exceptId)) continue;
                    try { c.bw.write(frame); c.bw.newLine(); c.bw.flush(); }
                    catch (IOException ignored) {}
                }
            }
        }

        /** Diffuse la liste des clients connectés à tous. */
        private void broadcastListe() {
            StringBuilder sb = new StringBuilder("__CLIENTS__|");
            synchronized (correspondantList) {
                for (Correspondant c : correspondantList) {
                    sb.append(c).append(",");
                }
                String frame = sb.toString();
                for (Correspondant c : correspondantList) {
                    try {
                        c.bw.write(frame);
                        c.bw.newLine();
                        c.bw.flush();
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}