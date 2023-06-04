package com.flipped.learn.orderservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.exception.enums.ServiceErrorCodeConstants;
import com.flipped.learn.orderservice.entity.EcommerceOrder;
import com.flipped.learn.orderservice.feign.AddressClient;
import com.flipped.learn.orderservice.feign.NotSecuredBalanceClient;
import com.flipped.learn.orderservice.feign.NotSecuredGoodsClient;
import com.flipped.learn.orderservice.feign.SecuredGoodsClient;
import com.flipped.learn.orderservice.repository.EcommerceOrderRepository;
import com.flipped.learn.orderservice.service.IOrderService;
import com.flipped.learn.orderservice.source.LogisticsSource;
import com.flipped.learn.orderservice.vo.PageSimpleOrderDetail;
import com.flipped.learn.serviceconfig.context.UserContextHolder;
import com.flipped.learn.servicesdk.account.AddressInfo;
import com.flipped.learn.servicesdk.account.BalanceInfo;
import com.flipped.learn.servicesdk.common.TableId;
import com.flipped.learn.servicesdk.goods.DeductGoodsInventory;
import com.flipped.learn.servicesdk.goods.SimpleGoodsInfo;
import com.flipped.learn.servicesdk.order.LogisticsMessage;
import com.flipped.learn.servicesdk.order.OrderInfo;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h1>订单相关服务接口实现</h1>
 */
@Slf4j
@Service
@EnableBinding(LogisticsSource.class)
public class OrderServiceImpl implements IOrderService {

    /**
     * 表的 dao 接口
     */
    @Resource
    private EcommerceOrderRepository orderRepository;

    /**
     * Feign 客户端
     */
    @Resource
    private AddressClient addressClient;

    @Resource
    private SecuredGoodsClient securedGoodsClient;

    @Resource
    private NotSecuredGoodsClient notSecuredGoodsClient;

    @Resource
    private NotSecuredBalanceClient notSecuredBalanceClient;

    /**
     * SpringCloud Stream 的发射器
     */
    private LogisticsSource logisticsSource;


    /**
     * <h2>创建订单: 这里会涉及到分布式事务</h2>
     * 创建订单会涉及到多个步骤和校验, 当不满足情况时直接抛出异常;
     * 1. 校验请求对象是否合法
     * 2. 创建订单
     * 3. 扣减商品库存
     * 4. 扣减用户余额
     * 5. 发送订单物流消息 SpringCloud Stream + Kafka
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public TableId createOrder(OrderInfo orderInfo) {

        // 获取地址信息
        AddressInfo addressInfo = addressClient.getAddressInfoByTablesId(
                new TableId(Collections.singletonList(
                        new TableId.Id(orderInfo.getUserAddress())))).getData();

        // 1. 校验请求对象是否合法(商品信息不需要校验, 扣减库存会做校验)
        if (CollectionUtils.isEmpty(addressInfo.getAddressItems())) {
            throw new BusinessException(ServiceErrorCodeConstants.ADDRESS_EXITS);
        }

        // 2. 创建订单
        EcommerceOrder newOrder = orderRepository.save(
                new EcommerceOrder(UserContextHolder.getLoginUserInfo().getId(),
                        orderInfo.getUserAddress(),
                        JSON.toJSONString(orderInfo.getOrderItems())
                )
        );
        log.info("create order success: [{}], [{}]", UserContextHolder.getLoginUserInfo().getId(), newOrder.getId());

        // 3. 扣减商品库存
        if (!notSecuredGoodsClient.deductGoodsInventory(
                        orderInfo.getOrderItems()
                                .stream()
                                .map(OrderInfo.OrderItem::toDeductGoodsInventory)
                                .collect(Collectors.toList())
                ).getData()
        ) {
            throw new RuntimeException("deduct goods inventory failure");
        }

        // 4. 扣减用户账户余额
        // 4.1 获取商品信息, 计算总价格
        List<SimpleGoodsInfo> goodsInfos = notSecuredGoodsClient.getSimpleGoodsInfoByTableId(
                new TableId(
                        orderInfo.getOrderItems()
                                .stream()
                                .map(o -> new TableId.Id(o.getGoodsId()))
                                .collect(Collectors.toList())
                )
        ).getData();
        Map<Long, SimpleGoodsInfo> goodsId2GoodsInfo = goodsInfos.stream()
                .collect(Collectors.toMap(SimpleGoodsInfo::getId, Function.identity()));
        long balance = 0;
        for (OrderInfo.OrderItem orderItem : orderInfo.getOrderItems()) {
            balance += (long) goodsId2GoodsInfo.get(orderItem.getGoodsId()).getPrice()
                    * orderItem.getCount();
        }
        assert balance > 0;

        // 4.2 填写总价格, 扣减账户余额
        BalanceInfo balanceInfo = notSecuredBalanceClient.deductBalance(
                new BalanceInfo(UserContextHolder.getLoginUserInfo().getId(), balance)
        ).getData();
        if (null == balanceInfo) {
            throw new RuntimeException("deduct user balance failure");
        }
        log.info("deduct user balance: [{}], [{}]", newOrder.getId(), JSON.toJSONString(balanceInfo));

        // 5. 发送订单物流消息 SpringCloud Stream + Kafka
        LogisticsMessage logisticsMessage = new LogisticsMessage(
                UserContextHolder.getLoginUserInfo().getId(),
                newOrder.getId(),
                orderInfo.getUserAddress(),
                null    // 没有备注信息
        );
        if (!logisticsSource.logisticsOutput().send(MessageBuilder.withPayload(JSON.toJSONString(logisticsMessage)).build())) {
            throw new RuntimeException("send logistics message failure");
        }
        log.info("send create order message to kafka with stream: [{}]", JSON.toJSONString(logisticsMessage));

        // 返回订单 id
        return new TableId(Collections.singletonList(new TableId.Id(newOrder.getId())));
    }

    @Override
    public PageSimpleOrderDetail getSimpleOrderDetailByPage(int page) {
        if (page <= 0) {
            page = 1;   // 默认是第一页
        }
        // 这里分页的规则是: 1页10条数据, 按照 id 倒序排列
        Pageable pageable = PageRequest.of(page - 1, 10,
                Sort.by("id").descending());
        Page<EcommerceOrder> orderPage = orderRepository.findAllByUserId(
                UserContextHolder.getLoginUserInfo().getId(), pageable
        );
        List<EcommerceOrder> orders = orderPage.getContent();

        // 如果是空, 直接返回空数组
        if (CollectionUtils.isEmpty(orders)) {
            return new PageSimpleOrderDetail(Collections.emptyList(), false);
        }

        // 获取当前订单中所有的 goodsId
        Set<Long> goodsIdsInOrders = new HashSet<>();
        orders.forEach(o -> {
            List<DeductGoodsInventory> goodsAndCount = JSON.parseArray(
                    o.getOrderDetail(), DeductGoodsInventory.class
            );
            goodsIdsInOrders.addAll(goodsAndCount.stream()
                    .map(DeductGoodsInventory::getGoodsId)
                    .collect(Collectors.toSet()));
        });

        assert CollectionUtils.isNotEmpty(goodsIdsInOrders);

        // 是否还有更多页: 总页数是否大于当前给定的页
        boolean hasMore = orderPage.getTotalPages() > page;

        // 获取商品信息
        List<SimpleGoodsInfo> goodsInfos = securedGoodsClient.getSimpleGoodsInfoByTableId(
                new TableId(goodsIdsInOrders.stream()
                        .map(TableId.Id::new).collect(Collectors.toList()))
        ).getData();

        // 获取地址信息
        AddressInfo addressInfo = addressClient.getAddressInfoByTablesId(
                new TableId(orders.stream()
                        .map(o -> new TableId.Id(o.getAddressId()))
                        .distinct().collect(Collectors.toList()))
        ).getData();

        // 组装订单中的商品, 地址信息 -> 订单信息
        return new PageSimpleOrderDetail(
                assembleSimpleOrderDetail(orders, goodsInfos, addressInfo),
                hasMore
        );
    }

    /**
     * <h2>组装订单详情</h2>
     */
    private List<PageSimpleOrderDetail.SingleOrderItem> assembleSimpleOrderDetail(
            List<EcommerceOrder> orders, List<SimpleGoodsInfo> goodsInfos,
            AddressInfo addressInfo
    ) {
        // goodsId -> SimpleGoodsInfo
        Map<Long, SimpleGoodsInfo> id2GoodsInfo = goodsInfos.stream()
                .collect(Collectors.toMap(SimpleGoodsInfo::getId, Function.identity()));
        // addressId -> AddressInfo.AddressItem
        Map<Long, AddressInfo.AddressItem> id2AddressItem = addressInfo.getAddressItems()
                .stream().collect(
                        Collectors.toMap(AddressInfo.AddressItem::getId, Function.identity())
                );

        List<PageSimpleOrderDetail.SingleOrderItem> result = new ArrayList<>(orders.size());
        orders.forEach(o -> {

            PageSimpleOrderDetail.SingleOrderItem orderItem = new PageSimpleOrderDetail.SingleOrderItem();
            orderItem.setId(o.getId());
            orderItem.setUserAddress(id2AddressItem.getOrDefault(o.getAddressId(),
                    new AddressInfo.AddressItem(-1L)).toUserAddress());
            orderItem.setGoodsItems(buildOrderGoodsItem(o, id2GoodsInfo));

            result.add(orderItem);
        });

        return result;
    }

    /**
     * <h2>构造订单中的商品信息</h2>
     */
    private List<PageSimpleOrderDetail.SingleOrderGoodsItem> buildOrderGoodsItem(
            EcommerceOrder order, Map<Long, SimpleGoodsInfo> id2GoodsInfo
    ) {

        List<PageSimpleOrderDetail.SingleOrderGoodsItem> goodsItems = new ArrayList<>();
        List<DeductGoodsInventory> goodsAndCount = JSON.parseArray(
                order.getOrderDetail(), DeductGoodsInventory.class
        );

        goodsAndCount.forEach(gc -> {

            PageSimpleOrderDetail.SingleOrderGoodsItem goodsItem =
                    new PageSimpleOrderDetail.SingleOrderGoodsItem();
            goodsItem.setCount(gc.getCount());
            goodsItem.setSimpleGoodsInfo(id2GoodsInfo.getOrDefault(gc.getGoodsId(),
                    new SimpleGoodsInfo(-1L)));

            goodsItems.add(goodsItem);
        });

        return goodsItems;
    }
}
