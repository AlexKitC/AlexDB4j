package io.github.alexkitc.entity;

import io.github.alexkitc.entity.enums.TreeNodeType;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/20 上午11:24
 */
@Data
@Accessors(chain = true)
public class TreeNode {

    // 节点的显示名
    private String name;

    // 节点类型
    private TreeNodeType treeNodeType;

    // 节点的图标
    private String icon;

    // 节点对应的实体
    private ConnItem connItem;

    // 节点的活跃状态（激活状态）
    private boolean active;

    public TreeNode(String name, TreeNodeType treeNodeType, String icon) {
        this.name = name;
        this.treeNodeType = treeNodeType;
        this.icon = icon;
    }

    public TreeNode(String name, String icon) {
        this.name = name;
        this.icon = icon;
    }

    public TreeNode(ConnItem connItem, String icon) {
        this.connItem = connItem;
        this.icon = icon;
    }
}
