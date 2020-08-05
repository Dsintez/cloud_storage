package view;

import IO.FileUtility;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {

    private static final int defaultPort = 8189;
    private static final int defaultSizeBuffer = 8192;
    @FXML
    private ListView listServer;
    @FXML
    private ListView listClient;
    @FXML
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick = "MyNick";
    private File homeDirectory;

    public void download(ActionEvent actionEvent) {
        String message;
        String fileName = (String) listServer.getSelectionModel().getSelectedItems().get(0);
        if ("Directory".equals(fileName.split(" ")[0])) return;
        fileName = fileName.split(" ")[1];
        try {
            out.writeUTF("download " + fileName);//fileName.getText());
            message = in.readUTF();
            if (message.equals("send")) {
                File file = new File(homeDirectory, fileName);
                file.createNewFile();
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    byte[] buffer = new byte[defaultSizeBuffer];
                    while (in.available() > 0) {
                        int r = in.read(buffer);
                        fout.write(buffer, 0, r);
                        try {
                            Thread.sleep(4);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void upload(ActionEvent actionEvent) {
        String message;
        String fileName = (String) listClient.getSelectionModel().getSelectedItems().get(0);
        if ("Directory".equals(fileName.split(" ")[0])) return;
        fileName = fileName.split(" ")[1];
        File file = new File(homeDirectory, fileName);
        try (FileInputStream fin = new FileInputStream(file)) {
            out.writeUTF("upload " + fileName);
            byte[] buffer = new byte[defaultSizeBuffer];
            while (fin.available() > 0) {
                int r = fin.read(buffer);
                out.write(buffer, 0, r);
            }
            message = in.readUTF();
            if ("successful".equals(message)) {
                System.out.printf("Файл %s отправлен%n", fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getListFilesClient() {
        List<String> listFiles = FileUtility.getListFiles(homeDirectory, "");
        listClient.getItems().addAll(listFiles);
    }

    public void getListFilesServer() {
        try {
            out.writeUTF("getListFiles"); // Тут должна еще посылаться выбранная папка.
            int countFiles = in.readInt();
            for (int i = 0; i < countFiles; i++) {
                listServer.getItems().add(in.readUTF());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*private String getFileNameAndSize(File dir, File file) {
        Long size = file.getTotalSpace();
        StringBuilder stringBuilder = new StringBuilder(dir.getName());
        stringBuilder.append("/");
        stringBuilder.append(file.getName());
        stringBuilder.append(" ");
        stringBuilder.append(size);
        return stringBuilder.toString();
    }*/

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        homeDirectory = new File("cloud/client/" + nick);

        try {
            socket = new Socket("localhost", defaultPort);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            out.writeUTF(nick); // and password
            getListFilesClient();
            getListFilesServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
