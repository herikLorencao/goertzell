package goertzell;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class ExemploServidor {
    public static void main(String[] args) {
        Integer porta = 8000;
        SocketServer socketServer = new SocketServer(porta);

        Socket client = socketServer.getClientConnection();

        try {
            PrintStream printStream = new PrintStream(client.getOutputStream());

            // lógica de conta Goertzell

            for (int i = 0; i < 200; i++) {
                printStream.println(i);
                TimeUnit.SECONDS.sleep(1);
            }

            client.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
