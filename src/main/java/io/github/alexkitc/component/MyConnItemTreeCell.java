package io.github.alexkitc.component;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.TreeNodeType;
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

    private final ImageView imageView = new ImageView();
    private final Text text = new Text();

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
                                getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(tableField.getName(), TreeNodeType.FIELD, Config.CONN_ICON_TABLE_PATH0, tableField.getConnItem())));
                            }
                        }
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
                    hBox = new HBox(new ImageView(), text);
                default:
                    break;

            }
            hBox.setSpacing(5);
            setGraphic(hBox);
        }


    }
}
