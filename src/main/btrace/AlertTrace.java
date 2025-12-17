import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;

/**
 * BTrace script to monitor thread pool alert system
 * Tracks alert checks and triggers
 */
@BTrace
public class AlertTrace {
    
    private static AtomicLong alertCheckCount = Atomic.newAtomicLong(0);
    private static AtomicLong alertTriggerCount = Atomic.newAtomicLong(0);
    private static AtomicLong lastAlertTime = Atomic.newAtomicLong(0);
    
    /**
     * Monitor alert check method entry
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "checkAlert",
        location = @Location(Kind.ENTRY)
    )
    public static void onAlertCheckEntry(@Self Object pool) {
        long checkId = Atomic.incrementAndGet(alertCheckCount);
        
        // Only print detailed info every 10 checks to reduce noise
        if (checkId % 10 == 0) {
            println("========================================");
            println(strcat("Alert Check #", str(checkId)));
            printAlertContext(pool);
            println("========================================");
        }
    }
    
    /**
     * Monitor when usage exceeds threshold (alert condition)
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "checkAlert",
        location = @Location(value = Kind.LINE, line = 89)
    )
    public static void onAlertConditionMet(@Self Object pool) {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        println("ALERT CONDITION MET");
        printAlertContext(pool);
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Monitor actual alert trigger (after cooldown check)
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "checkAlert",
        location = @Location(value = Kind.LINE, line = 95)
    )
    public static void onAlertTriggered(@Self Object pool) {
        long triggerId = Atomic.incrementAndGet(alertTriggerCount);
        long currentTime = timeMillis();
        long lastTime = Atomic.get(lastAlertTime);
        Atomic.set(lastAlertTime, currentTime);
        
        long timeSinceLastAlert = lastTime > 0 ? currentTime - lastTime : 0;
        
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        println("========== ALERT TRIGGERED ==========");
        println(strcat("Alert #", str(triggerId)));
        println(strcat("Time: ", str(currentTime)));
        
        if (lastTime > 0) {
            println(strcat("Time Since Last Alert: ", str(timeSinceLastAlert)));
            println(" ms");
        }
        
        // Print detailed thread pool state
        printDetailedPoolState(pool);
        
        // Print alert configuration
        printAlertConfig(pool);
        
        // Print call stack
        println("Alert Trigger Stack:");
        jstack();
        println("=====================================");
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Monitor alert threshold changes
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "setAlertThreshold",
        location = @Location(Kind.ENTRY)
    )
    public static void onThresholdChange(@Self Object pool, double newThreshold) {
        println("========================================");
        println("ALERT THRESHOLD CHANGED");
        println(strcat("  New Threshold: ", str(newThreshold * 100)));
        println("%");
        
        Object oldThreshold = get(field(classOf(pool), "alertThreshold"), pool);
        if (oldThreshold != null) {
            println(strcat("  Old Threshold: ", str(unbox(oldThreshold) * 100)));
            println("%");
        }
        
        println(strcat("  Time: ", str(timeMillis())));
        println("========================================");
    }
    
    /**
     * Monitor alert cooldown changes
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "setAlertCooldown",
        location = @Location(Kind.ENTRY)
    )
    public static void onCooldownChange(@Self Object pool, long newCooldown) {
        println("========================================");
        println("ALERT COOLDOWN CHANGED");
        println(strcat("  New Cooldown: ", str(newCooldown)));
        println(" ms");
        
        Object oldCooldown = get(field(classOf(pool), "alertCooldown"), pool);
        if (oldCooldown != null) {
            println(strcat("  Old Cooldown: ", str(oldCooldown)));
            println(" ms");
        }
        
        println(strcat("  Time: ", str(timeMillis())));
        println("========================================");
    }
    
    /**
     * Helper method to print alert context
     */
    private static void printAlertContext(Object pool) {
        try {
            Class poolClass = classOf(pool);
            
            Object activeCount = get(field(poolClass, "activeCount"), pool);
            Object maxPoolSize = get(field(poolClass, "maximumPoolSize"), pool);
            Object alertThreshold = get(field(poolClass, "alertThreshold"), pool);
            
            if (activeCount != null && maxPoolSize != null) {
                int active = unbox(activeCount);
                int max = unbox(maxPoolSize);
                
                if (max > 0) {
                    double usage = (double)active / max;
                    println(strcat("  Usage: ", str(usage * 100)));
                    println("%");
                    println(strcat("  Active: ", str(active)));
                    println(strcat("  Max: ", str(max)));
                }
            }
            
            if (alertThreshold != null) {
                println(strcat("  Threshold: ", str(unbox(alertThreshold) * 100)));
                println("%");
            }
        } catch (Throwable t) {
            println(strcat("  Error: ", str(t)));
        }
    }
    
    /**
     * Helper method to print detailed pool state
     */
    private static void printDetailedPoolState(Object pool) {
        try {
            println("Thread Pool State:");
            Class poolClass = classOf(pool);
            
            Object coreSize = get(field(poolClass, "corePoolSize"), pool);
            Object maxSize = get(field(poolClass, "maximumPoolSize"), pool);
            Object poolSize = get(field(poolClass, "poolSize"), pool);
            Object activeCount = get(field(poolClass, "activeCount"), pool);
            Object taskCount = get(field(poolClass, "taskCount"), pool);
            Object completedCount = get(field(poolClass, "completedTaskCount"), pool);
            Object queue = get(field(poolClass, "workQueue"), pool);
            
            println(strcat("  Core Pool Size: ", str(coreSize)));
            println(strcat("  Max Pool Size: ", str(maxSize)));
            println(strcat("  Current Pool Size: ", str(poolSize)));
            println(strcat("  Active Threads: ", str(activeCount)));
            println(strcat("  Total Tasks: ", str(taskCount)));
            println(strcat("  Completed Tasks: ", str(completedCount)));
            
            if (queue != null) {
                Object queueSize = Reflective.get(field(classOf(queue), "size"), queue);
                println(strcat("  Queue Size: ", str(queueSize)));
            }
            
            // Calculate usage
            if (maxSize != null && activeCount != null) {
                int max = unbox(maxSize);
                int active = unbox(activeCount);
                if (max > 0) {
                    double usage = (double)active / max;
                    println(strcat("  Thread Usage: ", str(usage * 100)));
                    println("%");
                }
            }
        } catch (Throwable t) {
            println(strcat("  Error: ", str(t)));
        }
    }
    
    /**
     * Helper method to print alert configuration
     */
    private static void printAlertConfig(Object pool) {
        try {
            println("Alert Configuration:");
            Class poolClass = classOf(pool);
            
            Object threshold = get(field(poolClass, "alertThreshold"), pool);
            Object cooldown = get(field(poolClass, "alertCooldown"), pool);
            Object lastAlert = get(field(poolClass, "lastAlertTime"), pool);
            
            if (threshold != null) {
                println(strcat("  Threshold: ", str(unbox(threshold) * 100)));
                println("%");
            }
            
            if (cooldown != null) {
                println(strcat("  Cooldown: ", str(cooldown)));
                println(" ms");
            }
            
            if (lastAlert != null) {
                Object lastAlertValue = Reflective.get(field(classOf(lastAlert), "value"), lastAlert);
                if (lastAlertValue != null) {
                    long timeSince = timeMillis() - unbox(lastAlertValue);
                    println(strcat("  Time Since Last: ", str(timeSince)));
                    println(" ms");
                }
            }
        } catch (Throwable t) {
            println(strcat("  Error: ", str(t)));
        }
    }
    
    /**
     * Periodic alert statistics
     */
    @OnTimer(60000)
    public static void printAlertStats() {
        long checks = Atomic.get(alertCheckCount);
        long triggers = Atomic.get(alertTriggerCount);
        long lastTime = Atomic.get(lastAlertTime);
        
        println("======== Alert Statistics ========");
        println(strcat("  Total Checks: ", str(checks)));
        println(strcat("  Total Triggers: ", str(triggers)));
        
        if (checks > 0 && triggers > 0) {
            double triggerRate = (double)triggers / checks * 100;
            println(strcat("  Trigger Rate: ", str(triggerRate)));
            println("%");
        }
        
        if (lastTime > 0) {
            long timeSince = timeMillis() - lastTime;
            println(strcat("  Time Since Last: ", str(timeSince)));
            println(" ms");
        }
        
        println(strcat("  Timestamp: ", str(timeMillis())));
        println("===================================");
    }
}
