package io.github.alexkitc;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.alexkitc.frame.MainFrame;

import javax.swing.*;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/9/5 21:25
 */
public class App {
    public static void main(String[] args) {
        // 使用主题
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        //渲染主Frame
        SwingUtilities.invokeLater(MainFrame::new);
    }
}