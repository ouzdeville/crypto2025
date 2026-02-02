package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dcssi.cfc.crypto.CryptoImpl;
import dcssi.cfc.crypto.ICrypto;

import javax.crypto.SecretKey;
import java.awt.*;
import java.io.File;

public class CryptoApp extends JFrame {
    private ICrypto crypto = new CryptoImpl();
    private JTextField pathField, passField;
    private JTextArea console;
    private SecretKey currentKey;
    private Color primaryColor = new Color(41, 128, 185); // Bleu élégant
    private Color bgColor = new Color(44, 62, 80);      // Gris foncé

    public CryptoApp() {
        setTitle("Gemini Shield - AES Cryptography");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Panneau Principal avec dégradé ou couleur unie
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // --- SECTION HAUT : SELECTION ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        topPanel.setOpaque(false);

        pathField = createStyledTextField("Sélectionnez un fichier ou dossier...");
        JButton browseBtn = createStyledButton("Parcourir", primaryColor);
        
        JPanel pathRow = new JPanel(new BorderLayout(10, 10));
        pathRow.setOpaque(false);
        pathRow.add(pathField, BorderLayout.CENTER);
        pathRow.add(browseBtn, BorderLayout.EAST);

        passField = createStyledTextField("Entrez un mot de passe ou générez-en un...");
        JButton genKeyBtn = createStyledButton("Générer Aléatoire", new Color(39, 174, 96));
        
        JPanel passRow = new JPanel(new BorderLayout(10, 10));
        passRow.setOpaque(false);
        passRow.add(passField, BorderLayout.CENTER);
        passRow.add(genKeyBtn, BorderLayout.EAST);

        topPanel.add(pathRow);
        topPanel.add(passRow);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // --- SECTION MILIEU : CONSOLE ---
        console = new JTextArea();
        console.setBackground(new Color(33, 47, 61));
        console.setForeground(new Color(46, 204, 113));
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));
        console.setEditable(false);
        mainPanel.add(new JScrollPane(console), BorderLayout.CENTER);

        // --- SECTION BAS : ACTIONS ---
        JPanel botPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        botPanel.setOpaque(false);

        JButton encryptBtn = createStyledButton("CHIFFRER (AES-256)", new Color(192, 57, 43));
        JButton decryptBtn = createStyledButton("DÉCHIFFRER", primaryColor);
        JButton saveKeyBtn = createStyledButton("Enregistrer Clé (.key)", Color.GRAY);

        botPanel.add(encryptBtn);
        botPanel.add(decryptBtn);
        botPanel.add(saveKeyBtn);
        mainPanel.add(botPanel, BorderLayout.SOUTH);

        // --- LOGIQUE DES ÉVÉNEMENTS ---

        browseBtn.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser();
            jfc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if(jfc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(jfc.getSelectedFile().getAbsolutePath());
            }
        });

        genKeyBtn.addActionListener(e -> {
            log("Génération d'entropie via souris en cours...");
            currentKey = crypto.generateKey();
            passField.setText(crypto.bytesToHex(currentKey.getEncoded()));
            log("Clé sécurisée générée avec succès.");
        });

        saveKeyBtn.addActionListener(e -> {
            if(currentKey == null && !passField.getText().isEmpty()) {
                currentKey = crypto.generatePBEKey(passField.getText());
            }
            if(currentKey != null) {
                String keyPath = pathField.getText() + ".key";
                crypto.saveHexKey(currentKey, keyPath, "");
                log("Clé sauvegardée sous : " + keyPath);
            }
        });

        encryptBtn.addActionListener(e -> process(true));
        decryptBtn.addActionListener(e -> process(false));
    }

    private void process(boolean isEncrypt) {
        String path = pathField.getText();
        String password = passField.getText();
        File target = new File(path);

        if(!target.exists() || password.isEmpty()) {
            log("Erreur : Chemin ou mot de passe invalide.");
            return;
        }

        // Dérivé la clé à partir du mot de passe si non déjà générée
        if(currentKey == null) currentKey = crypto.generatePBEKey(password);

        log(isEncrypt ? "Démarrage du chiffrement..." : "Démarrage du déchiffrement...");

        if(target.isDirectory()) {
            // Règle : Ne pas rechiffrer un dossier .enc
            if(isEncrypt && target.getName().endsWith(".enc")) {
                log("Annulation : Le dossier semble déjà chiffré (.enc)");
                return;
            }
            String outPath = isEncrypt ? path + ".enc" : path.replace(".enc", "_restored");
            boolean success = crypto.cipherProcessFolder(currentKey, path, outPath, 
                    isEncrypt ? 1 : 0, false); // Utilise Cipher.ENCRYPT_MODE/DECRYPT_MODE
            log(success ? "Opération réussie sur le dossier." : "Erreur lors du traitement.");
        } else {
            String outPath = isEncrypt ? path + ".enc" : path.replace(".enc", "");
            boolean success = crypto.cipherProcess(currentKey, path, outPath, 
                    isEncrypt ? 1 : 0, false);
            log(success ? "Fichier traité : " + outPath : "Échec sur le fichier.");
        }
    }

    private void log(String msg) {
        console.append("[LOG] " + msg + "\n");
    }

    // --- HELPERS POUR LE STYLE ---
    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        return btn;
    }

    private JTextField createStyledTextField(String hint) {
        JTextField tf = new JTextField();
        tf.setBackground(new Color(52, 73, 94));
        tf.setForeground(Color.WHITE);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(127, 140, 141)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        tf.setToolTipText(hint);
        return tf;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CryptoApp().setVisible(true));
    }
}
