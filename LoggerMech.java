package SchoolManager;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class LoggerMech implements Logger {
    private static final String DEFAULT_LOGS_FILE = "application_logs.txt";
    private static final int MAX_LOG_ENTRIES = 10000;
    private static final int DEFAULT_MAX_ENTRIES = 1000;
    
    private final String logsFile;
    private final Crypter crypter;
    private final String source;
    private final List<Log> logs;
    private final ReentrantLock lock;
    private Log.LogLevel minimumLogLevel;
    private final int maxEntries;
    private volatile boolean autoSave;
    
    public LoggerMech(Crypter crypter) {
        this(crypter, "Application", DEFAULT_LOGS_FILE, DEFAULT_MAX_ENTRIES);
    }
    
    public LoggerMech(Crypter crypter, String source) {
        this(crypter, source, DEFAULT_LOGS_FILE, DEFAULT_MAX_ENTRIES);
    }
    
    public LoggerMech(Crypter crypter, String source, String logsFile, int maxEntries) {
        this.crypter = Objects.requireNonNull(crypter, "Crypter cannot be null");
        this.source = Objects.requireNonNullElse(source, "Unknown");
        this.logsFile = Objects.requireNonNullElse(logsFile, DEFAULT_LOGS_FILE);
        this.maxEntries = Math.min(maxEntries, MAX_LOG_ENTRIES);
        this.logs = new CopyOnWriteArrayList<>();
        this.lock = new ReentrantLock();
        this.minimumLogLevel = Log.LogLevel.INFO;
        this.autoSave = true;
        
        loadLogs();
        info("Logger initialized for source: " + this.source);
    }
    
    // ==================== BASIC LOGGING METHODS ====================
    
    @Override
    public void trace(String message) {
        log(Log.LogLevel.TRACE, message, null);
    }
    
    @Override
    public void debug(String message) {
        log(Log.LogLevel.DEBUG, message, null);
    }
    
    @Override
    public void info(String message) {
        log(Log.LogLevel.INFO, message, null);
    }
    
    @Override
    public void warn(String message) {
        log(Log.LogLevel.WARN, message, null);
    }
    
    @Override
    public void error(String message) {
        log(Log.LogLevel.ERROR, message, null);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        log(Log.LogLevel.ERROR, message, throwable);
    }
    
    @Override
    public void fatal(String message, Throwable throwable) {
        log(Log.LogLevel.FATAL, message, throwable);
    }
    
    // ==================== CONTEXTUAL LOGGING ====================
    
    @Override
    public void log(Log.LogLevel level, String message) {
        log(level, message, null);
    }
    
    @Override
    public void log(Log.LogLevel level, String message, Throwable throwable) {
        if (level.getSeverity() < minimumLogLevel.getSeverity()) {
            return; // Skip logs below minimum level
        }
        
        Log logEntry = new Log.Builder()
            .withMessage(message)
            .withLevel(level)
            .withSource(source)
            .withThrowable(throwable)
            .build();
        
        addLog(logEntry);
    }
    
    @Override
    public void log(Log log) {
        if (log.getLevel().getSeverity() >= minimumLogLevel.getSeverity()) {
            addLog(log);
        }
    }
    
    // ==================== LOG RETRIEVAL ====================
    
    @Override
    public List<Log> getLogs() {
        return Collections.unmodifiableList(new ArrayList<>(logs));
    }
    
    @Override
    public List<Log> getLogs(Log.LogLevel minimumLevel) {
        return logs.stream()
                .filter(log -> log.getLevel().getSeverity() >= minimumLevel.getSeverity())
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Log> getLogs(String source) {
        return logs.stream()
                .filter(log -> log.getSource().equals(source))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Log> getLogsSince(LocalDateTime since) {
        return logs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .collect(Collectors.toList());
    }
    
    // ==================== LOG MANAGEMENT ====================
    
    @Override
    public void clearLogs() {
        lock.lock();
        try {
            logs.clear();
            if (autoSave) {
                saveLogs();
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public int getLogCount() {
        return logs.size();
    }
    
    @Override
    public boolean isEmpty() {
        return logs.isEmpty();
    }
    
    // ==================== FILTERING AND SEARCH ====================
    
    @Override
    public List<Log> searchLogs(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String term = searchTerm.toLowerCase();
        return logs.stream()
                .filter(log -> log.getMessage().toLowerCase().contains(term) ||
                              log.getSource().toLowerCase().contains(term) ||
                              log.getLevel().getDisplayName().toLowerCase().contains(term))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Log> getErrors() {
        return getLogs(Log.LogLevel.ERROR);
    }
    
    @Override
    public List<Log> getWarnings() {
        return logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.WARN)
                .collect(Collectors.toList());
    }
    
    // ==================== STATISTICS AND REPORTING ====================
    
    @Override
    public Map<Log.LogLevel, Long> getLogStatistics() {
        return logs.stream()
                .collect(Collectors.groupingBy(Log::getLevel, Collectors.counting()));
    }
    
    @Override
    public String generateReport() {
        return generateReport(minimumLogLevel);
    }
    
    @Override
    public String generateReport(Log.LogLevel minimumLevel) {
        StringBuilder report = new StringBuilder();
        Map<Log.LogLevel, Long> stats = getLogStatistics();
        
        report.append("=== LOGGING REPORT ===\n");
        report.append("Source: ").append(source).append("\n");
        report.append("Total Logs: ").append(getLogCount()).append("\n");
        report.append("Time Range: ");
        
        if (!logs.isEmpty()) {
            report.append(logs.get(0).getFormattedTimestamp())
                  .append(" to ")
                  .append(logs.get(logs.size() - 1).getFormattedTimestamp());
        }
        report.append("\n\n");
        
        report.append("Log Level Statistics:\n");
        stats.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(Log.LogLevel::getSeverity)))
                .forEach(entry -> report.append(String.format("  %-7s: %d\n", 
                    entry.getKey().getDisplayName(), entry.getValue())));
        
        report.append("\nRecent Logs (last 10):\n");
        logs.stream()
                .skip(Math.max(0, logs.size() - 10))
                .forEach(log -> report.append("  ").append(log.toString()).append("\n"));
        
        return report.toString();
    }
    
    // ==================== CONFIGURATION ====================
    
    @Override
    public void setMinimumLogLevel(Log.LogLevel level) {
        this.minimumLogLevel = Objects.requireNonNull(level);
    }
    
    @Override
    public Log.LogLevel getMinimumLogLevel() {
        return minimumLogLevel;
    }
    
    @Override
    public void setSource(String source) {
        // Source is final in this implementation, but interface allows setting
        // For immutability, we'll log the attempt but not change the source
        warn("Attempt to change logger source from " + this.source + " to " + source + " - Source is immutable");
    }
    
    @Override
    public String getSource() {
        return source;
    }
    
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
    }
    
    public boolean isAutoSave() {
        return autoSave;
    }
    
    // ==================== LIFECYCLE MANAGEMENT ====================
    
    @Override
    public void flush() {
        saveLogs();
    }
    
    @Override
    public void close() {
        flush();
        info("Logger shutting down for source: " + source);
    }
    
    // ==================== PRIVATE METHODS ====================
    
    private void addLog(Log log) {
        lock.lock();
        try {
            // Implement log rotation if max entries reached
            if (logs.size() >= maxEntries) {
                logs.remove(0); // Remove oldest log
            }
            
            logs.add(log);
            System.out.println(log.toString()); // Console output
            
            if (autoSave) {
                saveLogs();
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void loadLogs() {
        File file = new File(logsFile);
        if (!file.exists()) {
            return;
        }
        
        lock.lock();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            
            while ((line = reader.readLine()) != null && loadedCount < maxEntries) {
                if (!line.trim().isEmpty()) {
                    try {
                        String decryptedData = crypter.decodeDecrypt(line.trim());
                        Log log = Log.fromCsv(decryptedData);
                        logs.add(log);
                        loadedCount++;
                    } catch (Exception e) {
                        System.err.println("Failed to parse log entry: " + e.getMessage());
                    }
                }
            }
            
            if (loadedCount > 0) {
                info("Loaded " + loadedCount + " log entries from storage");
            }
        } catch (IOException e) {
            error("Failed to load logs from file: " + logsFile, e);
        } finally {
            lock.unlock();
        }
    }
    
    private void saveLogs() {
        lock.lock();
        try (PrintWriter writer = new PrintWriter(new FileWriter(logsFile, false))) {
            for (Log log : logs) {
                try {
                    String csvData = log.toCsv();
                    String encryptedData = crypter.encryptEncode(csvData);
                    writer.println(encryptedData);
                } catch (Exception e) {
                    System.err.println("Failed to encrypt log entry: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to save logs to file: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }
}

/*

### **4. Usage Examples**

```java
// Basic usage
Logger logger = new LoggerMech(crypter, "StudentRepository");
logger.info("Student record created successfully");
logger.error("Failed to save student data", exception);

// Advanced filtering
List<Log> errors = logger.getErrors();
List<Log> recentLogs = logger.getLogsSince(LocalDateTime.now().minusHours(1));

// Generate reports
String report = logger.generateReport();
System.out.println(report);

// Search functionality
List<Log> searchResults = logger.searchLogs("student");
```

*/



