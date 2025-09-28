package SchoolManager;

import java.util.List;
import java.util.Map;

public interface Logger {
    
    // ==================== BASIC LOGGING METHODS ====================
    
    void trace(String message);
    void debug(String message);
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable throwable);
    void fatal(String message, Throwable throwable);
    
    // ==================== CONTEXTUAL LOGGING ====================
    
    void log(Log.LogLevel level, String message);
    void log(Log.LogLevel level, String message, Throwable throwable);
    void log(Log log);
    
    // ==================== LOG RETRIEVAL ====================
    
    List<Log> getLogs();
    List<Log> getLogs(Log.LogLevel minimumLevel);
    List<Log> getLogs(String source);
    List<Log> getLogsSince(java.time.LocalDateTime since);
    
    // ==================== LOG MANAGEMENT ====================
    
    void clearLogs();
    int getLogCount();
    boolean isEmpty();
    
    // ==================== FILTERING AND SEARCH ====================
    
    List<Log> searchLogs(String searchTerm);
    List<Log> getErrors();
    List<Log> getWarnings();
    
    // ==================== STATISTICS AND REPORTING ====================
    
    Map<Log.LogLevel, Long> getLogStatistics();
    String generateReport();
    String generateReport(Log.LogLevel minimumLevel);
    
    // ==================== CONFIGURATION ====================
    
    void setMinimumLogLevel(Log.LogLevel level);
    Log.LogLevel getMinimumLogLevel();
    void setSource(String source);
    String getSource();
    
    // ==================== LIFECYCLE MANAGEMENT ====================
    
    void flush();
    void close();
}