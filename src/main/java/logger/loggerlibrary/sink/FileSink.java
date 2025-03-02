package logger.loggerlibrary.sink;

import logger.loggerlibrary.entities.LogLevel;
import logger.loggerlibrary.entities.Message;
import logger.loggerlibrary.entities.ThreadModel;
import logger.loggerlibrary.entities.WriteMode;
import logger.loggerlibrary.interfaces.logger.Sink;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPOutputStream;

public class FileSink implements Sink {

    private static final long MAX_FILE_SIZE = 512; // 512 bytes for testing
    private final String baseFilePath;
    private final DateTimeFormatter dateTimeFormatter;
    private final LogLevel minLevel;
    private final ThreadModel threadModel;
    private final WriteMode writeMode;
    private FileOutputStream fileOutputStream;
    private long currentFileSize;
    private final BlockingQueue<Message> messageQueue;
    private volatile boolean running;
    private Thread writeThread;

    public FileSink(String baseFilePath, String dateTimeFormatter, LogLevel minLevel,
                    ThreadModel threadModel, WriteMode writeMode) throws IOException {
        this.baseFilePath = baseFilePath;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimeFormatter);
        this.minLevel = minLevel;
        this.threadModel = threadModel;
        this.writeMode = writeMode;
        this.messageQueue = new LinkedBlockingQueue<>(); // Unbounded queue
        this.currentFileSize = 0;
        initializeFile();
        if (writeMode == WriteMode.ASYNC) {
            startWriterThread();
        }
    }

    private void initializeFile() throws IOException {
        File file = new File(baseFilePath);
        file.getParentFile().mkdirs();
        this.fileOutputStream = new FileOutputStream(file, true);
        this.currentFileSize = file.length();
    }

    @Override
    public void log(Message message) throws IOException {
        if (message.getLevel().ordinal() < minLevel.ordinal()) {
            return; // Filter out messages below minLevel
        }
        if (writeMode == WriteMode.SYNC) {
            if (threadModel == ThreadModel.SINGLE) {
                synchronized (this) {
                    writeMessageSync(message);
                }
            } else {
                writeMessageSync(message);
            }
        } else {
            if (!messageQueue.offer(message)) {
                throw new IOException("Failed to queue message: queue is full");
            }
        }
    }

    private void startWriterThread() {
        running = true;
        writeThread = new Thread(() -> {
            while (running || !messageQueue.isEmpty()) {
                try {
                    Message message = messageQueue.take(); // Blocks until a message is available
                    synchronized (this) {
                        writeMessageSync(message);
                    }
                } catch (InterruptedException e) {
                    // Handle interruption gracefully, continue draining queue
                    Thread.currentThread().interrupt();
                    // Drain remaining messages on interrupt
                    while (!messageQueue.isEmpty()) {
                        try {
                            Message msg = messageQueue.poll();
                            if (msg != null) {
                                synchronized (this) {
                                    writeMessageSync(msg);
                                }
                            }
                        } catch (IOException ex) {
                            System.err.println("Error writing queued message during interrupt: " + ex.getMessage());
                        }
                    }
                    break;
                } catch (IOException e) {
                    System.err.println("Error writing message in async thread: " + e.getMessage());
                    // Continue processing remaining messages instead of breaking
                }
            }
        });
        writeThread.start();
    }

    private void writeMessageSync(Message message) throws IOException {
        if (currentFileSize >= MAX_FILE_SIZE) {
            rotateLogs();
        }
        String formattedMessage = formatMessage(message);
        byte[] messageInBytes = formattedMessage.getBytes("UTF-8");
        fileOutputStream.write(messageInBytes);
        fileOutputStream.flush();
        currentFileSize += messageInBytes.length;
    }

    private String formatMessage(Message message) {
        String timeStampFormatted = dateTimeFormatter.format(message.getTimestamp());
        return String.format("%s [%s] %s%n", message.getLevel(), timeStampFormatted, message.getContent());
    }

    private void rotateLogs() throws IOException {
        closeCurrentFile();
        renameAndCompressFiles();
        initializeFile();
        currentFileSize = 0;
    }

    private void renameAndCompressFiles() throws IOException {
        Path currentFilePath = Paths.get(baseFilePath);
        if (!Files.exists(currentFilePath)) {
            return;
        }

        int rotationNumber = 1;
        Path compressedPath;
        do {
            compressedPath = Paths.get(baseFilePath + "." + rotationNumber + ".gz");
            rotationNumber++;
        } while (Files.exists(compressedPath));

        try (FileInputStream fis = new FileInputStream(currentFilePath.toFile());
             FileOutputStream fos = new FileOutputStream(compressedPath.toFile());
             GZIPOutputStream gos = new GZIPOutputStream(fos)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gos.write(buffer, 0, len);
            }
        }
        Files.deleteIfExists(currentFilePath);
    }

    public void shutdown() throws IOException {
        if (writeMode == WriteMode.ASYNC && writeThread != null) {
            running = false; // Signal thread to stop accepting new messages
            // Wait for queue to drain before interrupting
            while (!messageQueue.isEmpty()) {
                try {
                    Thread.sleep(100); // Wait briefly to allow processing
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            writeThread.interrupt();
            try {
                writeThread.join(); // Wait for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while shutting down writer thread", e);
            }
        }
        closeCurrentFile();
    }

    private void closeCurrentFile() throws IOException {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
            } finally {
                fileOutputStream = null;
            }
        }
    }
}