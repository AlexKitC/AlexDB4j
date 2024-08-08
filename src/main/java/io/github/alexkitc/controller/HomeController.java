package io.github.alexkitc.controller;

import io.github.alexkitc.component.MyConnItemTreeCell;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.entity.RowData;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.DbType;
import io.github.alexkitc.entity.enums.TreeNodeType;
import io.github.alexkitc.util.$;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.alexkitc.conf.Config.*;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/17 下午10:29
 */
public class HomeController {

    // 新建连接按钮
    @FXML
    private Button newConnBtn;
    // 连接树
    @FXML
    private TreeView<TreeNode> treeView;
    // 数据容器
    @FXML
    private AnchorPane mainDataContainer;

    // 底部最右边的内存指示器
    @FXML
    private ProgressBar memoryProgressbar;
    @FXML
    private Text memoryDetailText;

    private TabPane tabPane;

    // 初始化
    @FXML
    private void initialize() {
        // 1.按钮图标
        $.addButtonIcon(newConnBtn, NEW_CONN_ICON_PATH, ICON_SIZE, TOOLTIP_NEW_CONN);
        // 2.读取已有连接信息
        List<ConnItem> connItemList = readConnItemList();
        // 3.初始化树
        initTree(connItemList);

        // 4. 开启一个性能监控
        startMemoryTick();
    }

    //新建连接
    @FXML
    private void onClickNewConn() {
        // 创建并显示新窗口
        Stage newConnStage = new Stage();
        Parent newRoot;
        try {
            newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/new-conn.fxml")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        newConnStage.setTitle(Config.APP_NEW_CONN_TITLE);
        newConnStage.setScene(new Scene(newRoot));
        NewConnController.NewConnStage = newConnStage;
        newConnStage.getIcons().add(new Image(APP_AUTHOR_ICO));
        newConnStage.show();
    }

    // 2.读取当前连接列表
    private List<ConnItem> readConnItemList() {
        Path currentDir = Paths.get(".");
        try {
            List<Path> connFileList = Files.walk(currentDir)
                    .filter(f -> f.toString().endsWith(Config.CONFIG_FILE_SUFFIX))
                    .toList();

            return connFileList.stream()
                    .map(f -> {
                        try {
                            String[] strings = Files.readString(f).split(CONN_SPLIT_FLAG);
                            return new ConnItem()
                                    .setName(strings[0])
                                    .setHost(strings[1])
                                    .setPort(Integer.parseInt(strings[2]))
                                    .setDbType(DbType.valueOf(strings[3]))
                                    .setUsername(strings[4])
                                    .setPassword(strings[5]);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 3.初始化树根结点
    private void initTree(List<ConnItem> connItemList) {
        TreeNode treeNode = new TreeNode("连接列表", TreeNodeType.ROOT, Config.CONN_ICON_PATH0);
        TreeItem<TreeNode> root = new TreeItem<>(treeNode);
        root.setExpanded(true);
        connItemList.forEach(item -> root.getChildren().add(new TreeItem<>(new TreeNode(item.getName(), TreeNodeType.CONN, Config.CONN_ICON_DB_MYSQL_PATH0, item))));
        treeView.setRoot(root);

        //设置TreeCell工厂
        treeView.setCellFactory(item -> new MyConnItemTreeCell());
    }

    // 新建连接后刷新TreeView
    public void refreshTreeView() {
        List<ConnItem> connItemList = readConnItemList();
        initTree(connItemList);
    }

    // 新建Tab+TabPane容纳表数据，含4部分：1.功能按钮，2.搜索，排序，3.数据tableView，4.执行语句
    public void addTabPaneOfData(TreeNode parent, TreeNode treeNode) {
        treeNode.setCurrentPage(1);

        // 首次新建
        if (tabPane == null) {
            tabPane = new TabPane();
        }
        System.gc();
        List<String> tabNameList = tabPane.getTabs()
                .stream()
                .map(Tab::getText)
                .toList();
        String tabName = parent.getName() + " " + treeNode.getName() + " " + treeNode.getConnItem().getHost();
        Tab tab;

        //首次打开新标签
        if (!tabNameList.contains(tabName)) {
            tab = new Tab(tabName);
        } else {
            //已存在标签直接获得焦点
            tab = tabPane.getTabs()
                    .stream()
                    .filter(t -> t.getText().equals(tabName))
                    .limit(1)
                    .toList()
                    .getFirst();
        }

        // 存储当前连接信息已备后续展开逻辑
        tab.setUserData(treeNode);

        //content内容：需要包含4部分
        VBox vBox = new VBox();

        // row1
        HBox row1 = new HBox();
        row1.setPrefHeight(32);
        row1.setSpacing(10);

        // 翻页按钮
        Button pageFirstBtn = new Button();
        Button pagePrevBtn = new Button();
        Button pageNextBtn = new Button();
        Button pageLastBtn = new Button();

        $.addButtonIcon(pageFirstBtn, PAGE_ICON_FIRST, ICON_SMALL_SIZE, TOOLTIP_FIRST_PAGE);
        $.addButtonIcon(pagePrevBtn, PAGE_ICON_PREV, ICON_SMALL_SIZE, TOOLTIP_PREV_PAGE);
        $.addButtonIcon(pageNextBtn, PAGE_ICON_NEXT, ICON_SMALL_SIZE, TOOLTIP_NEXT_PAGE);
        $.addButtonIcon(pageLastBtn, PAGE_ICON_LAST, ICON_SMALL_SIZE, TOOLTIP_LAST_PAGE);
        row1.getChildren().addAll(pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(0, 0, 0, 6));


        // row2
        HBox row2 = new HBox();
        row2.setPrefHeight(32);
        row2.setSpacing(10);
        // limit输入框
        TextField defaultFetchRowTextField = new TextField();
        defaultFetchRowTextField.setPrefWidth(56);
        defaultFetchRowTextField.setText(String.valueOf(Config.DEFAULT_FETCH_ROW));
        // where输入框
        TextField whereTextField = new TextField();
        whereTextField.setPrefWidth(Config.WHERE_CONDITION_TEXT_FIELD_LENGH);
        // orderBy输入框
        TextField orderbyTextField = new TextField();

        row2.getChildren().addAll(new Text("WHERE "),
                whereTextField,
                new Text("ORDER BY "),
                orderbyTextField,
                new Text(" LIMIT "),
                defaultFetchRowTextField
        );
        row2.setPadding(new Insets(0, 0, 0, 6));
        row2.setAlignment(Pos.CENTER_LEFT);

        // row3
        TableView<RowData> tableView = new TableView<>();

        //row4
        HBox row4 = new HBox();
        row4.setPrefHeight(32);
        Text sqlText = new Text();
        row4.getChildren().add(sqlText);
        row4.setPadding(new Insets(0, 0, 0, 6));
        row4.setAlignment(Pos.CENTER_LEFT);

        vBox.getChildren().addAll(row1, row2, tableView, row4);

        // TabPane锚点
        AnchorPane.setTopAnchor(tabPane, 0.0);
        AnchorPane.setBottomAnchor(tabPane, 0.0);
        AnchorPane.setLeftAnchor(tabPane, 0.0);
        AnchorPane.setRightAnchor(tabPane, 0.0);

        // VBox锚点
        AnchorPane.setTopAnchor(vBox, 0.0);
        AnchorPane.setBottomAnchor(vBox, 0.0);
        AnchorPane.setLeftAnchor(vBox, 0.0);
        AnchorPane.setRightAnchor(vBox, 0.0);

        // TableView锚点
        AnchorPane.setTopAnchor(tableView, 0.0);
        AnchorPane.setBottomAnchor(tableView, 0.0);
        AnchorPane.setLeftAnchor(tableView, 0.0);
        AnchorPane.setRightAnchor(tableView, 0.0);

        //VBox尽可能的占据空间伸缩
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // 不允许相同tab反复添加到容器内
        if (!tabNameList.contains(tab.getText())) {
            tab.setContent(vBox);
            tabPane.getTabs().add(tab);
        }

        // 获得焦点
        tabPane.getSelectionModel().select(tab);

        if (!mainDataContainer.getChildren().contains(tabPane)) {
            mainDataContainer.getChildren().add(tabPane);
        }

        tableView.setFixedCellSize(25);
        //获得表字段渲染
        List<TreeNode> tableFieldList = treeNode.getTableFieldList(parent, treeNode);
        // 表头
        ObservableList<TableColumn<RowData, ?>> columns = FXCollections.observableArrayList();
        List<String> colNameList = new ArrayList<>();
        tableFieldList.forEach(col -> {
            String colName = col.getName();
            colNameList.add(colName);
            TableColumn<RowData, String> column = new TableColumn<>(colName);
            column.setMinWidth(Config.DEFAULT_COLUMN_MIN_WIDTH);
            column.setMaxWidth(Config.DEFAULT_COLUMN_MAX_WIDTH);
            // 单元格值工厂
            column.setCellValueFactory(field -> {
                try {
                    // 获取 RowData 对象
                    RowData rowData = field.getValue();

                    // 使用反射获取 Map 中的值
                    Method method = RowData.class.getMethod("get", String.class);
                    Object value = method.invoke(rowData, colName);
                    return new SimpleObjectProperty<>(value == null ? null : value.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            columns.add(column);
        });
        // 设置表列
        tableView.getColumns().setAll(columns);

        // 表数据
        ObservableList<RowData> tableDataList = FXCollections.observableArrayList();
        //获得表数据
        treeNode.getTableRowDataList(parent,
                treeNode,
                columns,
                tableDataList,
                whereTextField.getText(),
                orderbyTextField.getText(),
                Integer.parseInt(defaultFetchRowTextField.getText()),
                sqlText);

        // 表赋值数据
        tableView.setItems(tableDataList);

        //where, orderBy, limit输入框新增回车查询事件
        defaultFetchRowTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                tableView.setItems(newTableDataList);
            }
        });
        whereTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                tableView.setItems(newTableDataList);
            }
        });
        orderbyTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                tableView.setItems(newTableDataList);
            }
        });

        // orderBy输入框监听输入事件
        orderbyTextField.textProperty().addListener((ob, oldValue, newValue) -> {
            List<String> optionList = colNameList.stream()
                    .filter(col -> col.contains(newValue) || col.contains(newValue.toLowerCase()))
                    .toList();
            System.out.println(optionList);
        });

        // 分页事件
        pageFirstBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(1);
            ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
            treeNode.triggerPageEvent(parent,
                    treeNode,
                    columns,
                    newTableDataList,
                    whereTextField.getText(),
                    orderbyTextField.getText(),
                    Integer.parseInt(defaultFetchRowTextField.getText()),
                    sqlText);
            tableView.setItems(newTableDataList);
            pageFirstBtn.setDisable(true);
            pagePrevBtn.setDisable(true);
            pageNextBtn.setDisable(newTableDataList.size() < Integer.parseInt(defaultFetchRowTextField.getText()));
        });

        pagePrevBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(treeNode.getCurrentPage() == 1 ? 1 : treeNode.getCurrentPage() - 1);
            ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
            treeNode.triggerPageEvent(parent,
                    treeNode,
                    columns,
                    newTableDataList,
                    whereTextField.getText(),
                    orderbyTextField.getText(),
                    Integer.parseInt(defaultFetchRowTextField.getText()),
                    sqlText);
            tableView.setItems(newTableDataList);
            pagePrevBtn.setDisable(treeNode.getCurrentPage() == 1);
            pageFirstBtn.setDisable(treeNode.getCurrentPage() == 1);
            pageNextBtn.setDisable(newTableDataList.size() < Integer.parseInt(defaultFetchRowTextField.getText()));
        });

        pageNextBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(treeNode.getCurrentPage() + 1);
            ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
            treeNode.triggerPageEvent(parent,
                    treeNode,
                    columns,
                    newTableDataList,
                    whereTextField.getText(),
                    orderbyTextField.getText(),
                    Integer.parseInt(defaultFetchRowTextField.getText()),
                    sqlText);
            pageNextBtn.setDisable(newTableDataList.size() < Integer.parseInt(defaultFetchRowTextField.getText()));
            pagePrevBtn.setDisable(false);
            pageFirstBtn.setDisable(false);
            tableView.setItems(newTableDataList);
        });
        pageLastBtn.setDisable(true);
        pageFirstBtn.setDisable(true);
        pagePrevBtn.setDisable(true);
        if (tableDataList.size() < Integer.parseInt(defaultFetchRowTextField.getText())) {
            pageNextBtn.setDisable(true);
        } else {
            pagePrevBtn.setDisable(true);
        }
//        pageLastBtn.setOnAction(ev -> {
//            treeNode.setCurrentPage(1);
//            ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
//            treeNode.triggerPageEvent(parent,
//                    treeNode,
//                    columns,
//                    newTableDataList,
//                    whereTextField.getText(),
//                    orderbyTextField.getText(),
//                    Integer.parseInt(defaultFetchRowTextField.getText()),
//                    sqlText);
//            tableView.setItems(newTableDataList);
//        });

        tab.setOnClosed(ev -> {
            tab.setContent(null);
            System.gc();
        });
    }

    // 内存监控任务
    private void startMemoryTick() {
        //基本的ToolTips
        memoryProgressbar.setTooltip(new Tooltip("当前数据库工具内存监控"));

        Task<Double> memoryTask = new Task<>() {
            @Override
            protected Double call() throws Exception {
                while (!isCancelled()) {
                    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

                    MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
                    MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

                    long totalUsed = heapMemoryUsage.getUsed() / 1024 / 1024 + nonHeapMemoryUsage.getUsed() / 1024 / 1024;
                    long totalCommited = heapMemoryUsage.getCommitted() / 1024 / 1024 + nonHeapMemoryUsage.getCommitted() / 1024 / 1024;

                    // 使用 Platform.runLater 来更新 UI
                    Platform.runLater(() -> {
                        memoryProgressbar.setProgress((double) totalUsed / totalCommited);
                        memoryDetailText.setText(totalUsed + "/" + totalCommited + "MB");
                    });
                    Thread.sleep(2000); // Sleep for 2 seconds
                }
                return null;
            }
        };

        // 启动 Task
        Thread.ofVirtual().start(memoryTask);
    }

}
