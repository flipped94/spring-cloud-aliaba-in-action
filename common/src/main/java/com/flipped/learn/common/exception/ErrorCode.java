package com.flipped.learn.common.exception;

import com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants;
import com.flipped.learn.common.exception.enums.ServiceErrorCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误码对象
 * <p>
 * 全局错误码，占用 [0, 999], 参见 {@link GlobalErrorCodeConstants}
 * 业务异常错误码，占用 [1 000, +∞)，参见 {@link ServiceErrorCodeConstants}
 * <p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCode {

    /**
     * 错误码
     */
    private Integer code;
    /**
     * 错误提示
     */
    private String message;

}