package io.github.alexkitc.entity;

import io.github.alexkitc.entity.enums.TreeNodeType;
import javafx.scene.control.TreeItem;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/20 上午11:24
 */
@Data
@Accessors(chain = true)
public class TreeNode {

    // 节点的显示名
    private String name;

    // 节点类型
    private TreeNodeType treeNodeType;

    // 节点的图标
    private String icon;

    // 节点对应的实体
    private ConnItem connItem;

    // 节点的活跃状态（激活状态）
    private boolean active;

    public TreeNode() {}

    public TreeNode(String name, TreeNodeType treeNodeType, String icon) {
        this.name = name;
        this.treeNodeType = treeNodeType;
        this.icon = icon;
    }

    public TreeNode(String name, TreeNodeType treeNodeType, String icon, ConnItem connItem) {
        this.name = name;
        this.treeNodeType = treeNodeType;
        this.icon = icon;
        this.connItem = connItem;
    }

    public TreeNode(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public TreeNode(ConnItem connItem, String icon) {
        this.connItem = connItem;
        this.icon = icon;
    }

    public List<TreeNode> getDbList(TreeNode currentTreeNode) {
        List<TreeNode> dbList = new ArrayList<>();
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW DATABASES;");
            while (rs.next()) {
                TreeNode dbItem = new TreeNode();
                dbItem.setName(rs.getString(1));
                dbItem.setTreeNodeType(TreeNodeType.DB);
                //todo
                dbList.add(dbItem);
            }
            System.out.println(dbList);
            rs.close();
            stmt.close();
            conn.close();
            return dbList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
