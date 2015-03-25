/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import static com.sun.org.apache.bcel.internal.classfile.Utility.toHexString;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author jhcuser
 */
final class ClientThread extends Thread {
    
    private final Socket clientSocket;
    private final int clientID;
    private final DataInputStream input;
    private final DataOutputStream output;
    //private final CipherInputStream input;
    private static byte[] bufferIn, keyCipher, keyMac, message;
    private static Cipher cipher;

    public ClientThread(Socket socket, int clientID) throws IOException {
        this.clientSocket = socket;
        this.clientID = clientID;
        // Para receber IV em claro
        this.input = new DataInputStream(clientSocket.getInputStream());
        // Para cifras sem IV
        //this.input = new CipherInputStream(clientSocket.getInputStream(), GlobalVariables.cipher);
        this.output = new DataOutputStream(clientSocket.getOutputStream());
        
    }
    
    @Override
    public void run() {
       
        /*
        BigInteger p, g;
        
        p = new BigInteger("99494096650139337106186933977618513974146274831566768179581759037259788798151499814653951492724365471316253651463342255785311748602922458795201382445323499931625451272600173180136123245441204133515800495917242011863558721723303661523372572477211620144038809673692512025566673746993593384600667047373692203583");
        g = new BigInteger("44157404837960328768872680677686802650999163226766694797650810379076416463147265401084491113667624054557335394761604876882446924929840681990106974314935015501571333024773172440352475358750668213444607353872754650805031912866692119819377041901642732455911509867728218394542745330014071040326856846990119719675");
        
        DHParameterSpec dhps = new DHParameterSpec(p, g);
        */
        
        // Acordo de chaves DH
        try {
            
            // Recebe chave publica do cliente
            bufferIn = new byte[input.readInt()];
            input.read(bufferIn);
            
            KeyFactory serverKeyFac = KeyFactory.getInstance("DH");
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bufferIn);
            PublicKey clientPubKey = serverKeyFac.generatePublic(x509KeySpec);
            
            DHParameterSpec dhps = ((DHPublicKey)clientPubKey).getParams();

            KeyPairGenerator serverKpairGen = KeyPairGenerator.getInstance("DH");
            serverKpairGen.initialize(dhps);
            KeyPair serverKpair = serverKpairGen.generateKeyPair();

            KeyAgreement serverKeyAgree = KeyAgreement.getInstance("DH");
            serverKeyAgree.init(serverKpair.getPrivate());
            
            byte[] serverPubKeyEnc = serverKpair.getPublic().getEncoded();
            
            output.writeInt(serverPubKeyEnc.length);
            output.flush();
            
            output.write(serverPubKeyEnc);
            output.flush();
            
            serverKeyAgree.doPhase(clientPubKey, true);

            byte[] serverSharedSecret = serverKeyAgree.generateSecret();
            
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(serverSharedSecret);
            serverSharedSecret = md.digest();
            
            keyCipher = new byte[16];
            keyMac = new byte[16];

            System.arraycopy(serverSharedSecret, 0, keyCipher, 0, 16);
            System.arraycopy(serverSharedSecret, 16, keyMac, 0, 16);

        } catch (IOException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        bufferIn = new byte[16];
        
        try {
            // Recebe IV (AES)
            input.read(bufferIn);
            initCipher(new IvParameterSpec(bufferIn));
            // Sem IV (RC4)
            //initCipher(null);
        } catch (IOException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        try {  
            
            SecretKey key = new SecretKeySpec(keyMac, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            
            int msgLength;
            
            try {
                while((msgLength = input.readInt()) >= 0) {
                    message = new byte[msgLength];
                    //Le msg para message (cifrada)
                    if(input.read(message) >= 0) {
                        //Le MAC do cliente para clientMac
                        byte[] clientMac = new byte[32];
                        if(input.read(clientMac) >= 0) {
                            byte[] serverMac = mac.doFinal(message);
                            if(Arrays.equals(clientMac, serverMac)) {
                                // -> Decifrar
                                message = cipher.doFinal(message);
                                System.out.println("Client " + clientID + ": " + new String(message));
                            } else {
                                System.out.println("Mensagem nao autenticada");
                            }
                        }
                    }

                } 
            } catch (EOFException ex) {
                //
            }
            
            System.out.println("=[Client " + clientID + "]=");
            input.close();
            clientSocket.close();
            GlobalVariables.clientThreadList.remove(this);
        
        } catch (IOException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | InvalidKeyException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private void initCipher(IvParameterSpec iv) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
        
        //String keyString = "xptoxptoxptoxpto";
        
        SecretKey key;
        
        if(iv == null) {
            key = new SecretKeySpec(keyCipher, "RC4");
            cipher = Cipher.getInstance("RC4");
            cipher.init(Cipher.DECRYPT_MODE, key);
        }
        else {
            key = new SecretKeySpec(keyCipher, "AES");
            //cipher = Cipher.getInstance("AES/CBC/NOPADDING");
            //cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            //cipher = Cipher.getInstance("AES/CFB8/PKCS5Padding");
            //cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher = Cipher.getInstance("AES/CFB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
        }
            
    }
    

}
