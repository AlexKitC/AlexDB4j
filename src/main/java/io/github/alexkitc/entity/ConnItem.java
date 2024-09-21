package io.github.alexkitc.entity;

import io.github.alexkitc.entity.enums.DbTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/20 下午12:16
 */
@Data
@Accessors(chain = true)
public class ConnItem {

    // 显示的名称
    private String name;
    // 连接的主机
    private String host;
    // 连接的数据库类型
    private DbTypeEnum dbTypeEnum;
    // 连接的端口
    private int port;
    // 连接的用户名
    private String username;
    // 连接的密码
    private String password;

}
