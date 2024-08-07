package io.github.alexkitc.conf;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote 配置类
 * @since 2024/7/17 下午10:26
 */
public class Config {

    // app标题
    public static final String APP_TITLE = "alexDB";
    // app版本
    public static final String APP_VERSION = "0.0.1";
    // app作者
    public static final String APP_AUTHOR = "alexkitc";
    // 新建连接stage标题
    public static final String APP_NEW_CONN_TITLE = "新建连接";
    // app初始width
    public static final int APP_WIDTH = 1400;
    // app初始height
    public static final int APP_HEIGHT = 900;
    // app内图标尺寸
    public static final int ICON_SIZE = 18;
    public static final int ICON_SMALL_SIZE = 12;
    // 配置项分隔符
    public static final String CONN_SPLIT_FLAG = "###";
    // 配置文件后缀
    public static final String CONFIG_FILE_SUFFIX = ".conn";
    // 主页fxml路径
    public static final String FXML_HOME_FILE_PATH = "/fxml/home.fxml";
    // css路径
    public static final String APP_CSS_PATH = "/css/app.css";
    // 新建连接 按钮 图标
    public static final String NEW_CONN_ICON_PATH = "/icon/new-conn.png";
    // 连接树-连接图标
    public static final String CONN_ICON_PATH0 = "/icon/conn0.png";
    public static final String CONN_ICON_PATH1 = "/icon/conn1.png";

    // 连接树-db图标
    public static final String CONN_ICON_DB_PATH0 = "/icon/db0.png";
    public static final String CONN_ICON_DB_PATH1 = "/icon/db1.png";

    // 连接树-table图标
    public static final String CONN_ICON_TABLE_PATH0 = "/icon/table0.png";
    public static final String CONN_ICON_TABLE_PATH1 = "/icon/table1.png";

    // 连接树-field图标
    public static final String CONN_ICON_FIELD_PATH0 = "/icon/field.png";

    // 连接树-mysql图标
    public static final String CONN_ICON_DB_MYSQL_PATH0 = "/icon/mysql0.png";
    public static final String CONN_ICON_DB_MYSQL_PATH1 = "/icon/mysql1.png";

    // 分页图标
    public static final String PAGE_ICON_FIRST = "/icon/page-first.png";
    public static final String PAGE_ICON_PREV = "/icon/page-prev.png";
    public static final String PAGE_ICON_NEXT = "/icon/page-next.png";
    public static final String PAGE_ICON_LAST = "/icon/page-last.png";

    // 默认读取的数据行
    public static final Integer DEFAULT_FETCH_ROW = 500;
    //默认column最小/大宽度
    public static final Double DEFAULT_COLUMN_MIN_WIDTH = 96.0;
    public static final Double DEFAULT_COLUMN_MAX_WIDTH = 168.0;

    // tooltip文本
    public static final String TOOLTIP_NEW_CONN = "新建连接";
    public static final String TOOLTIP_FIRST_PAGE = "第一页";
    public static final String TOOLTIP_PREV_PAGE = "上一页";
    public static final String TOOLTIP_NEXT_PAGE = "下一页";
    public static final String TOOLTIP_LAST_PAGE = "最后一页";
}
