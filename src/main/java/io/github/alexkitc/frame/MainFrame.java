package io.github.alexkitc.frame;

import io.github.alexkitc.component.button.IconButton;
import io.github.alexkitc.component.jtree.ConnTreeCellRenderer;
import io.github.alexkitc.component.jtree.JTreeIconNode;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.util.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static io.github.alexkitc.conf.Vars.*;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/9/5 21:45
 */
public class MainFrame extends JFrame {
    public MainFrame() {
        init();
        setVisible(true);
    }

    // 初始化基础布局+控件
    private void init() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(APP_TITLE);
        setSize(APP_WIDTH,APP_HEIGHT);


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        // 1. 顶部按钮
        JPanel northPanel = renderFuncButtonList();
        mainPanel.add(northPanel, BorderLayout.NORTH);

        // 2. SplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 3. JTree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(JTREE_ROOT_TEXT);
        // 获取IConNode列表
        List<ConnItem> connItemList = Utils.readConnItemList();
        connItemList.forEach(connItem -> {
            JTreeIconNode node = new JTreeIconNode(connItem.getName(), CONN_ICON_DB_MYSQL_PATH0);
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
            root.add(treeNode);
        });
        DefaultTreeModel model = new DefaultTreeModel(root);
        JTree tree = new JTree(model);
        tree.setCellRenderer(new ConnTreeCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tree);
        splitPane.setLeftComponent(scrollPane);

        // 4. JTabPane
        DefaultTableModel tableModel = new DefaultTableModel(
                new Object[][]{
                        {"Row 1", "Data 1", "Data 2"},
                        {"Row 2", "Data 3", "Data 4"},
                        {"Row 3", "Data 5", "Data 6"}
                },
                new String[]{"Column 1", "Column 2", "Column 3"}
        );
        JTable rightTable = new JTable(tableModel);
        JScrollPane rightScrollPane = new JScrollPane(rightTable);
        splitPane.setRightComponent(rightScrollPane);

        add(mainPanel);

    }

    // 1.渲染顶部按钮
    private JPanel renderFuncButtonList() {
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS));
        JButton newConnButton = new IconButton(ICON_NEW_CONN, ICON_SIZE, 36, 28, ICON_NEW_CONN_TOOLTIPS);

        northPanel.add(newConnButton);
        northPanel.add(new JButton("btn2"));
        northPanel.add(new JButton("btn3"));
        northPanel.add(new JButton("btn4"));

        return northPanel;
    }

}
