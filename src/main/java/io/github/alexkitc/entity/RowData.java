package io.github.alexkitc.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alexKitc
 * @version 1.0.0
 * @apiNote table表数据
 * @since 2024/8/7 10:23
 */
public class RowData {

    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }
}
