package logger.loggerlibrary.entities;

public enum LogLevel {
    DEBUG(1), INFO(2), WARN(3), ERROR(4), FATAL(5);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public boolean isGreaterThanOrEqual(LogLevel other) {
        return this.severity >= other.severity;
    }
}
