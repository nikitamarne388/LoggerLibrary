package logger.loggerlibrary.logger;

import logger.loggerlibrary.config.DbLoggerConfig;
import logger.loggerlibrary.config.FileLoggerConfig;
import logger.loggerlibrary.config.LoggerConfig;
import logger.loggerlibrary.entities.LogLevel;
import logger.loggerlibrary.entities.Message;
import logger.loggerlibrary.entities.Pair;
import logger.loggerlibrary.entities.ThreadModel;
import logger.loggerlibrary.entities.WriteMode;
import logger.loggerlibrary.interfaces.logger.Sink;
import logger.loggerlibrary.sink.DbSink;
import logger.loggerlibrary.sink.FileSink;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

public class Logger {
    private final Map<String, Function<LoggerConfig, Sink>> sinkFactories;
    private final List<Pair<Sink, LogLevel>> sinkWithLevels;

    public Logger() {
        this.sinkFactories = new HashMap<>();
        this.sinkWithLevels = new ArrayList<>();
        registerDefaultFactories();
    }

    private void registerDefaultFactories() {
        sinkFactories.put("FILE", config -> {
            try {
                FileLoggerConfig fileConfig = (FileLoggerConfig) config;
                return createFileSink(fileConfig);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create FileSink", e);
            }
        });
        sinkFactories.put("DATABASE", config -> {
            try {
                DbLoggerConfig dbConfig = (DbLoggerConfig) config;
                return createDbSink(dbConfig);
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to create DbSink: " + e.getMessage(), e);
            }
        });
    }

    private Sink createFileSink(FileLoggerConfig config) throws IOException {
        String baseFilePath = config.getFileLocation();
        String formatter = config.getTsFormat() != null ? config.getTsFormat() : "yyyy-MM-dd HH:mm:ss";
        LogLevel minLevel = LogLevel.valueOf(config.getLogLevel() != null ? config.getLogLevel().toUpperCase() : "DEBUG");
        ThreadModel threadModel = ThreadModel.valueOf(config.getThreadModel() != null ? config.getThreadModel().toUpperCase() : "SINGLE");
        WriteMode writeMode = WriteMode.valueOf(config.getWriteMode() != null ? config.getWriteMode().toUpperCase() : "SYNC");
        return new FileSink(baseFilePath, formatter, minLevel, threadModel, writeMode);
    }

    private Sink createDbSink(DbLoggerConfig config) throws SQLException {
        String dbHost = config.getDbHost();
        String dbPort = config.getDbPort();
        String formatter = config.getTsFormat() != null ? config.getTsFormat() : "yyyy-MM-dd HH:mm:ss";
        LogLevel minLevel = LogLevel.valueOf(config.getLogLevel() != null ? config.getLogLevel().toUpperCase() : "DEBUG");
        ThreadModel threadModel = ThreadModel.valueOf(config.getThreadModel() != null ? config.getThreadModel().toUpperCase() : "SINGLE");
        WriteMode writeMode = WriteMode.valueOf(config.getWriteMode() != null ? config.getWriteMode().toUpperCase() : "SYNC");
        return new DbSink(dbHost, dbPort, formatter, minLevel, threadModel, writeMode);
    }

    public void configure(List<LoggerConfig> configurations) {
        for (LoggerConfig config : configurations) {
            Function<LoggerConfig, Sink> factory = sinkFactories.get(config.getSinkType());
            if (factory == null) {
                throw new IllegalStateException("Invalid Sink Type: " + config.getSinkType());
            }
            Sink sink = factory.apply(config);
            LogLevel minLevel = LogLevel.valueOf(config.getLogLevel() != null ? config.getLogLevel().toUpperCase() : "DEBUG");
            sinkWithLevels.add(new Pair<>(sink, minLevel));
        }
    }

    public void log(String level, String namespace, String content) throws IOException {
        LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
        Message message = new Message(content, logLevel, namespace, LocalDateTime.now());

        for (Pair<Sink, LogLevel> pair : sinkWithLevels) {
            Sink sink = pair.getKey();
            LogLevel minLevel = pair.getValue();
            if (logLevel.isGreaterThanOrEqual(minLevel)) {
                sink.log(message);
            }
        }
    }

    public List<String> getDatabaseLogs() throws SQLException {
        for (Pair<Sink, LogLevel> pair : sinkWithLevels) {
            if (pair.getKey() instanceof DbSink) {
                return pair.getKey().getAllLogs();
            }
        }
        throw new IllegalStateException("No DbSink configured");
    }

    public void shutdown() throws IOException {
        for (Pair<Sink, LogLevel> pair : sinkWithLevels) {
            pair.getKey().shutdown();
        }
    }
}