import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;

/**
 * BTrace script to monitor thread pool parameter changes
 * Tracks dynamic adjustments to pool configuration
 */
@BTrace
public class ParameterChangeTrace {
    
    private static AtomicLong changeCounter = Atomic.newAtomicLong(0);
    
    /**
     * Monitor core pool size changes
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "setCorePoolSize",
        location = @Location(Kind.ENTRY)
    )
    public static void onCorePoolSizeChange(@Self Object pool, int newSize) {
        long changeId = Atomic.incrementAndGet(changeCounter);
        
        println("========================================");
        println(strcat("CORE POOL SIZE CHANGED #", str(changeId)));
        println(strcat("  New Core Pool Size: ", str(newSize)));
        
        // Get old value
        Object oldSize = get(field(classOf(pool), "corePoolSize"), pool);
        if (oldSize != null) {
            println(strcat("  Old Core Pool Size: ", str(oldSize)));
        }
        
        println(strcat("  Time: ", str(timeMillis())));
        println(strcat("  Thread: ", name(currentThread())));
        
        // Print current pool state
        printPoolState(pool);
        
        println("Call Stack:");
        jstack();
        println("========================================");
    }
    
    /**
     * Monitor maximum pool size changes
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "setMaximumPoolSize",
        location = @Location(Kind.ENTRY)
    )
    public static void onMaxPoolSizeChange(@Self Object pool, int newSize) {
        long changeId = Atomic.incrementAndGet(changeCounter);
        
        println("========================================");
        println(strcat("MAX POOL SIZE CHANGED #", str(changeId)));
        println(strcat("  New Max Pool Size: ", str(newSize)));
        
        // Get old value
        Object oldSize = get(field(classOf(pool), "maximumPoolSize"), pool);
        if (oldSize != null) {
            println(strcat("  Old Max Pool Size: ", str(oldSize)));
        }
        
        println(strcat("  Time: ", str(timeMillis())));
        println(strcat("  Thread: ", name(currentThread())));
        
        // Print current pool state
        printPoolState(pool);
        
        println("Call Stack:");
        jstack();
        println("========================================");
    }
    
    /**
     * Monitor rejection handler changes
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "setRejectedExecutionHandler",
        location = @Location(Kind.ENTRY)
    )
    public static void onHandlerChange(@Self Object pool, Object newHandler) {
        long changeId = Atomic.incrementAndGet(changeCounter);
        
        println("========================================");
        println(strcat("REJECTION HANDLER CHANGED #", str(changeId)));
        println(strcat("  New Handler: ", name(classOf(newHandler))));
        
        // Get old handler
        Object oldHandler = get(field(classOf(pool), "handler"), pool);
        if (oldHandler != null) {
            println(strcat("  Old Handler: ", name(classOf(oldHandler))));
        }
        
        println(strcat("  Time: ", str(timeMillis())));
        println(strcat("  Thread: ", name(currentThread())));
        
        println("Call Stack:");
        jstack();
        println("========================================");
    }
    
    /**
     * Monitor keep alive time changes
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "setKeepAliveTime",
        location = @Location(Kind.ENTRY)
    )
    public static void onKeepAliveTimeChange(@Self Object pool, long time, Object unit) {
        long changeId = Atomic.incrementAndGet(changeCounter);
        
        println("========================================");
        println(strcat("KEEP ALIVE TIME CHANGED #", str(changeId)));
        println(strcat("  New Keep Alive Time: ", str(time)));
        println(strcat("  Time Unit: ", str(unit)));
        println(strcat("  Time: ", str(timeMillis())));
        println(strcat("  Thread: ", name(currentThread())));
        println("========================================");
    }
    
    /**
     * Monitor DynamicThreadPool.setCoreParameters method
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "setCoreParameters",
        location = @Location(Kind.ENTRY)
    )
    public static void onSetCoreParameters(@Self Object pool, int coreSize, int maxSize, Object handler) {
        long changeId = Atomic.incrementAndGet(changeCounter);
        
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        println(strcat("BATCH PARAMETER CHANGE #", str(changeId)));
        println("  New Configuration:");
        println(strcat("    Core Pool Size: ", str(coreSize)));
        println(strcat("    Max Pool Size: ", str(maxSize)));
        println(strcat("    Rejection Handler: ", name(classOf(handler))));
        
        println("  Old Configuration:");
        printPoolState(pool);
        
        println(strcat("  Time: ", str(timeMillis())));
        println(strcat("  Thread: ", name(currentThread())));
        
        println("Call Stack:");
        jstack();
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Monitor parameter change completion
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "setCoreParameters",
        location = @Location(Kind.RETURN)
    )
    public static void onSetCoreParametersComplete(@Self Object pool) {
        println("----------------------------------------");
        println("Parameter Change Completed");
        println("  New State:");
        printPoolState(pool);
        println("----------------------------------------");
    }
    
    /**
     * Helper method to print pool state
     */
    private static void printPoolState(Object pool) {
        try {
            Class poolClass = classOf(pool);
            
            Object coreSize = get(field(poolClass, "corePoolSize"), pool);
            Object maxSize = get(field(poolClass, "maximumPoolSize"), pool);
            Object poolSize = get(field(poolClass, "poolSize"), pool);
            Object activeCount = get(field(poolClass, "activeCount"), pool);
            
            println(strcat("    Core: ", str(coreSize)));
            println(strcat("    Max: ", str(maxSize)));
            println(strcat("    Current: ", str(poolSize)));
            println(strcat("    Active: ", str(activeCount)));
        } catch (Throwable t) {
            println(strcat("    Error: ", str(t)));
        }
    }
    
    /**
     * Periodic summary of parameter changes
     */
    @OnTimer(300000)
    public static void printChangeSummary() {
        long count = Atomic.get(changeCounter);
        
        println("======== Parameter Change Summary ========");
        println(strcat("  Total Changes: ", str(count)));
        println(strcat("  Timestamp: ", str(timeMillis())));
        println("===========================================");
    }
}
