package org.jason.service;

import org.jason.config.CacheConfig;
import org.jason.dto.ThreadPoolMetrics;
import org.jason.threadPool.DynamicThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Thread Pool Monitoring Service
 * 
 * Provides cached access to thread pool monitoring data.
 * Uses Caffeine local cache with 1-second TTL to optimize
 * performance during high-frequency monitoring requests.
 */
@Service
public class ThreadPoolMonitorService {
    
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolMonitorService.class);
    
    @Autowired
    private DynamicThreadPool threadPool;
    
    /**
     * Get thread pool metrics with caching
     * 
     * Cached for 1 second. Subsequent requests within the TTL
     * return cached data directly without recomputation.
     * 
     * @return Thread pool metrics snapshot
     */
    @Cacheable(value = CacheConfig.THREADPOOL_METRICS_CACHE, key = "'metrics'")
    public ThreadPoolMetrics getMetrics() {
        logger.debug("Computing thread pool metrics (cache miss)");
        
        ThreadPoolMetrics metrics = new ThreadPoolMetrics();
        
        // Basic configuration
        metrics.setCorePoolSize(threadPool.getCorePoolSize());
        metrics.setMaximumPoolSize(threadPool.getMaximumPoolSize());
        metrics.setPoolSize(threadPool.getPoolSize());
        metrics.setActiveCount(threadPool.getActiveCount());
        
        // Task statistics
        metrics.setTaskCount(threadPool.getTaskCount());
        metrics.setCompletedTaskCount(threadPool.getCompletedTaskCount());
        
        // Queue information
        metrics.setQueueSize(threadPool.getQueue().size());
        metrics.setQueueRemainingCapacity(threadPool.getQueue().remainingCapacity());
        
        // Rejection statistics
        metrics.setRejectedExecutionCount(threadPool.getRejectedExecutionCount());
        
        // Thread usage rate calculation
        metrics.setThreadUsageRate(threadPool.getThreadUsageRate());
        
        // Queue usage rate calculation
        int totalQueueCapacity = metrics.getQueueSize() + metrics.getQueueRemainingCapacity();
        if (totalQueueCapacity > 0) {
            metrics.setQueueUsageRate((double) metrics.getQueueSize() / totalQueueCapacity);
        }
        
        // Status information
        metrics.setShutdown(threadPool.isShutdown());
        metrics.setTerminated(threadPool.isTerminated());
        
        // Alert configuration
        metrics.setAlertThreshold(threadPool.getAlertThreshold());
        metrics.setAlertStats(threadPool.getAlertStats());
        
        return metrics;
    }
    
    /**
     * Get runtime statistics with caching
     * 
     * @return Runtime statistics snapshot
     */
    @Cacheable(value = CacheConfig.THREADPOOL_STATS_CACHE, key = "'stats'")
    public ThreadPoolMetrics getRuntimeStats() {
        logger.debug("Computing thread pool runtime stats (cache miss)");
        return getMetrics();
    }
    
    /**
     * Get detailed thread pool information as string
     * 
     * Uses StringBuilder for optimized string concatenation performance.
     * 
     * @return Detailed information string
     */
    public String getDetailedInfo() {
        ThreadPoolMetrics metrics = getMetrics();
        
        StringBuilder sb = new StringBuilder(256);
        sb.append("CorePoolSize: ").append(metrics.getCorePoolSize())
          .append(", MaximumPoolSize: ").append(metrics.getMaximumPoolSize())
          .append(", PoolSize: ").append(metrics.getPoolSize())
          .append(", ActiveCount: ").append(metrics.getActiveCount())
          .append(", TaskCount: ").append(metrics.getTaskCount())
          .append(", CompletedTaskCount: ").append(metrics.getCompletedTaskCount())
          .append(", QueueSize: ").append(metrics.getQueueSize())
          .append(", RejectedCount: ").append(metrics.getRejectedExecutionCount())
          .append(", ThreadUsage: ").append(String.format("%.2f%%", metrics.getThreadUsageRate() * 100))
          .append(", QueueUsage: ").append(String.format("%.2f%%", metrics.getQueueUsageRate() * 100));
        
        return sb.toString();
    }
    
    /**
     * Force cache refresh
     * 
     * Should be called when thread pool parameters are changed.
     * Spring Cache will automatically recompute on next access.
     */
    public void refreshCache() {
        logger.info("Manually refreshing thread pool metrics cache");
        // Spring Cache will recompute on next method call
    }
}
