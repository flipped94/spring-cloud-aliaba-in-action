package com.flipped.learn.accountservice.service.impl;

import com.flipped.learn.accountservice.domain.EcommerceBalance;
import com.flipped.learn.accountservice.repository.EcommerceBalanceRepository;
import com.flipped.learn.accountservice.service.IBalanceService;
import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.exception.enums.ServiceErrorCodeConstants;
import com.flipped.learn.common.vo.LoginUserInfo;
import com.flipped.learn.serviceconfig.context.UserContextHolder;
import com.flipped.learn.servicesdk.account.BalanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * <h1>用于余额相关服务接口实现</h1>
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class BalanceServiceImpl implements IBalanceService {

    @Resource
    private EcommerceBalanceRepository balanceRepository;

    @Override
    public BalanceInfo getCurrentUserBalanceInfo() {

        LoginUserInfo loginUserInfo = UserContextHolder.getLoginUserInfo();
        BalanceInfo balanceInfo = new BalanceInfo(loginUserInfo.getId(), 0L);

        EcommerceBalance ecommerceBalance = balanceRepository.findByUserId(loginUserInfo.getId());
        if (null != ecommerceBalance) {
            balanceInfo.setBalance(ecommerceBalance.getBalance());
        } else {
            // 如果还没有用户余额记录, 这里创建出来，余额设定为0即可
            EcommerceBalance newBalance = new EcommerceBalance();
            newBalance.setUserId(loginUserInfo.getId());
            newBalance.setBalance(0L);
            log.info("init user balance record: [{}]", balanceRepository.save(newBalance).getId());
        }

        return balanceInfo;
    }

    @Override
    public BalanceInfo deductBalance(BalanceInfo balanceInfo) {

        LoginUserInfo loginUserInfo = UserContextHolder.getLoginUserInfo();

        // 扣减用户余额的一个基本原则: 扣减额 <= 当前用户余额
        EcommerceBalance ecommerceBalance = balanceRepository.findByUserId(loginUserInfo.getId());
        if (null == ecommerceBalance || ecommerceBalance.getBalance() - balanceInfo.getBalance() < 0) {
            throw new BusinessException(ServiceErrorCodeConstants.BALANCE_NOT_ENOUGH);
        }

        Long sourceBalance = ecommerceBalance.getBalance();
        ecommerceBalance.setBalance(ecommerceBalance.getBalance() - balanceInfo.getBalance());
        log.info("deduct balance: [{}], [{}], [{}]",
                balanceRepository.save(ecommerceBalance).getId(), sourceBalance,
                balanceInfo.getBalance());

        return new BalanceInfo(ecommerceBalance.getUserId(), ecommerceBalance.getBalance());
    }
}
