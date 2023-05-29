package com.flipped.learn.goodsservice.converter;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.goodsservice.entity.EcommerceGoods;
import com.flipped.learn.goodsservice.enums.BrandCategory;
import com.flipped.learn.goodsservice.enums.GoodsCategory;
import com.flipped.learn.goodsservice.enums.GoodsStatus;
import com.flipped.learn.servicesdk.goods.GoodsInfo;
import com.flipped.learn.servicesdk.goods.SimpleGoodsInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface GoodsConverter {

    GoodsConverter INSTANCE = Mappers.getMapper(GoodsConverter.class);

    /**
     * <h2>将 GoodsInfo 转成实体对象</h2>
     */
    @Mappings({
            @Mapping(source = "goodsCategory", target = "goodsCategory", qualifiedByName = "setGoodsCategory"),
            @Mapping(source = "brandCategory", target = "brandCategory", qualifiedByName = "setBrandCategory"),
            @Mapping(source = "goodsProperty", target = "goodsProperty", qualifiedByName = "setGoodsProperty"),
            @Mapping(source = "goodsStatus", target = "goodsStatus", qualifiedByName = "setGoodsStatus"),
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "createTime", ignore = true),
            @Mapping(target = "updateTime", ignore = true)
    })
    EcommerceGoods goodsInfoToEcommerceGoods(GoodsInfo goodsInfo);

    @Named("setGoodsCategory")
    default GoodsCategory setGoodsCategory(String goodsCategory) {
        return GoodsCategory.of(goodsCategory);
    }

    @Named("setBrandCategory")
    default BrandCategory setBrandCategory(String brandCategory) {
        return BrandCategory.of(brandCategory);
    }

    @Named("setGoodsProperty")
    default String setBrandCategory(GoodsInfo.GoodsProperty goodsProperty) {
        return JSON.toJSONString(goodsProperty);
    }

    @Named("setGoodsStatus")
    default GoodsStatus setGoodsStatus(Integer goodsStatus) {
        return GoodsStatus.of(goodsStatus);
    }

    /**
     * <h2>将实体对象转成 GoodsInfo 对象</h2>
     */
    @Mappings({
            @Mapping(source = "goodsCategory", target = "goodsCategory", qualifiedByName = "convertGoodsCategoryEnumToString"),
            @Mapping(source = "brandCategory", target = "brandCategory", qualifiedByName = "convertBrandCategoryEnumToString"),
            @Mapping(source = "goodsStatus", target = "goodsStatus", qualifiedByName = "convertStatusEnumToInteger"),
            @Mapping(source = "goodsProperty", target = "goodsProperty", qualifiedByName = "convertGoodsPropertyJsonToObject")
    })
    GoodsInfo ecommerceGoodsToGoodsInfo(EcommerceGoods ecommerceGoods);


    @Named("convertGoodsCategoryEnumToString")
    default String convertGoodsCategoryEnumToString(GoodsCategory goodsCategory) {
        return goodsCategory.getCode();
    }

    @Named("convertBrandCategoryEnumToString")
    default String convertBrandCategoryEnumToString(BrandCategory brandCategory) {
        return brandCategory.getCode();
    }

    @Named("convertGoodsPropertyJsonToObject")
    default GoodsInfo.GoodsProperty convertGoodsPropertyJsonToObject(String goodsProperty) {
        return JSON.parseObject(goodsProperty, GoodsInfo.GoodsProperty.class);
    }

    @Named("convertStatusEnumToInteger")
    default Integer convertStatusEnumToInteger(GoodsStatus status) {
        return status.getStatus();
    }

    /**
     * <h2>将实体对象转成 SimpleGoodsInfo 对象</h2>
     */
    SimpleGoodsInfo ecommerceGoodsToSimpleGoodsInfo(EcommerceGoods ecommerceGoods);
}
