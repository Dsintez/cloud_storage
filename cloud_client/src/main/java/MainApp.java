import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        MainApp.launch(args);
    }


    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("fxml/mainWindow.fxml"));
        primaryStage.setTitle("Cloud_storage");
        primaryStage.setScene(new Scene(root, 1200, 600));
        //Image icon = new Image(String.valueOf(getClass().getResource("*.png")));
        //primaryStage.getIcons().add(icon);
        primaryStage.show();
    }
}
