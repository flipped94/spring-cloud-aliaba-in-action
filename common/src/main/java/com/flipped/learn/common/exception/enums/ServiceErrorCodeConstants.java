package com.flipped.learn.common.exception.enums;

import com.flipped.learn.common.exception.ErrorCode;

/**
 * 业务异常错误码
 */
public interface ServiceErrorCodeConstants {

    ErrorCode ADDRESS_EXITS = new ErrorCode(3000, "地址不存在");
    ErrorCode BALANCE_NOT_ENOUGH = new ErrorCode(3001, "余额不足");
    ErrorCode PURCHASE_GOODS_COUNT_LARGER_THAN_ZERO = new ErrorCode(4000, "购物须数量大于零");
    ErrorCode GOODS_NOT_EXITS = new ErrorCode(4001, "商品不存在");
    ErrorCode GOODS_INVENTORY_NOT_ENOUGH = new ErrorCode(4002, "商品库存不足");
    ErrorCode GOODS_INVENTORY_NOT_MATCH = new ErrorCode(4003, "商品库存数量不匹配");
}