package com.flipped.learn.goodsservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants;
import com.flipped.learn.common.exception.enums.ServiceErrorCodeConstants;
import com.flipped.learn.goodsservice.constant.GoodsConstant;
import com.flipped.learn.goodsservice.entity.EcommerceGoods;
import com.flipped.learn.goodsservice.repository.EcommerceGoodsRepository;
import com.flipped.learn.goodsservice.service.IGoodsService;
import com.flipped.learn.goodsservice.vo.PageSimpleGoodsInfo;
import com.flipped.learn.servicesdk.common.TableId;
import com.flipped.learn.servicesdk.goods.DeductGoodsInventory;
import com.flipped.learn.servicesdk.goods.GoodsInfo;
import com.flipped.learn.servicesdk.goods.SimpleGoodsInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <h1>商品微服务相关服务功能实现</h1>
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class GoodsServiceImpl implements IGoodsService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EcommerceGoodsRepository goodsRepository;

    @Override
    public List<GoodsInfo> getGoodsInfoByTableId(TableId tableId) {
        // 详细的商品信息, 不能从 redis cache 中去拿
        List<Long> ids = tableId.getIds().stream()
                .map(TableId.Id::getId)
                .collect(Collectors.toList());
        log.info("get goods info by ids: [{}]", JSON.toJSONString(ids));

        List<EcommerceGoods> ecommerceGoods = IterableUtils.toList(goodsRepository.findAllById(ids));

        return ecommerceGoods.stream()
                .map(EcommerceGoods::toGoodsInfo)
                .collect(Collectors.toList());
    }

    @Override
    public PageSimpleGoodsInfo getSimpleGoodsInfoByPage(int page) {

        // 分页不能从 redis cache 中去拿
        if (page <= 1) {
            page = 1;   // 默认是第一页
        }

        // 这里分页的规则(你可以自由修改): 1页10条数据, 按照 id 倒序排列
        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by("id").descending());
        Page<EcommerceGoods> orderPage = goodsRepository.findAll(pageable);

        // 是否还有更多页: 总页数是否大于当前给定的页
        boolean hasMore = orderPage.getTotalPages() > page;

        return new PageSimpleGoodsInfo(
                orderPage.getContent().stream()
                        .map(EcommerceGoods::toSimple)
                        .collect(Collectors.toList()),
                hasMore
        );
    }

    @Override
    public List<SimpleGoodsInfo> getSimpleGoodsInfoByTableId(TableId tableId) {

        // 获取商品的简单信息, 可以从 redis cache 中去拿, 拿不到需要从 DB 中获取并保存到 Redis 里面
        // Redis 中的 KV 都是字符串类型
        List<Object> goodIds = tableId.getIds().stream()
                .map(i -> i.getId().toString())
                .collect(Collectors.toList());

        List<Object> cachedSimpleGoodsInfos = stringRedisTemplate.opsForHash()
                .multiGet(GoodsConstant.ECOMMERCE_GOODS_DICT_KEY, goodIds)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 如果从 Redis 中查到了商品信息, 分两种情况去操作
        if (CollectionUtils.isNotEmpty(cachedSimpleGoodsInfos)) {
            // 1. 如果从缓存中查询出所有需要的 SimpleGoodsInfo
            if (cachedSimpleGoodsInfos.size() == goodIds.size()) {
                log.info("get simple goods info by ids (from cache): [{}]", JSON.toJSONString(goodIds));
                return parseCachedGoodsInfo(cachedSimpleGoodsInfos);
            } else {
                // 2. 一半从数据表中获取 (right), 一半从 redis cache 中获取 (left)
                List<SimpleGoodsInfo> left = parseCachedGoodsInfo(cachedSimpleGoodsInfos);
                // 取差集: 传递进来的参数 - 缓存中查到的 = 缓存中没有的
                Collection<Long> subtractIds = CollectionUtils.subtract(
                        goodIds.stream()
                                .map(g -> Long.valueOf(g.toString())).collect(Collectors.toList()),
                        left.stream()
                                .map(SimpleGoodsInfo::getId).collect(Collectors.toList())
                );
                // 缓存中没有的, 查询数据表并缓存
                List<SimpleGoodsInfo> right = queryGoodsFromDBAndCacheToRedis(
                        new TableId(subtractIds.stream().map(TableId.Id::new).collect(Collectors.toList())
                        ));
                // 合并 left 和 right 并返回
                log.info("get simple goods info by ids (from db and cache): [{}]",
                        JSON.toJSONString(subtractIds));
                return new ArrayList<>(CollectionUtils.union(left, right));
            }
        } else {
            // 从 redis 里面什么都没有查到
            return queryGoodsFromDBAndCacheToRedis(tableId);
        }
    }

    /**
     * <h2>将缓存中的数据反序列化成 Java Pojo 对象</h2>
     */
    private List<SimpleGoodsInfo> parseCachedGoodsInfo(List<Object> cachedSimpleGoodsInfo) {

        return cachedSimpleGoodsInfo.stream()
                .map(s -> JSON.parseObject(s.toString(), SimpleGoodsInfo.class))
                .collect(Collectors.toList());
    }

    /**
     * <h2>从数据表中查询数据, 并缓存到 Redis 中</h2>
     */
    private List<SimpleGoodsInfo> queryGoodsFromDBAndCacheToRedis(TableId tableId) {

        // 从数据表中查询数据并做转换
        List<Long> ids = tableId.getIds().stream()
                .map(TableId.Id::getId).collect(Collectors.toList());
        log.info("get simple goods info by ids (from db): [{}]",
                JSON.toJSONString(ids));
        List<EcommerceGoods> ecommerceGoods = IterableUtils.toList(goodsRepository.findAllById(ids));
        List<SimpleGoodsInfo> result = ecommerceGoods.stream()
                .map(EcommerceGoods::toSimple)
                .collect(Collectors.toList());
        // 将结果缓存, 下一次可以直接从 redis cache 中查询
        log.info("cache goods info: [{}]", JSON.toJSONString(ids));

        Map<String, String> id2JsonObject = new HashMap<>(result.size());
        result.forEach(g -> id2JsonObject.put(
                g.getId().toString(), JSON.toJSONString(g)
        ));
        // 保存到 Redis 中
        stringRedisTemplate.opsForHash().putAll(
                GoodsConstant.ECOMMERCE_GOODS_DICT_KEY, id2JsonObject);
        return result;
    }

    @Override
    public Boolean deductGoodsInventory(List<DeductGoodsInventory> deductGoodsInventories) {
        // 检验下参数是否合法
        deductGoodsInventories.forEach(d -> {
            if (d.getCount() <= 0) {
                throw new BusinessException(ServiceErrorCodeConstants.PURCHASE_GOODS_COUNT_LARGER_THAN_ZERO);
            }
        });

        List<EcommerceGoods> ecommerceGoods = IterableUtils.toList(
                goodsRepository.findAllById(deductGoodsInventories.stream()
                        .map(DeductGoodsInventory::getGoodsId)
                        .collect(Collectors.toList())
                )
        );
        // 根据传递的 goodsIds 查询不到商品对象, 抛异常
        if (CollectionUtils.isEmpty(ecommerceGoods)) {
            throw new BusinessException(ServiceErrorCodeConstants.GOODS_NOT_EXITS);
        }
        // 查询出来的商品数量与传递的不一致, 抛异常
        if (ecommerceGoods.size() != deductGoodsInventories.size()) {
            throw new BusinessException(GlobalErrorCodeConstants.BAD_REQUEST);
        }
        // goodsId -> DeductGoodsInventory
        Map<Long, DeductGoodsInventory> goodsId2Inventory = deductGoodsInventories.stream()
                .collect(Collectors.toMap(DeductGoodsInventory::getGoodsId, Function.identity()));

        // 检查是不是可以扣减库存, 再去扣减库存
        ecommerceGoods.forEach(g -> {
            Long currentInventory = g.getInventory();
            Integer needDeductInventory = goodsId2Inventory.get(g.getId()).getCount();
            if (currentInventory < needDeductInventory) {
                log.error("goods inventory is not enough: [{}], [{}]", currentInventory, needDeductInventory);
                throw new BusinessException(ServiceErrorCodeConstants.GOODS_INVENTORY_NOT_ENOUGH);
            }
            // 扣减库存
            g.setInventory(currentInventory - needDeductInventory);
            log.info("deduct goods inventory: [{}], [{}], [{}]", g.getId(), currentInventory, g.getInventory());
        });

        goodsRepository.saveAll(ecommerceGoods);
        log.info("deduct goods inventory done");

        return true;
    }
}