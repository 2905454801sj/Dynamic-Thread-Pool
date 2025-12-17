package org.jason.btrace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BTrace Manager
 * 
 * Manages the lifecycle of BTrace monitoring scripts including starting, stopping,
 * and tracking active trace processes. BTrace allows dynamic tracing of running
 * Java applications without restart.
 * 
 * Thread-Safety: Uses ConcurrentHashMap for thread-safe process tracking
 */
@Component
public class BTraceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(BTraceManager.class);
    
    /** Directory containing BTrace script files (.java) */
    private static final String BTRACE_SCRIPTS_DIR = "src/main/btrace";
    
    /** Directory for BTrace output logs */
    private static final String BTRACE_LOGS_DIR = "logs/btrace";
    
    /** 
     * Active BTrace processes mapped by script name
     * Thread-safe: ConcurrentHashMap allows concurrent access from multiple threads
     */
    private final Map<String, Process> activeTraces = new ConcurrentHashMap<>();
    
    /**
     * Start a BTrace monitoring script
     * 
     * Attaches the specified BTrace script to the current JVM process for dynamic tracing.
     * The script will run until explicitly stopped or the JVM terminates.
     * 
     * @param scriptName Name of the BTrace script (without .java extension)
     * @return true if script started successfully, false if already running
     * @throws IOException if script file not found or process creation fails
     */
    public boolean startTrace(String scriptName) throws IOException {
        // Prevent duplicate script execution
        if (activeTraces.containsKey(scriptName)) {
            logger.warn("BTrace script already running: {}", scriptName);
            return false;
        }
        
        // Get current JVM process ID for BTrace attachment
        String pid = getPid();
        String scriptPath = BTRACE_SCRIPTS_DIR + "/" + scriptName + ".java";
        
        // Validate script file exists before attempting to start
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            logger.error("BTrace script not found: {}", scriptPath);
            throw new IOException("Script file not found: " + scriptPath);
        }
        
        // Create log directory if it doesn't exist
        File logsDir = new File(BTRACE_LOGS_DIR);
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        // Build BTrace command line
        // Prerequisites: BTrace must be installed and available in system PATH
        List<String> command = new ArrayList<>();
        
        // Use platform-specific BTrace executable
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            command.add("btrace.bat");  // Windows batch script
        } else {
            command.add("btrace");      // Unix shell script
        }
        
        command.add(pid);
        command.add(scriptPath);
        
        try {
            // Configure process to redirect output to log files
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(new File(BTRACE_LOGS_DIR + "/" + scriptName + ".log"));
            pb.redirectError(new File(BTRACE_LOGS_DIR + "/" + scriptName + "-error.log"));
            
            // Start BTrace process and track it
            Process process = pb.start();
            activeTraces.put(scriptName, process);
            
            logger.info("BTrace started: {} (PID: {})", scriptName, pid);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to start BTrace: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Stop a running BTrace monitoring script
     * 
     * Attempts graceful shutdown first, then forces termination if needed.
     * Waits up to 5 seconds for graceful shutdown before forcing.
     * 
     * @param scriptName Name of the script to stop
     * @return true if stopped successfully, false if not running
     */
    public boolean stopTrace(String scriptName) {
        Process process = activeTraces.get(scriptName);
        if (process == null) {
            logger.warn("BTrace script not running: {}", scriptName);
            return false;
        }
        
        try {
            // Request graceful shutdown
            process.destroy();
            
            // Wait up to 5 seconds for process to exit
            boolean exited = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!exited) {
                // Force termination if graceful shutdown failed
                process.destroyForcibly();
            }
            
            // Remove from active traces map
            activeTraces.remove(scriptName);
            logger.info("BTrace stopped: {}", scriptName);
            return true;
            
        } catch (InterruptedException e) {
            logger.warn("Interrupted while stopping BTrace: {}", scriptName);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Stop all running BTrace monitoring scripts
     * 
     * Iterates through all active traces and stops them one by one.
     * Creates a copy of keySet to avoid ConcurrentModificationException.
     */
    public void stopAllTraces() {
        logger.info("Stopping all BTrace processes...");
        
        // Create copy of keys to avoid ConcurrentModificationException during iteration
        for (String scriptName : new ArrayList<>(activeTraces.keySet())) {
            stopTrace(scriptName);
        }
        
        logger.info("All BTrace processes stopped");
    }
    
    /**
     * Get list of currently running BTrace scripts
     * 
     * Returns a snapshot of active script names at the time of call.
     * 
     * @return List of active script names (without .java extension)
     */
    public List<String> getActiveTraces() {
        return new ArrayList<>(activeTraces.keySet());
    }
    
    /**
     * Check if a specific BTrace script is currently running
     * 
     * Verifies both that the process exists in our map and that it's still alive.
     * 
     * @param scriptName Name of the script to check
     * @return true if script is running, false otherwise
     */
    public boolean isTraceActive(String scriptName) {
        Process process = activeTraces.get(scriptName);
        return process != null && process.isAlive();
    }
    
    /**
     * Get list of available BTrace scripts in the scripts directory
     * 
     * Scans the BTrace scripts directory for .java files and returns their names
     * without the .java extension.
     * 
     * @return List of available script names (without .java extension)
     */
    public List<String> getAvailableScripts() {
        List<String> scripts = new ArrayList<>();
        File scriptsDir = new File(BTRACE_SCRIPTS_DIR);
        
        if (scriptsDir.exists() && scriptsDir.isDirectory()) {
            // Filter for .java files only
            File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".java"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    // Remove .java extension (5 characters)
                    scripts.add(name.substring(0, name.length() - 5));
                }
            }
        }
        
        return scripts;
    }
    
    /**
     * Get the log file path for a BTrace script
     * 
     * @param scriptName Name of the script
     * @return Full path to the script's log file
     */
    public String getLogFilePath(String scriptName) {
        return BTRACE_LOGS_DIR + "/" + scriptName + ".log";
    }
    
    /**
     * Read recent log entries from a BTrace script's log file
     * 
     * Implements a simple tail-like functionality by reading the entire file
     * and keeping only the last N lines in memory.
     * 
     * @param scriptName Name of the script
     * @param lines Number of recent lines to read
     * @return Log content (last N lines) or error message if file not found
     * @throws IOException if error reading the file
     */
    public String readRecentLogs(String scriptName, int lines) throws IOException {
        String logPath = getLogFilePath(scriptName);
        File logFile = new File(logPath);
        
        if (!logFile.exists()) {
            return "Log file not found: " + logPath;
        }
        
        // Simplified tail implementation: read all lines, keep last N
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new java.io.FileReader(logFile))) {
            
            // Circular buffer to keep only last N lines
            List<String> recentLines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                recentLines.add(line);
                if (recentLines.size() > lines) {
                    recentLines.remove(0);  // Remove oldest line
                }
            }
            
            // Build result string
            for (String recentLine : recentLines) {
                result.append(recentLine).append("\n");
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get the current JVM process ID
     * 
     * Extracts PID from the JVM name which is in format "pid@hostname".
     * 
     * @return Process ID as string
     */
    private String getPid() {
        // JVM name format: "12345@hostname"
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        return jvmName.split("@")[0];  // Extract PID before '@'
    }
    
    /**
     * Check if BTrace is installed and available in system PATH
     * 
     * Attempts to execute "btrace -version" command to verify installation.
     * 
     * @return true if BTrace is installed and accessible, false otherwise
     */
    public boolean isBTraceInstalled() {
        try {
            // Determine platform-specific command
            String os = System.getProperty("os.name").toLowerCase();
            String command = os.contains("win") ? "btrace.bat" : "btrace";
            
            // Try to execute version command
            ProcessBuilder pb = new ProcessBuilder(command, "-version");
            Process process = pb.start();
            
            // Exit code 0 indicates successful execution
            int exitCode = process.waitFor();
            return exitCode == 0;
            
        } catch (Exception e) {
            logger.warn("BTrace not installed or not in PATH: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get comprehensive status information about BTrace manager
     * 
     * Returns formatted status including installation status, current PID,
     * number of active traces, and list of running scripts.
     * 
     * @return Multi-line status report string
     */
    public String getStatus() {
        StringBuilder status = new StringBuilder();
        status.append("BTrace Manager Status:\n");
        status.append("  BTrace Installed: ").append(isBTraceInstalled()).append("\n");
        status.append("  Current PID: ").append(getPid()).append("\n");
        status.append("  Active Traces: ").append(activeTraces.size()).append("\n");
        
        // List running scripts if any
        if (!activeTraces.isEmpty()) {
            status.append("  Running Scripts:\n");
            for (String scriptName : activeTraces.keySet()) {
                status.append("    - ").append(scriptName).append("\n");
            }
        }
        
        return status.toString();
    }
}
