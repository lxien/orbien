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

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValueInListValidator implements ConstraintValidator<ValueInList, Object> {
    private final Set<String> validValues = new HashSet<>();
    private boolean ignoreCase;

    @Override
    public void initialize(ValueInList constraintAnnotation) {
        this.ignoreCase = constraintAnnotation.ignoreCase();

        String[] values = constraintAnnotation.valuesList();

        if (ignoreCase) {
            for (String value : values) {
                validValues.add(value.toLowerCase());
            }
        } else {
            validValues.addAll(Arrays.asList(values));
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        String valueStr = value.toString();
        if (ignoreCase) {
            return validValues.contains(valueStr.toLowerCase());
        } else {
            return validValues.contains(valueStr);
        }
    }
}