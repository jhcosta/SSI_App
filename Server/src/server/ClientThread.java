/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 *
 * @author jhcuser
 */
final class ClientThread extends Thread {
    
    private final Socket clientSocket;
    private final InputStream input;
    private final byte[] bufferIn;

    public ClientThread(Socket socket) throws IOException {
        clientSocket = socket;
        input = clientSocket.getInputStream();
        bufferIn = new byte[5];
    }
    
    @Override
    public void run() {
        try {        
            int numBytes;
            while((numBytes = input.read(bufferIn)) >= 0) {
                readInputStream(numBytes);
            }
            System.out.println("END");
            input.close();
            clientSocket.close();
            GlobalVariables.clientThreadList.remove(this);
        } catch (IOException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            Logger.getLogger(ClientThread.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    private void readInputStream(int numBytes) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        
        byte[] bufferOut, aux;
        
        if(numBytes < bufferIn.length || bufferIn[bufferIn.length - 1] == '\n') {
            numBytes--;
        } 

        bufferOut = new byte[numBytes];
        System.arraycopy(bufferIn, 0, bufferOut, 0, numBytes);

        while(numBytes == bufferIn.length) {
            
            numBytes = input.read(bufferIn);
            if(numBytes < bufferIn.length || bufferIn[bufferIn.length - 1] == '\n') {
                numBytes--;
            }
            aux = new byte[bufferOut.length];
            System.arraycopy(bufferOut, 0, aux, 0, aux.length);
            bufferOut = new byte[aux.length + numBytes];
            System.arraycopy(aux, 0, bufferOut, 0, aux.length);
            System.arraycopy(bufferIn, 0, bufferOut, aux.length, numBytes);
        }
        // -> Dec
        //cipher.init(Cipher.DECRYPT_MODE,key);
        bufferOut = GlobalVariables.cipher.doFinal(bufferOut);
        System.out.println(new String(bufferOut));
    }
}
