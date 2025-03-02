package logger.loggerlibrary.entities;

import java.time.LocalDateTime;

public class Message {

    private final String content;
    private final LogLevel level;
    private final String namespace;
    private final LocalDateTime timestamp;

    //Initialization of the new Message object
    public Message(String content, LogLevel level, String namespace, LocalDateTime timeStamp){
        this.content = content;
        this.level = level;
        this.namespace = namespace;
        this.timestamp = timeStamp;
    }

    //toString method
    @Override
    public String toString() {
        return String.format("[%s] [%s] %s - %s", timestamp, level, namespace, content);
    }

    //getters
    public String getContent() {
        return content;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getNamespace() {
        return namespace;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
