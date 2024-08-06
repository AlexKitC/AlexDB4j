package io.github.alexkitc.entity;

import io.github.alexkitc.conf.Config;
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

    private static final String SQL_SHOW_DATABASE = "SHOW DATABASES;";
    private static final String SQL_SHOW_TABLE = "SHOW TABLES;";

    // 节点的显示名
    private String name;

    // 节点为Field类型时的字段类型+长度
    private String typeAndLength;

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

    public TreeNode(String name, TreeNodeType treeNodeType, String icon, ConnItem connItem, String typeAndLength) {
        this.name = name;
        this.treeNodeType = treeNodeType;
        this.icon = icon;
        this.connItem = connItem;
        this.typeAndLength = null;
    }

    public TreeNode(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public TreeNode(ConnItem connItem, String icon) {
        this.connItem = connItem;
        this.icon = icon;
    }

    // 获取数据库列表
    public List<TreeNode> getDbList(TreeNode currentTreeNode) {
        List<TreeNode> dbList = new ArrayList<>();
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL_SHOW_DATABASE);
            while (rs.next()) {
                TreeNode dbItem = new TreeNode();
                dbItem.setName(rs.getString(1));
                dbItem.setTreeNodeType(TreeNodeType.DB);
                dbItem.setIcon(Config.CONN_ICON_DB_PATH0);
                dbItem.setConnItem(currentTreeNode.getConnItem());
                dbList.add(dbItem);
            }
            rs.close();
            stmt.close();
            conn.close();
            return dbList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 获取指定数据库table列表
    public List<TreeNode> getTableList(TreeNode currentTreeNode) {
        List<TreeNode> tableList = new ArrayList<>();
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort()
                + "/" + currentTreeNode.getName();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL_SHOW_TABLE);
            while (rs.next()) {
                TreeNode tableItem = new TreeNode();
                tableItem.setName(rs.getString(1));
                tableItem.setTreeNodeType(TreeNodeType.TABLE);
                tableItem.setIcon(Config.CONN_ICON_TABLE_PATH0);
                tableItem.setConnItem(currentTreeNode.getConnItem());
                tableList.add(tableItem);
            }

            rs.close();
            stmt.close();
            conn.close();
            return tableList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 获取指定数据库指定table的field列表
    public List<TreeNode> getTableFieldList(TreeNode parent, TreeNode currentTreeNode) {
        List<TreeNode> tableFieldList = new ArrayList<>();
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort()
                + "/" + parent.getName();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, currentTreeNode.getName(), null);
            while (columns.next()) {
                int dataType = columns.getInt("DATA_TYPE");
                String columnName = columns.getString("COLUMN_NAME");
                String columnTypeName = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                boolean isNullable = columns.getBoolean("IS_NULLABLE");

                TreeNode tableFieldItem = new TreeNode();
                tableFieldItem.setName(columnName);
                tableFieldItem.setTypeAndLength(columnTypeName + "(" + columnSize + ")");
                tableFieldItem.setTreeNodeType(TreeNodeType.FIELD);
                tableFieldItem.setIcon(Config.CONN_ICON_TABLE_PATH0);
                tableFieldItem.setConnItem(currentTreeNode.getConnItem());
                tableFieldList.add(tableFieldItem);
            }

            columns.close();
            conn.close();
            return tableFieldList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
