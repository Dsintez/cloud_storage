package controllers;

import IO.FileUtility;
import NIO.FileInfo;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainWindow implements Initializable {

    private static final int defaultPort = 8189;
    private static final int defaultSizeBuffer = 8192;
    private static final String host = "localhost";
    @FXML
    private VBox clientPanel;
    @FXML
    private VBox serverPanel;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String nick = "MyNick";
    private Path homeDirectory;
    private String serverDirectory;
    private List<FileInfo> filesFromServer;

    private PanelController clientPC;
    private PanelController serverPC;

    public void download(ActionEvent actionEvent) {
        String message;
        String fileName = null;// = (String) filesTable.getSelectionModel().getSelectedItems().get(0);
        if ("Directory".equals(fileName.split(" ")[0])) return;
        fileName = fileName.split(" ")[1];
        try {
            dos.writeUTF("./download " + fileName);//fileName.getText());
            message = dis.readUTF();
            if (message.equals("send")) {
                File file = new File(homeDirectory.toFile(), fileName);
                file.createNewFile();
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    byte[] buffer = new byte[defaultSizeBuffer];
                    while (dis.available() > 0) {
                        int r = dis.read(buffer);
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
        String fileName = null; //(String) filesTable.getSelectionModel().getSelectedItems().get(0);
        if ("Directory".equals(fileName.split(" ")[0])) return;
        fileName = fileName.split(" ")[1];
        File file = new File(homeDirectory.toFile(), fileName);
        try (FileInputStream fin = new FileInputStream(file)) {
            dos.writeUTF("./upload " + fileName);
            byte[] buffer = new byte[defaultSizeBuffer];
            while (fin.available() > 0) {
                int r = fin.read(buffer);
                dos.write(buffer, 0, r);
            }
            message = dis.readUTF();
            if ("successful".equals(message)) {
                System.out.printf("Файл %s отправлен%n", fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnExit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void btnReconnect(ActionEvent actionEvent) {
        connectToServer();
    }

    private void connectToServer() {
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clientPC.setListFilesForClient(homeDirectory);
        try {
            socket = new Socket(host, defaultPort);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(nick); // and password
            serverDirectory = dis.readUTF();
            filesFromServer = serverPC.getListFilesFromServer(dis, dos);
            serverPC.setListFilesForServer(filesFromServer, serverDirectory);
            serverPC.getDisksBox().getItems().add(nick);
            serverPC.getDisksBox().getSelectionModel().select(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Ник");
        textInputDialog.setHeaderText("Введите ваше имя.");
        textInputDialog.setContentText("Nick name: ");
        nick = textInputDialog.showAndWait().get();
        homeDirectory = Paths.get("./cloud/client/" + nick);
        clientPC = (PanelController) clientPanel.getProperties().get("ctrl");
        clientPC.setServer(false);
        serverPC = (PanelController) serverPanel.getProperties().get("ctrl");
        serverPC.setServer(true);
        connectToServer();
        clientPC.getDisksBox().getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            clientPC.getDisksBox().getItems().add(p.toString());
        }
        clientPC.getDisksBox().getSelectionModel().select(0);
    }

    public void btnDisconnect(ActionEvent actionEvent) {
        try {
            dos.writeUTF("./exit");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
