package com.blog.validator;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * SQL 注入验证工具
 */
public class SqlInjectionValidator {

    // SQL 注入关键字模式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE|CAST|CONVERT|SCRIPT|JAVASCRIPT|EVAL|EXPRESSION)\\b)" +
            "|(--)|(/\\*)|" +
            "(;\\s*(DROP|DELETE|UPDATE|INSERT|CREATE|ALTER))" +
            "|(\\bOR\\b.*=.*)" +
            "|(\\bAND\\b.*=.*)" +
            "|(\\b1\\s*=\\s*1\\b)" +
            "|(\\b1\\s*=\\s*'1'\\b)" +
            "|(\\bOR\\s+'1'\\s*=\\s*'1'\\b)"
    );

    /**
     * 检查字符串是否包含 SQL 注入风险
     */
    public static boolean containsSqlInjection(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return SQL_INJECTION_PATTERN.matcher(value).find();
    }

    /**
     * 验证并抛出异常
     */
    public static void validate(String value, String fieldName) {
        if (containsSqlInjection(value)) {
            throw new IllegalArgumentException(fieldName + " 包含非法字符");
        }
    }

    /**
     * 清理 SQL 注入风险字符
     */
    public static String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        // 移除 SQL 注释
        value = value.replaceAll("--", "");
        value = value.replaceAll("/\\*.*?\\*/", "");

        // 转义单引号
        value = value.replace("'", "''");

        return value;
    }
}
