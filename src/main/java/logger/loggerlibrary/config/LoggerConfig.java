package logger.loggerlibrary.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "sink_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FileLoggerConfig.class, name = "FILE"),
        @JsonSubTypes.Type(value = DbLoggerConfig.class, name = "DATABASE")
})
public abstract class LoggerConfig {
    @JsonProperty("ts_format")
    private String tsFormat;
    @JsonProperty("log_level")
    private String logLevel;
    @JsonProperty("thread_model")
    private String threadModel;
    @JsonProperty("write_mode")
    private String writeMode;

    public String getTsFormat() { return tsFormat; }
    public void setTsFormat(String tsFormat) { this.tsFormat = tsFormat; }
    public String getLogLevel() { return logLevel; }
    public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
    public String getThreadModel() { return threadModel; }
    public void setThreadModel(String threadModel) { this.threadModel = threadModel; }
    public String getWriteMode() { return writeMode; }
    public void setWriteMode(String writeMode) { this.writeMode = writeMode; }

    public abstract String getSinkType();
}