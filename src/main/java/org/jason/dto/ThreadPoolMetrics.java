package org.jason.dto;

import java.io.Serializable;

/**
 * Thread Pool Metrics Data Transfer Object
 * 
 * Encapsulates all thread pool monitoring metrics for caching.
 * This object is cached for 1 second to reduce computation overhead
 * during high-frequency monitoring requests.
 */
public class ThreadPoolMetrics implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private int corePoolSize;
    private int maximumPoolSize;
    private int poolSize;
    private int activeCount;
    private long taskCount;
    private long completedTaskCount;
    private int queueSize;
    private int queueRemainingCapacity;
    private long rejectedExecutionCount;
    private double threadUsageRate;
    private double queueUsageRate;
    private boolean isShutdown;
    private boolean isTerminated;
    private long timestamp;
    
    // Alert configuration
    private double alertThreshold;
    private String alertStats;
    
    public ThreadPoolMetrics() {
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }
    
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }
    
    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }
    
    public int getPoolSize() {
        return poolSize;
    }
    
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
    
    public int getActiveCount() {
        return activeCount;
    }
    
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }
    
    public long getTaskCount() {
        return taskCount;
    }
    
    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }
    
    public long getCompletedTaskCount() {
        return completedTaskCount;
    }
    
    public void setCompletedTaskCount(long completedTaskCount) {
        this.completedTaskCount = completedTaskCount;
    }
    
    public int getQueueSize() {
        return queueSize;
    }
    
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
    
    public int getQueueRemainingCapacity() {
        return queueRemainingCapacity;
    }
    
    public void setQueueRemainingCapacity(int queueRemainingCapacity) {
        this.queueRemainingCapacity = queueRemainingCapacity;
    }
    
    public long getRejectedExecutionCount() {
        return rejectedExecutionCount;
    }
    
    public void setRejectedExecutionCount(long rejectedExecutionCount) {
        this.rejectedExecutionCount = rejectedExecutionCount;
    }
    
    public double getThreadUsageRate() {
        return threadUsageRate;
    }
    
    public void setThreadUsageRate(double threadUsageRate) {
        this.threadUsageRate = threadUsageRate;
    }
    
    public double getQueueUsageRate() {
        return queueUsageRate;
    }
    
    public void setQueueUsageRate(double queueUsageRate) {
        this.queueUsageRate = queueUsageRate;
    }
    
    public boolean isShutdown() {
        return isShutdown;
    }
    
    public void setShutdown(boolean shutdown) {
        isShutdown = shutdown;
    }
    
    public boolean isTerminated() {
        return isTerminated;
    }
    
    public void setTerminated(boolean terminated) {
        isTerminated = terminated;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public double getAlertThreshold() {
        return alertThreshold;
    }
    
    public void setAlertThreshold(double alertThreshold) {
        this.alertThreshold = alertThreshold;
    }
    
    public String getAlertStats() {
        return alertStats;
    }
    
    public void setAlertStats(String alertStats) {
        this.alertStats = alertStats;
    }
    
    @Override
    public String toString() {
        return "ThreadPoolMetrics{" +
                "corePoolSize=" + corePoolSize +
                ", maximumPoolSize=" + maximumPoolSize +
                ", poolSize=" + poolSize +
                ", activeCount=" + activeCount +
                ", taskCount=" + taskCount +
                ", completedTaskCount=" + completedTaskCount +
                ", queueSize=" + queueSize +
                ", rejectedExecutionCount=" + rejectedExecutionCount +
                ", threadUsageRate=" + String.format("%.2f%%", threadUsageRate * 100) +
                ", timestamp=" + timestamp +
                '}';
    }
}
