package handler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private Socket socket;
    private String clientName;
    private File homeDirectory;
    private DataInputStream in;
    private DataOutputStream out;

    private static final int defaultSizeBuffer = 8192;

    public ClientHandler(Socket socket, String clientName) {
        this.socket = socket;
        this.clientName = clientName;
        this.homeDirectory = new File("cloud/server/" + clientName);
    }

    @Override
    public void run() {
        String[] messages = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            boolean work = true;
            while (work) {
                String message = in.readUTF();
                messages = message.split(" ");
                switch (messages[0]) {
                    case "exit":
                        work = false;
                        System.out.printf("Клиент %s отключился(сам)%n", clientName);
                        break;
                    case "download":
                        download(messages[1]);
                        break;
                    case "upload":
                        upload(messages[1]);
                        break;
                    case "getListFiles":
                        if (messages.length < 2) {
                            getListFiles("/");
                        } else {
                            getListFiles(messages[1]);
                        }
                        break;
                }
            }
        } catch (SocketException e) {
            System.out.printf("Клиент %s отключился%n", clientName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getListFiles(String path) throws IOException {
        File file = new File(homeDirectory, path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            out.writeInt(files.length);
            for (File file1 : files) {
                out.writeUTF((file1.isFile()? "File " : "Directory ") + file1.getName());
            }
        } else {
            out.writeInt(0);
            System.out.println("Выбран файл");
        }
    }

    private void upload(String fileName) throws IOException {
        System.out.println("fileName: " + fileName);
        File file = new File(homeDirectory, fileName);
        createDir(file.getParentFile());
        if (!file.exists()) {
            file.createNewFile();
        }
        try (FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[defaultSizeBuffer];
            while (in.available() > 0) {
                int r = in.read(buffer);
                os.write(buffer, 0, r);
            }
        }
        out.writeUTF("successful");
        System.out.printf("File %s uploaded!%n", fileName);
    }

    private void download(String fileName) throws IOException {
        File file = new File(homeDirectory, fileName);
        try (FileInputStream is = new FileInputStream(file)) {
            out.writeUTF("send"); // the file is found
            byte[] buffer = new byte[defaultSizeBuffer];
            while (is.available() > 0) {
                int r = is.read(buffer);
                out.write(buffer, 0, r);

            }
        } catch (FileNotFoundException e) {
            out.writeUTF("the file is not found");
            System.out.printf("Файл %s не найден%n", fileName);
        }
    }

    private void createDir(File file) {
        File folder = file.getParentFile();
        if (!folder.exists()) {
            createDir(folder);
        }
        file.mkdir();
    }
}