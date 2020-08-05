import handler.ClientHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private static int defaultPort = 8189;

    DataInputStream is;
    DataOutputStream os;
    ServerSocket server;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            new Server(Integer.parseInt(args[0]));
        } else {
            new Server(defaultPort);
        }
    }

    public Server(int port) throws IOException {
        server = new ServerSocket(port);
        Socket socket;
        String clientName;
        while (true) {
            socket = server.accept();
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            clientName = is.readUTF();
            System.out.printf("Клиент %s подключился%n", clientName);
            os.writeUTF(clientName);
            new ClientHandler(socket, clientName).run();
        }
    }
}
