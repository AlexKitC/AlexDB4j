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

import java.util.List;
import java.util.Objects;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote treeview节点的单元格重写（需要图标+实体的某个属性）
 * @since 2024/7/20 下午5:06
 */
public class MyConnItemTreeCell extends TreeCell<TreeNode> {

    private final ImageView imageView = new ImageView();
    private final Text text = new Text();

    public MyConnItemTreeCell() {
        setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                List<TreeNode> dbList = getTreeItem().getValue().getDbList(getTreeItem().getValue());
                for (TreeNode db : dbList) {
                    getTreeItem().getChildren().add(new TreeItem<>(new TreeNode(db.getName(), TreeNodeType.DB, Config.CONN_ICON_DB_PATH0, db.getConnItem())));
                }
                getTreeItem().setExpanded(true);
            }
        });
    }

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
            HBox hBox = item.getTreeNodeType().equals(TreeNodeType.ROOT) ? new HBox(new ImageView(), text) : new HBox(imageView, text);
            hBox.setSpacing(5);
            setGraphic(hBox);
        }


    }
}
