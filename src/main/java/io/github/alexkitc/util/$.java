package io.github.alexkitc.util;

import io.github.alexkitc.App;
import io.github.alexkitc.conf.Config;
import javafx.scene.control.Button;
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

    // TreeItem添加图标
}
