package com.graduate.graidaicommunity.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败 @Valid */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验失败: {}", msg);
        return Result.fail(400, msg);
    }

    /** 参数类型转换错误，字符串转数字失败，如id传字符串 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e){
        log.warn("参数类型错误: {}", e.getMessage());
        return Result.fail(400,"请求参数类型不正确");
    }

    /** 缺少必传参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e){
        log.warn("缺少请求参数: {}", e.getParameterName());
        return Result.fail(400,"缺少参数："+e.getParameterName());
    }

    /** AI接口异常 */
    @ExceptionHandler(AiException.class)
    public Result<?> handleAiException(AiException e) {
        log.error("AI服务异常: {}", e.getMessage());
        return Result.fail(503, "AI服务繁忙，请稍后再试");
    }

    /** 其他未知异常兜底 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后再试");
    }
}
