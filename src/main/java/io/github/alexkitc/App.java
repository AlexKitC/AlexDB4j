package io.github.alexkitc;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.controller.HomeController;
import io.github.alexkitc.controller.NewConnController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.BoxBlur;
import javafx.scene.image.Image;
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
    public static NewConnController newConnControllerInstance;

    @Override
    public void start(Stage stage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_HOME_FILE_PATH));

        try {
            Parent root = loader.load();
            root.getStylesheets().add(Objects.requireNonNull(getClass().getResource(APP_CSS_PATH)).toExternalForm());
            homeControllerInstance = loader.getController();

            // 添加BoxBlur效果
            BoxBlur boxBlur = new BoxBlur(10, 10, 3);
            root.setEffect(boxBlur);

            Scene scene = new Scene(root, Config.APP_WIDTH, Config.APP_HEIGHT);

            stage.setScene(scene);

            stage.setTitle(Config.APP_TITLE + " Designed By " + Config.APP_AUTHOR + " App Version: " + Config.APP_VERSION);
            stage.getIcons().add(new Image(Config.APP_AUTHOR_ICO));
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static void main(String[] args) {
        launch(args);
    }
}
