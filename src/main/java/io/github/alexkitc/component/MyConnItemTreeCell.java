package io.github.alexkitc.component;

import io.github.alexkitc.App;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.controller.NewConnController;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.DbTypeEnum;
import io.github.alexkitc.entity.enums.TreeNodeTypeEnum;
import io.github.alexkitc.util.$;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.github.alexkitc.conf.Config.APP_AUTHOR_ICO;
import static io.github.alexkitc.conf.Config.FXML_NEW_CONN_FILE_PATH;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote treeview节点的单元格重写（需要图标+实体的某个属性）
 * @since 2024/7/20 下午5:06
 */
public class MyConnItemTreeCell extends TreeCell<TreeNode> {

    // 图标
    private final ImageView imageView = new ImageView();
    // 名称
    private final Text text = new Text();
    // 字段类型+长度
    private final Text typeAndLength = new Text();
    // 表数据行数
    private final Text rowText = new Text();


    // 添加树节点的点击事件
    public MyConnItemTreeCell() {
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                switch (getTreeItem().getValue().getTreeNodeTypeEnum()) {
                    case ROOT:
                        break;
                    case CONN:
                        // mysql
                        switch (getTreeItem().getValue().getConnItem().getDbTypeEnum()) {
                            case MYSQL:
                            case MONGODB:
                            case REDIS: {
                                Future<List<TreeNode>> future = CompletableFuture.supplyAsync(() -> getTreeItem().getValue().getDbList(getTreeItem().getValue()),
                                        Executors.newVirtualThreadPerTaskExecutor());
                                List<TreeNode> dbList;
                                try {
                                    dbList = future.get();
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                                List<String> historyDbList = getTreeItem().getChildren()
                                        .stream()
                                        .map(item -> item.getValue().getName())
                                        .toList();
                                if (dbList == null) {
                                    return;
                                }
                                for (TreeNode db : dbList) {
                                    if (!historyDbList.contains(db.getName())) {
                                        getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(db.getName(), TreeNodeTypeEnum.DB, Config.CONN_ICON_DB_PATH0, db.getConnItem())));
                                    }
                                }
                                getTreeItem().setExpanded(true);
                                break;
                            }

                            default: {
                                break;
                            }
                        }

                        break;
                    case DB: {
                        CompletableFuture<List<TreeNode>> future = CompletableFuture.supplyAsync(() -> getTreeItem().getValue().getTableList(getTreeItem().getValue()),
                                Executors.newVirtualThreadPerTaskExecutor());
                        List<TreeNode> tableList;
                        try {
                            tableList = future.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                        List<String> historyTableList = getTreeItem().getChildren()
                                .stream()
                                .map(item -> item.getValue().getName())
                                .toList();
                        for (TreeNode table : tableList) {
                            if (!historyTableList.contains(table.getName())) {
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(table.getName(), TreeNodeTypeEnum.TABLE, Config.CONN_ICON_TABLE_PATH0, table.getConnItem())));
                            }
                        }
                        getTreeItem().setExpanded(true);
                        break;
                    }

                    case TABLE: {
                        List<TreeNode> tableFieldList;
                        // mongo不允许在用户线程中执行
                        if (getTreeItem().getValue().getConnItem().getDbTypeEnum().equals(DbTypeEnum.MONGODB)) {
                            tableFieldList = getTreeItem().getValue().getTableFieldList(getTreeItem().getParent().getValue(), getTreeItem().getValue());
                        } else {
                            Future<List<TreeNode>> future = CompletableFuture.supplyAsync(() -> getTreeItem().getValue().getTableFieldList(getTreeItem().getParent().getValue(), getTreeItem().getValue()),
                                    Executors.newVirtualThreadPerTaskExecutor());

                            try {
                                tableFieldList = future.get();
                            } catch (InterruptedException | ExecutionException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        List<String> historyTableFieldList = getTreeItem().getChildren()
                                .stream()
                                .map(item -> item.getValue().getName())
                                .toList();
                        if (tableFieldList == null) {
                            return;
                        }
                        for (TreeNode tableField : tableFieldList) {
                            if (!historyTableFieldList.contains(tableField.getName())) {
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(tableField.getName(), TreeNodeTypeEnum.FIELD, Config.CONN_ICON_FIELD_PATH0, tableField.getConnItem(), tableField.getTypeAndLength())));
                            }
                        }
                        TreeNode innerParent = getTreeItem().getParent().getValue();
                        getTreeItem().getValue().setParent(innerParent);
                        // 新建TabPane容器展示数据
                        switch (getTreeItem().getValue().getConnItem().getDbTypeEnum()) {
                            case MYSQL:
                                App.homeControllerInstance.addMysqlTabPaneOfData(getTreeItem().getParent().getValue(), getTreeItem().getValue());
                                break;
                            // redis类型需要展开TreeView
                            case REDIS:
                                TreeNode parent = getTreeItem().getParent().getValue();
                                parent.setParent(parent.getParent());
                                getTreeItem().setExpanded(true);
                                break;
                            case MONGODB:
                                break;
                            default:
                                break;
                        }
                        break;
                    }

                    case FIELD: {
                        //暂时字段类型仅针对redis方可点击
                        if (getTreeItem().getValue().getConnItem().getDbTypeEnum().equals(DbTypeEnum.REDIS)) {
                            App.homeControllerInstance.addRedisTabPaneOfData(getTreeItem().getParent().getValue(), getTreeItem().getValue());
                        }
                        break;
                    }
                    default:
                        break;
                }


            }
        });
    }

    // 重写TreeItem：添加图标和间距
    @Override
    protected void updateItem(TreeNode item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
            setContextMenu(null);
        } else {
            // 渲染TextItem图标+文本
            text.setText(item.getName());
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(item.getIcon()))));
            imageView.setFitWidth(Config.ICON_SIZE);
            imageView.setFitHeight(Config.ICON_SIZE);
            // 根节点不设置图标
            HBox hBox = null;
            switch (getTreeItem().getValue().getTreeNodeTypeEnum()) {
                case ROOT:
                case CONN:
                case DB:
                case TABLE:
                    if (item.getTableViewRowCount() != null) {
                        rowText.setText("(" + item.getTableViewRowCount() + ")");
                        rowText.setStyle("-fx-font-size: 10px;-fx-fill: #707070;");
                        hBox = new HBox(imageView, text, rowText);
                    } else {
                        hBox = new HBox(imageView, text);
                    }

                    break;
                case FIELD:
                    typeAndLength.setText(item.getTypeAndLength());
                    typeAndLength.setStyle("-fx-font-size: 10px;-fx-fill: #707070;");
                    hBox = new HBox(imageView, text, typeAndLength);

                default:
                    break;

            }
            //居中
            hBox.setAlignment(Pos.CENTER_LEFT);
            // 图标和nama边距
            hBox.setSpacing(5);
            setGraphic(hBox);

            //根据类型设置上下文鼠标右键菜单
            ContextMenu contextMenu = addContextMenuByNodeType(item, getTreeItem().getValue().getTreeNodeTypeEnum());
            setContextMenu(contextMenu);
        }


    }

    //添加上下文菜单
    private ContextMenu addContextMenuByNodeType(TreeNode treeNode, TreeNodeTypeEnum treeNodeType) {
        switch (treeNodeType) {
            case CONN: {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem menuItemOfConnDetail = new MenuItem("连接属性");
                MenuItem menuItemOfNewDb = new MenuItem("新建数据库");
                MenuItem menuItemOfRename = new MenuItem("重命名");
                MenuItem menuItemOfRefresh = new MenuItem("刷新");
                MenuItem menuItemOfDeleteConn = new MenuItem("删除连接");
                contextMenu.getItems().addAll(menuItemOfConnDetail,
                        menuItemOfNewDb,
                        menuItemOfRename,
                        menuItemOfRefresh,
                        menuItemOfDeleteConn);

                //连接属性点击事件
                menuItemOfConnDetail.setOnAction(ev -> {
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
                    newConnStage.getIcons().add(new Image(APP_AUTHOR_ICO));
                    App.newConnControllerInstance = fxmlLoader.getController();
                    App.newConnControllerInstance.fillConnDetail(treeNode);
                    newConnStage.show();
                });

                // 新建连接
                menuItemOfNewDb.setOnAction(ev -> {
                    $.info("敬请期待", "作者还在开发中");
                });

                // 重命名
                menuItemOfRename.setOnAction(ev -> {
                    $.info("敬请期待", "作者还在开发中");
                });

                //刷新点击事件
                menuItemOfRefresh.setOnAction(ev -> {
                    App.homeControllerInstance.refreshTreeView();
                });

                //删除连接事件
                menuItemOfDeleteConn.setOnAction(ev -> {
                    ButtonType buttonType = $.confirm("删除提醒", "确定要删除当前连接配置文件吗？");
                    if (buttonType == ButtonType.OK) {
                        System.out.println("删除");
                    }
                });

                return contextMenu;
            }

            case DB: {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem menuItemDeleteDb = new MenuItem("删除连接");
                contextMenu.getItems().add(menuItemDeleteDb);
                return contextMenu;
            }

            default:
                break;
        }

        return null;
    }
}
