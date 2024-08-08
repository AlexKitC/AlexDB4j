package io.github.alexkitc.entity;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.enums.TreeNodeType;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    // field 类型+长度
    private String typeAndLength;

    // 节点类型
    private TreeNodeType treeNodeType;

    // 节点的图标
    private String icon;

    // 节点对应的实体
    private ConnItem connItem;

    // 节点的活跃状态（激活状态）
    private boolean active;

    // 当前分页页码
    private int currentPage;

    public TreeNode() {
    }

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

    public TreeNode(String name, TreeNodeType treeNodeType, String icon, ConnItem connItem, String typeAndLength) {
        this.name = name;
        this.typeAndLength = typeAndLength;
        this.treeNodeType = treeNodeType;
        this.icon = icon;
        this.connItem = connItem;
    }

    // 获取数据库列表
    public List<TreeNode> getDbList(TreeNode currentTreeNode) {
        List<TreeNode> dbList = new ArrayList<>();
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
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
                + "/" + currentTreeNode.getName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
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
                + "/" + parent.getName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(parent.getName(), parent.getName(), currentTreeNode.getName(), null);
            while (columns.next()) {
                int dataType = columns.getInt("DATA_TYPE");
                String columnName = columns.getString("COLUMN_NAME");
                String columnTypeName = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int decimalDigits = columns.getInt("DECIMAL_DIGITS");
                boolean isNullable = columns.getBoolean("IS_NULLABLE");
//                String memo = columns.getString("COLUMN_COMMENT");

                TreeNode tableItem = new TreeNode();
                tableItem.setName(columnName);
                tableItem.setTypeAndLength(columnTypeName + "(" + columnSize + ")");
                tableItem.setTreeNodeType(TreeNodeType.FIELD);
                tableItem.setIcon(Config.CONN_ICON_FIELD_PATH0);
                tableItem.setConnItem(currentTreeNode.getConnItem());
                tableFieldList.add(tableItem);
            }

            columns.close();
            conn.close();
            return tableFieldList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // 查询表数据
    public ObservableList<RowData> getTableRowDataList(TreeNode parent,
                                                       TreeNode currentTreeNode,
                                                       ObservableList<TableColumn<RowData, ?>> columns,
                                                       ObservableList<RowData> rowList,
                                                       List<Map<String, String>> conditionList,
                                                       String orderby,
                                                       Integer limitRows,
                                                       Text sqlText) {
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort()
                + "/" + parent.getName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM " + currentTreeNode.getName();
            if (!Objects.isNull(conditionList) && !conditionList.isEmpty()) {

            }
            if (!Objects.isNull(orderby)) {
                sql += " ORDER BY ";
            }
            if (Objects.nonNull(limitRows)) {
                sql += " LIMIT " + limitRows;
            }

            ResultSet rs = stmt.executeQuery(sql);
            sqlText.setText(sql);
            while (rs.next()) {
                RowData rowData = new RowData();
                for (TableColumn<RowData, ?> column : columns) {
                    String columnName = column.getText();
                    Object value = rs.getObject(columnName);
                    rowData.put(columnName, value);
                }
                rowList.add(rowData);
            }

            rs.close();
            stmt.close();
            conn.close();
            return rowList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public ObservableList<RowData> triggerPageEvent(TreeNode parent,
                                                    TreeNode currentTreeNode,
                                                    ObservableList<TableColumn<RowData, ?>> columns,
                                                    ObservableList<RowData> rowList,
                                                    Integer limitRows,
                                                    Text sqlText) {
        String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                + ":" + currentTreeNode.getConnItem().getPort()
                + "/" + parent.getName()
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
            Statement stmt = conn.createStatement();
            String sql = "SELECT * FROM " + currentTreeNode.getName();

            if (Objects.nonNull(limitRows)) {
                sql += " LIMIT " + String.valueOf((currentTreeNode.getCurrentPage() - 1) * limitRows) + ", " + limitRows;
            }

            ResultSet rs = stmt.executeQuery(sql);
            sqlText.setText(sql);
            while (rs.next()) {
                RowData rowData = new RowData();
                for (TableColumn<RowData, ?> column : columns) {
                    String columnName = column.getText();
                    Object value = rs.getObject(columnName);
                    rowData.put(columnName, value);
                }
                rowList.add(rowData);
            }

            rs.close();
            stmt.close();
            conn.close();
            return rowList;
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
