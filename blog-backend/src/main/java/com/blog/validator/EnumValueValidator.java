package com.blog.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

/**
 * 枚举值校验器
 */
public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {

    private Class<? extends Enum<?>> enumClass;
    private String enumMethod;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        this.enumClass = constraintAnnotation.enumClass();
        this.enumMethod = constraintAnnotation.enumMethod();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null值由@NotNull等注解处理
        }

        try {
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            for (Enum<?> enumConstant : enumConstants) {
                Method method = enumClass.getMethod(enumMethod);
                Object enumValue = method.invoke(enumConstant);
                if (value.equals(enumValue)) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("枚举值校验失败", e);
        }

        return false;
    }
}
