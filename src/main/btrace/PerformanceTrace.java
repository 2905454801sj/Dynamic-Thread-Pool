import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;

/**
 * BTrace script for performance analysis
 * Monitors method execution times and identifies bottlenecks
 */
@BTrace
public class PerformanceTrace {
    
    private static Map<String, Long> methodStartTimes = Collections.newHashMap();
    private static Map<String, Long> methodCallCounts = Collections.newHashMap();
    private static Map<String, Long> methodTotalTimes = Collections.newHashMap();
    
    // Threshold for slow method detection (in milliseconds)
    private static final long SLOW_METHOD_THRESHOLD = 100;
    
    /**
     * Monitor all Controller method entries
     */
    @OnMethod(
        clazz = "org.jason.controller.DynamicThreadPoolController",
        method = "/.*/",
        location = @Location(Kind.ENTRY)
    )
    public static void onControllerMethodEntry(@ProbeClassName String className, 
                                               @ProbeMethodName String methodName) {
        String key = strcat(name(currentThread()), strcat("-", methodName));
        put(methodStartTimes, key, box(timeNanos()));
    }
    
    /**
     * Monitor all Controller method exits
     */
    @OnMethod(
        clazz = "org.jason.controller.DynamicThreadPoolController",
        method = "/.*/",
        location = @Location(Kind.RETURN)
    )
    public static void onControllerMethodExit(@ProbeClassName String className, 
                                              @ProbeMethodName String methodName,
                                              @Duration long durationNs) {
        String key = strcat(name(currentThread()), strcat("-", methodName));
        Long startTime = get(methodStartTimes, key);
        
        if (startTime != null) {
            long durationMs = (timeNanos() - unbox(startTime)) / 1000000;
            
            // Update statistics
            Long count = get(methodCallCounts, methodName);
            if (count == null) {
                put(methodCallCounts, methodName, box(1L));
                put(methodTotalTimes, methodName, box(durationMs));
            } else {
                put(methodCallCounts, methodName, box(unbox(count) + 1));
                Long totalTime = get(methodTotalTimes, methodName);
                put(methodTotalTimes, methodName, box(unbox(totalTime) + durationMs));
            }
            
            // Report slow methods
            if (durationMs > SLOW_METHOD_THRESHOLD) {
                println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                println("SLOW METHOD DETECTED");
                println(strcat("  Method: ", methodName));
                println(strcat("  Duration: ", str(durationMs)));
                println(" ms");
                println(strcat("  Thread: ", name(currentThread())));
                println(strcat("  Time: ", str(timeMillis())));
                println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            
            remove(methodStartTimes, key);
        }
    }
    
    /**
     * Monitor DynamicThreadPool method performance
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "/^(execute|submit|setCoreParameters|checkAlert)$/",
        location = @Location(Kind.ENTRY)
    )
    public static void onPoolMethodEntry(@ProbeMethodName String methodName) {
        String key = strcat(name(currentThread()), strcat("-pool-", methodName));
        put(methodStartTimes, key, box(timeNanos()));
    }
    
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "/^(execute|submit|setCoreParameters|checkAlert)$/",
        location = @Location(Kind.RETURN)
    )
    public static void onPoolMethodExit(@ProbeMethodName String methodName) {
        String key = strcat(name(currentThread()), strcat("-pool-", methodName));
        Long startTime = get(methodStartTimes, key);
        
        if (startTime != null) {
            long durationMs = (timeNanos() - unbox(startTime)) / 1000000;
            
            // Report if execution takes too long
            if (durationMs > 10) {
                println("----------------------------------------");
                println("Thread Pool Method Performance");
                println(strcat("  Method: ", methodName));
                println(strcat("  Duration: ", str(durationMs)));
                println(" ms");
                println("----------------------------------------");
            }
            
            remove(methodStartTimes, key);
        }
    }
    
    /**
     * Monitor lock acquisition
     */
    @OnMethod(
        clazz = "java.util.concurrent.locks.ReentrantLock",
        method = "lock",
        location = @Location(Kind.ENTRY)
    )
    public static void onLockEntry(@Self Object lock) {
        String key = strcat(name(currentThread()), str(identityHashCode(lock)));
        put(methodStartTimes, key, box(timeNanos()));
    }
    
    @OnMethod(
        clazz = "java.util.concurrent.locks.ReentrantLock",
        method = "lock",
        location = @Location(Kind.RETURN)
    )
    public static void onLockAcquired(@Self Object lock) {
        String key = strcat(name(currentThread()), str(identityHashCode(lock)));
        Long startTime = get(methodStartTimes, key);
        
        if (startTime != null) {
            long waitTimeMs = (timeNanos() - unbox(startTime)) / 1000000;
            
            // Report lock contention
            if (waitTimeMs > 1) {
                println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                println("LOCK CONTENTION DETECTED");
                println(strcat("  Lock: ", str(identityHashCode(lock))));
                println(strcat("  Wait Time: ", str(waitTimeMs)));
                println(" ms");
                println(strcat("  Thread: ", name(currentThread())));
                println("  Stack Trace:");
                jstack();
                println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            
            remove(methodStartTimes, key);
        }
    }
    
    /**
     * Monitor thread creation
     */
    @OnMethod(
        clazz = "java.lang.Thread",
        method = "<init>",
        location = @Location(Kind.RETURN)
    )
    public static void onThreadCreated(@Self Object thread) {
        // Only track thread pool threads
        String threadName = name(thread);
        if (Strings.startsWith(threadName, "GlobalDynamicPool-") || 
            Strings.startsWith(threadName, "ThreadPool-")) {
            println("========================================");
            println("Thread Pool Thread Created");
            println(strcat("  Thread: ", threadName));
            println(strcat("  Time: ", str(timeMillis())));
            println("========================================");
        }
    }
    
    /**
     * Monitor garbage collection
     */
    @OnMethod(
        clazz = "+java.lang.management.GarbageCollectorMXBean",
        method = "getCollectionCount",
        location = @Location(Kind.RETURN)
    )
    public static void onGCCount(@Return long count) {
        // Sample GC activity
        if (count % 10 == 0) {
            println("========================================");
            println("GC Activity Detected");
            println(strcat("  Collection Count: ", str(count)));
            println(strcat("  Time: ", str(timeMillis())));
            println("========================================");
        }
    }
    
    /**
     * Periodic performance statistics
     */
    @OnTimer(60000)
    public static void printPerformanceStats() {
        println("======== Performance Statistics ========");
        println("Method Call Statistics:");
        
        // Print top methods by call count
        printMap(methodCallCounts);
        
        println("Method Total Execution Time (ms):");
        printMap(methodTotalTimes);
        
        // Calculate average execution times
        println("Average Execution Time (ms):");
        Enumeration keys = keys(methodCallCounts);
        while (hasMoreElements(keys)) {
            String method = (String)nextElement(keys);
            Long count = get(methodCallCounts, method);
            Long totalTime = get(methodTotalTimes, method);
            
            if (count != null && totalTime != null && unbox(count) > 0) {
                long avg = unbox(totalTime) / unbox(count);
                println(strcat("  ", strcat(method, strcat(": ", str(avg)))));
            }
        }
        
        println(strcat("Timestamp: ", str(timeMillis())));
        println("=========================================");
    }
    
    /**
     * On exit, print final statistics
     */
    @OnExit
    public static void onExit(int exitCode) {
        println("========================================");
        println("BTrace Performance Monitoring Stopped");
        println("Final Statistics:");
        printPerformanceStats();
        println("========================================");
    }
}
