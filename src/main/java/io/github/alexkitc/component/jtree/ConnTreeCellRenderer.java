package io.github.alexkitc.component.jtree;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Objects;

import static io.github.alexkitc.conf.Vars.CONN_ICON_PATH0;
import static io.github.alexkitc.conf.Vars.ICON_SIZE;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote 重写JTree的渲染器
 * @since 2024/9/5 23:03
 */
public class ConnTreeCellRenderer implements TreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (leaf) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            JTreeIconNode data = (JTreeIconNode) node.getUserObject();
            JLabel label = new JLabel(data.getName());
            label.setIcon(data.getIcon());
            if (selected) {
                label.setBackground(Color.gray);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(tree.getBackground());
                label.setForeground(tree.getForeground());
            }
            return label;
        } else {
            // 根结点
            JLabel label = new JLabel(value.toString());
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource(CONN_ICON_PATH0)));
            Image scaledImage = icon.getImage().getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            label.setIcon(scaledIcon);
            return label;
        }

    }
}
