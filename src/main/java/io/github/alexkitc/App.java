package io.github.alexkitc;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.controller.HomeController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

import static io.github.alexkitc.conf.Config.APP_CSS_PATH;
import static io.github.alexkitc.conf.Config.FXML_HOME_FILE_PATH;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/17 下午10:22
 */
public class App extends Application {

    public static HomeController homeControllerInstance;

    @Override
    public void start(Stage stage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_HOME_FILE_PATH));

        try {
            Parent root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource(APP_CSS_PATH)).toExternalForm());
            homeControllerInstance = loader.getController();

            stage.setScene(new Scene(root, Config.APP_WIDTH, Config.APP_HEIGHT));

            stage.setTitle(Config.APP_TITLE);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static void main(String[] args) {
        launch(args);
    }
}
