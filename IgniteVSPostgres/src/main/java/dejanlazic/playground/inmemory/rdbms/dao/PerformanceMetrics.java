package dejanlazic.playground.inmemory.rdbms.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performance metrics tracker for DAO operations
 * Logs detailed performance statistics to entity-specific log files
 */
public class PerformanceMetrics {
    
    private static final Logger LOGGER = Logger.getLogger(PerformanceMetrics.class.getName());
    private static final String LOG_DIR = "logs/performance";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final String entityName;
    private final String operation;
    private final long startTime;
    private long endTime;
    private int recordCount;
    private long recordSizeBytes;
    
    /**
     * Create a new performance metrics tracker
     * @param entityName Name of the entity (e.g., "BenefitPlan", "Member")
     * @param operation Operation being performed (e.g., "INSERT", "SELECT", "UPDATE")
     */
    public PerformanceMetrics(String entityName, String operation) {
        this.entityName = entityName;
        this.operation = operation;
        this.startTime = System.nanoTime();
        this.recordCount = 0;
        this.recordSizeBytes = 0;
    }
    
    /**
     * Set the number of records processed
     */
    public void setRecordCount(int count) {
        this.recordCount = count;
    }
    
    /**
     * Set the total size of records in bytes
     */
    public void setRecordSizeBytes(long sizeBytes) {
        this.recordSizeBytes = sizeBytes;
    }
    
    /**
     * Complete the metrics collection and log results
     */
    public void complete() {
        this.endTime = System.nanoTime();
        logMetrics();
    }
    
    /**
     * Calculate total time in milliseconds
     */
    private double getTotalTimeMs() {
        return (endTime - startTime) / 1_000_000.0;
    }
    
    /**
     * Calculate time per record in milliseconds
     */
    private double getTimePerRecordMs() {
        if (recordCount == 0) return 0.0;
        return getTotalTimeMs() / recordCount;
    }
    
    /**
     * Calculate throughput in records per second
     */
    private double getRecordsPerSecond() {
        double totalTimeSec = getTotalTimeMs() / 1000.0;
        if (totalTimeSec == 0) return 0.0;
        return recordCount / totalTimeSec;
    }
    
    /**
     * Calculate time per KB in milliseconds
     */
    private double getTimePerKB() {
        if (recordSizeBytes == 0) return 0.0;
        double sizeKB = recordSizeBytes / 1024.0;
        return getTotalTimeMs() / sizeKB;
    }
    
    /**
     * Calculate throughput in MB per second
     */
    private double getMBPerSecond() {
        double totalTimeSec = getTotalTimeMs() / 1000.0;
        if (totalTimeSec == 0) return 0.0;
        double sizeMB = recordSizeBytes / (1024.0 * 1024.0);
        return sizeMB / totalTimeSec;
    }
    
    /**
     * Format size in human-readable format
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Log metrics to entity-specific log file in pipe-delimited CSV format
     */
    private void logMetrics() {
        try {
            // Ensure log directory exists
            Path logDirPath = Paths.get(LOG_DIR);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }
            
            // Create log file path
            String logFileName = String.format("%s_performance.log", entityName.toLowerCase());
            Path logFilePath = logDirPath.resolve(logFileName);
            
            // Write header if file doesn't exist
            if (!Files.exists(logFilePath)) {
                String header = "Timestamp|Entity|Operation|Total_Time_Ms|Record_Count|Time_Per_Record_Ms|Records_Per_Sec|Total_Size_Bytes|Time_Per_KB_Ms|MB_Per_Sec|Avg_Record_Size_Bytes\n";
                Files.writeString(logFilePath, header,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
            }
            
            // Build CSV record (pipe-delimited)
            StringBuilder csvRecord = new StringBuilder();
            csvRecord.append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("|");
            csvRecord.append(entityName).append("|");
            csvRecord.append(operation).append("|");
            csvRecord.append(String.format("%.3f", getTotalTimeMs())).append("|");
            csvRecord.append(recordCount).append("|");
            csvRecord.append(recordCount > 0 ? String.format("%.6f", getTimePerRecordMs()) : "0").append("|");
            csvRecord.append(recordCount > 0 ? String.format("%.2f", getRecordsPerSecond()) : "0").append("|");
            csvRecord.append(recordSizeBytes).append("|");
            csvRecord.append(recordSizeBytes > 0 ? String.format("%.6f", getTimePerKB()) : "0").append("|");
            csvRecord.append(recordSizeBytes > 0 ? String.format("%.2f", getMBPerSecond()) : "0").append("|");
            csvRecord.append(recordCount > 0 && recordSizeBytes > 0 ? (recordSizeBytes / recordCount) : "0");
            csvRecord.append("\n");
            
            // Write to log file
            Files.writeString(logFilePath, csvRecord.toString(),
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
            
            // Also log to console for immediate feedback
            LOGGER.log(Level.INFO, "Performance: {0} {1} - {2} records in {3} ms ({4} records/sec)",
                new Object[]{entityName, operation, recordCount,
                    String.format("%.3f", getTotalTimeMs()),
                    String.format("%.2f", getRecordsPerSecond())});
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write performance metrics to log file", e);
        }
    }
    
    /**
     * Estimate size of a string in bytes (UTF-8)
     */
    public static long estimateStringSize(String str) {
        if (str == null) return 0;
        // Rough estimate: most characters are 1-2 bytes in UTF-8
        return str.length() * 2L;
    }
}
