package io.github.lxien.orbien.client.config;

public class ConfigHelper {
    private static AppConfig appConfig;

    public static void set(AppConfig config) {
        appConfig = config;
    }

    public static AppConfig get() {
        if (appConfig == null) {
            throw new NullPointerException("AppConfig is null");
        }
        return appConfig;
    }
}
