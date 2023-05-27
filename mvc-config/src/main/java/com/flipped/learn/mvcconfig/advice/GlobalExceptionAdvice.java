package com.flipped.learn.mvcconfig.advice;

import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.vo.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

import static com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    /**
     * 业务异常
     *
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(value = BusinessException.class)
    public CommonResponse<?> handleBusinessException(HttpServletRequest req, BusinessException ex) {
        CommonResponse<?> response = new CommonResponse<>(ex.getCode(), ex.getMessage());
        log.error("service has error: [{}]", ex.getMessage());
        return response;
    }

    /**
     * 参数绑定异常
     *
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(value = BindException.class)
    public CommonResponse<?> handleBindException(HttpServletRequest req, BindException ex) {
        FieldError fieldError = ex.getFieldError();
        assert fieldError != null; // 断言，避免告警
        CommonResponse<?> response = new CommonResponse<>(BAD_REQUEST.getCode(), String.format("请求参数不正确:%s", fieldError.getDefaultMessage()));
        log.warn("params error: [{}]", response.getMessage());
        return response;
    }

    /**
     * 兜底异常
     *
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public CommonResponse<?> handleException(HttpServletRequest req, Exception ex) {
        CommonResponse<?> response = new CommonResponse<>(INTERNAL_SERVER_ERROR.getCode(), INTERNAL_SERVER_ERROR.getMessage());
        log.error("Has error: [{}]", ex.getMessage());
        return response;
    }
}
