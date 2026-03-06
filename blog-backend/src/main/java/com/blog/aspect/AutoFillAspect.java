package com.blog.aspect;

import com.blog.annotation.AutoFill;
import com.blog.context.BaseContext;
import com.blog.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自动填充切面，用于自动填充公共字段
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 切入点：所有添加了 AutoFill 注解的 Mapper 方法
     */
    @Pointcut("execution(* com.blog.mapper.*.*(..)) && @annotation(com.blog.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知，在方法执行前进行公共字段填充
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.debug("开始进行公共字段自动填充");

        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        // 获取方法参数（实体对象）
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return;
        }

        Object entity = args[0];

        // 准备填充的数据
        String now = LocalDateTime.now().format(FORMATTER);
        Long currentId = BaseContext.getCurrentId();

        // 根据操作类型填充字段
        try {
            if (operationType == OperationType.INSERT) {
                // 插入操作：填充创建时间、创建人、更新时间、更新人
                Method setCreatedAt = entity.getClass().getDeclaredMethod("setCreatedAt", String.class);
                Method setUpdatedAt = entity.getClass().getDeclaredMethod("setUpdatedAt", String.class);
                Method setUserId = entity.getClass().getDeclaredMethod("setUserId", Long.class);

                setCreatedAt.invoke(entity, now);
                setUpdatedAt.invoke(entity, now);
                if (currentId != null) {
                    setUserId.invoke(entity, currentId);
                }
            } else if (operationType == OperationType.UPDATE) {
                // 更新操作：填充更新时间、更新人
                Method setUpdatedAt = entity.getClass().getDeclaredMethod("setUpdatedAt", String.class);
                setUpdatedAt.invoke(entity, now);
            }
        } catch (Exception e) {
            log.error("公共字段自动填充失败", e);
        }
    }
}
