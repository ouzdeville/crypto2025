package com.dcssi.cfc.chat;

import com.dcssi.cfc.crypto.*;
import java.io.*;
import java.net.Socket;
import javax.swing.JOptionPane;

public class Recepteur extends Thread {

    private final Socket socket;
    private final ICrypto crypto = new CryptoImpl();
    private final Correspondant serveurCorr;
    private final ChatFrame fen;

    public Recepteur(Socket socket, String name,
            Correspondant serveurCorr, ChatFrame fen) {
        super(name);
        this.socket = socket;
        this.serveurCorr = serveurCorr;
        this.fen = fen;
        setDaemon(true); // s'arrête avec l'app
    }

    @Override
    public void run() {
        try (BufferedReader br
                = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String line;
            while ((line = br.readLine()) != null) {
                traiterLigne(line);
            }

        } catch (IOException e) {
            System.err.println("[Recepteur] Connexion perdue : " + e.getMessage());
        }
    }

    // ── Dispatch principal ────────────────────────────────────────────────────
    private void traiterLigne(String line) {
        System.out.println("[Recepteur] ← " + line);

        // ── Liste des clients ─────────────────────────────────────────────────
        if (line.startsWith("__CLIENTS__|")) {
            String listStr = line.substring("__CLIENTS__|".length());
            traiterListe(listStr);
            return;
        }

        // ── Messages serveur ignorés ──────────────────────────────────────────
        if (line.startsWith("OK|") || line.startsWith("ERROR|")) {
            return;
        }

        // ── Message utilisateur : "FROM|payload" ──────────────────────────────
        int sep = line.indexOf('|');
        if (sep == -1) {
            return;
        }

        String from = line.substring(0, sep);
        String payload = line.substring(sep + 1);

        traiterMessage(from, payload);
    }

    // ── Mise à jour de la liste des correspondants ────────────────────────────
    private void traiterListe(String listStr) {
        String[] entries = listStr.split(",");

        javax.swing.SwingUtilities.invokeLater(() -> {
            ChatFrame.correspondantList.clear();

            for (String entry : entries) {
                if (entry.isBlank()) {
                    continue;
                }

                String[] info = entry.split("\\|", -1);
                String id = info[0];
                if (id.equalsIgnoreCase(serveurCorr.mon_id)) {
                    continue;
                }

                // Chercher si le correspondant existe déjà (pour conserver PSK)
                Correspondant existing = findCorrespondant(id);

                if (existing == null) {
                    existing = new Correspondant();
                    existing.son_id = id;
                    existing.mon_id = serveurCorr.mon_id;
                    existing.nom = info.length > 1 ? info[1] : "";
                    existing.prenom = info.length > 2 ? info[2] : "";
                    existing.socket = this.socket;
                    existing.isServer = true;
                    // bw sera partagé via le writer du serveurCorr
                    existing.bw = serveurCorr.bw;
                }

                ChatFrame.correspondantList.add(existing);
            }

            fen.rafraichirTableCorrespondants();
        });
    }

    // ── Traitement d'un message entrant ───────────────────────────────────────
    private void traiterMessage(String from, String payload) {
        // -- Annonce de protocoles supportés par un pair (__BROADCAST__) --
        if (payload.startsWith("__PROTO__|")) {
            String protos = payload.substring("__PROTO__|".length());
            Correspondant c = findOrCreate(from);
            c.protocols.clear();
            for (String p : protos.split(",")) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    c.protocols.add(trimmed);
                }
            }
            updateCorrespondant(c);
            System.out.println("[PROTO] " + from + " supporte : " + c.protocols);
            // Rafraîchir la table pour afficher le badge protocoles
            javax.swing.SwingUtilities.invokeLater(() -> fen.rafraichirTableCorrespondants());
            return;
        }

        // -- Négociation PSK côté récepteur (Bob) --
        if (payload.startsWith("__NEGOCIER__|psk")) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                String pwd = JOptionPane.showInputDialog(
                        fen, "Mot de passe partagé avec " + from + " :");
                if (pwd == null || pwd.isBlank()) {
                    return;
                }

                Correspondant c = findOrCreate(from);
                c.initpsk(pwd);
                updateCorrespondant(c);

                try {
                    c.sendClearMessage(from, "__PSK_READY__");
                } catch (Exception e) {
                    System.err.println("[PSK] Erreur envoi PSK_READY : " + e.getMessage());
                }
            });
            return;
        }

        // -- Confirmation PSK côté initiateur (Alice) --
        if (payload.startsWith("__PSK_READY__")) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                String pwd = JOptionPane.showInputDialog(
                        fen, "Entrez le même mot de passe partagé avec " + from + " :");
                if (pwd == null || pwd.isBlank()) {
                    return;
                }

                Correspondant c = findOrCreate(from);
                c.initpsk(pwd);
                updateCorrespondant(c);
                System.out.println("[PSK] Établi avec " + from);
            });
            return;
        }

        if (payload.startsWith("__NEGOCIER__|enc")) {

            return;
        }

        if (payload.startsWith("__NEGOCIER__|dh")) {

            return;
        }
        if (payload.startsWith("__NEGOCIER__|dh+sign")) {

            return;
        }

        // -- Message chiffré --
        Correspondant c = findCorrespondant(from);
        if (c == null) {
            System.err.println("[Recepteur] Correspondant inconnu : " + from);
            return;
        }
        if (!c.isPskReady()) {
            System.err.println("[Recepteur] PSK non initialisé pour " + from + " — message ignoré.");
            return;
        }

        try {
            byte[] enc = crypto.hextoBytes(payload);
            String msg = new String(c.decryptor.doFinal(enc), "UTF-8");

            System.out.println("[" + from + "] " + msg);
            c.ajouterMessage(from + " > " + msg);

            final String affichage = from + " > " + msg;
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (fen.currentCorrespondant != null
                        && from.equals(fen.currentCorrespondant.son_id)) {
                    fen.ajouterMessageChat(affichage);
                }
            });

        } catch (Exception e) {
            System.err.println("[Recepteur] Déchiffrement échoué pour " + from
                    + " : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Correspondant findCorrespondant(String id) {
        for (Correspondant c : ChatFrame.correspondantList) {
            if (id.equals(c.son_id)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Retrouve ou crée un correspondant (pour les cas de négociation avant
     * liste).
     */
    private Correspondant findOrCreate(String id) {
        Correspondant c = findCorrespondant(id);
        if (c == null) {
            c = new Correspondant();
            c.son_id = id;
            c.mon_id = serveurCorr.mon_id;
            c.socket = this.socket;
            c.bw = serveurCorr.bw;
            c.isServer = true;
            ChatFrame.correspondantList.add(c);
        }
        return c;
    }

    private void updateCorrespondant(Correspondant updated) {
        for (int i = 0; i < ChatFrame.correspondantList.size(); i++) {
            if (updated.son_id.equals(ChatFrame.correspondantList.get(i).son_id)) {
                ChatFrame.correspondantList.set(i, updated);
                return;
            }
        }
    }
}
