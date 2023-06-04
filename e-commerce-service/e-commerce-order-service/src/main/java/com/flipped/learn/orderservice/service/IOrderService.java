package com.flipped.learn.orderservice.service;


import com.flipped.learn.orderservice.vo.PageSimpleOrderDetail;
import com.flipped.learn.servicesdk.common.TableId;
import com.flipped.learn.servicesdk.order.OrderInfo;

/**
 * <h1>订单相关服务接口定义</h1>
 */
public interface IOrderService {

    /**
     * <h2>下单(分布式事务): 创建订单 -> 扣减库存 -> 扣减余额 -> 创建物流信息(Stream + Kafka)</h2>
     */
    TableId createOrder(OrderInfo orderInfo);

    /**
     * <h2>获取当前用户的订单信息: 带有分页</h2>
     */
    PageSimpleOrderDetail getSimpleOrderDetailByPage(int page);
}
