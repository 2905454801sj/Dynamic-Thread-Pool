package org.jason.threadPool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class DynamicThreadPool extends ThreadPoolExecutor {

    private static final Logger logger = Logger.getLogger(DynamicThreadPool.class.getName());
    private final AtomicLong lastAlertTime = new AtomicLong(0);
    // Count of rejected executions
    private final AtomicLong rejectedExecutionCount = new AtomicLong(0);
    // Scheduled task related
    private final ScheduledThreadPoolExecutor alertScheduler;
    private final ScheduledFuture<?> alertTask;
    // Save the original rejection policy
    private final RejectedExecutionHandler originalHandler;
    // Alert related configuration
    private volatile double alertThreshold = 0.8; // Default 80%
    private volatile long alertCooldown = 60000; // Alert cooldown time, default 1 minute

    public DynamicThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, new RejectedExecutionHandlerWrapper(handler, new AtomicLong(0)));

        this.originalHandler = handler;
        // Set the wrapped rejection policy to count rejected executions
        ((RejectedExecutionHandlerWrapper) this.getRejectedExecutionHandler()).setRejectedCount(rejectedExecutionCount);

        ThreadFactory alertThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ThreadPool-Alert-Checker");
                t.setDaemon(true); // Set as daemon thread
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        // Use ScheduledThreadPoolExecutor constructor directly
        this.alertScheduler = new ScheduledThreadPoolExecutor(1, // Core thread count
                alertThreadFactory, new ThreadPoolExecutor.DiscardPolicy() // Rejection policy
        );

        // Start periodic alert check task, execute once per minute
        this.alertTask = alertScheduler.scheduleAtFixedRate(this::checkAlert, 1, // Initial delay 1 minute
                1, // Execute every 1 minute
                TimeUnit.MINUTES);

        logger.info("Thread pool alert system started, auto-check every minute");
    }

    public void setCoreParameters(int corePoolSize, int maximumPoolSize, RejectedExecutionHandler rejectedExecutionHandler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || corePoolSize > maximumPoolSize) {
            throw new IllegalArgumentException("corePoolSize must be >= 0, maximumPoolSize must be > 0, and corePoolSize <= maximumPoolSize");
        }
        if (rejectedExecutionHandler == null) {
            throw new IllegalArgumentException("RejectedExecutionHandler must not be null");
        }
        this.setCorePoolSize(corePoolSize);
        this.setMaximumPoolSize(maximumPoolSize);

        // Wrap the new rejection policy
        RejectedExecutionHandlerWrapper wrappedHandler = new RejectedExecutionHandlerWrapper(rejectedExecutionHandler, rejectedExecutionCount);
        this.setRejectedExecutionHandler(wrappedHandler);

        // Check alert after parameter changes
        checkAlert();
    }

    public String showDetails() {
        String details = "CorePoolSize: " + this.getCorePoolSize() + ", MaximumPoolSize: " + this.getMaximumPoolSize() + ", PoolSize: " + this.getPoolSize() + ", ActiveCount: " + this.getActiveCount() + ", TaskCount: " + this.getTaskCount() + ", CompletedTaskCount: " + this.getCompletedTaskCount() + ", QueueSize: " + this.getQueue().size() + ", RejectedCount: " + rejectedExecutionCount.get();

        return details;
    }

    /**
     * Check if alert needs to be triggered
     */
    public void checkAlert() {
        int activeCount = this.getActiveCount();
        int maxPoolSize = this.getMaximumPoolSize();

        if (maxPoolSize == 0) {
            return; // Avoid division by zero
        }

        double usage = (double) activeCount / maxPoolSize;

        if (usage >= alertThreshold) {
            long currentTime = System.currentTimeMillis();
            long lastAlert = lastAlertTime.get();

            // Check cooldown time
            if (currentTime - lastAlert >= alertCooldown) {
                if (lastAlertTime.compareAndSet(lastAlert, currentTime)) {
                    String alertMessage = String.format("Thread pool alert: Active thread usage is too high! " + "Current usage rate: %.2f%% (Threshold: %.2f%%), " + "Active threads: %d, Max pool size: %d, " + "Queue size: %d, " + "Rejected execution count: %d, Time: %s", usage * 100, alertThreshold * 100, activeCount, maxPoolSize, this.getQueue().size(), rejectedExecutionCount.get(), new java.util.Date());

                    logger.warning(alertMessage);
                    System.err.println("[ALERT] " + alertMessage);
                }
            }
        } else {
            // Log debug info when usage is normal
            logger.fine(String.format("Thread pool usage is normal: %.2f%% (Threshold: %.2f%%), Rejected execution count: %d", usage * 100, alertThreshold * 100, rejectedExecutionCount.get()));
        }
    }

    /**
     * Get the count of rejected executions
     */
    public long getRejectedExecutionCount() {
        return rejectedExecutionCount.get();
    }

    /**
     * Reset the rejected execution count statistics
     */
    public void resetRejectedExecutionCount() {
        rejectedExecutionCount.set(0);
        logger.info("Rejected execution count statistics has been reset");
    }

    /**
     * Get current alert threshold
     */
    public double getAlertThreshold() {
        return alertThreshold;
    }

    /**
     * Set alert threshold
     *
     * @param threshold Threshold value (0.0 - 1.0)
     */
    public void setAlertThreshold(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("Alert threshold must be between 0.0 and 1.0");
        }
        double oldThreshold = this.alertThreshold;
        this.alertThreshold = threshold;

        logger.info(String.format("Alert threshold adjusted from %.2f%% to %.2f%%", oldThreshold * 100, threshold * 100));

        // Check alert immediately after parameter changes
        checkAlert();
    }

    /**
     * Set alert cooldown time (milliseconds)
     */
    public void setAlertCooldown(long cooldownMs) {
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("Alert cooldown time cannot be negative");
        }
        long oldCooldown = this.alertCooldown;
        this.alertCooldown = cooldownMs;

        logger.info(String.format("Alert cooldown time adjusted from %d milliseconds to %d milliseconds", oldCooldown, cooldownMs));
    }

    /**
     * Get current thread usage rate
     */
    public double getThreadUsageRate() {
        int maxPoolSize = this.getMaximumPoolSize();
        if (maxPoolSize == 0) {
            return 0.0;
        }
        return (double) this.getActiveCount() / maxPoolSize;
    }

    /**
     * Get alert statistics information
     */
    public String getAlertStats() {
        long timeSinceLastAlert = System.currentTimeMillis() - lastAlertTime.get();
        return String.format("Alert config: Threshold=%.2f%%, Cooldown=%d milliseconds, Time since last alert=%d milliseconds, Rejected execution count=%d", alertThreshold * 100, alertCooldown, timeSinceLastAlert, rejectedExecutionCount.get());
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down thread pool alert system...");

        // Output final statistics
        logger.info(String.format("Statistics on thread pool shutdown: Total tasks=%d, Completed tasks=%d, Rejected execution count=%d", getTaskCount(), getCompletedTaskCount(), rejectedExecutionCount.get()));

        // Stop alert scheduled task
        if (alertTask != null && !alertTask.isCancelled()) {
            alertTask.cancel(false);
        }

        // Shutdown alert scheduler
        if (alertScheduler != null && !alertScheduler.isShutdown()) {
            alertScheduler.shutdown();
            try {
                if (!alertScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    alertScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                alertScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        super.shutdown();
        logger.info("Thread pool alert system has been shut down");
    }

    @Override
    public java.util.List<Runnable> shutdownNow() {
        logger.info("Force shutting down thread pool alert system...");

        // Output final statistics
        logger.info(String.format("Statistics on thread pool force shutdown: Total tasks=%d, Completed tasks=%d, Rejected execution count=%d", getTaskCount(), getCompletedTaskCount(), rejectedExecutionCount.get()));

        // Immediately stop alert scheduled task
        if (alertTask != null) {
            alertTask.cancel(true);
        }

        // Force shutdown alert scheduler
        if (alertScheduler != null) {
            alertScheduler.shutdownNow();
        }

        logger.info("Thread pool alert system has been force shut down");
        return super.shutdownNow();
    }

    /**
     * Wrapper for RejectedExecutionHandler to count rejections
     */
    private static class RejectedExecutionHandlerWrapper implements RejectedExecutionHandler {
        private final RejectedExecutionHandler delegate;
        private AtomicLong rejectedCount;

        public RejectedExecutionHandlerWrapper(RejectedExecutionHandler delegate, AtomicLong rejectedCount) {
            this.delegate = delegate;
            this.rejectedCount = rejectedCount;
        }

        public void setRejectedCount(AtomicLong rejectedCount) {
            this.rejectedCount = rejectedCount;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (rejectedCount != null) {
                rejectedCount.incrementAndGet();
            }
            delegate.rejectedExecution(r, executor);
        }
    }
}