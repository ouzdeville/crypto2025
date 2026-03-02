package com.dcssi.cfc.chat;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.interfaces.ECKey;
import java.security.spec.*;
import java.util.*;
import java.util.Base64;

/**
 * Panneau "Mes Clés" :
 *  - Chargement clé publique et clé privée séparément (boutons dédiés)
 *  - Génération RSA 2048
 *  - Affichage algorithme + taille de clé
 *  - Sauvegarde sur disque (deux fichiers PEM)
 *  - Cases à cocher protocoles → annoncés au serveur via __BROADCAST__
 */
public class KeyPanel extends JDialog {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_DARK     = new Color(28, 30, 36);
    private static final Color BG_PANEL    = new Color(38, 41, 50);
    private static final Color BG_INPUT    = new Color(48, 52, 64);
    private static final Color BG_CARD     = new Color(44, 47, 58);
    private static final Color ACCENT      = new Color(99, 179, 237);
    private static final Color ACCENT_DARK = new Color(66, 135, 200);
    private static final Color ACCENT_OK   = new Color(72, 199, 116);
    private static final Color ACCENT_WARN = new Color(240, 180, 60);
    private static final Color ACCENT_PRIV = new Color(220, 100, 100);
    private static final Color TEXT_MAIN   = new Color(220, 225, 235);
    private static final Color TEXT_MUTED  = new Color(130, 140, 160);
    private static final Color TEXT_GREEN  = new Color(150, 220, 150);
    private static final Color BORDER_COL  = new Color(60, 65, 80);

    private static final Font FONT_MAIN   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_MONO   = new Font("Consolas",  Font.PLAIN, 11);
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  15);
    private static final Font FONT_SMALL  = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  10);

    // ── Protocoles : { code, libellé, description } ───────────────────────────
    private static final String[][] PROTOCOLES = {
        { "psk",      "PSK",           "Pre-Shared Key — mot de passe partagé hors-bande" },
        { "dh",       "Diffie-Hellman","Échange DH — forward secrecy sans authentification" },
        { "dh_signe", "DH + Signature","Échange DH authentifié par signature RSA"           },
        { "kem",      "KEM / RSA-OAEP","Chiffrement de la clé AES avec la clé publique RSA" },
    };

    // ── État ──────────────────────────────────────────────────────────────────
    private PublicKey  pubLoaded  = null;
    private PrivateKey privLoaded = null;
    private KeyPair    keyPair    = null;

    private final Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();

    // ── Composants ────────────────────────────────────────────────────────────
    private JTextArea     areaPublicKey;
    private JTextArea     areaPrivateKey;
    private JLabel        labelAlgoPub;
    private JLabel        labelAlgoPriv;
    private JLabel        labelStatut;
    private JButton       btnSauvegarder;
    private JToggleButton btnTogglePriv;

    private final String        login;
    private final Correspondant serveurCorr;
    private final ChatFrame     chatFrame;   // callback pour mise à jour sidebar

    // ─────────────────────────────────────────────────────────────────────────
    public KeyPanel(JFrame parent, String login, Correspondant serveurCorr, ChatFrame chatFrame) {
        super(parent, "Mes Clés — " + login.toUpperCase(), true);
        this.login       = login;
        this.serveurCorr = serveurCorr;
        this.chatFrame   = chatFrame;
        buildUI();
        restaurerEtat();
        setVisible(true);
    }

    // ── Construction UI ───────────────────────────────────────────────────────

    private void buildUI() {
        setSize(720, 660);
        setResizable(true);
        setMinimumSize(new Dimension(640, 560));
        setLocationRelativeTo(getParent());
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── En-tête ───────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(14, 20, 14, 20)));

        JLabel titre = new JLabel("🔑  Gestion des Clés & Protocoles");
        titre.setFont(FONT_TITLE);
        titre.setForeground(TEXT_MAIN);

        labelStatut = new JLabel("Aucune clé chargée");
        labelStatut.setFont(FONT_MAIN);
        labelStatut.setForeground(TEXT_MUTED);

        p.add(titre,       BorderLayout.WEST);
        p.add(labelStatut, BorderLayout.EAST);
        return p;
    }

    // ── Corps ─────────────────────────────────────────────────────────────────

    private JPanel buildCenter() {
        JPanel root = new JPanel();
        root.setBackground(BG_DARK);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(14, 18, 6, 18));

        root.add(sectionLabel("Clé Publique"));
        root.add(Box.createVerticalStrut(5));
        root.add(buildBlocPublique());
        root.add(Box.createVerticalStrut(12));

        root.add(sectionLabel("Clé Privée"));
        root.add(Box.createVerticalStrut(5));
        root.add(buildBlocPrivee());
        root.add(Box.createVerticalStrut(12));

        root.add(sectionLabel("Protocoles Supportés"));
        root.add(Box.createVerticalStrut(5));
        root.add(buildBlocProtocoles());

        return root;
    }

    // ── Bloc clé publique ─────────────────────────────────────────────────────

    private JPanel buildBlocPublique() {
        JPanel card = mkCard(BG_INPUT, BORDER_COL);

        // Barre supérieure
        JPanel bar = mkToolbar(BG_CARD);
        labelAlgoPub = mkAlgoLabel("—");

        JButton btnCharger = miniButton("📂  Charger (.pem)");
        btnCharger.addActionListener(e -> chargerClePublique());
        JButton btnCopier = miniButton("Copier");
        btnCopier.addActionListener(e -> copierPresse(areaPublicKey.getText()));

        JPanel right = flowRight(btnCopier, btnCharger);
        bar.add(labelAlgoPub, BorderLayout.WEST);
        bar.add(right,        BorderLayout.EAST);

        areaPublicKey = mkKeyArea(TEXT_MUTED);
        areaPublicKey.setText("Aucune clé publique chargée…");

        card.add(bar,               BorderLayout.NORTH);
        card.add(mkScroll(areaPublicKey), BorderLayout.CENTER);
        return card;
    }

    // ── Bloc clé privée ───────────────────────────────────────────────────────

    private JPanel buildBlocPrivee() {
        Color dangerBg     = new Color(50, 35, 35);
        Color dangerBorder = new Color(100, 50, 50);
        JPanel card = mkCard(new Color(42, 33, 33), dangerBorder);

        JPanel bar = mkToolbar(dangerBg);

        JLabel warnLabel = new JLabel("⚠  Clé Privée");
        warnLabel.setFont(FONT_BOLD);
        warnLabel.setForeground(ACCENT_PRIV);

        labelAlgoPriv = mkAlgoLabel("—");
        labelAlgoPriv.setForeground(new Color(200, 140, 80));

        btnTogglePriv = new JToggleButton("👁  Révéler");
        styleToggle(btnTogglePriv);
        btnTogglePriv.addActionListener(e -> togglePrivateKey());

        JButton btnCharger = miniButton("📂  Charger (.pem)");
        btnCharger.setForeground(new Color(240, 160, 100));
        btnCharger.addActionListener(e -> chargerClePrivee());

        JPanel left  = flowLeft(warnLabel, labelAlgoPriv);
        JPanel right = flowRight(btnTogglePriv, btnCharger);
        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        areaPrivateKey = mkKeyArea(new Color(180, 80, 80));
        areaPrivateKey.setText("●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●");
        areaPrivateKey.setEnabled(false);

        card.add(bar,                    BorderLayout.NORTH);
        card.add(mkScroll(areaPrivateKey), BorderLayout.CENTER);
        return card;
    }

    // ── Bloc protocoles ───────────────────────────────────────────────────────

    private JPanel buildBlocProtocoles() {
        JPanel card = new JPanel(new GridLayout(PROTOCOLES.length, 1, 0, 1));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER_COL, 1));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, PROTOCOLES.length * 50));

        for (String[] proto : PROTOCOLES) {
            String code  = proto[0];
            String label = proto[1];
            String desc  = proto[2];

            JCheckBox cb = new JCheckBox("  " + label);
            cb.setFont(FONT_BOLD);
            cb.setForeground(TEXT_MAIN);
            cb.setBackground(BG_CARD);
            cb.setFocusPainted(false);
            cb.setToolTipText(desc);
            // PSK toujours dispo, DH aussi, les autres nécessitent la paire RSA
            boolean needsRsa = code.equals("dh_signe") || code.equals("kem");
            if (needsRsa) {
                cb.setEnabled(false);
                cb.setForeground(TEXT_MUTED);
            }
            // Notifier ChatFrame dès qu'une case est cochée/décochée
            cb.addActionListener(e -> notifierChatFrame());

            JLabel descLabel = new JLabel(desc);
            descLabel.setFont(FONT_SMALL);
            descLabel.setForeground(TEXT_MUTED);

            // Badge à droite
            JLabel badge = mkBadge(needsRsa ? "🔐 RSA requis" : (code.equals("dh") ? "⇌ DH" : "🔑 PSK"));

            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setBackground(BG_CARD);
            row.setBorder(new EmptyBorder(7, 12, 7, 14));
            row.add(cb,        BorderLayout.WEST);
            row.add(descLabel, BorderLayout.CENTER);
            row.add(badge,     BorderLayout.EAST);

            checkBoxes.put(code, cb);
            card.add(row);
        }

        return card;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL),
                new EmptyBorder(10, 18, 10, 18)));

        JButton btnGenerer = buildButton("⚙  Générer RSA 2048", ACCENT_DARK);
        btnGenerer.setForeground(Color.WHITE);
        btnGenerer.addActionListener(e -> genererCles(btnGenerer));

        btnSauvegarder = buildButton("💾  Sauvegarder", BG_INPUT);
        btnSauvegarder.setEnabled(false);
        btnSauvegarder.addActionListener(e -> sauvegarderCles());

        JButton btnAnnoncer = buildButton("📡  Annoncer au serveur", new Color(60, 110, 70));
        btnAnnoncer.setForeground(Color.WHITE);
        btnAnnoncer.addActionListener(e -> annoncerProtocoles());

        JPanel left  = flowLeft(btnGenerer);
        JPanel right = flowRight(btnSauvegarder, btnAnnoncer);
        p.add(left,  BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    // ── Génération ────────────────────────────────────────────────────────────

    private void genererCles(JButton btn) {
        btn.setEnabled(false);
        btn.setText("⏳  Génération…");
        setStatut("Génération RSA 2048 en cours…", TEXT_MUTED);

        new SwingWorker<KeyPair, Void>() {
            @Override protected KeyPair doInBackground() throws Exception {
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048, new SecureRandom());
                return gen.generateKeyPair();
            }
            @Override protected void done() {
                try {
                    KeyPair kp = get();
                    pubLoaded  = kp.getPublic();
                    privLoaded = kp.getPrivate();
                    keyPair    = kp;
                    appliquerCles();
                    setStatut("✔  Paire RSA 2048 générée (non sauvegardée)", ACCENT_WARN);
                    notifierChatFrame();
                } catch (Exception ex) {
                    erreur("Erreur génération : " + ex.getMessage());
                    setStatut("✖  Erreur génération", ACCENT_PRIV);
                } finally {
                    btn.setEnabled(true);
                    btn.setText("⚙  Générer RSA 2048");
                }
            }
        }.execute();
    }

    // ── Chargement ───────────────────────────────────────────────────────────

    private void chargerClePublique() {
        JFileChooser fc = pemChooser("Charger la clé publique (.pem)");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            byte[] der = fromPem(Files.readString(fc.getSelectedFile().toPath()));
            pubLoaded  = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
            afficherPublique();
            mettreAJourEtat();  // notifierChatFrame() appelé dedans
            setStatut("✔  Clé publique : " + fc.getSelectedFile().getName(), ACCENT_OK);
        } catch (Exception ex) {
            erreur("Lecture clé publique :\n" + ex.getMessage());
        }
    }

    private void chargerClePrivee() {
        JFileChooser fc = pemChooser("Charger la clé privée (.pem)");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            byte[] der = fromPem(Files.readString(fc.getSelectedFile().toPath()));
            privLoaded = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
            areaPrivateKey.putClientProperty("__raw__", toPem("PRIVATE KEY", privLoaded.getEncoded()));
            afficherAlgoPriv();
            // Masquer + remettre le toggle
            if (btnTogglePriv.isSelected()) { btnTogglePriv.setSelected(false); togglePrivateKey(); }
            areaPrivateKey.setText("●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●");
            areaPrivateKey.setEnabled(false);
            mettreAJourEtat();  // notifierChatFrame() appelé dedans
            setStatut("✔  Clé privée : " + fc.getSelectedFile().getName(), ACCENT_OK);
        } catch (Exception ex) {
            erreur("Lecture clé privée :\n" + ex.getMessage());
        }
    }

    // ── Affichage ─────────────────────────────────────────────────────────────

    private void appliquerCles() {
        afficherPublique();
        areaPrivateKey.putClientProperty("__raw__", toPem("PRIVATE KEY", privLoaded.getEncoded()));
        if (btnTogglePriv.isSelected()) { btnTogglePriv.setSelected(false); togglePrivateKey(); }
        areaPrivateKey.setText("●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●");
        areaPrivateKey.setEnabled(false);
        afficherAlgoPriv();
        mettreAJourEtat();
    }

    private void afficherPublique() {
        areaPublicKey.setForeground(TEXT_GREEN);
        areaPublicKey.setText(toPem("PUBLIC KEY", pubLoaded.getEncoded()));
        labelAlgoPub.setText(infoAlgo(pubLoaded));
        labelAlgoPub.setForeground(ACCENT_OK);
    }

    private void afficherAlgoPriv() {
        labelAlgoPriv.setText(infoAlgo(privLoaded));
        labelAlgoPriv.setForeground(new Color(220, 160, 80));
    }

    /** Active/désactive les checkboxes selon ce qui est chargé. */
    private void mettreAJourEtat() {
        boolean pairOk = pubLoaded != null && privLoaded != null;
        if (pairOk) keyPair = new KeyPair(pubLoaded, privLoaded);

        for (Map.Entry<String, JCheckBox> e : checkBoxes.entrySet()) {
            boolean needsRsa = e.getKey().equals("dh_signe") || e.getKey().equals("kem");
            if (needsRsa) {
                e.getValue().setEnabled(pairOk);
                e.getValue().setForeground(pairOk ? TEXT_MAIN : TEXT_MUTED);
            } else {
                e.getValue().setEnabled(true);
                e.getValue().setForeground(TEXT_MAIN);
            }
        }

        btnSauvegarder.setEnabled(pairOk);
        notifierChatFrame();  // ← mise à jour sidebar
    }

    /**
     * Notifie ChatFrame de l'état courant (clé + protocoles cochés).
     * Appelé à chaque changement : chargement, génération, coche protocole.
     */
    private void notifierChatFrame() {
        if (chatFrame == null) return;
        java.util.Set<String> protos = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, JCheckBox> e : checkBoxes.entrySet()) {
            if (e.getValue().isSelected()) protos.add(e.getKey());
        }
        chatFrame.refreshLocalConfig(keyPair, protos);
    }

    // ── Toggle clé privée ─────────────────────────────────────────────────────

    private void togglePrivateKey() {
        if (btnTogglePriv.isSelected()) {
            String raw = (String) areaPrivateKey.getClientProperty("__raw__");
            if (raw == null) { btnTogglePriv.setSelected(false); erreur("Aucune clé privée chargée."); return; }
            areaPrivateKey.setEnabled(true);
            areaPrivateKey.setForeground(new Color(200, 110, 110));
            areaPrivateKey.setText(raw);
            btnTogglePriv.setText("🙈  Masquer");
        } else {
            areaPrivateKey.setText("●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●●");
            areaPrivateKey.setForeground(new Color(180, 80, 80));
            areaPrivateKey.setEnabled(false);
            btnTogglePriv.setText("👁  Révéler");
        }
    }

    // ── Sauvegarde ────────────────────────────────────────────────────────────

    private void sauvegarderCles() {
        if (pubLoaded == null || privLoaded == null) {
            erreur("Les deux clés sont requises pour sauvegarder."); return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choisir le dossier de sauvegarde");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File dir  = fc.getSelectedFile();
            Path pub  = dir.toPath().resolve(login + "_pub.pem");
            Path priv = dir.toPath().resolve(login + "_priv.pem");
            Files.writeString(pub,  toPem("PUBLIC KEY",  pubLoaded.getEncoded()));
            Files.writeString(priv, toPem("PRIVATE KEY", privLoaded.getEncoded()));
            setStatut("✔  Sauvegardé dans " + dir.getName(), ACCENT_OK);
            JOptionPane.showMessageDialog(this,
                    "<html>Fichiers créés :<br><b>" + pub.getFileName()
                    + "</b><br><b>" + priv.getFileName() + "</b></html>",
                    "Sauvegarde réussie", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            erreur("Erreur sauvegarde : " + ex.getMessage());
        }
    }

    // ── Annonce protocoles ────────────────────────────────────────────────────

    /**
     * Envoie au serveur : __BROADCAST__|__PROTO__|psk,dh,...
     * Le serveur le rediffuse à tous — chaque client met à jour son
     * objet Correspondant avec les protocoles supportés par ce user.
     */
    private void annoncerProtocoles() {
        java.util.List<String> actifs = new ArrayList<>();
        for (Map.Entry<String, JCheckBox> e : checkBoxes.entrySet())
            if (e.getValue().isSelected()) actifs.add(e.getKey());

        if (actifs.isEmpty()) { erreur("Cochez au moins un protocole avant d'annoncer."); return; }

        String protos  = String.join(",", actifs);
        String payload = "__PROTO__|" + protos;

        try {
            serveurCorr.sendClearMessage("__BROADCAST__", payload);
            setStatut("📡  Annoncé : " + protos, ACCENT_OK);
        } catch (Exception ex) {
            // Mode dégradé : simulation
            System.out.println("[KeyPanel] Annonce simulée : " + payload);
            setStatut("⚠  Non connecté — annonce simulée : " + protos, ACCENT_WARN);
        }
    }

    // ── Auto-chargement ───────────────────────────────────────────────────────

    /**
     * Restaure l'état dans cet ordre de priorité :
     *   1. Mémoire de ChatFrame  (clés déjà en RAM + protocoles cochés)
     *   2. Fichiers .pem sur le disque  (premier lancement ou après redémarrage)
     */
    private void restaurerEtat() {

        // ── 1. Restaurer depuis ChatFrame (prioritaire) ────────────────────
        if (chatFrame != null && chatFrame.localKeyPair != null) {
            pubLoaded  = chatFrame.localKeyPair.getPublic();
            privLoaded = chatFrame.localKeyPair.getPrivate();
            keyPair    = chatFrame.localKeyPair;
            afficherPublique();
            areaPrivateKey.putClientProperty("__raw__",
                    toPem("PRIVATE KEY", privLoaded.getEncoded()));
            afficherAlgoPriv();
        }

        // ── Restaurer les protocoles cochés ───────────────────────────────
        if (chatFrame != null && !chatFrame.localProtocols.isEmpty()) {
            for (Map.Entry<String, JCheckBox> e : checkBoxes.entrySet()) {
                e.getValue().setSelected(chatFrame.localProtocols.contains(e.getKey()));
            }
        }

        if (pubLoaded != null || privLoaded != null) {
            mettreAJourEtat();
            setStatut("✔  Configuration restaurée", ACCENT_OK);
            return;   // pas besoin de lire le disque
        }

        // ── 2. Fallback : lire les fichiers .pem sur le disque ─────────────
        try {
            File pubFile  = new File(login + "_pub.pem");
            File privFile = new File(login + "_priv.pem");
            if (pubFile.exists()) {
                pubLoaded = KeyFactory.getInstance("RSA")
                        .generatePublic(new X509EncodedKeySpec(
                                fromPem(Files.readString(pubFile.toPath()))));
                afficherPublique();
            }
            if (privFile.exists()) {
                privLoaded = KeyFactory.getInstance("RSA")
                        .generatePrivate(new PKCS8EncodedKeySpec(
                                fromPem(Files.readString(privFile.toPath()))));
                areaPrivateKey.putClientProperty("__raw__",
                        toPem("PRIVATE KEY", privLoaded.getEncoded()));
                afficherAlgoPriv();
            }
            if (pubLoaded != null || privLoaded != null) {
                mettreAJourEtat();
                setStatut("✔  Clés chargées depuis le disque", ACCENT_OK);
            }
        } catch (Exception e) {
            System.err.println("[KeyPanel] Lecture disque : " + e.getMessage());
        }
    }

    // ── Helpers crypto ────────────────────────────────────────────────────────

    private static String toPem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    private static byte[] fromPem(String pem) {
        return Base64.getDecoder().decode(
                pem.replaceAll("-----BEGIN [^-]+-----", "")
                   .replaceAll("-----END [^-]+-----",   "")
                   .replaceAll("\\s+", ""));
    }

    private static String infoAlgo(Key k) {
        String algo = k.getAlgorithm();
        int bits = 0;
        if (k instanceof RSAKey)  bits = ((RSAKey) k).getModulus().bitLength();
        else if (k instanceof ECKey) bits = ((ECKey) k).getParams().getCurve().getField().getFieldSize();
        return algo + (bits > 0 ? "  " + bits + " bits" : "");
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void copierPresse(String t) {
        if (t == null || t.startsWith("●") || t.startsWith("Aucune")) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(t), null);
        setStatut("📋  Copié dans le presse-papier", ACCENT_OK);
    }

    private void setStatut(String txt, Color c) {
        SwingUtilities.invokeLater(() -> { labelStatut.setText(txt); labelStatut.setForeground(c); });
    }

    private void erreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.WARNING_MESSAGE);
    }

    private JFileChooser pemChooser(String title) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PEM (*.pem)", "pem"));
        return fc;
    }

    // ── Helpers construction UI ───────────────────────────────────────────────

    private JTextArea mkKeyArea(Color fg) {
        JTextArea a = new JTextArea(5, 1);
        a.setFont(FONT_MONO); a.setBackground(BG_INPUT); a.setForeground(fg);
        a.setCaretColor(ACCENT); a.setEditable(false);
        a.setLineWrap(true); a.setWrapStyleWord(false);
        a.setBorder(new EmptyBorder(8, 10, 8, 10));
        return a;
    }

    private JScrollPane mkScroll(JTextArea a) {
        JScrollPane s = new JScrollPane(a);
        s.setBorder(BorderFactory.createEmptyBorder());
        s.setBackground(BG_INPUT); s.getViewport().setBackground(BG_INPUT);
        return s;
    }

    private JPanel mkCard(Color bg, Color border) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(BorderFactory.createLineBorder(border, 1));
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));
        p.setPreferredSize(new Dimension(0, 155));
        return p;
    }

    private JPanel mkToolbar(Color bg) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(5, 10, 5, 10));
        return p;
    }

    private JLabel sectionLabel(String txt) {
        JLabel l = new JLabel(txt.toUpperCase());
        l.setFont(FONT_LABEL); l.setForeground(TEXT_MUTED);
        l.setBorder(new EmptyBorder(0, 2, 0, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JLabel mkAlgoLabel(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(FONT_MONO); l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel mkBadge(String txt) {
        JLabel l = new JLabel("  " + txt + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(45, 52, 68));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        l.setFont(FONT_SMALL); l.setForeground(ACCENT); l.setOpaque(false);
        return l;
    }

    private JPanel flowLeft(JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        p.setOpaque(false); for (JComponent c : cs) p.add(c); return p;
    }

    private JPanel flowRight(JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        p.setOpaque(false); for (JComponent c : cs) p.add(c); return p;
    }

    private JButton buildButton(String label, Color bg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                        ? (getModel().isPressed() ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg)
                        : new Color(55, 58, 70));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD); btn.setForeground(TEXT_MAIN);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private JButton miniButton(String label) {
        JButton btn = buildButton(label, new Color(55, 60, 76));
        btn.setFont(FONT_SMALL); btn.setForeground(ACCENT);
        btn.setBorder(new EmptyBorder(3, 10, 3, 10));
        return btn;
    }

    private void styleToggle(JToggleButton btn) {
        btn.setFont(FONT_SMALL); btn.setForeground(ACCENT_WARN);
        btn.setBackground(new Color(65, 52, 35));
        btn.setContentAreaFilled(true); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(3, 10, 3, 10));
    }

    public KeyPair getKeyPair() { return keyPair; }
}