package io.github.alexkitc.entity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mongodb.client.*;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.enums.RedisKeyTypeEnum;
import io.github.alexkitc.entity.enums.TreeNodeTypeEnum;
import io.github.alexkitc.util.$;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import lombok.Data;
import lombok.experimental.Accessors;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;

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
    
    // 当前表的主键
    private String pkName;

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
                        this.icon = Config.CONN_ICON_DB_MONGO_PATH1;
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
                    $.warning("SQLException", e.getMessage());
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
                    $.warning("RedisConnectionException", e.getMessage());
                }

                break;
            }
            case MONGODB: {
                List<TreeNode> dbList = new ArrayList<>();
                try (MongoClient mongoClient = MongoClients.create("mongodb://" + currentTreeNode.getConnItem().getUsername() + ":" + currentTreeNode.getConnItem().getPassword() + "@" + currentTreeNode.getConnItem().getHost() + ":" + currentTreeNode.getConnItem().getPort())) {
                    // 获取所有数据库的名字
                    MongoIterable<String> mongoIterable = mongoClient.listDatabaseNames();
                    List<String> dbNameList = new ArrayList<>();
                    mongoIterable.into(dbNameList);
                    for (String dbName : dbNameList) {
                        TreeNode treeNode = new TreeNode();
                        treeNode.setTreeNodeTypeEnum(TreeNodeTypeEnum.DB);
                        treeNode.setIcon(Config.CONN_ICON_DB_MONGO_PATH1);
                        treeNode.setConnItem(currentTreeNode.getConnItem());
                        treeNode.setName(dbName);
                        dbList.add(treeNode);
                    }
                    return dbList;
                } catch (Exception e) {
                    $.warning("MongoException", e.getMessage());
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
            case MONGODB: {
                List<TreeNode> tableList = new ArrayList<>();
                try (MongoClient mongoClient = MongoClients.create("mongodb://" + currentTreeNode.getConnItem().getUsername() + ":" + currentTreeNode.getConnItem().getPassword() + "@" + currentTreeNode.getConnItem().getHost() + ":" + currentTreeNode.getConnItem().getPort())) {
                    String databaseName = currentTreeNode.getName();
                    MongoDatabase database = mongoClient.getDatabase(databaseName);

                    MongoIterable<Document> collectionsInfo = database.listCollections();

                    // 将集合信息转换为 List
                    List<Document> collectionsInfoList = new ArrayList<>();
                    collectionsInfo.into(collectionsInfoList);

                    // 提取集合名称
                    List<String> collectionNames = new ArrayList<>();
                    for (Document collectionInfo : collectionsInfoList) {
                        String name = collectionInfo.getString("name");
                        collectionNames.add(name);
                    }
                    for (String tableName : collectionNames) {
                        TreeNode treeNode = new TreeNode();
                        treeNode.setTreeNodeTypeEnum(TreeNodeTypeEnum.DB);
                        treeNode.setIcon(Config.CONN_ICON_DB_MONGO_PATH1);
                        treeNode.setConnItem(currentTreeNode.getConnItem());
                        treeNode.setName(tableName);
                        tableList.add(treeNode);
                    }
                    return tableList;
                } catch (Exception e) {
                    $.warning("MongoException", e.getMessage());
                }
                break;
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

                    //获取主键
                    ResultSet primaryKeys = metaData.getPrimaryKeys(parent.getName(), parent.getName(), currentTreeNode.getName());
                    while (primaryKeys.next()) {
                        currentTreeNode.setPkName(primaryKeys.getString("COLUMN_NAME"));
                    }

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

                        Pipeline pipeline = jedis.pipelined();
                        for (String key : scanResult.getResult()) {
                            pipeline.type(key);
                        }

                        List<Object> types = pipeline.syncAndReturnAll();

                        for (int i = 0; i < types.size(); i++) {
                            // 仅返回当前类型key数据
                            if (currentTreeNode.getName().toLowerCase().equals(types.get(i))) {
                                TreeNode keyItem = new TreeNode();
                                keyItem.setName(scanResult.getResult().get(i));
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
            case MONGODB: {
                List<TreeNode> tableFieldList = new ArrayList<>();
                try (MongoClient mongoClient = MongoClients.create("mongodb://" + currentTreeNode.getConnItem().getUsername() + ":" + currentTreeNode.getConnItem().getPassword() + "@" + currentTreeNode.getConnItem().getHost() + ":" + currentTreeNode.getConnItem().getPort())) {
                    String databaseName = parent.getName();
                    MongoDatabase database = mongoClient.getDatabase(databaseName);

                    String collectionName = currentTreeNode.getName();
                    MongoCollection<Document> collection = database.getCollection(collectionName);

                    Set<String> fieldNames = new HashSet<>();
                    for (Document doc : collection.find()) {
                        fieldNames.addAll(doc.keySet());
                    }

                    for (String fieldName : fieldNames) {
                        TreeNode tableItem = new TreeNode();
                        tableItem.setName(fieldName);
                        tableItem.setTypeAndLength(null);
                        tableItem.setTreeNodeTypeEnum(TreeNodeTypeEnum.FIELD);
                        tableItem.setIcon(Config.CONN_ICON_FIELD_PATH0);
                        tableItem.setConnItem(currentTreeNode.getConnItem());
                        tableFieldList.add(tableItem);
                    }

                    return tableFieldList;
                } catch (Exception e) {
                    $.warning("MongoException", e.getMessage());
                }
                break;
            }
            default: {
                break;
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
                        sql += " LIMIT " + (currentTreeNode.getCurrentPage() - 1) * limitRows + ", " + limitRows;
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
            case MONGODB: {
                try (MongoClient mongoClient = MongoClients.create("mongodb://" + currentTreeNode.getConnItem().getUsername() + ":" + currentTreeNode.getConnItem().getPassword() + "@" + currentTreeNode.getConnItem().getHost() + ":" + currentTreeNode.getConnItem().getPort())) {
                    String databaseName = parent.getName();
                    MongoDatabase database = mongoClient.getDatabase(databaseName);

                    String collectionName = currentTreeNode.getName();
                    MongoCollection<Document> collection = database.getCollection(collectionName);

                    long countDocuments = collection.countDocuments();
                    currentTreeNode.setTableViewRowCount(countDocuments);

                    FindIterable<Document> documentFindIterable = collection.find();
                    if (Objects.nonNull(limitRows)) {
                        int start = (currentTreeNode.getCurrentPage() - 1) * limitRows;
                        documentFindIterable.skip(start).limit(limitRows);
                    }

                    List<Document> documents = documentFindIterable.into(new ArrayList<>());

                    // 打印查询结果
                    for (Document document : documents) {
                        String documentJson = String.valueOf(convertDocumentToJson(document));
                        Gson gson = new Gson();
                        Type mapType = new TypeToken<Map<String, Object>>() {
                        }.getType();
                        Map<String, Object> map = gson.fromJson(documentJson, mapType);
                        RowData rowData = new RowData();
                        for (TableColumn<RowData, ?> column : columns) {

                            String columnName = column.getText();
                            Object value = map.get(columnName);
                            rowData.put(columnName, value);
                        }
                        rowList.add(rowData);
                    }

                    sqlText.setText("db." + collectionName + ".find({})." + "skip(" + (currentTreeNode.getCurrentPage() - 1) * limitRows + ").limit(" + limitRows + ")");

                } catch (Exception e) {
                    $.warning("MongoException", e.getMessage());
                }

                break;
            }
            default:
                break;
        }

    }

    // mongo处理document返回的json
    private static String convertDocumentToJson(Document document) {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        for (String key : document.keySet()) {
            Object value = document.get(key);
//            if (value instanceof Document) {
//                // 递归处理嵌套的 Document
//                jsonObject.add(key, (JsonElement) convertDocumentToJson((Document) value));
//            } else
            if (value instanceof List) {
                // 处理 List 类型
                List<?> list = (List<?>) value;
                JsonArray jsonArray = new JsonArray();
                for (Object item : list) {
                    jsonArray.add(gson.toJsonTree(item));
                }
                jsonObject.add(key, jsonArray);
            } else {
                // 处理其他类型
                jsonObject.add(key, gson.toJsonTree(value));
            }
        }

        return jsonObject.toString();
    }

}
