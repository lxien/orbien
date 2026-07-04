package io.github.lxien.orbien.client.config;

import lombok.Setter;

public class ConfigUtils {
    @Setter
    private static AppConfig config;
    
    private ConfigUtils() {}

    public static AppConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("Config not set");
        }
        return config;
    }
    
    public static boolean hasConfig() {
        return config != null;
    }
}