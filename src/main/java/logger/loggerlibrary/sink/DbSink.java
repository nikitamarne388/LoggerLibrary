package logger.loggerlibrary.sink;


import logger.loggerlibrary.entities.Message;
import logger.loggerlibrary.entities.ThreadModel;
import logger.loggerlibrary.entities.WriteMode;
import logger.loggerlibrary.entities.LogLevel;
import logger.loggerlibrary.interfaces.logger.Sink;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DbSink implements Sink {
    private final Connection connection;
    private final DateTimeFormatter formatter;
    private final LogLevel minLevel;
    private final ThreadModel threadModel;
    private final WriteMode writeMode;

    public DbSink(String dbHost, String dbPort, String formatterPattern, LogLevel minLevel,
                  ThreadModel threadModel, WriteMode writeMode) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 Driver not found in classpath. Ensure H2 dependency is in pom.xml and Maven is synced.", e);
        }

        String url = "jdbc:h2:mem:loggerdb;DB_CLOSE_DELAY=-1";  // Embedded mode
        this.connection = DriverManager.getConnection(url);
        this.formatter = DateTimeFormatter.ofPattern(formatterPattern);
        this.minLevel = minLevel;
        this.threadModel = threadModel;
        this.writeMode = writeMode;
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS logs (" +
                    "id IDENTITY, " +
                    "timestamp VARCHAR(50), " +
                    "level VARCHAR(20), " +
                    "namespace VARCHAR(100), " +
                    "message VARCHAR(1000))");
        }
    }

    @Override
    public void log(Message message) throws IOException {
        try {
            String sql = "INSERT INTO logs (timestamp, level, namespace, message) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, message.getTimestamp().format(formatter));
                pstmt.setString(2, message.getLevel().toString());
                pstmt.setString(3, message.getNamespace());
                pstmt.setString(4, message.getContent());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to log to database", e);
        }
    }

    // New method to select all logs
    public List<String> getAllLogs() throws SQLException {
        List<String> logs = new ArrayList<>();
        String sql = "SELECT * FROM logs ORDER BY id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String logEntry = String.format("ID: %d, Time: %s, Level: %s, Namespace: %s, Message: %s",
                        rs.getLong("id"),
                        rs.getString("timestamp"),
                        rs.getString("level"),
                        rs.getString("namespace"),
                        rs.getString("message"));
                logs.add(logEntry);
            }
        }
        return logs;
    }

    public void shutdown() throws IOException {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new IOException("Failed to close database connection", e);
        }
    }
}