/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author jhcuser aqui
 */
public class Client {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws javax.crypto.NoSuchPaddingException
     * @throws java.security.InvalidKeyException
     * @throws javax.crypto.IllegalBlockSizeException
     * @throws javax.crypto.BadPaddingException
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        
        // String host = args[0];
        String host = "localhost";
        //int port = Integer.parseInt(args[1]);
        int port = 1000;
        
        String keyString = "xptoxptoxptoxpto";
        //KeyGenerator kg = KeyGenerator.getInstance("RC4");
        // Inicializa gerador de chaves (128bit) e gera chave
        //kg.init(128);
        //SecretKey key = kg.generateKey();
        SecretKey key = new SecretKeySpec(keyString.getBytes(),"RC4");
        Cipher cipher = Cipher.getInstance("RC4");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        
        String inputString;
        byte[] inputBytes, outputBytes;

        try {
            Socket s = new Socket(host, port);
            //BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            //PrintWriter out = new PrintWriter(s.getOutputStream());
            OutputStream out = s.getOutputStream();
            while (true) {
                inputString = System.console().readLine();
                if (inputString == null || inputString.equals("quit")) 
                    break;
                
                inputBytes = inputString.getBytes();
                
                // -> Enc
                //cipher.init(Cipher.ENCRYPT_MODE,key);
                inputBytes = cipher.doFinal(inputBytes);
                
                outputBytes = new byte[inputBytes.length + 1];
                System.arraycopy(inputBytes, 0, outputBytes, 0, inputBytes.length);
                outputBytes[inputBytes.length] = '\n';
                
                out.write(outputBytes);
                //out.println(inputString + '\n');
                out.flush();
            }
            //in.close();
            out.close();
            s.close();
        } catch (UnknownHostException e) {
           // check spelling of hostname
        } catch (ConnectException e) {
           // connection refused - is server down? Try another port.
        } catch (NoRouteToHostException e) {
           // The connect attempt timed out.  Try connecting through a proxy
        } catch (IOException e) {
           // another error occurred
        }
    }
}
