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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import static io.github.alexkitc.conf.Config.NEW_CONN_ICON_PATH;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/17 下午10:29
 */
public class HomeController {

    @FXML
    private Button newConnBtn;
    @FXML
    private TreeView<TreeNode> treeView;

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
                            String[] strings = Files.readString(f).split("###");
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
        connItemList.forEach(item -> root.getChildren().add(new TreeItem<>(new TreeNode(item.getName(), TreeNodeType.CONN, Config.CONN_ICON_PATH0))));
        treeView.setRoot(root);

        //设置TreeCell工厂
        treeView.setCellFactory(cell -> new MyConnItemTreeCell());
    }
}
