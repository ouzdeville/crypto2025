package GUI;

import javax.swing.*;

import dcssi.cfc.crypto.CryptoImpl;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.awt.*;
import java.io.File;

public class CryptoGUI extends JFrame {
    private JTextArea textArea;
    private JTextField keyField;
    private JComboBox<String> modeCombo;
    private JTextArea debugOutput;
    private CryptoImpl crypto = new CryptoImpl();
    private SecretKey currentKey;

    public CryptoGUI() {
        setTitle("Crypto - File & Folder Security");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- Zone de texte principale (Haut) ---
        textArea = new JTextArea(10, 50);
        add(new JScrollPane(textArea), BorderLayout.NORTH);

        // --- Panneau Central (Contrôles) ---
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        // Ligne 1 : Mode
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        modePanel.add(new JLabel("Mode:"));
        modeCombo = new JComboBox<>(new String[]{"Encrypt File", "Decrypt File", "Encrypt Folder", "Decrypt Folder"});
        modePanel.add(modeCombo);
        
        // Ligne 2 : Clé
        JPanel keyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        keyPanel.add(new JLabel("Save OR Enter a Key:"));
        keyField = new JTextField(30);
        keyPanel.add(keyField);
        JButton saveKeyBtn = new JButton("Save Key");
        JButton loadKeyBtn = new JButton("Load Key");
        keyPanel.add(saveKeyBtn);
        keyPanel.add(loadKeyBtn);

        // Ligne 3 : Actions
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearBtn = new JButton("Clear Text Box");
        JButton genKeyBtn = new JButton("Gen Key");
        JButton goBtn = new JButton("GO");
        goBtn.setBackground(Color.GREEN);
        genKeyBtn.setBackground(Color.YELLOW);
        clearBtn.setBackground(Color.RED);
        actionPanel.add(clearBtn);
        actionPanel.add(genKeyBtn);
        actionPanel.add(goBtn);

        centerPanel.add(modePanel);
        centerPanel.add(keyPanel);
        centerPanel.add(actionPanel);
        add(centerPanel, BorderLayout.CENTER);

        // --- Console de Debug (Bas) ---
        debugOutput = new JTextArea(8, 50);
        debugOutput.setEditable(false);
        debugOutput.setBackground(new Color(240, 240, 240));
        add(new JScrollPane(debugOutput), BorderLayout.SOUTH);

        // --- LOGIQUE DES BOUTONS ---

        // Générer une clé avec la souris
        genKeyBtn.addActionListener(e -> {
            log("Moving mouse for entropy... Please wait.");
            currentKey = crypto.generateKey();
            keyField.setText(crypto.bytesToHex(currentKey.getEncoded()));
            log("Created a new Key!");
        });

        // Bouton GO (Exécution)
        goBtn.addActionListener(e -> executeCryptoAction());

        // Sauvegarder la clé dans un fichier
        saveKeyBtn.addActionListener(e -> {
            if (currentKey != null) {
                crypto.saveHexKey(currentKey, "secret.key", "");
                log("Key saved to secret.key");
            }
        });
    }

    private void executeCryptoAction() {
        String modeStr = (String) modeCombo.getSelectedItem();
        log("Running using Mode: " + modeStr);

        JFileChooser chooser = new JFileChooser();
        if (modeStr.contains("Folder")) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }

        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File source = chooser.getSelectedFile();
            String destination = source.getAbsolutePath() + (modeStr.contains("Encrypt") ? ".enc_out" : "_dec_out");

            int mode = modeStr.contains("Encrypt") ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE;
            
            boolean success;
            if (modeStr.contains("Folder")) {
                success = crypto.cipherProcessFolder(currentKey, source.getAbsolutePath(), destination, mode, false);
            } else {
                success = crypto.cipherProcess(currentKey, source.getAbsolutePath(), destination, mode, false);
            }

            if (success) {
                log("SUCCESS: Operation completed.");
                log("Output: " + destination);
            } else {
                log("ERROR: Operation failed.");
            }
        }
    }

    private void log(String msg) {
        debugOutput.append(msg + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CryptoGUI().setVisible(true));
    }
}