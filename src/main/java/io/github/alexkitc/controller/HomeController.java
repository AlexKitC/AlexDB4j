package io.github.alexkitc.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.alexkitc.App;
import io.github.alexkitc.component.MyConnItemTreeCell;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.entity.RowData;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.DbTypeEnum;
import io.github.alexkitc.entity.enums.RedisKeyTypeEnum;
import io.github.alexkitc.entity.enums.TreeNodeTypeEnum;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
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
import java.util.*;

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
        FXMLLoader fxmlLoader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(FXML_NEW_CONN_FILE_PATH)));
        Parent newRoot;
        try {
            newRoot = fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        newConnStage.setTitle(Config.APP_NEW_CONN_TITLE);
        newConnStage.setScene(new Scene(newRoot));
        NewConnController.NewConnStage = newConnStage;
        App.newConnControllerInstance = fxmlLoader.getController();
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
                                    .setDbTypeEnum(DbTypeEnum.valueOf(strings[3]))
                                    .setUsername(strings.length > 4 ? strings[4] : "")
                                    .setPassword(strings.length > 5 ? strings[5] : "");
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
        TreeNode treeNode = new TreeNode("连接列表", TreeNodeTypeEnum.ROOT, Config.CONN_ICON_PATH0);
        TreeItem<TreeNode> root = new TreeItem<>(treeNode);
        root.setExpanded(true);
        connItemList.forEach(item -> root.getChildren().add(new TreeItem<>(new TreeNode(item.getName(), TreeNodeTypeEnum.CONN, Config.CONN_ICON_DB_MYSQL_PATH0, item))));
        treeView.setRoot(root);

        //设置TreeCell工厂
        treeView.setCellFactory(item -> new MyConnItemTreeCell());
    }

    // 新建连接后刷新TreeView
    public void refreshTreeView() {
        List<ConnItem> connItemList = readConnItemList();
        initTree(connItemList);
    }

    // 新建mysql类型Tab+TabPane容纳表数据，含4部分：1.功能按钮，2.搜索，排序，3.数据tableView，4.执行语句
    public void addMysqlTabPaneOfData(TreeNode parent, TreeNode treeNode) {
        treeNode.setCurrentPage(1);

        //初始化一个用于语法提示的whereByStackPane
        StackPane whereConditionStackPane = new StackPane();
        ListView<String> whereByListView = new ListView<>();
        whereConditionStackPane.getChildren().add(whereByListView);
        whereConditionStackPane.setVisible(false);

        //初始化一个用于语法提示的orderByStackPane
        StackPane orderByStackPane = new StackPane();
        ListView<String> orderByListView = new ListView<>();
        orderByStackPane.getChildren().add(orderByListView);
        orderByStackPane.setVisible(false);

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

        // mongo暂不支持搜索和排序
        if (treeNode.getConnItem().getDbTypeEnum().equals(DbTypeEnum.MONGODB)) {
            whereTextField.setDisable(true);
            orderbyTextField.setDisable(true);
        }

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
        // 表头
        ObservableList<TableColumn<RowData, ?>> columns = FXCollections.observableArrayList();
        List<String> colNameList = new ArrayList<>();

        Thread.ofVirtual().start(() -> {
            List<TreeNode> tableFieldList = treeNode.getTableFieldList(parent, treeNode);
            Platform.runLater(() -> {
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
            });
        });


        // 表数据
        Thread.ofVirtual().start(() -> {
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
            Platform.runLater(() -> {
                // 表赋值数据
                tableView.setItems(tableDataList);
                //刷新翻页按钮状态
                refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
            });
        });

        //where, orderBy, limit输入框新增回车查询事件
        defaultFetchRowTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                Thread.ofVirtual().start(() -> {
                    ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                    treeNode.getTableRowDataList(parent,
                            treeNode,
                            columns,
                            newTableDataList,
                            whereTextField.getText(),
                            orderbyTextField.getText(),
                            Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                    Platform.runLater(() -> {
                        tableView.setItems(newTableDataList);

                        refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                    });
                });
            }
        });
        whereTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                Thread.ofVirtual().start(() -> {
                    ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                    treeNode.getTableRowDataList(parent,
                            treeNode,
                            columns,
                            newTableDataList,
                            whereTextField.getText(),
                            orderbyTextField.getText(),
                            Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                    Platform.runLater(() -> {
                        tableView.setItems(newTableDataList);
                        refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                        if (whereConditionStackPane.isVisible()) {
                            whereConditionStackPane.setVisible(false);
                        }
                    });
                });
            }

            //退出则隐藏语法提示面板
            //当语法提示出现的时候，Tab按键和方向键可直接提供输入
            if (ev.getCode().equals(KeyCode.ESCAPE)) {
                whereByListView.getItems().clear();
                if (whereConditionStackPane.isVisible()) {
                    whereConditionStackPane.setVisible(false);
                }
            } else if (ev.getCode().equals(KeyCode.TAB)) {
                whereTextField.setText(whereByListView.getSelectionModel().getSelectedItem());
                if (whereConditionStackPane.isVisible()) {
                    whereConditionStackPane.setVisible(false);
                }
                whereTextField.requestFocus();
            } else if (ev.getCode().equals(KeyCode.DOWN)) {
                whereByListView.requestFocus();
            }
        });
        orderbyTextField.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ENTER) {
                Thread.ofVirtual().start(() -> {
                    ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                    treeNode.getTableRowDataList(parent,
                            treeNode,
                            columns,
                            newTableDataList,
                            whereTextField.getText(),
                            orderbyTextField.getText(),
                            Integer.parseInt(defaultFetchRowTextField.getText()), sqlText);
                    Platform.runLater(() -> {
                        tableView.setItems(newTableDataList);

                        refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                        if (orderByStackPane.isVisible()) {
                            orderByStackPane.setVisible(false);
                        }
                    });
                });
            }
            //退出则隐藏语法提示面板
            //当语法提示出现的时候，Tab按键和方向键可直接提供输入
            if (ev.getCode().equals(KeyCode.ESCAPE)) {
                orderByListView.getItems().clear();
                if (orderByStackPane.isVisible()) {
                    orderByStackPane.setVisible(false);
                }
            } else if (ev.getCode().equals(KeyCode.TAB)) {
                orderbyTextField.setText(orderByListView.getSelectionModel().getSelectedItem());
                if (orderByStackPane.isVisible()) {
                    orderByStackPane.setVisible(false);
                }
                orderbyTextField.requestFocus();
            } else if (ev.getCode().equals(KeyCode.DOWN)) {
                orderByListView.requestFocus();
            }

        });

        // where输入框输入内容监听输入事件
        whereTextField.textProperty().addListener((ob, oldValue, newValue) -> {
            List<String> optionList = colNameList.stream()
                    .filter(col -> col.contains(newValue) || col.contains(newValue.toLowerCase()) || col.contains(newValue.toUpperCase()))
                    .distinct()
                    .toList();
            whereByListView.getItems().clear();
            whereByListView.getItems().addAll(optionList);
            if (!whereByListView.getItems().isEmpty()) {
                whereByListView.getSelectionModel().selectFirst();
            }
            if (!whereConditionStackPane.isVisible()) {
                whereConditionStackPane.setVisible(true);
            }

        });
        mainDataContainer.getChildren().add(whereConditionStackPane);
        AnchorPane.setTopAnchor(whereConditionStackPane, 96.0);
        AnchorPane.setLeftAnchor(whereConditionStackPane, 62.0);

        whereByListView.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode().equals(KeyCode.ENTER) || ev.getCode().equals(KeyCode.TAB)) {
                whereTextField.setText(whereByListView.getSelectionModel().getSelectedItem());
                if (whereConditionStackPane.isVisible()) {
                    whereConditionStackPane.setVisible(false);
                }
                whereTextField.requestFocus();
            }
        });

        // orderBy输入框输入内容监听输入事件
        orderbyTextField.textProperty().addListener((ob, oldValue, newValue) -> {
            List<String> optionList = colNameList.stream()
                    .filter(col -> newValue != null && (col.contains(newValue) || col.contains(newValue.toLowerCase()) || col.contains(newValue.toUpperCase())))
                    .distinct()
                    .toList();
            orderByListView.getItems().clear();
            orderByListView.getItems().addAll(optionList);
            if (!orderByListView.getItems().isEmpty()) {
                orderByListView.getSelectionModel().selectFirst();
            }
            if (!orderByStackPane.isVisible()) {
                orderByStackPane.setVisible(true);
            }
        });

        mainDataContainer.getChildren().add(orderByStackPane);

        AnchorPane.setTopAnchor(orderByStackPane, 96.0);
        AnchorPane.setLeftAnchor(orderByStackPane, 464.0);

        orderByListView.addEventFilter(KeyEvent.KEY_PRESSED, ev -> {
            if (ev.getCode().equals(KeyCode.ENTER) || ev.getCode().equals(KeyCode.TAB)) {
                orderbyTextField.setText(orderByListView.getSelectionModel().getSelectedItem());
                if (orderByStackPane.isVisible()) {
                    orderByStackPane.setVisible(false);
                }
                orderbyTextField.requestFocus();
            }
        });

        // 分页事件
        pageFirstBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(1);
            Thread.ofVirtual().start(() -> {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()),
                        sqlText);
                Platform.runLater(() -> {
                    tableView.setItems(newTableDataList);
                    refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                });
            });
        });

        pagePrevBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(treeNode.getCurrentPage() == 1 ? 1 : treeNode.getCurrentPage() - 1);
            Thread.ofVirtual().start(() -> {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()),
                        sqlText);
                Platform.runLater(() -> {
                    tableView.setItems(newTableDataList);
                    refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                });
            });
        });

        pageNextBtn.setOnAction(ev -> {
            treeNode.setCurrentPage(treeNode.getCurrentPage() + 1);

            Thread.ofVirtual().start(() -> {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()),
                        sqlText);
                Platform.runLater(() -> {
                    tableView.setItems(newTableDataList);
                    refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                });
            });

        });

        //最后一页
        pageLastBtn.setOnAction(ev -> {
            treeNode.setCurrentPage((int) Math.ceil((double) treeNode.getTableViewRowCount() / Integer.parseInt(defaultFetchRowTextField.getText())));

            Thread.ofVirtual().start(() -> {
                ObservableList<RowData> newTableDataList = FXCollections.observableArrayList();
                treeNode.getTableRowDataList(parent,
                        treeNode,
                        columns,
                        newTableDataList,
                        whereTextField.getText(),
                        orderbyTextField.getText(),
                        Integer.parseInt(defaultFetchRowTextField.getText()),
                        sqlText);

                Platform.runLater(() -> {
                    tableView.setItems(newTableDataList);
                    refreshPageBtnReCalc(treeNode, Integer.parseInt(defaultFetchRowTextField.getText()), treeNode.getCurrentPage(), pageFirstBtn, pagePrevBtn, pageNextBtn, pageLastBtn);
                });
            });

        });

        //tab切换语法提示框应当关闭
        tabPane.getSelectionModel().selectedIndexProperty().addListener(ev -> {
            if (whereConditionStackPane.isVisible()) {
                whereConditionStackPane.setVisible(false);
            }
            if (orderByStackPane.isVisible()) {
                orderByStackPane.setVisible(false);
            }
        });

//
        // 表数据行双击事件
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                // 获取选中的行
                RowData selectedStudent = tableView.getSelectionModel().getSelectedItem();
                Map<String, String> dataMap = new LinkedHashMap<>();
                if (selectedStudent != null) {
                    // 获取所有列
                    for (TableColumn<RowData, ?> column : tableView.getColumns()) {
                        // 获取列名
                        String columnName = column.getText();

                        // 获取数据值
                        Object value = column.getCellData(selectedStudent);

                        dataMap.put(columnName, String.valueOf(value));
                    }
                }

                drawTableViewDataEditPane(dataMap, treeNode, sqlText);
            }
        });

        tab.setOnClosed(ev -> {
            tab.setContent(null);
            System.gc();
        });
    }


    // 新建redis类型Tab
    public void addRedisTabPaneOfData(TreeNode parent, TreeNode treeNode) {
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

        //content内容：需要包含2部分
        VBox vBox = new VBox();

        TableView<RowData> tableView = new TableView<>();
        HBox row2 = new HBox();
        row2.setPrefHeight(32);
        Text sqlText = new Text();
        row2.getChildren().add(sqlText);
        row2.setPadding(new Insets(0, 0, 0, 6));
        row2.setAlignment(Pos.CENTER_LEFT);
        vBox.getChildren().addAll(tableView, row2);

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

        // 表头 string list set 3种类型仅有一个value类型， zset 和 hash 需要两个字段
        ObservableList<TableColumn<RowData, ?>> columns = FXCollections.observableArrayList();

        if (parent.getName().toUpperCase().equals(RedisKeyTypeEnum.STRING.name())
                || parent.getName().toUpperCase().equals(RedisKeyTypeEnum.LIST.name())
                || parent.getName().toUpperCase().equals(RedisKeyTypeEnum.SET.name())) {
            TableColumn<RowData, String> valueColumn = new TableColumn<>("value");
            valueColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get("value")).asString());
            columns.add(valueColumn);
        } else if (parent.getName().toUpperCase().equals(RedisKeyTypeEnum.ZSET.name())) {

            TableColumn<RowData, String> valueColumn = new TableColumn<>("value");
            valueColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get("value")).asString());
            columns.add(valueColumn);

            TableColumn<RowData, String> scoreColumn = new TableColumn<>("score");
            scoreColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get("score")).asString());
            columns.add(scoreColumn);
        } else if (parent.getName().toUpperCase().equals(RedisKeyTypeEnum.HASH.name())) {

            TableColumn<RowData, String> fieldColumn = new TableColumn<>("field");
            fieldColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get("field")).asString());
            columns.add(fieldColumn);

            TableColumn<RowData, String> valueColumn = new TableColumn<>("value");
            valueColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().get("value")).asString());
            columns.add(valueColumn);
        }
        // 设置表列
        tableView.getColumns().setAll(columns);

        // 表数据
        ObservableList<RowData> tableDataList = FXCollections.observableArrayList();
        //获得表数据
        parent.setParent(parent.getParent());
        treeNode.getTableRowDataList(parent,
                treeNode,
                columns,
                tableDataList,
                null,
                null,
                null,
                sqlText);
        // 表赋值数据
        tableView.setItems(tableDataList);
    }

    // 双击行数据传入列名和值绘制一个编辑面板
    private void drawTableViewDataEditPane(Map<String, String> dataMap, TreeNode treeNode, Text sqlText) {
        Stage tableViewDataEditStage = new Stage();

        tableViewDataEditStage.setTitle(Config.EDIT_TABLE_VIEW_DATA_TITLE + " " + treeNode.getConnItem().getHost() + " " + treeNode.getName());

        VBox vBox = new VBox();
        vBox.setSpacing(10);
        vBox.setPadding(new Insets(10));

        ScrollPane tableViewDataEditPane = new ScrollPane(vBox);
        tableViewDataEditPane.setFitToWidth(true);
        tableViewDataEditPane.setPrefWidth(APP_DATA_EDIT_WIDTH);
        tableViewDataEditPane.setPrefHeight(APP_DATA_EDIT_HEIGHT);
        tableViewDataEditPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableViewDataEditPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        Scene tableViewDataEditScene = new Scene(tableViewDataEditPane);

        tableViewDataEditScene.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.ESCAPE) {
                tableViewDataEditStage.close();
            }
        });
        tableViewDataEditStage.setScene(tableViewDataEditScene);
        tableViewDataEditStage.getIcons().add(new Image(APP_AUTHOR_ICO));

        // 绘制key-value
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            HBox hBox = new HBox();
            VBox.setVgrow(hBox, Priority.ALWAYS);
            Label columnName = new Label(entry.getKey());
            columnName.setMinWidth(200.0);
            columnName.setPadding(new Insets(0, 0, 0, 5));
            hBox.setSpacing(10.0);
            hBox.getChildren().add(columnName);

            if (entry.getValue().startsWith("[") || entry.getValue().startsWith("{")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try {
                    String json = gson.toJson(gson.fromJson(entry.getValue(), Map.class));
                    int initialRowCount = json.split("\n").length;
                    TextArea textArea = new TextArea((json));
                    textArea.setPrefRowCount(initialRowCount);
                    textArea.setPrefWidth(APP_DATA_EDIT_WIDTH - 450);
                    hBox.getChildren().add(textArea);

                    textArea.textProperty().addListener((ob, oldVal, newVal) -> {
                        //如果值改变则创建更新按钮
                        if (!newVal.equals(oldVal)) {
                            Button updateBtn = new Button("更新");
                            if (hBox.getChildren().stream().noneMatch(child -> child instanceof Button)) {
                                hBox.getChildren().add(updateBtn);
                            }
                            updateRowDataTrigger(updateBtn, treeNode.getParent(), treeNode, dataMap.get(treeNode.getPkName()), entry.getKey(), newVal, sqlText);
                        }
                    });

                } catch (Exception e) {
                    TextField textField = new TextField(entry.getValue());
                    textField.setPrefWidth(APP_DATA_EDIT_WIDTH - 450);
                    hBox.getChildren().add(textField);

                    textField.textProperty().addListener((ob, oldVal, newVal) -> {
                        //如果值改变则创建更新按钮
                        if (!newVal.equals(oldVal)) {
                            Button updateBtn = new Button("更新");
                            if (hBox.getChildren().stream().noneMatch(child -> child instanceof Button)) {
                                hBox.getChildren().add(updateBtn);
                            }
                            updateRowDataTrigger(updateBtn, treeNode.getParent(), treeNode, dataMap.get(treeNode.getPkName()), entry.getKey(), newVal, sqlText);
                        }
                    });
                }


            } else {
                TextField textField = new TextField(entry.getValue());
                //主键禁止编辑
                if (entry.getKey().equals(treeNode.getPkName())) {
                    textField.setDisable(true);
                }
                textField.setPrefWidth(APP_DATA_EDIT_WIDTH - 450);
                hBox.getChildren().add(textField);

                textField.textProperty().addListener((ob, oldVal, newVal) -> {
                    //如果值改变则创建更新按钮
                    if (!newVal.equals(entry.getValue())) {
                        Button updateBtn = new Button("更新");
                        if (hBox.getChildren().stream().noneMatch(child -> child instanceof Button)) {
                            hBox.getChildren().add(updateBtn);
                        }
                        updateRowDataTrigger(updateBtn, treeNode.getParent(), treeNode, dataMap.get(treeNode.getPkName()), entry.getKey(), newVal, sqlText);
                    } else {
                        hBox.getChildren().removeIf(node -> node instanceof Button);
                    }
                });
            }


            hBox.setAlignment(Pos.CENTER_LEFT);
            vBox.getChildren().add(hBox);
        }
        tableViewDataEditStage.show();
    }

    //更新按钮触发逻辑
    private void updateRowDataTrigger(Button updateButton,
                                      TreeNode parent,
                                      TreeNode treeNode,
                                      String pkValue,
                                      String key,
                                      String value,
                                      Text sqlText) {
        updateButton.setOnMouseClicked(ev -> {
            //获取当前行数据主键
            String pkName = treeNode.getPkName();
            if (pkName == null || pkName.isEmpty()) {
                $.warning("提醒", "当前暂不支持缺乏主键的表数据更新");
                return;
            }
            treeNode.updateRowField(parent, treeNode, pkValue, key, value, sqlText);

        });
    }

    // 分页计算（当需要触发翻页按钮状态重新计算时触发）
    private void refreshPageBtnReCalc(TreeNode currentTreeNode,
                                      int limitRowValue,
                                      int currentPage,
                                      Button pageFirstBtn,
                                      Button pagePrevBtn,
                                      Button pageNextBtn,
                                      Button pageLastBtn) {
        //当前表数据行
        long totalRows = currentTreeNode.getTableViewRowCount() == null ? 0 : currentTreeNode.getTableViewRowCount();
        int totalPage = (int) Math.ceil((double) totalRows / limitRowValue);
        boolean pageFirstBtnEnable = currentPage > 1;
        boolean pagePrevBtnEnable = currentPage > 1;
        boolean pageNextBtnEnable = currentPage < totalPage;
        boolean pageLastBtnEnable = currentPage < totalPage;

        pageFirstBtn.setDisable(!pageFirstBtnEnable);
        pagePrevBtn.setDisable(!pagePrevBtnEnable);
        pageNextBtn.setDisable(!pageNextBtnEnable);
        pageLastBtn.setDisable(!pageLastBtnEnable);
    }

    // 内存监控任务
    private void startMemoryTick() {
        //基本的ToolTips
        memoryProgressbar.setTooltip(new Tooltip("当前堆+非堆已使用/已申请内存"));

        Task<Void> memoryTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
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
