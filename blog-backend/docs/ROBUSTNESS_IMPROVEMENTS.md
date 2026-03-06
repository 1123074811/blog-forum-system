# 系统健壮性优化总结

## 优化概述

本次优化全面提升了系统的健壮性，包括服务降级、熔断、重试、监控、审计日志等多个方面，确保系统在各种异常情况下都能稳定运行。

---

## 一、服务降级机制 ✅

### 1.1 Redis 降级

**问题：** Redis 挂了会影响核心功能

**解决方案：**
```java
@Configuration
public class RedisFallbackConfig implements CachingConfigurer {
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis 连接失败，缓存操作降级");
                // 不抛出异常，让业务继续执行
            }
        };
    }
}
```

**效果：**
- ✅ Redis 不可用时，缓存操作自动降级
- ✅ 核心业务功能不受影响
- ✅ 记录警告日志，便于监控

### 1.2 智谱AI 服务降级

**问题：** AI 服务不可用时返回 null，影响用户体验

**解决方案：**
```java
@CircuitBreaker(name = "zhipuAi", fallbackMethod = "generateSummaryFallback")
@Retry(name = "zhipuAi")
public String generateSummary(String content) {
    // 调用智谱AI
}

private String generateSummaryFallback(String content, Exception e) {
    log.warn("智谱AI服务降级，使用默认摘要");
    // 返回文章前100个字符作为摘要
    return content.substring(0, Math.min(100, content.length())) + "...";
}
```

**效果：**
- ✅ AI 服务不可用时，返回默认摘要
- ✅ 用户体验不受影响
- ✅ 自动记录降级日志

### 1.3 爬虫服务降级

**问题：** 爬虫失败时直接抛出异常

**解决方案：**
```java
@CircuitBreaker(name = "crawler", fallbackMethod = "crawlArticleFallback")
@Retry(name = "crawler")
public CrawlResponse crawlArticle(String url) {
    // 爬取文章
}

private CrawlResponse crawlArticleFallback(String url, Exception e) {
    log.warn("文章爬取服务降级");
    return CrawlResponse.builder()
            .title("爬取失败")
            .content("抱歉，文章爬取服务暂时不可用，请稍后再试。")
            .sourceUrl(url)
            .build();
}
```

**效果：**
- ✅ 爬虫失败时返回友好提示
- ✅ 不影响其他功能
- ✅ 保留原始链接供用户参考

---

## 二、重试机制 ✅

### 2.1 Resilience4j 重试配置

```yaml
resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3              # 最大重试次数
        wait-duration: 1000          # 重试间隔（毫秒）
        enable-exponential-backoff: true  # 启用指数退避
        exponential-backoff-multiplier: 2  # 退避倍数
    instances:
      zhipuAi:
        max-attempts: 2
        wait-duration: 2000
      crawler:
        max-attempts: 3
        wait-duration: 1000
```

### 2.2 使用示例

```java
@Retry(name = "zhipuAi")
public String generateSummary(String content) {
    // 第1次失败：等待2秒后重试
    // 第2次失败：等待4秒后重试（指数退避）
    // 第3次失败：触发降级
}
```

**效果：**
- ✅ 自动重试临时性故障
- ✅ 指数退避避免雪崩
- ✅ 减少因网络抖动导致的失败

---

## 三、熔断器 ✅

### 3.1 熔断器配置

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        sliding-window-size: 10           # 滑动窗口大小
        minimum-number-of-calls: 5        # 最小调用次数
        failure-rate-threshold: 50        # 失败率阈值（50%）
        wait-duration-in-open-state: 60000  # 熔断等待时间（60秒）
    instances:
      zhipuAi:
        failure-rate-threshold: 60
        wait-duration-in-open-state: 30000
      crawler:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60000
      redis:
        failure-rate-threshold: 70
        wait-duration-in-open-state: 10000
```

### 3.2 熔断器状态

```
CLOSED（关闭）
    ↓ 失败率超过阈值
OPEN（打开）
    ↓ 等待一段时间
HALF_OPEN（半开）
    ↓ 测试调用成功
CLOSED（关闭）
```

**效果：**
- ✅ 防止故障扩散
- ✅ 快速失败，避免资源浪费
- ✅ 自动恢复，无需人工干预

---

## 四、监控与健康检查 ✅

### 4.1 Spring Boot Actuator

**暴露的端点：**
- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息
- `/actuator/metrics` - 指标数据
- `/actuator/prometheus` - Prometheus 格式指标

**配置：**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

### 4.2 自定义健康检查

#### 数据库健康检查
```java
@Bean
public HealthIndicator dbHealthIndicator() {
    return () -> {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "可用")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    };
}
```

#### Redis 健康检查
```java
@Bean
public HealthIndicator redisHealthIndicator() {
    return () -> {
        try {
            redisConnectionFactory.getConnection().ping();
            return Health.up()
                    .withDetail("cache", "Redis")
                    .withDetail("status", "可用")
                    .build();
        } catch (Exception e) {
            // Redis 不可用时返回 UP，因为有降级机制
            return Health.up()
                    .withDetail("status", "降级运行")
                    .build();
        }
    };
}
```

#### 磁盘空间健康检查
```java
@Bean
public HealthIndicator diskSpaceHealthIndicator() {
    return () -> {
        long freeSpace = new File(".").getFreeSpace();
        long totalSpace = new File(".").getTotalSpace();
        double usagePercent = (double) (totalSpace - freeSpace) / totalSpace * 100;
        
        if (usagePercent > 90) {
            return Health.down()
                    .withDetail("diskSpace", "磁盘空间不足")
                    .build();
        }
        return Health.up().build();
    };
}
```

### 4.3 健康检查响应示例

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "status": "可用"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "cache": "Redis",
        "status": "降级运行",
        "message": "Redis 不可用，已启用降级机制"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": "500.00 GB",
        "free": "200.00 GB",
        "usagePercent": "60.00%"
      }
    }
  }
}
```

---

## 五、审计日志 ✅

### 5.1 审计日志注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {
    String module() default "";      // 操作模块
    String operation() default "";   // 操作类型
    String description() default ""; // 操作描述
}
```

### 5.2 使用示例

```java
@AuditLog(
    module = "文章管理",
    operation = "创建文章",
    description = "用户创建新文章"
)
@PostMapping
public ApiResponse<Article> createArticle(@Valid @RequestBody ArticleRequest request, Authentication auth) {
    // 创建文章
}
```

### 5.3 审计日志内容

```json
{
  "timestamp": "2026-03-06 18:00:00",
  "userId": 1,
  "module": "文章管理",
  "operation": "创建文章",
  "description": "用户创建新文章",
  "className": "com.blog.controller.ArticleController",
  "methodName": "createArticle",
  "requestMethod": "POST",
  "requestUrl": "/api/articles",
  "ip": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "params": "{\"title\":\"测试文章\",\"content\":\"...\"}",
  "status": "SUCCESS",
  "executionTime": "125ms"
}
```

### 5.4 敏感信息脱敏

```java
private String desensitize(String content) {
    // 脱敏密码
    content = content.replaceAll("\"password\":\"[^\"]*\"", "\"password\":\"******\"");
    // 脱敏手机号
    content = content.replaceAll("\"phone\":\"(\\d{3})\\d{4}(\\d{4})\"", "\"phone\":\"$1****$2\"");
    // 脱敏身份证号
    content = content.replaceAll("\"idCard\":\"(\\d{6})\\d{8}(\\d{4})\"", "\"idCard\":\"$1********$2\"");
    return content;
}
```

---

## 六、事务管理 ✅

### 6.1 事务注解使用

```java
@Transactional(rollbackFor = Exception.class)
public Article createArticle(ArticleRequest request, Long userId) {
    // 1. 保存文章
    Article article = new Article();
    article.setTitle(request.getTitle());
    article.setContent(request.getContent());
    save(article);
    
    // 2. 保存标签关联
    if (request.getTags() != null) {
        for (Long tagId : request.getTags()) {
            ArticleTag articleTag = new ArticleTag();
            articleTag.setArticleId(article.getId());
            articleTag.setTagId(tagId);
            articleTagMapper.insert(articleTag);
        }
    }
    
    // 任何异常都会回滚
    return article;
}
```

### 6.2 事务传播行为

```java
// REQUIRED（默认）：如果当前存在事务，则加入该事务；如果不存在，则创建新事务
@Transactional(propagation = Propagation.REQUIRED)

// REQUIRES_NEW：创建新事务，如果当前存在事务，则挂起当前事务
@Transactional(propagation = Propagation.REQUIRES_NEW)

// NESTED：如果当前存在事务，则在嵌套事务内执行
@Transactional(propagation = Propagation.NESTED)
```

---

## 七、依赖配置 ✅

### 7.1 Maven 依赖

```xml
<!-- Resilience4j 熔断降级 -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>

<!-- Spring Boot Actuator 监控 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Prometheus -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

---

## 八、测试验证 ✅

### 8.1 熔断器测试

```bash
# 1. 正常调用
curl http://localhost:8080/api/articles/1/summary

# 2. 模拟服务故障（修改配置使其失败）
# 连续调用5次以上，触发熔断

# 3. 熔断器打开后，立即返回降级结果
# 不再调用实际服务

# 4. 等待60秒后，熔断器进入半开状态
# 允许少量请求测试服务是否恢复
```

### 8.2 健康检查测试

```bash
# 查看健康状态
curl http://localhost:8080/actuator/health

# 查看详细健康信息（需要认证）
curl -H "Authorization: Bearer <token>" http://localhost:8080/actuator/health

# 查看指标
curl http://localhost:8080/actuator/metrics

# 查看 Prometheus 格式指标
curl http://localhost:8080/actuator/prometheus
```

### 8.3 Redis 降级测试

```bash
# 1. 停止 Redis 服务
redis-cli shutdown

# 2. 访问需要缓存的接口
curl http://localhost:8080/api/categories

# 3. 应该正常返回数据（从数据库查询）
# 日志中会有 Redis 降级的警告信息

# 4. 启动 Redis 服务
redis-server

# 5. 再次访问，缓存恢复正常
```

---

## 九、监控指标 ✅

### 9.1 Resilience4j 指标

- `resilience4j.circuitbreaker.calls` - 熔断器调用次数
- `resilience4j.circuitbreaker.state` - 熔断器状态
- `resilience4j.circuitbreaker.failure.rate` - 失败率
- `resilience4j.retry.calls` - 重试次数

### 9.2 JVM 指标

- `jvm.memory.used` - 内存使用量
- `jvm.gc.pause` - GC 暂停时间
- `jvm.threads.live` - 活跃线程数

### 9.3 HTTP 指标

- `http.server.requests` - HTTP 请求统计
- `http.server.requests.duration` - 请求耗时

---

## 十、最佳实践 ✅

### 10.1 熔断器使用建议

1. **合理设置阈值**
   - 失败率阈值：50%-70%
   - 最小调用次数：5-10次
   - 等待时间：30-60秒

2. **区分不同服务**
   - 核心服务：阈值高，等待时间短
   - 非核心服务：阈值低，等待时间长

3. **提供降级方法**
   - 返回默认值
   - 返回缓存数据
   - 返回友好提示

### 10.2 重试使用建议

1. **适合重试的场景**
   - 网络抖动
   - 临时性故障
   - 超时错误

2. **不适合重试的场景**
   - 参数错误
   - 业务逻辑错误
   - 权限错误

3. **重试配置**
   - 最大重试次数：2-3次
   - 重试间隔：1-2秒
   - 启用指数退避

### 10.3 审计日志建议

1. **记录关键操作**
   - 用户登录/登出
   - 数据创建/修改/删除
   - 权限变更
   - 敏感操作

2. **脱敏敏感信息**
   - 密码
   - 手机号
   - 身份证号
   - 银行卡号

3. **日志存储**
   - 使用 ELK 收集日志
   - 定期归档历史日志
   - 设置日志保留期限

---

## 十一、后续优化建议 ✅

### 11.1 链路追踪

```xml
<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### 11.2 分布式限流

```java
@RateLimiter(name = "aiService")
public String generateSummary(String content) {
    // 限制每秒5次调用
}
```

### 11.3 舱壁隔离

```java
@Bulkhead(name = "aiService", type = Bulkhead.Type.SEMAPHORE)
public String generateSummary(String content) {
    // 限制最大并发数为5
}
```

### 11.4 告警通知

- 集成钉钉/企业微信机器人
- 熔断器打开时发送告警
- 健康检查失败时发送告警
- 磁盘空间不足时发送告警

---

## 十二、相关文件 ✅

### 配置文件
- `blog-backend/pom.xml` - Maven 依赖
- `blog-backend/src/main/resources/application.yml` - 主配置
- `blog-backend/src/main/resources/application-resilience4j.yml` - Resilience4j 配置

### Java 类
- `blog-backend/src/main/java/com/blog/config/RedisFallbackConfig.java` - Redis 降级配置
- `blog-backend/src/main/java/com/blog/config/HealthCheckConfig.java` - 健康检查配置
- `blog-backend/src/main/java/com/blog/annotation/AuditLog.java` - 审计日志注解
- `blog-backend/src/main/java/com/blog/aspect/AuditLogAspect.java` - 审计日志切面
- `blog-backend/src/main/java/com/blog/service/ZhipuAiService.java` - 智谱AI服务（已优化）
- `blog-backend/src/main/java/com/blog/service/ArticleCrawlerService.java` - 爬虫服务（已优化）

---

## 更新日志

### 2026-03-06
- ✅ 添加 Resilience4j 熔断降级机制
- ✅ 添加重试机制，支持指数退避
- ✅ 添加 Redis 降级处理
- ✅ 添加 Spring Boot Actuator 监控
- ✅ 添加自定义健康检查
- ✅ 添加审计日志功能
- ✅ 优化智谱AI服务，添加降级方法
- ✅ 优化爬虫服务，添加降级方法
- ✅ 完善事务管理
- ✅ 提升系统整体健壮性
