package io.github.alexkitc.controller;

import io.github.alexkitc.component.MyConnItemTreeCell;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.DbType;
import io.github.alexkitc.entity.enums.TreeNodeType;
import io.github.alexkitc.util.$;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static io.github.alexkitc.conf.Config.CONN_SPLIT_FLAG;
import static io.github.alexkitc.conf.Config.NEW_CONN_ICON_PATH;

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

    private TabPane tabPane;

    // 初始化
    @FXML
    private void initialize() {
        // 1.按钮图标
        $.addNewConnButtonIcon(newConnBtn, NEW_CONN_ICON_PATH);
        // 2.读取已有连接信息
        List<ConnItem> connItemList = readConnItemList();
        // 3.初始化树
        initTree(connItemList);
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

    // 新建Tab+TabPane容纳表数据，含4部分：1.功能按钮，2.搜索，排序，3.数据tableView，4.执行语句
    public void addTabPaneOfData(TreeNode treeNode) {
        // 首次新建
        if (tabPane == null) {
            tabPane = new TabPane();
        }
        Tab tab = new Tab(treeNode.getName() + " " + treeNode.getConnItem().getHost());

        //content内容：需要包含4部分
        VBox vBox = new VBox();

        // row1
        HBox row1 = new HBox();
        row1.setPrefHeight(32);
        row1.setSpacing(10);
        row1.getChildren().addAll(new Button("占位btn1"), new Button("占位btn2"));
        row1.setAlignment(Pos.CENTER_LEFT);
        row1.setPadding(new Insets(0, 0, 0, 6));

        // row2
        HBox row2 = new HBox();
        row2.setPrefHeight(32);
        row2.setSpacing(10);
        row2.getChildren().addAll(new Text("WHERE "),
                new TextField(),
                new Text("ORDER BY "),
                new TextField());
        row2.setPadding(new Insets(0, 0, 0, 6));
        row2.setAlignment(Pos.CENTER_LEFT);

        // row3
        TableView tableView = new TableView();

        //row4
        HBox row4 = new HBox();
        row4.setPrefHeight(32);
        row4.getChildren().add(new Text("select * from xxx"));
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

        // 存储当前连接信息已备后续展开逻辑
        tab.setUserData(treeNode);

        // 不允许相同tab反复添加到容器内
        List<String> tabNameList = tabPane.getTabs()
                .stream()
                .map(Tab::getText)
                .toList();
        if (!tabNameList.contains(tab.getText())) {
            tab.setContent(vBox);
            tabPane.getTabs().add(tab);
        }

        if (!mainDataContainer.getChildren().contains(tabPane)) {
            mainDataContainer.getChildren().add(tabPane);
        }


    }
}
