package io.github.alexkitc.component;

import io.github.alexkitc.App;
import io.github.alexkitc.conf.Config;
import io.github.alexkitc.controller.HomeController;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.TreeNodeType;
import javafx.geometry.Pos;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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



    // 添加连接的点击事件
    public MyConnItemTreeCell() {
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                switch (getTreeItem().getValue().getTreeNodeType()) {
                    case ROOT:
                        break;
                    case CONN:
                        List<TreeNode> dbList = getTreeItem().getValue().getDbList(getTreeItem().getValue());
                        List<String> historyDbList = getTreeItem().getChildren()
                                .stream()
                                .map(item -> item.getValue().getName())
                                .toList();
                        for (TreeNode db : dbList) {
                            if (!historyDbList.contains(db.getName())) {
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(db.getName(), TreeNodeType.DB, Config.CONN_ICON_DB_PATH0, db.getConnItem())));
                            }
                        }
                        getTreeItem().setExpanded(true);
                        break;
                    case DB:
                        List<TreeNode> tableList = getTreeItem().getValue().getTableList(getTreeItem().getValue());
                        List<String> historyTableList = getTreeItem().getChildren()
                                .stream()
                                .map(item -> item.getValue().getName())
                                .toList();
                        for (TreeNode table : tableList) {
                            if (!historyTableList.contains(table.getName())) {
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(table.getName(), TreeNodeType.TABLE, Config.CONN_ICON_TABLE_PATH0, table.getConnItem())));
                            }
                        }
                        getTreeItem().setExpanded(true);
                        break;
                    case TABLE:
                        List<TreeNode> tableFieldList = getTreeItem().getValue().getTableFieldList(getTreeItem().getParent().getValue(), getTreeItem().getValue());
                        List<String> historyTableFieldList = getTreeItem().getChildren()
                                .stream()
                                .map(item -> item.getValue().getName())
                                .toList();
                        for (TreeNode tableField : tableFieldList) {
                            if (!historyTableFieldList.contains(tableField.getName())) {
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(tableField.getName(), TreeNodeType.FIELD, Config.CONN_ICON_FIELD_PATH0, tableField.getConnItem(), tableField.getTypeAndLength())));
                            }
                        }
                        // 新建TabPane容器展示数据
                        App.homeControllerInstance.addTabPaneOfData(getTreeItem().getValue());
                        break;
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
        } else {
            text.setText(item.getName());
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(item.getIcon()))));
            imageView.setFitWidth(Config.ICON_SIZE);
            imageView.setFitHeight(Config.ICON_SIZE);
            // 根节点不设置图标
            HBox hBox = null;
            switch (getTreeItem().getValue().getTreeNodeType()) {
                case ROOT:
                case CONN:
                case DB:
                case TABLE:
                    hBox = new HBox(imageView, text);
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
        }


    }
}
