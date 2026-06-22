
import com.dcssi.cfc.crypto.CryptoImpl;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ousmane3ndiaye
 */
public class TestHachage {
    public static void main(String[] args) throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-384");
        String mdp="Bonjour Chiffreur";
        byte[] hsh = md.digest(mdp.getBytes());
        System.out.println(Base64.getEncoder().encodeToString(hsh));
        mdp="Bonjour  Chiffreur";
        hsh = md.digest(mdp.getBytes());
        System.out.println(Base64.getEncoder().encodeToString(hsh));
        
        
        Mac mac=Mac.getInstance("HmacSHA256");
        KeyGenerator kg=KeyGenerator.getInstance("AES");
        SecretKey key = kg.generateKey();
        mac.init(key);
        byte[] hmac = mac.doFinal(mdp.getBytes());
        System.out.println(Base64.getEncoder().encodeToString(hmac));
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
    }
    
}
















