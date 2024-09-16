package io.github.alexkitc.util;

import io.github.alexkitc.conf.Vars;
import io.github.alexkitc.entity.ConnItem;
import io.github.alexkitc.entity.enums.DbTypeEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.github.alexkitc.conf.Vars.CONFIG_FILE_SUFFIX;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote
 * @since 2024/9/5 22:36
 */
public class Utils {

    public static List<ConnItem> readConnItemList() {
        Path currentDir = Paths.get(".");
        try {
            List<Path> connFileList = Files.walk(currentDir)
                    .filter(f -> f.toString().endsWith(CONFIG_FILE_SUFFIX))
                    .toList();

            return connFileList.stream()
                    .map(f -> {
                        try {
                            String[] strings = Files.readString(f).split(Vars.CONN_SPLIT_FLAG);
                            return new ConnItem()
                                    .setName(strings[0])
                                    .setHost(strings[1])
                                    .setPort(Integer.parseInt(strings[2]))
                                    .setDbTypeEnum(DbTypeEnum.valueOf(strings[3]))
                                    .setUsername(strings.length > 4 ? strings[4] : "")
                                    .setPassword(strings.length > 5 ? strings[5] : "");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
