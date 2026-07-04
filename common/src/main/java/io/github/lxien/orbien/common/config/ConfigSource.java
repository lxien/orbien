package io.github.lxien.orbien.common.config;

public interface ConfigSource {
    Config load() ;
    ConfigSourceType getSourceType();
}
