package io.github.alexkitc.util;

import io.github.alexkitc.App;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Objects;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/20 上午11:43
 */
public class $ {

    public static boolean isEmpty(Object o) {
        return o == null || o.toString().trim().isEmpty();
    }

    // 新建连接 按钮添加图标
    public static void addButtonIcon(Button button, String icon, double size, String tooltip) {
        Image image = new Image(Objects.requireNonNull(App.class.getResourceAsStream(icon)));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        button.setGraphic(imageView);

        button.setTooltip(new Tooltip(tooltip));
    }

    // 弹框info提示
    public static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        // 显示 Alert 对话框
        alert.showAndWait();
    }

    // 弹框warning提示
    public static void warning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        // 显示 Alert 对话框
        alert.showAndWait();
    }

    // 弹框确认提示
    public static ButtonType confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);

        // 显示 Alert 对话框
        alert.showAndWait();
        return alert.getResult();
    }
}
