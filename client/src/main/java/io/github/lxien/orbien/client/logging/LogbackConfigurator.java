/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.client.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Logback 日志配置管理器
 *
 * @author liuxin
 */
public class LogbackConfigurator {

    private final LogbackConfig config;

    private LogbackConfigurator(LogbackConfig config) {
        this.config = config;
    }

    public static class LogbackConfig {
        /**
         * 日志文件存储目录
         */
        private final String path;
        /**
         * 当前活动日志文件路径
         */
        private final String logFilePath;
        /**
         * 归档日志文件模式
         */
        private final String archiveFilePatternPath;
        /**
         * 日志输出格式
         */
        private final String logPattern;
        /**
         * 保留历史文件天数
         */
        private final int maxHistory;
        /**
         * 日志总大小限制
         */
        private final String totalSizeCap;
        /**
         * 根logger日志级别
         */
        private final Level logLevel;
        /**
         * 自定义logger日志级别，用于精细化控制
         */
        private final Map<String, Level> specificLoggers;

        private LogbackConfig(String path, String logFilePath, String archiveFilePatternPath, String logPattern,
            int maxHistory, String totalSizeCap, Level logLevel, Map<String, Level> specificLoggers) {
            this.path = path;
            this.logFilePath = logFilePath;
            this.archiveFilePatternPath = archiveFilePatternPath;
            this.logPattern = logPattern;
            this.maxHistory = maxHistory;
            this.totalSizeCap = totalSizeCap;
            this.logLevel = logLevel;
            this.specificLoggers = specificLoggers != null ? Collections.unmodifiableMap(specificLoggers) : Collections.emptyMap();
        }

        public String getPath() {
            return path;
        }

        public String getLogFilePath() {
            return logFilePath;
        }

        public String getArchiveFilePatternPath() {
            return archiveFilePatternPath;
        }

        public String getLogPattern() {
            return logPattern;
        }

        public int getMaxHistory() {
            return maxHistory;
        }

        public String getTotalSizeCap() {
            return totalSizeCap;
        }

        public Level getLogLevel() {
            return logLevel;
        }

        public Map<String, Level> getSpecificLoggers() {
            return specificLoggers;
        }
    }

    public static class Builder {
        private String path = "logs";
        private String logName = "app.log";
        private String archivePattern = "app.%d{yyyy-MM-dd}.log";
        private String logPattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
        private int maxHistory = 30;
        private String totalSizeCap = "3GB";
        private Level logLevel = Level.INFO;
        private final Map<String, Level> specificLoggers = new LinkedHashMap<>();

        public Builder setPath(String path) {
            this.path = path;
            return this;
        }

        public Builder setLogName(String logName) {
            this.logName = logName;
            return this;
        }

        public Builder setArchivePattern(String archivePattern) {
            this.archivePattern = archivePattern;
            return this;
        }

        public Builder setLogPattern(String logPattern) {
            this.logPattern = logPattern;
            return this;
        }

        public Builder setMaxHistory(int maxHistory) {
            this.maxHistory = maxHistory;
            return this;
        }

        public Builder setTotalSizeCap(String totalSizeCap) {
            this.totalSizeCap = totalSizeCap;
            return this;
        }

        public Builder setLogLevel(Level logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder addLogger(String loggerName, Level level) {
            this.specificLoggers.put(loggerName, level);
            return this;
        }

        public Builder addLoggers(Map<String, Level> loggers) {
            this.specificLoggers.putAll(loggers);
            return this;
        }

        public LogbackConfigurator build() {
            LogbackConfig config = new LogbackConfig(
                path,
                path + File.separator + logName,
                path + File.separator + archivePattern,
                logPattern,
                maxHistory,
                totalSizeCap,
                logLevel,
                new LinkedHashMap<>(specificLoggers));
            return new LogbackConfigurator(config);
        }
    }

    public void configure() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        // 配置编码器
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(config.getLogPattern());
        encoder.start();

        // 配置控制台输出
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("Console");
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // 配置文件输出
        RollingFileAppender<ILoggingEvent> fileAppender = new RollingFileAppender<>();
        fileAppender.setContext(context);
        fileAppender.setName("File");
        fileAppender.setFile(config.getLogFilePath());

        // 配置按天滚动的策略
        TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(config.getArchiveFilePatternPath());
        rollingPolicy.setMaxHistory(config.getMaxHistory());
        rollingPolicy.setTotalSizeCap(FileSize.valueOf(config.getTotalSizeCap()));
        rollingPolicy.start();

        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.setEncoder(encoder);
        fileAppender.start();

        // 配置根 Logger
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(config.getLogLevel());
        rootLogger.addAppender(consoleAppender);
        rootLogger.addAppender(fileAppender);
        //自定义Logger
        for (Map.Entry<String, Level> entry : config.getSpecificLoggers().entrySet()) {
            String loggerName = entry.getKey();
            Level level = entry.getValue();
            Logger logger = context.getLogger(loggerName);
            logger.setLevel(level);
        }
    }
}
