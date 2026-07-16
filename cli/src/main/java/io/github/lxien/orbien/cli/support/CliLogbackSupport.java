package io.github.lxien.orbien.cli.support;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.slf4j.LoggerFactory;

public final class CliLogbackSupport {

    private static final String STDERR_PATTERN = "%msg%n";

    private CliLogbackSupport() {
    }

    public static void configureQuietSession() {
        LoggerContext context = resolveLoggerContext();
        context.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(STDERR_PATTERN);
        encoder.start();

        ConsoleAppender<ILoggingEvent> errorAppender = new ConsoleAppender<>();
        errorAppender.setContext(context);
        errorAppender.setName("CliErrorConsole");
        errorAppender.setEncoder(encoder);
        errorAppender.start();

        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.ERROR);
        rootLogger.addAppender(errorAppender);

        silenceFrameworkLoggers(context);
        bindNettyToSlf4j(context);
    }

    private static void silenceFrameworkLoggers(LoggerContext context) {
        String[] noisyLoggers = {
                "io.netty",
                "io.netty.util.internal.logging.InternalLoggerFactory",
                "io.github.lxien.orbien.client.config",
                "io.github.lxien.orbien.client.transport",
                "org.slf4j",
                "ch.qos.logback"
        };
        for (String loggerName : noisyLoggers) {
            context.getLogger(loggerName).setLevel(Level.OFF);
        }
    }

    private static void bindNettyToSlf4j(LoggerContext context) {
        context.getLogger("io.netty.util.internal.logging.InternalLoggerFactory").setLevel(Level.OFF);
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private static LoggerContext resolveLoggerContext() {
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            return loggerContext;
        }
        LoggerContext context = new LoggerContext();
        context.setName("orbien-cli");
        context.start();
        return context;
    }
}
