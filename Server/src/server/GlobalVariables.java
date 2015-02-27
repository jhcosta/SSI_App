/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author jhcuser
 */
public class GlobalVariables {
    
    public static ArrayList<ClientThread> clientThreadList;
    public static Cipher cipher;
    
    static {
        try {
            clientThreadList = new ArrayList<>();
            String keyString = "xptoxptoxptoxpto";
            SecretKey key = new SecretKeySpec(keyString.getBytes(),"RC4");
            cipher = Cipher.getInstance("RC4");
            cipher.init(Cipher.DECRYPT_MODE,key);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            Logger.getLogger(GlobalVariables.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /*
    public void sendToOtherClients(ClientThread client, String str) throws IOException {
        
      Iterator<ClientThread> itr = clientThreadList.iterator();
      while(itr.hasNext()) {
         ClientThread element = itr.next();
         if(element != client) {
                element.output.print(str);
                element.output.flush();
         }
      }
    }
    */
    public void printClientThreadList() {
        Iterator<ClientThread> itr = clientThreadList.iterator();
        System.out.println("Number of Clients: " + clientThreadList.size());
    }
}
