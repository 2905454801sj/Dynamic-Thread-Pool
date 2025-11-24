package org.jason.config;

import org.jason.threadPool.DynamicThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池配置类
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 创建全局动态线程池Bean
     */
    @Bean(destroyMethod = "shutdown")
    public DynamicThreadPool globalDynamicThreadPool() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "GlobalDynamicPool-" + (++counter));
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };

        DynamicThreadPool pool = new DynamicThreadPool(
                5,    // 核心线程数
                20,   // 最大线程数
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 设置告警阈值为70%
        pool.setAlertThreshold(0.7);

        return pool;
    }
}