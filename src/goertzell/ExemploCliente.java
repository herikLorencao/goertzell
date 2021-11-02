package goertzell;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ExemploCliente {
    public static void main(String[] args) {
        String endereco = "127.0.0.1";
        Integer porta = 8000;

        try {
            Socket cliente = new Socket(endereco, porta);
            Scanner scanner = new Scanner(cliente.getInputStream());

            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }

            scanner.close();
            cliente.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
