package server;

import java.io.IOException;
import java.net.*;

public class Server {

    int port;
    ServerSocket serverSocket;
    
    public Server(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }
    
    public static void main(String[] args) throws Exception {
        
        Server server = new Server(4567);
        int clientID = 1;

        while (true) {
            Socket clientSocket;
            clientSocket = server.serverSocket.accept();
            ClientThread clientThread;
            clientThread = new ClientThread(clientSocket, clientID++);
            clientThread.start();
            GlobalVariables.clientThreadList.add(clientThread);
        }
  }
}
