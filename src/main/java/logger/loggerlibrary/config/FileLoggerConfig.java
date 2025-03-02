package logger.loggerlibrary.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileLoggerConfig extends LoggerConfig {
    @JsonProperty("file_location")
    private String fileLocation;

    public String getFileLocation() { return fileLocation; }
    public void setFileLocation(String fileLocation) { this.fileLocation = fileLocation; }

    @Override
    public String getSinkType() { return "FILE"; }
}