package goertzell;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private ServerSocket serverSocket;
    private Socket clientConnection;
    private final Integer port;

    public SocketServer(Integer port) {
        this.port = port;
        this.startServer();
    }

    public Socket getClientConnection() {
        return clientConnection;
    }

    private void startServer() {
        try {
            this.serverSocket = new ServerSocket(port);
            System.out.println("Servidor Socket aberto na porta: " + port);

            this.listenClient();
        } catch (IOException e) {
            throw new RuntimeException("O socket não pode ser iniciado!");
        }
    }

    private void listenClient() throws IOException {
        clientConnection = this.serverSocket.accept();
        System.out.println("Novo cliente conectado: " + clientConnection.getInetAddress().getHostAddress());
    }
}
