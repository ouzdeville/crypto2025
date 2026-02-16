package dcssi.cfc.chat;

public class Fenetre extends javax.swing.JFrame {
    private javax.swing.JTextArea textArea;

    public Fenetre() {
        initComponents();
    }

    private void initComponents() {
        textArea = new javax.swing.JTextArea();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        textArea.setColumns(20);
        textArea.setRows(5);
        getContentPane().add(new javax.swing.JScrollPane(textArea), java.awt.BorderLayout.CENTER);
        pack();
        this.setVisible(true);
    }

    public void ajouterMessage(String message) {
        textArea.append(message + "\n");
    }


}
