package controllers;

import IO.FileUtility;
import NIO.FileInfo;
import com.sun.corba.se.spi.activation.Server;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PanelController implements Initializable {
    @FXML
    private TextField pathField;
    @FXML
    private ComboBox<String> disksBox;
    @FXML
    private TableView<FileInfo> filesTable;
    private ContextMenu tableConMenu;
    private boolean isServer;

    public void setServer(boolean server) {
        isServer = server;
    }

    public ComboBox<String> getDisksBox() {
        return disksBox;
    }

    public void setListFilesForServer(List<FileInfo> filesForServer, String path) {
        pathField.setText("\\");
        filesTable.getItems().clear();
        filesTable.getItems().addAll(filesForServer);
        filesTable.sort();
    }

    public void setListFilesForClient(Path path) {
        if (!path.toFile().exists()) FileUtility.createDir(path.toFile());
        try {
            pathField.setText(path.normalize().toAbsolutePath().toString());
            filesTable.getItems().clear();
            filesTable.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
            filesTable.sort();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING, "По какой-то причине не удалось обновить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void btnPathUpAction(ActionEvent actionEvent) {
        if (!isServer) {
            Path upperPath = Paths.get(pathField.getText()).getParent();
            if (upperPath != null) {
                setListFilesForClient(upperPath);
            }
        }
    }

    public void selectDiskAction(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        setListFilesForClient(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public TextField getPathField() {
        return pathField;
    }

    public List<FileInfo> getListFilesFromServer(DataInputStream dis, DataOutputStream dos) {
        ArrayList<FileInfo> listFiles = new ArrayList<FileInfo>();
        try {
            dos.writeUTF("./getListFiles " + pathField.getText());
            FileInfo fileInfo;
            int counter = dis.readInt();
            for (int i = 0; i < counter; i++) {
                fileInfo = new FileInfo();
                String fileType = dis.readUTF();
                if ("F".equals(fileType)) {
                    fileInfo.setType(FileInfo.FileType.FILE);
                } else if ("D".equals(fileType)) {
                    fileInfo.setType(FileInfo.FileType.DIRECTORY);
                }
                fileInfo.setFileName(dis.readUTF());
                fileInfo.setSize(dis.readLong());
                fileInfo.setLastModified(LocalDateTime.parse(dis.readUTF()));
                listFiles.add(fileInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listFiles;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);

        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(150);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setPrefWidth(80);
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text;
                        if (item < 1024L) {
                            text = String.format("%,d bytes", item);
                        } else if (item < 1048576L) {
                            text = String.format("%,.2f kilobytes", item / 1024.0);
                        } else if (item < 1073741824L) {
                            text = String.format("%,.2f megabytes", item / 1048576.0);
                        } else {
                            text = String.format("%,.2f gigabytes", item / 1073741824.0);
                        }
                        if (item == -1L) {
                            text = "[DIR]";
                        }
                        setText(text);
                    }
                }
            };
        });

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
        TableColumn<FileInfo, String> fileDateColumn = new TableColumn<>("Дата изменения");
        fileDateColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified().format(dtf)));
        fileDateColumn.setPrefWidth(140);

        filesTable.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn, fileDateColumn);
        filesTable.getSortOrder().add(fileTypeColumn);

        filesTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (event.getClickCount() == 2) {
                        if (!isServer) {
                            Path path = Paths.get(pathField.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
                            if (Files.isDirectory(path)) {
                                setListFilesForClient(path);
                            }
                        } else {
                            FileInfo fileInfo = filesTable.getSelectionModel().getSelectedItem();
                            if (fileInfo.getType() == FileInfo.FileType.DIRECTORY) {
                                pathField.setText(pathField.getText() + "\\" + fileInfo.getFileName());
                                System.out.println(Paths.get(pathField.getText()).getParent().toString());
                            }
                        }
                    }
                }
            }
        });
        tableConMenu = new ContextMenu();
        MenuItem item1 = new MenuItem("Новая папка");
        item1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Новая папка");
                textInputDialog.setHeaderText("Введите имя папки.");
                textInputDialog.setContentText("Имя папки: ");
                if (!isServer) {
                    File file = new File(pathField.getText(), textInputDialog.showAndWait().get());
                    FileUtility.createDir(file);
                    setListFilesForClient(Paths.get(pathField.getText()));
                } else {

                }
            }
        });
        MenuItem item2 = new MenuItem("Копировать с/на сервер");
        item2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                FileInfo fileInfo = filesTable.getSelectionModel().getSelectedItem();
                System.out.println(fileInfo.getFileName());
            }
        });
        MenuItem item3 = new MenuItem("Переименовать");

        MenuItem item4 = new MenuItem("Переместить");

        MenuItem item5 = new MenuItem("...");
        tableConMenu.getItems().addAll(item1, item2, item3, item4, item5);
        filesTable.setContextMenu(tableConMenu);
    }
}
