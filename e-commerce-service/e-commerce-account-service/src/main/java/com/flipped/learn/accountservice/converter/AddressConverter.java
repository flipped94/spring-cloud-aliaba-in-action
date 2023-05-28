package com.flipped.learn.accountservice.converter;

import com.flipped.learn.accountservice.domain.EcommerceAddress;
import com.flipped.learn.serviceconfig.context.UserContextHolder;
import com.flipped.learn.servicesdk.account.AddressInfo;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AddressConverter {

    AddressConverter INSTANCE = Mappers.getMapper(AddressConverter.class);


    @Mappings({
            @Mapping(target = "userId", ignore = true),
            @Mapping(target = "createTime",ignore = true),
            @Mapping(target = "updateTime",ignore = true)
    })
    EcommerceAddress itemToAddress(AddressInfo.AddressItem addressItem);

    AddressInfo.AddressItem addressToItem(EcommerceAddress address);

    @AfterMapping
    default void setUserId(@MappingTarget EcommerceAddress ecommerceAddress) {
        ecommerceAddress.setUserId(UserContextHolder.getLoginUserInfo().getId());
    }
}
