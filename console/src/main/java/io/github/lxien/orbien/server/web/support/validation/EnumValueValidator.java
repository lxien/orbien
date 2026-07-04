/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.github.lxien.orbien.server.web.support.validation;

import io.github.lxien.orbien.server.web.common.exception.SystemException;
import org.springframework.util.ReflectionUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class EnumValueValidator implements ConstraintValidator<EnumValue, Object> {
    private final Set<Object> validValues = new HashSet<>();
    private boolean ignoreCase;

    @Override
    public void initialize(EnumValue constraintAnnotation) {
        Class<? extends Enum<?>> enumClass = constraintAnnotation.enumClass();
        this.ignoreCase = constraintAnnotation.ignoreCase();

        Enum<?>[] enums = enumClass.getEnumConstants();

        Field codeField = ReflectionUtils.findField(enumClass, "code");
        if (codeField != null) {
            ReflectionUtils.makeAccessible(codeField);
            for (Enum<?> e : enums) {
                try {
                    Object codeValue = ReflectionUtils.getField(codeField, e);
                    validValues.add(codeValue);
                } catch (Exception ex) {
                    throw new SystemException("解析枚举code字段失败", ex);
                }
            }
        } else {
            throw new SystemException("枚举类缺少code字段");
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        for (Object validValue : validValues) {
            if (ignoreCase) {
                if (validValue.toString().equalsIgnoreCase(value.toString())) {
                    return true;
                }
            } else {
                if (validValue.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }
}