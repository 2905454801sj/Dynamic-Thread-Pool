import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;

/**
 * BTrace script to monitor thread pool task rejections
 * Tracks all rejection events with detailed context
 */
@BTrace
public class RejectionTrace {
    
    private static AtomicLong rejectionCount = Atomic.newAtomicLong(0);
    private static AtomicLong lastRejectionTime = Atomic.newAtomicLong(0);
    
    /**
     * Monitor rejection handler wrapper execution
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool$RejectedExecutionHandlerWrapper",
        method = "rejectedExecution",
        location = @Location(Kind.ENTRY)
    )
    public static void onRejectionWrapper(@Self Object handler, Runnable task, Object executor) {
        long count = Atomic.incrementAndGet(rejectionCount);
        long currentTime = timeMillis();
        long lastTime = Atomic.get(lastRejectionTime);
        Atomic.set(lastRejectionTime, currentTime);
        
        long timeSinceLastRejection = lastTime > 0 ? currentTime - lastTime : 0;
        
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        println("========== TASK REJECTED ==========");
        println(strcat("Rejection #", str(count)));
        println(strcat("Time: ", str(currentTime)));
        println(strcat("Time Since Last Rejection: ", str(timeSinceLastRejection)));
        println(" ms");
        println(strcat("Handler: ", name(classOf(handler))));
        println(strcat("Task: ", str(task)));
        println(strcat("Task Type: ", name(classOf(task))));
        
        // Print thread pool state
        println("Thread Pool State:");
        printThreadPoolState(executor);
        
        // Print call stack
        println("Rejection Call Stack:");
        jstack();
        println("===================================");
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Monitor all standard rejection policies
     */
    @OnMethod(
        clazz = "/java\\.util\\.concurrent\\.ThreadPoolExecutor\\$.*/",
        method = "rejectedExecution",
        location = @Location(Kind.ENTRY)
    )
    public static void onStandardRejection(@Self Object handler, Runnable task, Object executor) {
        println("----------------------------------------");
        println("Standard Rejection Policy Invoked");
        println(strcat("  Policy: ", name(classOf(handler))));
        println(strcat("  Task: ", str(identityHashCode(task))));
        println("----------------------------------------");
    }
    
    /**
     * Helper method to print thread pool state
     */
    private static void printThreadPoolState(Object executor) {
        try {
            Class poolClass = classOf(executor);
            
            Object coreSize = get(field(poolClass, "corePoolSize"), executor);
            Object maxSize = get(field(poolClass, "maximumPoolSize"), executor);
            Object poolSize = get(field(poolClass, "poolSize"), executor);
            Object activeCount = get(field(poolClass, "activeCount"), executor);
            Object taskCount = get(field(poolClass, "taskCount"), executor);
            Object completedCount = get(field(poolClass, "completedTaskCount"), executor);
            Object queue = get(field(poolClass, "workQueue"), executor);
            
            println(strcat("  Core Pool Size: ", str(coreSize)));
            println(strcat("  Maximum Pool Size: ", str(maxSize)));
            println(strcat("  Current Pool Size: ", str(poolSize)));
            println(strcat("  Active Threads: ", str(activeCount)));
            println(strcat("  Total Tasks: ", str(taskCount)));
            println(strcat("  Completed Tasks: ", str(completedCount)));
            
            if (queue != null) {
                Object queueSize = Reflective.get(field(classOf(queue), "size"), queue);
                println(strcat("  Queue Size: ", str(queueSize)));
                
                // Calculate utilization
                if (maxSize != null && activeCount != null) {
                    int max = unbox(maxSize);
                    int active = unbox(activeCount);
                    if (max > 0) {
                        int utilization = (active * 100) / max;
                        println(strcat("  Thread Utilization: ", str(utilization)));
                        println("%");
                    }
                }
            }
        } catch (Throwable t) {
            println(strcat("  Error printing state: ", str(t)));
        }
    }
    
    /**
     * Periodic rejection statistics
     */
    @OnTimer(60000)
    public static void printRejectionStats() {
        long count = Atomic.get(rejectionCount);
        long lastTime = Atomic.get(lastRejectionTime);
        
        println("======== Rejection Statistics ========");
        println(strcat("  Total Rejections: ", str(count)));
        
        if (lastTime > 0) {
            long timeSince = timeMillis() - lastTime;
            println(strcat("  Time Since Last: ", str(timeSince)));
            println(" ms");
        } else {
            println("  No rejections detected");
        }
        
        println(strcat("  Timestamp: ", str(timeMillis())));
        println("=======================================");
    }
}
