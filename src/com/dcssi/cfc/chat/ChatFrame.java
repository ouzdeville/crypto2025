package com.dcssi.cfc.chat;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Interface principale du chat — remplace NewMDIApplication.
 * Design épuré, thème sombre, sans code généré NetBeans.
 */
public class ChatFrame extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(28, 30, 36);
    private static final Color BG_PANEL     = new Color(38, 41, 50);
    private static final Color BG_INPUT     = new Color(48, 52, 64);
    private static final Color ACCENT       = new Color(99, 179, 237);
    private static final Color ACCENT_DARK  = new Color(66, 135, 200);
    private static final Color TEXT_MAIN    = new Color(220, 225, 235);
    private static final Color TEXT_MUTED   = new Color(130, 140, 160);
    private static final Color MSG_ME       = new Color(55, 65, 90);
    private static final Color MSG_OTHER    = new Color(45, 50, 62);
    private static final Color ONLINE_DOT   = new Color(72, 199, 116);

    private static final Font  FONT_MAIN    = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font  FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font  FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font  FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  16);

    // ── État ──────────────────────────────────────────────────────────────────
    public static final List<Correspondant> correspondantList =
            Collections.synchronizedList(new ArrayList<>());
    public Correspondant currentCorrespondant;
    private Correspondant serveurCorr;
    private Socket        serveurSocket;
    private Recepteur     recepteur;     // référence pour registerNegociateur()

    // ── Composants UI ─────────────────────────────────────────────────────────
    // ── Config locale visible dans la sidebar ─────────────────────────────────
    public java.security.KeyPair localKeyPair = null;
    public final Set<String> localProtocols   = new java.util.LinkedHashSet<>();

    private DefaultTableModel tableModel;
    private JTable tableCorrespondants;
    private JTextArea chatArea;
    private JTextArea inputArea;
    private JLabel labelStatus;
    private JLabel labelInterlocuteur;

    // Composants du panneau "Ma Config"
    private JLabel    lblConfigCle;
    private JLabel    lblConfigAlgo;
    private JPanel    panelProtoTags;

    // ── Constructeur ──────────────────────────────────────────────────────────
    private String login; // kept for KeyPanel callback

    public ChatFrame(String login, String password) {
        super("CFC Secure Chat — " + login.toUpperCase());
        this.login = login;
        buildUI();
        connecter(login, password);
    }

    // ── Construction de l'interface ───────────────────────────────────────────

    private void buildUI() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new Dimension(700, 480));
        setLocationRelativeTo(null);

        // Fond général
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildSidebar(),  BorderLayout.WEST);
        add(buildChatZone(), BorderLayout.CENTER);

        applyGlobalFont(getContentPane());
    }

    // ── Sidebar (liste correspondants) ────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 8));
        sidebar.setBackground(BG_PANEL);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(12, 10, 12, 0));

        // Titre
        JLabel titre = new JLabel("  Contacts");
        titre.setFont(FONT_TITLE);
        titre.setForeground(TEXT_MAIN);
        titre.setBorder(new EmptyBorder(0, 0, 8, 0));
        sidebar.add(titre, BorderLayout.NORTH);

        // Table contacts
        tableModel = new DefaultTableModel(new String[]{"Correspondants"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tableCorrespondants = new JTable(tableModel);
        tableCorrespondants.setBackground(BG_PANEL);
        tableCorrespondants.setForeground(TEXT_MAIN);
        tableCorrespondants.setFont(FONT_MAIN);
        tableCorrespondants.setRowHeight(42);
        tableCorrespondants.setShowGrid(false);
        tableCorrespondants.setSelectionBackground(BG_INPUT);
        tableCorrespondants.setSelectionForeground(ACCENT);
        tableCorrespondants.getTableHeader().setVisible(false);
        tableCorrespondants.setTableHeader(null);

        // Renderer personnalisé
        tableCorrespondants.setDefaultRenderer(Object.class, new ContactRenderer());

        tableCorrespondants.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                onContactClick();
            }
        });

        JScrollPane scroll = new JScrollPane(tableCorrespondants);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_PANEL);
        scroll.getViewport().setBackground(BG_PANEL);
        sidebar.add(scroll, BorderLayout.CENTER);

        // ── Panneau "Ma Configuration" ───────────────────────────────────────
        sidebar.add(buildLocalConfigPanel(), BorderLayout.SOUTH);

        return sidebar;
    }

    // ── Panneau config locale ─────────────────────────────────────────────────

    private JPanel buildLocalConfigPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 6));
        outer.setBackground(BG_PANEL);
        outer.setBorder(new EmptyBorder(8, 0, 0, 8));

        // ── Carte identité / clé ──────────────────────────────────────────────
        JPanel card = new JPanel(new BorderLayout(0, 3));
        card.setBackground(new Color(33, 36, 46));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 60, 76), 1),
                new EmptyBorder(8, 10, 8, 10)));

        // Ligne 1 : icône + login
        JLabel lblLogin = new JLabel("👤  " + login.toUpperCase());
        lblLogin.setFont(FONT_BOLD);
        lblLogin.setForeground(TEXT_MAIN);

        // Ligne 2 : état de la clé
        lblConfigCle = new JLabel("Aucune clé chargée");
        lblConfigCle.setFont(FONT_SMALL);
        lblConfigCle.setForeground(new Color(200, 100, 100));

        // Ligne 3 : algo + taille
        lblConfigAlgo = new JLabel("");
        lblConfigAlgo.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblConfigAlgo.setForeground(new Color(130, 140, 160));

        JPanel lines = new JPanel(new GridLayout(3, 1, 0, 2));
        lines.setOpaque(false);
        lines.add(lblLogin);
        lines.add(lblConfigCle);
        lines.add(lblConfigAlgo);
        card.add(lines, BorderLayout.CENTER);

        // ── Tags protocoles ───────────────────────────────────────────────────
        panelProtoTags = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        panelProtoTags.setBackground(BG_PANEL);
        panelProtoTags.setBorder(new EmptyBorder(4, 0, 4, 0));
        refreshProtoTags(); // initialiser (vide)

        // ── Boutons ───────────────────────────────────────────────────────────
        JButton btnRefresh = buildButton("⟳  Rafraîchir", ACCENT_DARK);
        btnRefresh.addActionListener(e -> demanderListe());

        JButton btnMesCles = buildButton("🔑  Mes Clés", new Color(90, 75, 130));
        btnMesCles.addActionListener(e -> new KeyPanel(this, login, serveurCorr, ChatFrame.this));

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 0, 6));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        btnPanel.add(btnRefresh);
        btnPanel.add(btnMesCles);

        outer.add(card,           BorderLayout.NORTH);
        outer.add(panelProtoTags, BorderLayout.CENTER);
        outer.add(btnPanel,       BorderLayout.SOUTH);
        return outer;
    }

    // ── Callback depuis KeyPanel ──────────────────────────────────────────────

    /**
     * Appelé par KeyPanel chaque fois que la config change
     * (clé générée/chargée, protocoles cochés).
     */
    public void refreshLocalConfig(java.security.KeyPair kp, Set<String> protocols) {
        this.localKeyPair = kp;
        this.localProtocols.clear();
        this.localProtocols.addAll(protocols);

        // Propager les protocoles dans serveurCorr pour la négociation
        if (serveurCorr != null) {
            serveurCorr.protocols.clear();
            serveurCorr.protocols.addAll(protocols);
            if (kp != null) serveurCorr.setKeyPair(kp);
        }

        SwingUtilities.invokeLater(() -> {
            // Mettre à jour le label clé
            if (kp != null) {
                java.security.Key pub = kp.getPublic();
                String algo = pub.getAlgorithm();
                int bits = 0;
                if (pub instanceof java.security.interfaces.RSAKey)
                    bits = ((java.security.interfaces.RSAKey) pub).getModulus().bitLength();
                lblConfigCle.setText("🔑 Clé chargée");
                lblConfigCle.setForeground(ONLINE_DOT);
                lblConfigAlgo.setText(algo + (bits > 0 ? "  " + bits + " bits" : ""));
            } else {
                lblConfigCle.setText("Aucune clé chargée");
                lblConfigCle.setForeground(new Color(200, 100, 100));
                lblConfigAlgo.setText("");
            }
            refreshProtoTags();
        });
    }

    /** Redessine les tags de protocoles dans la sidebar. */
    private void refreshProtoTags() {
        panelProtoTags.removeAll();

        if (localProtocols.isEmpty()) {
            JLabel none = new JLabel("Aucun protocole");
            none.setFont(FONT_SMALL);
            none.setForeground(TEXT_MUTED);
            panelProtoTags.add(none);
        } else {
            // Couleurs par protocole
            java.util.Map<String, Color> colors = new java.util.HashMap<>();
            colors.put("psk",      new Color(80, 120, 80));
            colors.put("dh",       new Color(60, 100, 140));
            colors.put("dh_signe", new Color(120, 80, 140));
            colors.put("kem",      new Color(140, 100, 50));

            for (String p : localProtocols) {
                Color bg = colors.getOrDefault(p, new Color(60, 65, 80));
                panelProtoTags.add(makeProtoTag(p.toUpperCase(), bg));
            }
        }
        panelProtoTags.revalidate();
        panelProtoTags.repaint();
    }

    private JLabel makeProtoTag(String label, Color bg) {
        JLabel tag = new JLabel("  " + label + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose(); super.paintComponent(g);
            }
        };
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setForeground(new Color(210, 220, 235));
        tag.setOpaque(false);
        return tag;
    }

    // ── Zone de chat ──────────────────────────────────────────────────────────

    private JPanel buildChatZone() {
        JPanel zone = new JPanel(new BorderLayout(0, 0));
        zone.setBackground(BG_DARK);

        // En-tête
        zone.add(buildChatHeader(), BorderLayout.NORTH);

        // Zone messages
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_DARK);
        chatArea.setForeground(TEXT_MAIN);
        chatArea.setFont(FONT_MAIN);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scrollChat = new JScrollPane(chatArea);
        scrollChat.setBorder(BorderFactory.createEmptyBorder());
        scrollChat.setBackground(BG_DARK);
        scrollChat.getViewport().setBackground(BG_DARK);
        zone.add(scrollChat, BorderLayout.CENTER);

        // Zone saisie
        zone.add(buildInputPanel(), BorderLayout.SOUTH);

        return zone;
    }

    private JPanel buildChatHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BG_INPUT),
                new EmptyBorder(12, 16, 12, 16)));

        labelInterlocuteur = new JLabel("Sélectionnez un contact");
        labelInterlocuteur.setFont(FONT_BOLD);
        labelInterlocuteur.setForeground(TEXT_MAIN);

        labelStatus = new JLabel("●  Connecté au serveur");
        labelStatus.setFont(FONT_SMALL);
        labelStatus.setForeground(ONLINE_DOT);
        labelStatus.setHorizontalAlignment(SwingConstants.RIGHT);

        header.add(labelInterlocuteur, BorderLayout.WEST);
        header.add(labelStatus,        BorderLayout.EAST);
        return header;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BG_INPUT),
                new EmptyBorder(10, 14, 10, 14)));

        inputArea = new JTextArea(3, 1);
        inputArea.setBackground(BG_INPUT);
        inputArea.setForeground(TEXT_MAIN);
        inputArea.setCaretColor(ACCENT);
        inputArea.setFont(FONT_MAIN);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Envoyer avec Entrée, nouvelle ligne avec Shift+Entrée
        inputArea.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    envoyerMessage();
                }
            }
        });

        JScrollPane scrollInput = new JScrollPane(inputArea);
        scrollInput.setBorder(BorderFactory.createLineBorder(BG_INPUT, 1, true));
        scrollInput.setBackground(BG_INPUT);

        JButton btnEnvoyer = buildButton("Envoyer  ▶", ACCENT);
        btnEnvoyer.setForeground(Color.WHITE);
        btnEnvoyer.setFont(FONT_BOLD);
        btnEnvoyer.setPreferredSize(new Dimension(110, 50));
        btnEnvoyer.addActionListener(e -> envoyerMessage());

        panel.add(scrollInput, BorderLayout.CENTER);
        panel.add(btnEnvoyer,  BorderLayout.EAST);
        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onContactClick() {
        int row = tableCorrespondants.getSelectedRow();
        if (row == -1) return;

        Correspondant c = (Correspondant) tableModel.getValueAt(row, 0);
        currentCorrespondant = c;
        labelInterlocuteur.setText("  " + c.son_id
                + (c.nom != null && !c.nom.isBlank() ? "  (" + c.nom + ")" : ""));

        // Afficher l'historique
        chatArea.setText("");
        for (String msg : c.messages) appendChat(msg);

        // Si clé déjà établie → juste afficher
        if (c.isPskReady()) {
            setStatus("🔒 Chiffré (" + c.protocols.stream().findFirst().orElse("?") + ")", ONLINE_DOT);
            return;
        }

        // Calculer les protocoles locaux (ceux cochés dans KeyPanel)
        // stockés dans serveurCorr.protocols
        Set<String> mesProtos  = serveurCorr != null ? serveurCorr.protocols : new java.util.LinkedHashSet<>();
        Set<String> sesProtos  = c.protocols;

        if (mesProtos.isEmpty() && sesProtos.isEmpty()) {
            // Aucune annonce de part et d'autre → fallback PSK direct
            //mesProtos = new java.util.LinkedHashSet<>(java.util.Arrays.asList("psk"));
            //sesProtos = mesProtos;
        }

        // Ouvrir le dialogue de sélection
        NegociationDialog dlg = new NegociationDialog(this, mesProtos, c.son_id, sesProtos);
        String proto = dlg.getChoix();
        if (proto == null) return; // annulé

        // Lancer la négociation dans un thread séparé
        final Correspondant target = c;
        final String choix = proto;
        new Thread(() -> {
            try {
                ProtocolNegotiator neg = new ProtocolNegotiator(
                        serveurCorr, target, true,   // initiateur=true
                        (p, key) -> {
                            target.initAesKey(key);
                            target.protocols.add(p);
                            updateCorrespondant(target);
                            SwingUtilities.invokeLater(() -> {
                                rafraichirTableCorrespondants();
                                onKeyEstablished(target.son_id, p);
                            });
                        });

                // ── CRITIQUE : enregistrer l'initiateur dans Recepteur AVANT d'envoyer
                // Sinon, quand la réponse arrive, Recepteur créerait un nouveau
                // négociateur (répondeur) sans la clé privée DH d'Alice.
                if (recepteur != null) {
                    recepteur.registerNegociateur(target.son_id, neg);
                }
                System.out.println("Le choix de l'initiateur->"+choix);
                neg.sendInit(choix, this);

            } catch (ProtocolNegotiator.CancelledException ex) {
                System.out.println("[Négo] Annulé par l'utilisateur");
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    erreur("Erreur de négociation (" + choix + ") :\n" + ex.getMessage()));
                // Nettoyer le négociateur en cas d'erreur
                if (recepteur != null) recepteur.registerNegociateur(target.son_id, null);
            }
        }, "negociation-" + target.son_id).start();
    }

    /** Met à jour un correspondant dans la liste globale ET currentCorrespondant si besoin. */
    private void updateCorrespondant(Correspondant updated) {
        for (int i = 0; i < correspondantList.size(); i++) {
            if (updated.son_id.equals(correspondantList.get(i).son_id)) {
                correspondantList.set(i, updated);
                break;
            }
        }
        // Synchroniser currentCorrespondant si c'est le même peer
        if (currentCorrespondant != null
                && updated.son_id.equals(currentCorrespondant.son_id)) {
            currentCorrespondant = updated;
        }
    }

    private void envoyerMessage() {
        if (currentCorrespondant == null) {
            erreur("Sélectionnez un contact avant d'envoyer.");
            return;
        }

        // Toujours relire l'objet le plus récent depuis la liste
        // (la négociation peut avoir mis à jour les champs cipher entre-temps)
        Correspondant fresh = findFresh(currentCorrespondant.son_id);
        if (fresh != null) currentCorrespondant = fresh;

        if (!currentCorrespondant.isPskReady()) {
            erreur("Clé non encore négociée avec " + currentCorrespondant.son_id
                    + ".\nCliquez sur le contact pour lancer la négociation.");
            return;
        }

        String message = inputArea.getText().trim();
        if (message.isEmpty()) return;

        try {
            currentCorrespondant.sendMessage(currentCorrespondant.son_id, message);
            currentCorrespondant.ajouterMessage("Moi > " + message);
            appendChat("Moi > " + message);
            inputArea.setText("");
        } catch (Exception ex) {
            erreur("Envoi échoué : " + ex.getMessage());
        }
    }

    /** Retourne l'objet Correspondant le plus récent dans la liste par son id. */
    private Correspondant findFresh(String id) {
        for (Correspondant c : correspondantList) {
            if (id.equals(c.son_id)) return c;
        }
        return null;
    }

    private void demanderListe() {
        try {
            serveurCorr.demanderListeClients();
        } catch (Exception e) {
            erreur("Impossible de rafraîchir la liste : " + e.getMessage());
        }
    }

    // ── Connexion serveur ─────────────────────────────────────────────────────

    private void connecter(String login, String password) {
        try {
            serveurSocket = new Socket("127.0.0.1", 2026);

            // Enregistrement
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(serveurSocket.getOutputStream()));
            bw.write(login + "|nom|prenom");
            bw.newLine();
            bw.flush();

            // Créer le correspondant serveur
            serveurCorr = new Correspondant(login, "", serveurSocket, false, password);
            serveurCorr.bw = bw; // partager le writer

            // Thread d'écoute
            recepteur = new Recepteur(serveurSocket, login, serveurCorr, this);
            recepteur.start();

            setStatus("● Connecté au serveur", ONLINE_DOT);
            JOptionPane.showMessageDialog(this, "Connecté !", "Succès", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            setStatus("✖ Hors ligne", new Color(220, 80, 80));
            JOptionPane.showMessageDialog(this,
                    "Connexion au serveur impossible :\n" + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Méthodes publiques appelées par Recepteur ─────────────────────────────

    public void rafraichirTableCorrespondants() {
        tableModel.setRowCount(0);
        synchronized (correspondantList) {
            for (Correspondant c : correspondantList) {
                tableModel.addRow(new Object[]{c});
            }
        }
    }

    public void ajouterMessageChat(String message) {
        appendChat(message);
    }

    /** Appelé quand une clé est établie (initiateur ou répondeur). */
    public void onKeyEstablished(String peerId, String protocole) {
        setStatus("🔒 Chiffré (" + protocole + ") avec " + peerId, ONLINE_DOT);
        rafraichirTableCorrespondants();
    }

    /**
     * Synchronise currentCorrespondant avec l'objet le plus récent.
     * Appelé par Recepteur après initAesKey() côté répondeur.
     */
    public void syncCurrentCorrespondant(String peerId, Correspondant fresh) {
        if (currentCorrespondant != null
                && peerId.equals(currentCorrespondant.son_id)) {
            currentCorrespondant = fresh;
        }
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            labelStatus.setText(text);
            labelStatus.setForeground(color);
        });
    }

    private void erreur(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.WARNING_MESSAGE);
    }

    private JButton buildButton(String label, Color bg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed()
                        ? bg.darker() : getModel().isRollover() ? bg.brighter() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(TEXT_MAIN);
        btn.setFont(FONT_MAIN);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void applyGlobalFont(Container c) {
        c.setFont(FONT_MAIN);
        for (Component comp : c.getComponents()) {
            if (comp instanceof Container) applyGlobalFont((Container) comp);
        }
    }

    // ── Renderer contact ──────────────────────────────────────────────────────

    private class ContactRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object val,
                boolean sel, boolean foc, int row, int col) {

            JPanel cell = new JPanel(new BorderLayout(10, 0));
            cell.setBackground(sel ? BG_INPUT : BG_PANEL);
            cell.setBorder(new EmptyBorder(6, 12, 6, 12));

            if (val instanceof Correspondant) {
                Correspondant c = (Correspondant) val;

                // Avatar circle
                JLabel avatar = new JLabel(c.son_id.substring(0, 1).toUpperCase()) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(ACCENT_DARK);
                        g2.fillOval(0, 0, getWidth(), getHeight());
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                avatar.setForeground(Color.WHITE);
                avatar.setFont(FONT_BOLD);
                avatar.setHorizontalAlignment(SwingConstants.CENTER);
                avatar.setPreferredSize(new Dimension(32, 32));

                JLabel name = new JLabel(c.son_id);
                name.setFont(FONT_BOLD);
                name.setForeground(sel ? ACCENT : TEXT_MAIN);

                // Ligne 2 : statut PSK + protocoles annoncés
                String protoStr = c.protocols.isEmpty()
                        ? (c.isPskReady() ? "🔒 Chiffré" : "En clair")
                        : "⚡ " + String.join(" · ", c.protocols);
                JLabel sub = new JLabel(protoStr);
                sub.setFont(FONT_SMALL);
                sub.setForeground(c.isPskReady() ? ONLINE_DOT
                        : c.protocols.isEmpty() ? TEXT_MUTED : ACCENT);

                JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
                info.setOpaque(false);
                info.add(name);
                info.add(sub);

                cell.add(avatar, BorderLayout.WEST);
                cell.add(info,   BorderLayout.CENTER);
            }
            return cell;
        }
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        // Demander login/password avant ouverture
        JTextField loginField = new JTextField(12);
        JPasswordField passField = new JPasswordField(12);
        loginField.setBorder(BorderFactory.createTitledBorder("Login"));
        passField.setBorder(BorderFactory.createTitledBorder("Mot de passe"));

        JPanel panel = new JPanel(new GridLayout(2, 1, 6, 6));
        panel.add(loginField);
        panel.add(passField);

        int res = JOptionPane.showConfirmDialog(null, panel,
                "CFC Secure Chat — Connexion",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (res != JOptionPane.OK_OPTION) System.exit(0);

        String login = loginField.getText().trim();
        String pass  = new String(passField.getPassword());
        if (login.isEmpty()) { JOptionPane.showMessageDialog(null, "Login requis."); System.exit(1); }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new ChatFrame(login, pass).setVisible(true);
        });
    }
}