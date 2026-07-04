package io.github.lxien.orbien.common.utils;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Toml文件工具类
 *
 * @author liuxin
 */
public class TomlUtils {
    public static Toml readToml(String filePath) {
        return new Toml().read(new File(filePath));
    }

    public static Map<String, Object> readMap(String filePath) {
        return new Toml().read(new File(filePath)).toMap();
    }

    public static void write(Map<String, Object> map, String path) throws IOException {
        TomlWriter tomlWriter = new TomlWriter();
        tomlWriter.write(map, new File(path));
    }
}
