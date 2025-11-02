package com.vim.common.handler;


import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.util.SaResult;
import com.vim.common.exception.VimBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Objects;

/**
 * 全局异常处理器
 *
 * @author ruoyi
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 权限校验异常
     */
    @ExceptionHandler(NotLoginException.class)
    public SaResult handleAccessDeniedException(NotLoginException e) {
        String requestURI = SaHolder.getRequest().getRequestPath();
        log.error("请求地址'{}' 未登录的错误", requestURI, e);
        SaResult saResult = new SaResult();
        saResult.setCode(401);
        saResult.setMsg(e.getMessage());
        return saResult;
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public SaResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String requestURI = SaHolder.getRequest().getRequestPath();
        log.error("请求地址不支持'{}'请求 {}", requestURI, e.getMethod());
        return SaResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(VimBaseException.class)
    public SaResult handleServiceException(VimBaseException e) {
        String requestURI = SaHolder.getRequest().getRequestPath();
        log.error("请求地址'{}',发生业务异常", requestURI, e);
        return SaResult.error(e.getMessage());
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public SaResult handleRuntimeException(RuntimeException e) {
        String requestURI = SaHolder.getRequest().getRequestPath();
        log.error("请求地址'{}',发生未知异常", requestURI, e);
        return SaResult.error(e.getMessage());
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public SaResult handleException(Exception e) {
        String requestURI = SaHolder.getRequest().getRequestPath();
        log.error("请求地址'{}',发生系统异常", requestURI, e);
        return SaResult.error(e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public SaResult handleBindException(BindException e) {
        log.error("自定义验证异常", e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return SaResult.error(message);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = Objects.requireNonNull(e.getBindingResult().getFieldError()).getDefaultMessage();
        return SaResult.error(message);
    }

}
