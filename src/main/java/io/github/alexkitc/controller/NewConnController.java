package io.github.alexkitc.controller;

import io.github.alexkitc.App;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.entity.enums.DbType;
import io.github.alexkitc.util.$;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/7/19 下午11:43
 */
public class NewConnController {

    public static Stage NewConnStage;


    @FXML
    private TextField conName;
    @FXML
    private TextField host;
    @FXML
    private TextField port;
    @FXML
    private ComboBox<DbType> dbType;
    @FXML
    private TextField username;
    @FXML
    private TextField pwd;

    //初始化db类型选项
    @FXML
    private void initialize() {
        // 1.初始化db类型选项
        initDbTypeCombox();

    }


    // 存储连接
    @FXML
    private void onClickSaveConn() {
        ConnItem connItem = validConn();
        if (!$.isEmpty(connItem)) {
            writeConnConf(connItem);
        }
    }

    // 测试连接
    @FXML
    private void onClickTestConn() {

    }

    // 关闭新建连接面板
    @FXML
    private void onClickCloseNewConn() {
        NewConnStage.close();
    }


    // 1.初始化db类型选项
    private void initDbTypeCombox() {
        ObservableList<DbType> dbTypeList = FXCollections.observableArrayList(DbType.values());
        //设置选项
        dbType.setItems(dbTypeList);
        //设置默认选中
        dbType.setValue(DbType.MYSQL);
        port.setText("3306");

        //切换事件更换默认端口
        dbType.setOnAction(ev -> {
            DbType currentDbType = dbType.getValue();
            switch (currentDbType) {
                case MYSQL:
                    port.setText("3306");
                    break;
                case REDIS:
                    port.setText("6379");
                    break;
                case ORACLE:
                    port.setText("1521");
                    break;
                case MONGODB:
                    port.setText("27017");
                    break;
                case POSTGRESQL:
                    port.setText("5432");
                    break;
                case SQLSERVER:
                    port.setText("1433");
                    break;
                default:
                    break;
            }
        });
    }

    // 校验连接信息
    private ConnItem validConn() {
        String connNameString = conName.getText();
        String hostNameString = host.getText();
        String portString = port.getText();
        DbType dbTypeString = dbType.getValue();
        String usernameString = username.getText();
        String passwordString = pwd.getText();

        if ($.isEmpty(connNameString)) {
            conName.setPromptText("请输入一个连接名以更好的辨识连接概要");
            conName.requestFocus();
            return null;
        }

        if ($.isEmpty(hostNameString)) {
            host.setPromptText("请输入连接的url或ip地址");
            host.requestFocus();
            return null;
        }

        if ($.isEmpty(portString)) {
            port.setPromptText("请输入连接端口");
            port.requestFocus();
            return null;
        }

        if ($.isEmpty(usernameString)) {
            username.setPromptText("请输入连接的用户名");
            username.requestFocus();
            return null;
        }

        if ($.isEmpty(passwordString)) {
            pwd.setPromptText("请输入连接密码");
            pwd.requestFocus();
            return null;
        }

        return new ConnItem()
                .setName(connNameString)
                .setHost(hostNameString)
                .setDbType(dbTypeString)
                .setPort(Integer.parseInt(portString))
                .setUsername(usernameString)
                .setPassword(passwordString);
    }

    // 把内容写入文件，并以连接名作为文件名
    private void writeConnConf(ConnItem connItem) {
        Path filePath = Paths.get(String.format("%s.conn", connItem.getName()));
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }

            Files.write(filePath, String.format("%s###%s###%s###%s###%s###%s",
                    connItem.getName(),
                    connItem.getHost(),
                    connItem.getPort(),
                    connItem.getDbType(),
                    connItem.getUsername(),
                    connItem.getPassword()).getBytes());

            NewConnStage.close();
            App.homeControllerInstance.refreshTreeView();
            //刷新TreeView
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
