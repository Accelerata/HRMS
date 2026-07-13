package com.hrms.common.exception;

import com.hrms.result.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理切面
 * 统一将各类异常包装为 Result 返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ═══════════════ 业务异常 ═══════════════

    /** 自定义业务异常 */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBaseException(BaseException e, HttpServletResponse response) {
        log.warn("业务异常: {}", e.getMessage());
        if (e.getCode() == 401) response.setStatus(401);
        else if (e.getCode() == 403) response.setStatus(403);
        else if (e.getCode() == 404) response.setStatus(404);
        return Result.error(e.getMessage());
    }

    // ═══════════════ 参数校验 ═══════════════

    /** @Valid 校验失败 (RequestBody) */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", msg);
        return Result.error(msg);
    }

    /** @Validated 校验失败 (Query/Path param) */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数校验失败: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /** 缺少必填参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少参数: {}", e.getParameterName());
        return Result.error("缺少必要参数: " + e.getParameterName());
    }

    /** 参数类型转换错误 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型错误: {} = {}", e.getName(), e.getValue());
        return Result.error("参数类型错误: " + e.getName());
    }

    /** JSON 反序列化失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.error("请求体格式错误");
    }

    // ═══════════════ 请求方式 / 路径 ═══════════════

    /** 不支持的 HTTP 方法 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("不支持的请求方法: {}", e.getMethod());
        return Result.error("不支持的请求方法: " + e.getMethod());
    }

    /** 404 (需配置 spring.mvc.throw-exception-if-no-handler-found=true) */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        log.warn("路径不存在: {}", e.getRequestURL());
        return Result.error("请求路径不存在");
    }

    // ═══════════════ 数据库异常 ═══════════════

    /** MyBatis / SQL 异常 */
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleDataAccess(DataAccessException e) {
        log.error("数据库异常", e);
        return Result.error("数据操作异常，请稍后重试");
    }

    // ═══════════════ 兜底 ═══════════════

    /** 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("服务器内部错误，请联系管理员");
    }

}
