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

package io.github.lxien.orbien.server.web.common.exception;

import io.github.lxien.orbien.server.web.common.message.Ajax;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * todo 全局异常处理器
 * <p>
 * 处理系统中各类异常，包括业务异常、系统异常、参数校验异常等
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BizException.class)
    public Ajax handleBusinessException(BizException e) {
        logger.error("业务异常", e);
        return Ajax.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param e 系统异常
     * @return 错误响应
     */
    @ExceptionHandler(SystemException.class)
    public Ajax handleSystemException(SystemException e) {
        logger.error("系统异常", e);
        return Ajax.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理通用异常
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Throwable.class)
    public Ajax handleException(Throwable e) {
        logger.error("错误", e);
        return Ajax.error(e.getMessage());
    }

    /**
     * 处理 @Valid 注解校验 @RequestBody 参数时的异常
     *
     * @param e 参数校验异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Ajax handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        logger.error("参数校验异常", e);
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = formatBindingResult(bindingResult);
        return Ajax.error(400, errorMessage);
    }

    /**
     * 处理 @Valid 注解校验普通参数时的异常
     *
     * @param e 参数绑定异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public Ajax handleBindException(BindException e) {
        logger.error("参数绑定异常", e);
        BindingResult bindingResult = e.getBindingResult();
        String errorMessage = formatBindingResult(bindingResult);
        return Ajax.error(400, errorMessage);
    }

    /**
     * 处理 @Validated 注解校验单个参数时的异常
     *
     * @param e 约束违反异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Ajax handleConstraintViolationException(ConstraintViolationException e) {
        logger.error("参数校验异常", e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String errorMessage = violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return Ajax.error(400, errorMessage);
    }

    /**
     * 格式化 BindingResult 中的错误信息
     *
     * @param bindingResult 绑定结果
     * @return 格式化后的错误信息
     */
    private String formatBindingResult(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<ObjectError> errors = bindingResult.getAllErrors();
            return errors.stream()
                    .map(error -> {
                        if (error instanceof FieldError fieldError) {
                            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                        } else {
                            return error.getDefaultMessage();
                        }
                    })
                    .collect(Collectors.joining("; "));
        }
        return "未知错误";
    }
}

