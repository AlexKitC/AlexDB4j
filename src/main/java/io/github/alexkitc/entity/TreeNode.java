package io.github.alexkitc.entity;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.enums.RedisKeyTypeEnum;
import io.github.alexkitc.entity.enums.TreeNodeTypeEnum;
import io.github.alexkitc.util.$;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.experimental.Accessors;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

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

    // 当类型为field 类型+长度
    private String typeAndLength;

    //当类型为table 存储表数据count
    private Long tableViewRowCount;

    // 节点类型
    private TreeNodeTypeEnum treeNodeTypeEnum;

    private TreeNode parent;

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

    public TreeNode(String name, TreeNodeTypeEnum treeNodeTypeEnum, String icon) {
        this.name = name;
        this.treeNodeTypeEnum = treeNodeTypeEnum;
        this.icon = icon;
    }

    public TreeNode(String name, TreeNodeTypeEnum treeNodeTypeEnum, String icon, ConnItem connItem) {
        this.name = name;
        this.treeNodeTypeEnum = treeNodeTypeEnum;
        switch (treeNodeTypeEnum) {
            case CONN: {
                switch (connItem.getDbTypeEnum()) {
                    case MYSQL:
                        this.icon = Config.CONN_ICON_DB_MYSQL_PATH0;
                        break;
                    case REDIS:
                        this.icon = Config.CONN_ICON_DB_REDIS_PATH0;
                        break;
                    case MONGODB:
                        this.icon = Config.CONN_ICON_DB_MONGO_PATH0;
                        break;
                    default:
                        this.icon = icon;
                }
                break;
            }

            default: {
                this.icon = icon;
                break;
            }

        }

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

    public TreeNode(String name, TreeNodeTypeEnum treeNodeTypeEnum, String icon, ConnItem connItem, String typeAndLength) {
        this.name = name;
        this.typeAndLength = typeAndLength;
        this.treeNodeTypeEnum = treeNodeTypeEnum;
        this.icon = icon;
        this.connItem = connItem;
    }

    // 获取数据库列表
    public List<TreeNode> getDbList(TreeNode currentTreeNode) {
        switch (currentTreeNode.getConnItem().getDbTypeEnum()) {
            // mysql获取数据库
            case MYSQL: {
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
                        dbItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.DB);
                        dbItem.setIcon(Config.CONN_ICON_DB_PATH0);
                        dbItem.setConnItem(currentTreeNode.getConnItem());
                        dbList.add(dbItem);
                    }
                    rs.close();
                    stmt.close();
                    conn.close();
                    return dbList;
                } catch (ClassNotFoundException | SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("SQLException");
                    alert.setHeaderText("SQLException");
                    alert.setContentText(e.getMessage());

                    // 显示 Alert 对话框
                    alert.showAndWait();

                }
                break;
            }
            // REDIS获取数据库
            case REDIS: {
                List<TreeNode> dbList = new ArrayList<>();
                try (Jedis jedis = new Jedis(currentTreeNode.getConnItem().getHost(), currentTreeNode.getConnItem().getPort())) {
                    //如果有密码添加密码认证
                    if (!$.isEmpty(currentTreeNode.getConnItem().getPassword())) {
                        jedis.auth(currentTreeNode.getConnItem().getPassword());
                    }
                    //遍历所有数据库
                    int numberOfDatabases = Integer.parseInt(jedis.configGet("databases").get("databases"));
                    for (int dbIndex = 0; dbIndex <= numberOfDatabases; dbIndex++) {
                        TreeNode treeNode = new TreeNode();
                        treeNode.setTreeNodeTypeEnum(TreeNodeTypeEnum.DB);
                        treeNode.setIcon(Config.CONN_ICON_DB_PATH0);
                        treeNode.setConnItem(currentTreeNode.getConnItem());
                        treeNode.setName(String.valueOf(dbIndex));
                        dbList.add(treeNode);
                    }
                    return dbList;
                } catch (JedisConnectionException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("RedisConnectionException");
                    alert.setHeaderText("RedisConnectionException");
                    alert.setContentText(e.getMessage());

                    // 显示 Alert 对话框
                    alert.showAndWait();
                }

                break;
            }
            default: {
                break;
            }
        }

        return null;
    }

    // 获取指定数据库table列表
    public List<TreeNode> getTableList(TreeNode currentTreeNode) {
        switch (currentTreeNode.getConnItem().getDbTypeEnum()) {
            case MYSQL: {
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
                        tableItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.TABLE);
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
            case REDIS: {
                List<TreeNode> tableList = new ArrayList<>();
                for (RedisKeyTypeEnum key : RedisKeyTypeEnum.values()) {
                    TreeNode tableItem = new TreeNode();
                    tableItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.TABLE);
                    tableItem.setIcon(Config.CONN_ICON_TABLE_PATH0);
                    tableItem.setConnItem(currentTreeNode.getConnItem());
                    tableItem.setName(String.valueOf(key));
                    tableList.add(tableItem);
                }

                return tableList;
            }
            default:
                break;
        }
        return null;
    }

    // 获取指定数据库指定table的field列表
    public List<TreeNode> getTableFieldList(TreeNode parent, TreeNode currentTreeNode) {
        switch (currentTreeNode.getConnItem().getDbTypeEnum()) {
            case MYSQL: {
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
                        tableItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.FIELD);
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
            //对于redis而言，获取field即为获取key的列表
            case REDIS: {
                List<TreeNode> keyList = new ArrayList<>();
                try (Jedis jedis = new Jedis(currentTreeNode.getConnItem().getHost(), currentTreeNode.getConnItem().getPort())) {
                    //如果有密码添加密码认证
                    if (!$.isEmpty(currentTreeNode.getConnItem().getPassword())) {
                        jedis.auth(currentTreeNode.getConnItem().getPassword());
                    }

                    // 选择当前的数据库
                    int dbIndex = Integer.parseInt(parent.getName());
                    jedis.select(dbIndex);

                    String cursor = "0";
                    ScanParams params = new ScanParams();
                    params.match("*");
                    params.count(100);

                    do {
                        ScanResult<String> scanResult = jedis.scan(cursor, params);
                        cursor = scanResult.getCursor();

                        for (String key : scanResult.getResult()) {
                            // 获取键的类型
                            String type = jedis.type(key);
                            // 仅返回当前类型key数据
                            if (currentTreeNode.getName().toLowerCase().equals(type)) {
                                TreeNode keyItem = new TreeNode();
                                keyItem.setName(key);
                                keyItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.FIELD);
                                keyItem.setConnItem(currentTreeNode.getConnItem());
                                keyItem.setIcon(Config.CONN_ICON_FIELD_PATH0);
                                keyList.add(keyItem);
                            }

                        }
                    } while (!cursor.equals("0"));

                    // 同时设置总的key count值
                    currentTreeNode.setTableViewRowCount((long) keyList.size());

                }

                return keyList;
            }
        }
        return null;
    }

    // 查询表数据
    public void getTableRowDataList(TreeNode parent,
                                    TreeNode currentTreeNode,
                                    ObservableList<TableColumn<RowData, ?>> columns,
                                    ObservableList<RowData> rowList,
                                    String whereCondition,
                                    String orderby,
                                    Integer limitRows,
                                    Text sqlText) {
        switch (currentTreeNode.getConnItem().getDbTypeEnum()) {
            case MYSQL: {
                String url = "jdbc:mysql://" + currentTreeNode.getConnItem().getHost()
                        + ":" + currentTreeNode.getConnItem().getPort()
                        + "/" + parent.getName()
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    Connection conn = DriverManager.getConnection(url, currentTreeNode.getConnItem().getUsername(), currentTreeNode.getConnItem().getPassword());
                    Statement stmt = conn.createStatement();
                    String sql = "SELECT * FROM " + currentTreeNode.getName();
                    if (!Objects.isNull(whereCondition) && !whereCondition.trim().isEmpty()) {
                        sql += " WHERE " + whereCondition;
                    }
                    if (!Objects.isNull(orderby) && !orderby.trim().isEmpty()) {
                        sql += " ORDER BY " + orderby;
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
                    String countSql = "SELECT COUNT(*) FROM " + currentTreeNode.getName();
                    if (!Objects.isNull(whereCondition) && !whereCondition.trim().isEmpty()) {
                        countSql += " WHERE " + whereCondition;
                    }
                    if (!Objects.isNull(orderby) && !orderby.trim().isEmpty()) {
                        countSql += " ORDER BY " + orderby;
                    }

                    ResultSet countRs = stmt.executeQuery(countSql);
                    if (countRs.next()) {
                        long count = countRs.getLong(1);
                        currentTreeNode.setTableViewRowCount(count);
                    }
                    countRs.close();
                    rs.close();
                    stmt.close();
                    conn.close();

                } catch (ClassNotFoundException | SQLException e) {
                    sqlText.setText(e.getMessage());
                    throw new RuntimeException(e);
                }
                break;
            }
            case REDIS: {
                try (Jedis jedis = new Jedis(currentTreeNode.getConnItem().getHost(), currentTreeNode.getConnItem().getPort())) {
                    //如果有密码添加密码认证
                    if (!$.isEmpty(currentTreeNode.getConnItem().getPassword())) {
                        jedis.auth(currentTreeNode.getConnItem().getPassword());
                    }

                    // 选择当前的数据库
                    int dbIndex = Integer.parseInt(parent.getParent().getName());
                    jedis.select(dbIndex);

                    switch (parent.getName().toLowerCase()) {
                        case "string": {
                            RowData rowData = new RowData();
                            for (TableColumn<RowData, ?> column : columns) {
                                String columnName = column.getText();
                                String value = jedis.get(currentTreeNode.getName());
                                rowData.put(columnName, value);
                            }
                            rowList.add(rowData);

                            sqlText.setText("GET " + currentTreeNode.getName());
                            break;
                        }
                        case "list": {
                            jedis.lrange(currentTreeNode.getName(), 0, -1)
                                    .forEach(value -> {
                                        RowData rowData = new RowData();
                                        for (TableColumn<RowData, ?> column : columns) {
                                            String columnName = column.getText();
                                            rowData.put(columnName, value);
                                        }
                                        rowList.add(rowData);
                                    });

                            sqlText.setText("LRANGE " + currentTreeNode.getName());
                            break;
                        }
                        case "set": {
                            jedis.smembers(currentTreeNode.getName()).forEach(
                                    value -> {
                                        RowData rowData = new RowData();
                                        for (TableColumn<RowData, ?> column : columns) {
                                            String columnName = column.getText();
                                            rowData.put(columnName, value);
                                        }
                                        rowList.add(rowData);
                                    }
                            );

                            sqlText.setText("SMEMBERS " + currentTreeNode.getName());
                            break;
                        }
                        case "zset": {
                            jedis.zrangeWithScores(currentTreeNode.getName(), 0, -1).forEach(
                                    value -> {
                                        RowData rowData = new RowData();
                                        rowData.put("value", value.getElement());
                                        rowData.put("score", value.getScore());
                                        rowList.add(rowData);
                                    }
                            );

                            sqlText.setText("ZGRANGEBYSCORE  " + currentTreeNode.getName());
                            break;
                        }
                        case "hash": {
                            Map<String, String> hashValue = jedis.hgetAll(currentTreeNode.getName());
                            for (Map.Entry<String, String> entry : hashValue.entrySet()) {
                                RowData rowData = new RowData();
                                rowData.put("field", entry.getKey());
                                rowData.put("value", entry.getValue());
                                rowList.add(rowData);
                            }

                            sqlText.setText("HGETALL  " + currentTreeNode.getName());
                            break;
                        }
                        default:
                            break;
                    }

                }

                break;
            }
            default:
                break;
        }

    }


    public ObservableList<RowData> triggerPageEvent(TreeNode parent,
                                                    TreeNode currentTreeNode,
                                                    ObservableList<TableColumn<RowData, ?>> columns,
                                                    ObservableList<RowData> rowList,
                                                    String whereCondition,
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

            if (!Objects.isNull(whereCondition) && !whereCondition.trim().isEmpty()) {
                sql += " WHERE " + whereCondition;
            }
            if (!Objects.isNull(orderby) && !orderby.trim().isEmpty()) {
                sql += " ORDER BY " + orderby;
            }

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
