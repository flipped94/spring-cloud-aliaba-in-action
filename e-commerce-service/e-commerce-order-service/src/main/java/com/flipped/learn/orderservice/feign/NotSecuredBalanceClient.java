package com.flipped.learn.orderservice.feign;


import com.flipped.learn.common.vo.CommonResponse;
import com.flipped.learn.servicesdk.account.BalanceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * <h1>用户账户服务 Feign 接口</h1>
 */
@FeignClient(contextId = "NotSecuredBalanceClient", value = "e-commerce-account-service")
public interface NotSecuredBalanceClient {

    @RequestMapping(
            value = "/ecommerce-account-service/balance/deduct-balance",
            method = RequestMethod.PUT
    )
    CommonResponse<BalanceInfo> deductBalance(@RequestBody BalanceInfo balanceInfo);
}
