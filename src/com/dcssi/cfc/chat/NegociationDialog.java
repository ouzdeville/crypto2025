package com.dcssi.cfc.chat;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Dialogue de sélection du protocole de négociation.
 *
 * Affiche l'intersection entre les protocoles supportés par l'utilisateur local
 * et ceux annoncés par le correspondant distant.
 * Si un seul protocole commun → sélection automatique.
 */
public class NegociationDialog extends JDialog {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_DARK    = new Color(28, 30, 36);
    private static final Color BG_PANEL   = new Color(38, 41, 50);
    private static final Color BG_INPUT   = new Color(48, 52, 64);
    private static final Color BG_CARD    = new Color(44, 47, 58);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color ACCENT_OK  = new Color(72, 199, 116);
    private static final Color ACCENT_OFF = new Color(100, 60, 60);
    private static final Color TEXT_MAIN  = new Color(220, 225, 235);
    private static final Color TEXT_MUTED = new Color(130, 140, 160);
    private static final Color BORDER_COL = new Color(60, 65, 80);

    private static final Font FONT_MAIN  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD  = new Font("Segoe UI", Font.BOLD,  13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD,  15);
    private static final Font FONT_MONO  = new Font("Consolas",  Font.PLAIN, 11);

    // ── Métadonnées des protocoles ────────────────────────────────────────────
    private static final Map<String, String[]> META = new LinkedHashMap<>();
    static {
        //               code          icône  nom complet          description courte                         exigence
        META.put("psk",      new String[]{"🔑", "PSK",           "Mot de passe partagé hors-bande",          "Aucune clé RSA requise"});
        META.put("dh",       new String[]{"⇌",  "Diffie-Hellman","Échange DH MODP-2048 + HKDF-SHA256",       "Aucune clé RSA requise"});
        META.put("dh_signe", new String[]{"✍",  "DH + Signature","DH authentifié par signature RSA-SHA256",  "Paire RSA requise des deux côtés"});
        META.put("kem",      new String[]{"📦", "KEM / RSA-OAEP","Clé AES256 encapsulée dans RSA-OAEP",      "Clé publique RSA du destinataire requise"});
    }

    // ── Résultat ──────────────────────────────────────────────────────────────
    private String choixProtocole = null;

    // ── Composants ────────────────────────────────────────────────────────────
    private final ButtonGroup group = new ButtonGroup();
    private JButton btnOk;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * @param parent      fenêtre parente
     * @param localProtos protocoles cochés par l'utilisateur local (dans KeyPanel)
     * @param remoteId    identifiant du correspondant
     * @param remoteProtos protocoles annoncés par le correspondant
     */
    public NegociationDialog(JFrame parent,
                             Set<String> localProtos,
                             String remoteId,
                             Set<String> remoteProtos) {
        super(parent, "Négociation avec " + remoteId, true);

        // Intersection
        List<String> communs = intersection(localProtos, remoteProtos);

        buildUI(remoteId, localProtos, remoteProtos, communs);

        // Pré-sélectionner le premier protocole commun
        /*if (!communs.isEmpty()) {
            group.getElements().asIterator().forEachRemaining(btn -> {
                if (btn.getActionCommand().equals(communs.get(0))) {
                    btn.setSelected(true);
                    choixProtocole = communs.get(0);
                }
            });
        }*/

        pack();
        setMinimumSize(new Dimension(520, 300));
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    // ── Construction UI ───────────────────────────────────────────────────────

    private void buildUI(String remoteId,
                         Set<String> local,
                         Set<String> remote,
                         List<String> communs) {
        setResizable(false);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(remoteId, communs), BorderLayout.NORTH);
        add(buildProtoList(local, remote, communs), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ── En-tête ───────────────────────────────────────────────────────────────

    private JPanel buildHeader(String remoteId, List<String> communs) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(BG_PANEL);
        p.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COL),
                new EmptyBorder(14, 20, 14, 20)));

        JLabel titre = new JLabel("Choisir le protocole avec  " + remoteId);
        titre.setFont(FONT_TITLE);
        titre.setForeground(TEXT_MAIN);

        String sub = communs.isEmpty()
                ? "⚠  Aucun protocole en commun"
                : communs.size() + " protocole(s) en commun";
        JLabel subtit = new JLabel(sub);
        subtit.setFont(FONT_SMALL);
        subtit.setForeground(communs.isEmpty() ? new Color(220, 100, 100) : ACCENT_OK);

        p.add(titre,  BorderLayout.NORTH);
        p.add(subtit, BorderLayout.SOUTH);
        return p;
    }

    // ── Liste des protocoles ──────────────────────────────────────────────────

    private JPanel buildProtoList(Set<String> local, Set<String> remote, List<String> communs) {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(BG_DARK);
        list.setBorder(new EmptyBorder(12, 16, 12, 16));

        for (String code : META.keySet()) {
            boolean enCommun   = communs.contains(code);
            boolean jaiProto   = local.contains(code);
            boolean luiProto   = remote.contains(code);
            list.add(buildProtoRow(code, enCommun, jaiProto, luiProto));
            list.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG_DARK);
        wrap.add(scroll);
        return wrap;
    }

    private JPanel buildProtoRow(String code, boolean enCommun,
                                  boolean jaiProto, boolean luiProto) {
        String[] m    = META.get(code);
        String   icon = m[0], nom = m[1], desc = m[2], req = m[3];

        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(enCommun ? BG_CARD : new Color(35, 35, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 14, 10, 14));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        row.setAlignmentX(LEFT_ALIGNMENT);

        // Radio bouton
        JRadioButton radio = new JRadioButton();
        radio.setActionCommand(code);
        radio.setEnabled(enCommun);
        radio.setOpaque(false);
        radio.addActionListener(e -> {
            choixProtocole = code;
            btnOk.setEnabled(true);
        });
        group.add(radio);

        // Icône
        JLabel icLabel = new JLabel(icon);
        icLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));

        // Texte central
        JLabel nomLabel = new JLabel(nom);
        nomLabel.setFont(FONT_BOLD);
        nomLabel.setForeground(enCommun ? TEXT_MAIN : TEXT_MUTED);

        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(FONT_SMALL);
        descLabel.setForeground(TEXT_MUTED);

        JLabel reqLabel = new JLabel(req);
        reqLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        reqLabel.setForeground(new Color(100, 110, 130));

        JPanel center = new JPanel(new GridLayout(3, 1, 0, 1));
        center.setOpaque(false);
        center.add(nomLabel);
        center.add(descLabel);
        center.add(reqLabel);

        // Badges disponibilité
        JPanel badges = new JPanel(new GridLayout(2, 1, 0, 3));
        badges.setOpaque(false);
        badges.add(mkBadge(jaiProto ? "✔ Moi"    : "✖ Moi",    jaiProto));
        badges.add(mkBadge(luiProto ? "✔ " + "Lui" : "✖ Lui",  luiProto));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(radio);
        left.add(icLabel);

        row.add(left,   BorderLayout.WEST);
        row.add(center, BorderLayout.CENTER);
        row.add(badges, BorderLayout.EAST);

        // Bordure de sélection verte si disponible
        if (enCommun) {
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(60, 90, 60), 1, true),
                    new EmptyBorder(9, 13, 9, 13)));
        } else {
            row.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(50, 50, 55), 1, true),
                    new EmptyBorder(9, 13, 9, 13)));
        }

        return row;
    }

    // ── Footer ────────────────────────────────────────────────────────────────

    private JPanel buildFooter() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        p.setBackground(BG_PANEL);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COL));

        JButton btnAnnuler = buildButton("Annuler", BG_INPUT);
        btnAnnuler.setForeground(TEXT_MUTED);
        btnAnnuler.addActionListener(e -> { choixProtocole = null; dispose(); });

        btnOk = buildButton("Négocier  →", ACCENT);
        btnOk.setForeground(Color.WHITE);
        btnOk.setEnabled(false);
        btnOk.addActionListener(e -> dispose());

        p.add(btnAnnuler);
        p.add(btnOk);
        return p;
    }

    // ── Résultat ──────────────────────────────────────────────────────────────

    /** Retourne le protocole choisi, ou null si annulé. */
    public String getChoix() { return choixProtocole; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static List<String> intersection(Set<String> a, Set<String> b) {
        List<String> res = new ArrayList<>();
        // Respecter l'ordre de META
        for (String code : META.keySet()) {
            if (a.contains(code) && b.contains(code)) res.add(code);
        }
        return res;
    }

    private JLabel mkBadge(String txt, boolean ok) {
        JLabel l = new JLabel("  " + txt + "  ") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ok ? new Color(30, 60, 40) : new Color(55, 40, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose(); super.paintComponent(g);
            }
        };
        l.setFont(FONT_SMALL);
        l.setForeground(ok ? ACCENT_OK : new Color(160, 80, 80));
        l.setOpaque(false);
        return l;
    }

    private JButton buildButton(String label, Color bg) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                        ? (getModel().isPressed()  ? bg.darker()
                         : getModel().isRollover() ? bg.brighter() : bg)
                        : new Color(50, 53, 65));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BOLD); btn.setForeground(TEXT_MAIN);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        return btn;
    }
}