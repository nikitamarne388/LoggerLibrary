package logger.loggerlibrary.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigWrapper {
    @JsonProperty("sinks")
    private List<LoggerConfig> sinks;

    public List<LoggerConfig> getSinks() { return sinks; }
    public void setSinks(List<LoggerConfig> sinks) { this.sinks = sinks; }
}