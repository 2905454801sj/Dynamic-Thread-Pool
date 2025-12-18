# Dynamic Thread Pool / 动态线程池

A dynamic thread pool with runtime parameter tuning and monitoring.

支持运行时调参、监控告警的动态线程池实现。

## Features / 功能

- **Runtime Tuning / 运行时调参** - Adjust core size, max size, rejection policy without restart
- **Real-time Monitoring / 实时监控** - Thread usage, queue usage, rejection count
- **Alert System / 告警系统** - Threshold-based alerts with cooldown
- **Caffeine Cache / 缓存优化** - 1s TTL for high-frequency requests
- **BTrace Tracing / 动态追踪** - Runtime JVM instrumentation

## Tech Stack / 技术栈

- Java 17 + Spring Boot 3.5.3
- Caffeine Cache
- BTrace
- SLF4J + Logback

## Quick Start / 快速开始

```bash
mvn spring-boot:run
```

## API

```bash
# Get metrics / 获取指标
GET /api/threadpool/details

# Update parameters / 调整参数
POST /api/threadpool/config/parameters
{
  "corePoolSize": 10,
  "maximumPoolSize": 30
}

# Set alert threshold / 设置告警阈值
POST /api/threadpool/alert/threshold?threshold=0.8
```

## Config / 配置

```properties
threadpool.core-size=5
threadpool.max-size=20
threadpool.queue-capacity=200
threadpool.alert-threshold=0.7
```

## Author / 作者

孙杰 (Jason)
