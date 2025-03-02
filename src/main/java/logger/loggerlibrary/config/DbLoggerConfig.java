package logger.loggerlibrary.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DbLoggerConfig extends LoggerConfig {
    @JsonProperty("db_host")
    private String dbHost;
    @JsonProperty("db_port")
    private String dbPort;

    public String getDbHost() { return dbHost; }
    public void setDbHost(String dbHost) { this.dbHost = dbHost; }
    public String getDbPort() { return dbPort; }
    public void setDbPort(String dbPort) { this.dbPort = dbPort; }

    @Override
    public String getSinkType() { return "DATABASE"; }
}