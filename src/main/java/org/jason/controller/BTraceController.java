package org.jason.controller;

import org.jason.btrace.BTraceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BTrace Monitoring Management Controller
 * 
 * Provides REST API endpoints for managing BTrace monitoring scripts.
 * Supports starting, stopping, querying, and viewing logs of BTrace traces.
 * 
 * Base Path: /api/btrace
 * CORS: Enabled for all origins
 */
@RestController
@RequestMapping("/api/btrace")
@CrossOrigin(origins = "*")
public class BTraceController {
    
    @Autowired
    private BTraceManager btraceManager;
    
    /**
     * Start a BTrace monitoring script
     * 
     * Endpoint: POST /api/btrace/start/{scriptName}
     * 
     * Starts the specified BTrace script and attaches it to the current JVM.
     * Returns the log file path if successful.
     * 
     * @param scriptName Name of the BTrace script (without .java extension)
     * @return Response with success status, message, and log file path
     *         - 200 OK: Script started or already running
     *         - 500 Internal Server Error: Failed to start script
     */
    @PostMapping("/start/{scriptName}")
    public ResponseEntity<Map<String, Object>> startTrace(@PathVariable String scriptName) {
        try {
            boolean success = btraceManager.startTrace(scriptName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            
            if (success) {
                response.put("message", "BTrace script started successfully: " + scriptName);
                response.put("logFile", btraceManager.getLogFilePath(scriptName));
            } else {
                response.put("message", "BTrace script is already running: " + scriptName);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            // Return error response with exception details
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to start BTrace: " + e.getMessage());
            error.put("error", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Stop a running BTrace monitoring script
     * 
     * Endpoint: POST /api/btrace/stop/{scriptName}
     * 
     * Stops the specified BTrace script gracefully (with 5-second timeout).
     * Forces termination if graceful shutdown fails.
     * 
     * @param scriptName Name of the BTrace script to stop
     * @return Response with success status and message
     *         - 200 OK: Script stopped or not running
     *         - 500 Internal Server Error: Failed to stop script
     */
    @PostMapping("/stop/{scriptName}")
    public ResponseEntity<Map<String, Object>> stopTrace(@PathVariable String scriptName) {
        try {
            boolean success = btraceManager.stopTrace(scriptName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            
            if (success) {
                response.put("message", "BTrace script stopped successfully: " + scriptName);
            } else {
                response.put("message", "BTrace script is not running: " + scriptName);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to stop BTrace: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Stop all running BTrace monitoring scripts
     * 
     * Endpoint: POST /api/btrace/stop-all
     * 
     * Stops all currently active BTrace scripts. Useful for cleanup
     * or when switching monitoring strategies.
     * 
     * @return Response with success status and message
     */
    @PostMapping("/stop-all")
    public ResponseEntity<Map<String, Object>> stopAllTraces() {
        try {
            btraceManager.stopAllTraces();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "All BTrace scripts stopped successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to stop all BTrace scripts: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get list of available BTrace scripts
     * 
     * Endpoint: GET /api/btrace/scripts
     * 
     * Returns all BTrace scripts found in the scripts directory,
     * along with their descriptions.
     * 
     * @return Response with script list, count, and descriptions
     */
    @GetMapping("/scripts")
    public ResponseEntity<Map<String, Object>> listAvailableScripts() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> scripts = btraceManager.getAvailableScripts();
        response.put("scripts", scripts);
        response.put("count", scripts.size());
        
        // Add script descriptions for user reference
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ThreadPoolExecutionTrace", "Monitor task execution time and status");
        descriptions.put("RejectionTrace", "Monitor task rejection events");
        descriptions.put("ParameterChangeTrace", "Monitor dynamic parameter adjustments");
        descriptions.put("AlertTrace", "Monitor alert system triggers");
        descriptions.put("PerformanceTrace", "Performance hotspot analysis and slow method detection");
        
        response.put("descriptions", descriptions);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get list of currently running BTrace scripts
     * 
     * Endpoint: GET /api/btrace/active
     * 
     * Returns a snapshot of all active BTrace monitoring scripts.
     * 
     * @return Response with active script list and count
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> listActiveTraces() {
        Map<String, Object> response = new HashMap<>();
        
        List<String> activeScripts = btraceManager.getActiveTraces();
        response.put("activeScripts", activeScripts);
        response.put("count", activeScripts.size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if a specific BTrace script is running
     * 
     * Endpoint: GET /api/btrace/status/{scriptName}
     * 
     * Returns the current status of the specified script.
     * Includes log file path if script is running.
     * 
     * @param scriptName Name of the script to check
     * @return Response with script status and log file path (if running)
     */
    @GetMapping("/status/{scriptName}")
    public ResponseEntity<Map<String, Object>> checkScriptStatus(@PathVariable String scriptName) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isActive = btraceManager.isTraceActive(scriptName);
        response.put("scriptName", scriptName);
        response.put("isActive", isActive);
        response.put("status", isActive ? "RUNNING" : "STOPPED");
        
        // Include log file path if script is running
        if (isActive) {
            response.put("logFile", btraceManager.getLogFilePath(scriptName));
        }
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get recent log entries from a BTrace script
     * 
     * Endpoint: GET /api/btrace/logs/{scriptName}?lines=100
     * 
     * Reads and returns the last N lines from the script's log file.
     * Default is 100 lines if not specified.
     * 
     * @param scriptName Name of the script
     * @param lines Number of recent lines to read (default: 100)
     * @return Response with log content and metadata
     */
    @GetMapping("/logs/{scriptName}")
    public ResponseEntity<Map<String, Object>> getScriptLogs(
            @PathVariable String scriptName,
            @RequestParam(defaultValue = "100") int lines) {
        
        try {
            String logs = btraceManager.readRecentLogs(scriptName, lines);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("scriptName", scriptName);
            response.put("lines", lines);
            response.put("logs", logs);
            response.put("logFile", btraceManager.getLogFilePath(scriptName));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to read logs: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * Get BTrace manager status
     * 
     * Endpoint: GET /api/btrace/status
     * 
     * Returns comprehensive status information including installation status,
     * active trace count, and available script count.
     * 
     * @return Response with BTrace manager status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getBTraceStatus() {
        Map<String, Object> response = new HashMap<>();
        
        response.put("status", btraceManager.getStatus());
        response.put("installed", btraceManager.isBTraceInstalled());
        response.put("activeCount", btraceManager.getActiveTraces().size());
        response.put("availableCount", btraceManager.getAvailableScripts().size());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Quick start common monitoring combinations
     * 
     * Endpoint: POST /api/btrace/quick-start/{preset}
     * 
     * Starts predefined combinations of BTrace scripts for common monitoring scenarios.
     * 
     * Available presets:
     * - basic: Task execution + Rejection monitoring
     * - alert: Alert + Parameter change monitoring
     * - performance: Performance analysis + Task execution
     * - full: All monitoring scripts
     * 
     * @param preset Name of the preset configuration
     * @return Response with list of started scripts and results
     *         - 200 OK: Scripts started successfully
     *         - 400 Bad Request: Unknown preset
     */
    @PostMapping("/quick-start/{preset}")
    public ResponseEntity<Map<String, Object>> quickStart(@PathVariable String preset) {
        Map<String, Object> response = new HashMap<>();
        List<String> scriptsToStart = new java.util.ArrayList<>();
        
        switch (preset.toLowerCase()) {
            case "basic":
                // Basic monitoring: Task execution + Rejection
                scriptsToStart.add("ThreadPoolExecutionTrace");
                scriptsToStart.add("RejectionTrace");
                break;
                
            case "alert":
                // Alert monitoring: Alert + Parameter changes
                scriptsToStart.add("AlertTrace");
                scriptsToStart.add("ParameterChangeTrace");
                break;
                
            case "performance":
                // Performance monitoring: Performance analysis + Task execution
                scriptsToStart.add("PerformanceTrace");
                scriptsToStart.add("ThreadPoolExecutionTrace");
                break;
                
            case "full":
                // Full monitoring: All scripts
                scriptsToStart.add("ThreadPoolExecutionTrace");
                scriptsToStart.add("RejectionTrace");
                scriptsToStart.add("ParameterChangeTrace");
                scriptsToStart.add("AlertTrace");
                scriptsToStart.add("PerformanceTrace");
                break;
                
            default:
                response.put("success", false);
                response.put("message", "Unknown preset: " + preset);
                response.put("availablePresets", new String[]{"basic", "alert", "performance", "full"});
                return ResponseEntity.badRequest().body(response);
        }
        
        // Track successfully started and failed scripts
        List<String> started = new java.util.ArrayList<>();
        List<String> failed = new java.util.ArrayList<>();
        
        // Attempt to start each script in the preset
        for (String script : scriptsToStart) {
            try {
                if (btraceManager.startTrace(script)) {
                    started.add(script);
                } else {
                    failed.add(script + " (already running)");
                }
            } catch (Exception e) {
                failed.add(script + " (" + e.getMessage() + ")");
            }
        }
        
        response.put("success", true);
        response.put("preset", preset);
        response.put("started", started);
        response.put("failed", failed);
        response.put("message", "Quick start completed: " + started.size() + " scripts started");
        
        return ResponseEntity.ok(response);
    }
}
