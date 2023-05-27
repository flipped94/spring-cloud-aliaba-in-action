package com.flipped.learn.common.vo;

import com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 通用响应定义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> implements Serializable {
    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 泛型响应数据
     */
    private T Data;

    public CommonResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public static <T> CommonResponse<T> success(T data) {
        CommonResponse<T> commonResponse =
                new CommonResponse<>(GlobalErrorCodeConstants.SUCCESS.getCode(), GlobalErrorCodeConstants.SUCCESS.getMessage());
        commonResponse.setData(data);
        return commonResponse;
    }
}
