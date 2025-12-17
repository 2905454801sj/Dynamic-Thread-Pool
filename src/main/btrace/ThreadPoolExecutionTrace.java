import org.openjdk.btrace.core.annotations.*;
import static org.openjdk.btrace.core.BTraceUtils.*;

/**
 * BTrace script to monitor thread pool task execution
 * Tracks task submission, execution start, completion, and errors
 */
@BTrace
public class ThreadPoolExecutionTrace {
    
    private static Map<String, Long> taskStartTimes = Collections.newHashMap();
    private static AtomicLong taskCounter = Atomic.newAtomicLong(0);
    
    /**
     * Monitor task submission to thread pool
     */
    @OnMethod(
        clazz = "org.jason.threadPool.DynamicThreadPool",
        method = "execute",
        location = @Location(Kind.ENTRY)
    )
    public static void onTaskSubmit(@Self Object pool, Runnable task) {
        long taskId = Atomic.incrementAndGet(taskCounter);
        String taskKey = str(identityHashCode(task));
        long submitTime = timeMillis();
        
        put(taskStartTimes, taskKey, box(submitTime));
        
        println("========================================");
        println(strcat("Task Submitted #", str(taskId)));
        println(strcat("  Task ID: ", taskKey));
        println(strcat("  Submit Time: ", str(submitTime)));
        println(strcat("  Thread: ", name(currentThread())));
        
        // Print thread pool state
        Object queue = get(field(classOf(pool), "workQueue"), pool);
        if (queue != null) {
            println(strcat("  Queue Size: ", str(Reflective.get(field(classOf(queue), "size"), queue))));
        }
        println("========================================");
    }
    
    /**
     * Monitor task execution start
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "beforeExecute",
        location = @Location(Kind.ENTRY)
    )
    public static void onTaskStart(@Self Object pool, Thread t, Runnable r) {
        String taskKey = str(identityHashCode(r));
        Long submitTime = get(taskStartTimes, taskKey);
        
        if (submitTime != null) {
            long waitTime = timeMillis() - unbox(submitTime);
            
            println("----------------------------------------");
            println(strcat("Task Started: ", taskKey));
            println(strcat("  Thread: ", name(t)));
            println(strcat("  Wait Time: ", str(waitTime)));
            println(" ms");
            
            if (waitTime > 1000) {
                println("  [WARNING] Long wait time detected!");
            }
            println("----------------------------------------");
        }
    }
    
    /**
     * Monitor task completion
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor",
        method = "afterExecute",
        location = @Location(Kind.ENTRY)
    )
    public static void onTaskComplete(@Self Object pool, Runnable r, Throwable t) {
        String taskKey = str(identityHashCode(r));
        Long submitTime = get(taskStartTimes, taskKey);
        
        if (submitTime != null) {
            long totalTime = timeMillis() - unbox(submitTime);
            
            println("========================================");
            println(strcat("Task Completed: ", taskKey));
            println(strcat("  Total Time: ", str(totalTime)));
            println(" ms");
            println(strcat("  Thread: ", name(currentThread())));
            
            if (t != null) {
                println("  [ERROR] Task failed with exception");
            }
            
            // Remove from tracking map
            remove(taskStartTimes, taskKey);
            println("========================================");
        }
    }
    
    /**
     * Monitor task execution errors
     */
    @OnMethod(
        clazz = "java.util.concurrent.ThreadPoolExecutor$Worker",
        method = "run",
        location = @Location(Kind.ERROR)
    )
    public static void onTaskError(@Self Object worker, Throwable error) {
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        println("TASK EXECUTION ERROR DETECTED");
        println(strcat("  Thread: ", name(currentThread())));
        println(strcat("  Error Type: ", name(classOf(error))));
        println(strcat("  Error Message: ", str(error)));
        println("  Stack Trace:");
        jstack();
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    
    /**
     * Periodic statistics output (every 30 seconds)
     */
    @OnTimer(30000)
    public static void printStatistics() {
        println("======== Thread Pool Statistics ========");
        println(strcat("  Pending Tasks: ", str(size(taskStartTimes))));
        println(strcat("  Total Submitted: ", str(Atomic.get(taskCounter))));
        println(strcat("  Timestamp: ", str(timeMillis())));
        println("=========================================");
    }
}
