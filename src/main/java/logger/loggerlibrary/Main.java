package logger.loggerlibrary;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import logger.loggerlibrary.config.ConfigWrapper;
import logger.loggerlibrary.config.DbLoggerConfig;
import logger.loggerlibrary.config.FileLoggerConfig;
import logger.loggerlibrary.config.LoggerConfig;
import logger.loggerlibrary.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.*;


public class Main {
    public static void main(String[] args) throws IOException {
        Logger logger = new Logger();

        String yamlConfigPath = "src/main/resources/config.yml";
        ConfigWrapper configWrapper = loadConfiguration(yamlConfigPath);

        if (configWrapper != null && configWrapper.getSinks() != null) {
            List<LoggerConfig> sinkConfigurations = configWrapper.getSinks();

            if (sinkConfigurations.isEmpty()) {
                err.println("No sink configurations found in config.yml");
                return;
            }

            // Log where messages 1will be sent
            for (LoggerConfig config : sinkConfigurations) {
                if (config instanceof FileLoggerConfig) {
                    out.println("Logging Messages to File at location: " + ((FileLoggerConfig) config).getFileLocation());
                } else if (config instanceof DbLoggerConfig) {
                    out.println("Logging Messages to Database at: " +
                            ((DbLoggerConfig) config).getDbHost() + ":" + ((DbLoggerConfig) config).getDbPort());
                }
            }

            // Configure logger with all sinks
            logger.configure(sinkConfigurations);

            // Prompt user for logging mode
            Scanner scanner = new Scanner(System.in);
            out.println("Select logging mode:");
            out.println("1. Hardcoded messages");
            out.println("2. Console input");
            out.print("Enter your choice (1 or 2): ");

            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                // Option 1: Hardcoded log messages
                out.println("Logging hardcoded messages:");
                logger.log("INFO", "app.core", "Application started");
                logger.log("WARN", "app.customerService", "Incorrect customer id");
                logger.log("INFO", "app.userService", "Empty txnIds Nothing to fetch");
                logger.log("WARN", "app.userService", "No user found for the phone number");
                logger.log("ERROR", "app.userService", "ERROR LEVEL LOG");
                logger.log("FATAL", "app.userService", "FATAL LEVEL LOG");
                logger.log("DEBUG", "app.userService", "DEBUG LEVEL LOG");
                out.println("Hardcoded messages logged.");1
            } else if ("2".equals(choice)) {
                // Option 2: Console input
                out.println("\nEnter log lines in format: <LEVEL> <NAMESPACE> <CONTENT>");
                out.println("Example: INFO app.core Application started");
                out.println("Type 'exit' to stop.");

                while (true) {
                    String input = scanner.nextLine().trim();
                    if ("exit".equalsIgnoreCase(input)) {
                        break;
                    }

                    try {
                        String[] parts = input.split("\\s+", 3); // Split into 3 parts: level, namespace, content
                        if (parts.length != 3) {
                            err.println("Invalid input format. Use: <LEVEL> <NAMESPACE> <CONTENT>");
                            continue;
                        }

                        String level = parts[0].toUpperCase();
                        String namespace = parts[1];
                        String content = parts[2];

                        try {
                            logger.log(level, namespace, content);
                            out.println("Logged: " + input);
                        } catch (IllegalArgumentException e) {
                            err.println("Invalid log level: " + level + ". Use: DEBUG, INFO, WARN, ERROR, FATAL");
                        }
                    } catch (Exception e) {
                        err.println("Error processing input: " + e.getMessage());
                    }
                }
            } else {
                err.println("Invalid choice. Please enter 1 or 2.");
            }

            // Display database logs if a DATABASE sink is configured
            try {
                List<String> dbLogs = logger.getDatabaseLogs();
                out.println("Database Logs:");
                dbLogs.forEach(out::println);
            } catch (SQLException e) {
                err.println("No database sink configured or error retrieving logs: " + e.getMessage());
            }

            logger.shutdown();
            out.println("Finished Logging..");
        } else {
            err.println("Error while creating the configuration");
        }
    }

    public static ConfigWrapper loadConfiguration(String filePath) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(new File(filePath), ConfigWrapper.class);
        } catch (IOException ex) {
            err.println("ConfigReader:: Error while loading the configuration: " + ex.getMessage());
            return null;
        }
    }
}