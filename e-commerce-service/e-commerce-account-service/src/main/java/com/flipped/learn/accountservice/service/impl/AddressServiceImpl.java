package com.flipped.learn.accountservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.accountservice.converter.AddressConverter;
import com.flipped.learn.accountservice.domain.EcommerceAddress;
import com.flipped.learn.accountservice.repository.EcommerceAddressRepository;
import com.flipped.learn.accountservice.service.IAddressService;
import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.exception.enums.ServiceErrorCodeConstants;
import com.flipped.learn.common.vo.LoginUserInfo;
import com.flipped.learn.serviceconfig.context.UserContextHolder;
import com.flipped.learn.servicesdk.account.AddressInfo;
import com.flipped.learn.servicesdk.common.TableId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <h1>用户地址相关服务接口实现</h1>
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class AddressServiceImpl implements IAddressService {

    @Resource
    private EcommerceAddressRepository addressRepository;

    /**
     * <h2>存储多个地址信息</h2>
     */
    @Override
    public TableId createAddressInfo(AddressInfo addressInfo) {

        // 不能直接从参数中获取用户的 id 信息
        LoginUserInfo loginUserInfo = UserContextHolder.getLoginUserInfo();

        // 将传递的参数转换成实体对象
        List<EcommerceAddress> ecommerceAddresses = addressInfo.getAddressItems().stream()
                .map(AddressConverter.INSTANCE::itemToAddress)
                .collect(Collectors.toList());

        // 保存到数据表并把返回记录的 id 给调用方
        List<EcommerceAddress> savedRecords = addressRepository.saveAll(ecommerceAddresses);
        List<Long> ids = savedRecords.stream()
                .map(EcommerceAddress::getId)
                .collect(Collectors.toList());
        log.info("create address info: [{}], [{}]", loginUserInfo.getId(), JSON.toJSONString(ids));

        return new TableId(ids.stream()
                .map(TableId.Id::new)
                .collect(Collectors.toList()));
    }

    @Override
    public AddressInfo getCurrentAddressInfo() {
        LoginUserInfo loginUserInfo = UserContextHolder.getLoginUserInfo();

        // 根据 userId 查询到用户的地址信息, 再实现转换
        List<EcommerceAddress> ecommerceAddresses = addressRepository.findAllByUserId(loginUserInfo.getId());
        List<AddressInfo.AddressItem> addressItems = ecommerceAddresses.stream()
                .map(AddressConverter.INSTANCE::addressToItem)
                .collect(Collectors.toList());

        return new AddressInfo(loginUserInfo.getId(), addressItems);
    }

    @Override
    public AddressInfo getAddressInfoById(Long id) {

        EcommerceAddress ecommerceAddress = addressRepository.findById(id).orElse(null);
        if (null == ecommerceAddress) {
            throw new BusinessException(ServiceErrorCodeConstants.ADDRESS_EXITS);
        }

        return new AddressInfo(ecommerceAddress.getUserId(),
                Collections.singletonList(AddressConverter.INSTANCE.addressToItem(ecommerceAddress))
        );
    }

    @Override
    public AddressInfo getAddressInfoByTableId(TableId tableId) {

        List<Long> ids = tableId.getIds()
                .stream()
                .map(TableId.Id::getId)
                .collect(Collectors.toList());
        log.info("get address info by table id: [{}]", JSON.toJSONString(ids));

        List<EcommerceAddress> ecommerceAddresses = addressRepository.findAllById(ids);
        if (CollectionUtils.isEmpty(ecommerceAddresses)) {
            return new AddressInfo(-1L, Collections.emptyList());
        }

        List<AddressInfo.AddressItem> addressItems = ecommerceAddresses.stream()
                .map(AddressConverter.INSTANCE::addressToItem)
                .collect(Collectors.toList());

        return new AddressInfo(ecommerceAddresses.get(0).getUserId(), addressItems);
    }
}
