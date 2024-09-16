package io.github.alexkitc.component.jtree;

import lombok.Data;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static io.github.alexkitc.conf.Vars.ICON_SIZE;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/9/5 22:53
 */
@Data
public class JTreeIconNode {
    private String name;
    private ImageIcon icon;

    public JTreeIconNode(String name, String iconPath) {
        this.name = name;
        // 加载图标
        this.icon = new ImageIcon(Objects.requireNonNull(this.getClass().getResource(iconPath)));
        Image scaledImage = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        setIcon(scaledIcon);
    }
}
