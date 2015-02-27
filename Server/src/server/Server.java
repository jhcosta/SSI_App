package server;

import java.io.IOException;
import java.net.*;

public class Server {

    int port;
    ServerSocket serverSocket;
    
    public Server() throws IOException {
        port = 1000;
        serverSocket = new ServerSocket(port);
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server();
        while (true) {
            Socket clientSocket;
            clientSocket = server.serverSocket.accept();
            ClientThread clientThread;
            clientThread = new ClientThread(clientSocket);
            clientThread.start();
            GlobalVariables.clientThreadList.add(clientThread);
        }
  }
}
