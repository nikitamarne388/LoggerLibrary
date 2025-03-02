package logger.loggerlibrary.interfaces.logger;


import logger.loggerlibrary.entities.Message;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface Sink {
    void log(Message message) throws IOException;
    void shutdown() throws IOException;
    default List<String> getAllLogs() throws SQLException {
        throw new UnsupportedOperationException("getAllLogs not supported by this sink");
    }
}