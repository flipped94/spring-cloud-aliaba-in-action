package com.flipped.learn.common.exception.enums;

import com.flipped.learn.common.exception.ErrorCode;

/**
 * 业务异常错误码
 */
public interface ServiceErrorCodeConstants {

    ErrorCode ADDRESS_EXITS = new ErrorCode(3000, "地址不存在");
    ErrorCode BALANCE_NOT_ENOUGH = new ErrorCode(3001, "余额不足");
}