/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jhcuser
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import static com.sun.org.apache.bcel.internal.classfile.Utility.toHexString;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.X509EncodedKeySpec;
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
 * @author jhcusocketer aqui
 */
public class Client {

    /**
     * @param args the command line argumentsocket
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws javax.crypto.NoSuchPaddingException
     * @throws java.security.InvalidKeyException
     * @throws javax.crypto.IllegalBlockSizeException
     * @throws javax.crypto.BadPaddingException
     * @throws java.security.InvalidAlgorithmParameterException
     * @throws java.security.spec.InvalidParameterSpecException
     * @throws java.security.NoSuchProviderException
     * @throws java.security.spec.InvalidKeySpecException
     */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidParameterSpecException, NoSuchProviderException, InvalidKeySpecException {       
        
        
        // String host = args[0];
        String host = "localhost";
        //int port = Integer.parseInt(args[1]);
        int port = 4567;
        
        String inputString;
        byte[] inputBytes, outputBytes, keyCipher, keyMac;
        
        Socket socket = new Socket(host, port);
        
        // Para enviar em claro
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        
        // Para enviar cifrado
        //CipherOutputStream output;
        
        DataInputStream input = new DataInputStream(socket.getInputStream());
        
        // Acordo de chaves DH
        DHParameterSpec dhps;
        
        /*
        BigInteger p, g;
        p = new BigInteger("99494096650139337106186933977618513974146274831566768179581759037259788798151499814653951492724365471316253651463342255785311748602922458795201382445323499931625451272600173180136123245441204133515800495917242011863558721723303661523372572477211620144038809673692512025566673746993593384600667047373692203583");
        g = new BigInteger("44157404837960328768872680677686802650999163226766694797650810379076416463147265401084491113667624054557335394761604876882446924929840681990106974314935015501571333024773172440352475358750668213444607353872754650805031912866692119819377041901642732455911509867728218394542745330014071040326856846990119719675");
        dhps = new DHParameterSpec(p, g);
        */
        
        AlgorithmParameterGenerator apg = AlgorithmParameterGenerator.getInstance("DH");
        apg.init(1024);
        dhps = (DHParameterSpec)apg.generateParameters().getParameterSpec(DHParameterSpec.class);

        KeyPairGenerator clientKpairGen = KeyPairGenerator.getInstance("DH");
        clientKpairGen.initialize(dhps);
        KeyPair clientKpair = clientKpairGen.generateKeyPair();
        
        KeyAgreement clientKeyAgree = KeyAgreement.getInstance("DH");
        clientKeyAgree.init(clientKpair.getPrivate());

        byte[] clientPubKeyEnc = clientKpair.getPublic().getEncoded();     
        
        System.out.println(clientPubKeyEnc.length);
        
        output.writeInt(clientPubKeyEnc.length);
        output.flush();
        
        output.write(clientPubKeyEnc);
        output.flush();
        
        inputBytes = new byte[input.readInt()];
        
        input.read(inputBytes);
        
        KeyFactory clientKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(inputBytes);
        PublicKey serverPubKey = clientKeyFac.generatePublic(x509KeySpec);
        
        clientKeyAgree.doPhase(serverPubKey, true);
        
        byte[] clientSharedSecret = clientKeyAgree.generateSecret();
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(clientSharedSecret);
        clientSharedSecret = md.digest();
        
        keyCipher = new byte[16];
        keyMac = new byte[16];
        
        System.arraycopy(clientSharedSecret, 0, keyCipher, 0, 16);
        System.arraycopy(clientSharedSecret, 16, keyMac, 0, 16);
        
        
        SecretKey key = new SecretKeySpec(keyMac, "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        
        //String keyString = "xptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxptoxpto";
        //String keyString = "xptoxptoxptoxpto";
        
        // gerador de chaves de 128 bit
        // KeyGenerator kg = KeyGenerator.getInstance("RC4");
        // kg.init(128);
        // SecretKey key = kg.generateKey();
        
        // Cifra RC4
        //SecretKey key = new SecretKeySpec(keyString.getBytes(), "RC4");
        //Cipher cipher = Cipher.getInstance("RC4");
        //cipher.init(Cipher.ENCRYPT_MODE,key);
        
        // Cifra AES
        key = new SecretKeySpec(keyCipher, "AES");
        //Cipher cipher = Cipher.getInstance("AES/CBC/NOPADDING");
        //Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        //Cipher cipher = Cipher.getInstance("AES/CFB8/PKCS5Padding");
        //Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
        Cipher cipher = Cipher.getInstance("AES/CFB/NoPadding");
        
        // Vector de inicialização
        byte [] iv = "1234567812345678".getBytes();
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        
        output.write(iv);
        output.flush();
        
        try {
            
            while (true) {
                
                inputString = System.console().readLine();
                
                if (inputString == null || inputString.equals("quit")) {
                     break;
                }
                
                inputBytes = inputString.getBytes();
                
                // Contrução de array mutiplo de 16 bytes
                if(inputString.length() % 16 == 0) 
                    outputBytes = new byte[inputString.length()];
                else
                    outputBytes = new byte[(inputString.length() / 16) * 16 + 16];
                
                System.arraycopy(inputBytes, 0, outputBytes, 0, inputBytes.length);
                
                // -> Cifrar
                outputBytes = cipher.doFinal(outputBytes);
                
                // Envia msg cifrada
                //outputBytes = new byte[inputBytes.length + 1];
                //System.arraycopy(inputBytes, 0, outputBytes, 0, inputBytes.length);
                //outputBytes[inputBytes.length] = '\n';
                
                output.writeInt(outputBytes.length);
                output.flush();
                
                output.write(outputBytes);
                output.flush();
                
                // Envia MAC
                output.write(mac.doFinal(outputBytes));
                output.flush();
            }
            
            output.close();
            socket.close();
            
        } catch (UnknownHostException e) {
           // check spelling of hostname
        } catch (ConnectException e) {
           // connection refused - is server down? Try another port.
        } catch (NoRouteToHostException e) {
           // The connect attempt timed output.  Try connecting through a proxy
        } catch (IOException e) {
           // another error occurred
        }
    }
}

