package io.github.alexkitc.component;

import io.github.alexkitc.conf.Config;
import io.github.alexkitc.entity.TreeNode;
import io.github.alexkitc.entity.enums.TreeNodeType;
import javafx.scene.control.TreeCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.Objects;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote treeview节点的单元格重写（需要图标+实体的某个属性）
 * @since 2024/7/20 下午5:06
 */
public class MyConnItemTreeCell extends TreeCell<TreeNode> {

    private final ImageView imageView;
    private final Text text;

    public MyConnItemTreeCell() {
        HBox row = new HBox();
        imageView = new ImageView();
        text = new Text();
        row.getChildren().addAll(imageView, text);
        setGraphic(row);
    }

    @Override
    protected void updateItem(TreeNode item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(item.getIcon()))));
            imageView.setFitWidth(Config.ICON_SIZE);
            imageView.setFitHeight(Config.ICON_SIZE);
            text.setText(item.getName());
            //根节点不设置图标
            if (!item.getTreeNodeType().equals(TreeNodeType.ROOT)) {
                setGraphic(imageView);
            }
            setText(item.getName());
        }
    }
}
