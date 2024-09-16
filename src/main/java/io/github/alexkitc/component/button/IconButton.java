package io.github.alexkitc.component.button;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/9/5 22:05
 */
public class IconButton extends JButton {

    /**
     * @apiNote 带图标的按钮
     * @param path 图标路径
     * @param size 图标尺寸
     * @param width 按钮宽
     * @param height 按钮高
     */
    public IconButton(String path, int size, int width, int height) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(path)));
        Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        setIcon(scaledIcon);

        setMaximumSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setSize(new Dimension(width, height));
    }

    /**
     * @apiNote 带图标+文本提示的按钮
     * @param path 图标路径
     * @param size 图标尺寸
     * @param width 按钮宽
     * @param height 按钮高
     * @param tooltips 提示文本
     */
    public IconButton(String path, int size, int width, int height, String tooltips) {
        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(path)));
        Image scaledImage = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaledImage);
        setIcon(scaledIcon);

        setMaximumSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setSize(new Dimension(width, height));

        setToolTipText(tooltips);
    }
}
