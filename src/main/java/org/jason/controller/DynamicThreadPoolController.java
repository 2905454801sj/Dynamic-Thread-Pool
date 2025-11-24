package org.jason.controller;

import org.jason.threadPool.DynamicThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 动态线程池控制器
 * 提供线程池管理和监控的REST API接口
 */
@RestController
@RequestMapping("/api/threadpool")
@CrossOrigin(origins = "")
public class DynamicThreadPoolController {
    @Autowired
    private DynamicThreadPool threadPool;

    /**
     * 获取线程池详细信息
     */
    @GetMapping("/details")
    public ResponseEntity<Map<String, Object>> getThreadPoolDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("corePoolSize", threadPool.getCorePoolSize());
        details.put("maximumPoolSize", threadPool.getMaximumPoolSize());
        details.put("poolSize", threadPool.getPoolSize());
        details.put("activeCount", threadPool.getActiveCount());
        details.put("taskCount", threadPool.getTaskCount());
        details.put("completedTaskCount", threadPool.getCompletedTaskCount());
        details.put("queueSize", threadPool.getQueue().size());
        details.put("rejectedExecutionCount", threadPool.getRejectedExecutionCount());
        details.put("threadUsageRate", String.format("%.2f%%", threadPool.getThreadUsageRate() * 100));
        details.put("isShutdown", threadPool.isShutdown());
        details.put("isTerminated", threadPool.isTerminated());
        details.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(details);
    }

    /**
     * 获取告警配置信息
     */
    @GetMapping("/alert/config")
    public ResponseEntity<Map<String, Object>> getAlertConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("alertThreshold", threadPool.getAlertThreshold());
        config.put("alertStats", threadPool.getAlertStats());
        config.put("rejectedExecutionCount", threadPool.getRejectedExecutionCount());
        config.put("threadUsageRate", threadPool.getThreadUsageRate());

        return ResponseEntity.ok(config);
    }

    /**
     * 设置告警阈值
     */
    @PostMapping("/alert/threshold")
    public ResponseEntity<Map<String, Object>> setAlertThreshold(@RequestBody Map<String, Double> request) {
        try {
            Double threshold = request.get("threshold");
            if (threshold == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("告警阈值不能为空"));
            }

            threadPool.setAlertThreshold(threshold);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "告警阈值设置成功");
            response.put("newThreshold", threadPool.getAlertThreshold());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("设置告警阈值失败: " + e.getMessage()));
        }
    }

    /**
     * 设置告警冷却时间
     */
    @PostMapping("/alert/cooldown")
    public ResponseEntity<Map<String, Object>> setAlertCooldown(@RequestBody Map<String, Long> request) {
        try {
            Long cooldown = request.get("cooldown");
            if (cooldown == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("冷却时间不能为空"));
            }

            threadPool.setAlertCooldown(cooldown);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "告警冷却时间设置成功");
            response.put("cooldownMs", cooldown);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("设置告警冷却时间失败: " + e.getMessage()));
        }
    }

    /**
     * 动态调整线程池核心参数
     */
    @PostMapping("/config/parameters")
    public ResponseEntity<Map<String, Object>> setCoreParameters(@RequestBody Map<String, Object> request) {
        try {
            Integer corePoolSize = (Integer) request.get("corePoolSize");
            Integer maximumPoolSize = (Integer) request.get("maximumPoolSize");
            String rejectionPolicy = (String) request.get("rejectionPolicy");

            if (corePoolSize == null || maximumPoolSize == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("核心线程数和最大线程数不能为空"));
            }

            RejectedExecutionHandler handler = getRejectionHandler(rejectionPolicy);
            threadPool.setCoreParameters(corePoolSize, maximumPoolSize, handler);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "线程池参数设置成功");
            response.put("corePoolSize", threadPool.getCorePoolSize());
            response.put("maximumPoolSize", threadPool.getMaximumPoolSize());
            response.put("rejectionPolicy", rejectionPolicy);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("设置线程池参数失败: " + e.getMessage()));
        }
    }

    /**
     * 手动触发告警检查
     */
    @PostMapping("/alert/check")
    public ResponseEntity<Map<String, Object>> manualAlertCheck() {
        try {
            threadPool.checkAlert();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "手动告警检查已触发");
            response.put("currentUsage", String.format("%.2f%%", threadPool.getThreadUsageRate() * 100));
            response.put("alertStats", threadPool.getAlertStats());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("手动告警检查失败: " + e.getMessage()));
        }
    }

    /**
     * 重置拒绝执行次数统计
     */
    @PostMapping("/stats/reset-rejected")
    public ResponseEntity<Map<String, Object>> resetRejectedExecutionCount() {
        try {
            long oldCount = threadPool.getRejectedExecutionCount();
            threadPool.resetRejectedExecutionCount();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "拒绝执行次数统计已重置");
            response.put("previousCount", oldCount);
            response.put("currentCount", threadPool.getRejectedExecutionCount());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("重置统计失败: " + e.getMessage()));
        }
    }

    /**
     * 提交测试任务（用于测试线程池）
     */
    @PostMapping("/test/submit-task")
    public ResponseEntity<Map<String, Object>> submitTestTask(@RequestBody Map<String, Object> request) {
        try {
            Integer taskCount = (Integer) request.getOrDefault("taskCount", 1);
            Integer sleepMs = (Integer) request.getOrDefault("sleepMs", 1000);

            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                threadPool.execute(() -> {
                    System.out.println("测试任务 " + taskId + " 开始执行，线程: " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("测试任务 " + taskId + " 执行完成");
                });
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "测试任务提交成功");
            response.put("submittedTaskCount", taskCount);
            response.put("taskSleepMs", sleepMs);
            response.put("currentPoolState", threadPool.showDetails());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(createErrorResponse("提交测试任务失败: " + e.getMessage()));
        }
    }

    /**
     * 获取线程池运行时统计信息
     */
    @GetMapping("/stats/runtime")
    public ResponseEntity<Map<String, Object>> getRuntimeStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("corePoolSize", threadPool.getCorePoolSize());
        stats.put("maximumPoolSize", threadPool.getMaximumPoolSize());
        stats.put("poolSize", threadPool.getPoolSize());
        stats.put("activeCount", threadPool.getActiveCount());
        stats.put("taskCount", threadPool.getTaskCount());
        stats.put("completedTaskCount", threadPool.getCompletedTaskCount());
        stats.put("queueSize", threadPool.getQueue().size());
        stats.put("queueRemainingCapacity", threadPool.getQueue().remainingCapacity());
        stats.put("rejectedExecutionCount", threadPool.getRejectedExecutionCount());
        stats.put("threadUsageRate", threadPool.getThreadUsageRate());
        stats.put("alertThreshold", threadPool.getAlertThreshold());
        stats.put("alertStats", threadPool.getAlertStats());
        stats.put("isShutdown", threadPool.isShutdown());
        stats.put("isTerminated", threadPool.isTerminated());
        stats.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(stats);
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = !threadPool.isShutdown() && !threadPool.isTerminated();

        health.put("status", isHealthy ? "UP" : "DOWN");
        health.put("threadPoolActive", !threadPool.isShutdown());
        health.put("currentTime", System.currentTimeMillis());
        health.put("usageRate", String.format("%.2f%%", threadPool.getThreadUsageRate() * 100));

        return ResponseEntity.ok(health);
    }

    /**
     * 获取拒绝策略处理器
     */
    private RejectedExecutionHandler getRejectionHandler(String policy) {
        if (policy == null) {
            return new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();
        }

        switch (policy.toLowerCase()) {
            case "abort":
                return new java.util.concurrent.ThreadPoolExecutor.AbortPolicy();
            case "discard":
                return new java.util.concurrent.ThreadPoolExecutor.DiscardPolicy();
            case "discardoldest":
                return new java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy();
            case "calleruns":
            default:
                return new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();
        }
    }

    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}