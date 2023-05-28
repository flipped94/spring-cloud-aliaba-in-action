package com.flipped.learn.accountservice.repository;

import com.flipped.learn.accountservice.domain.EcommerceBalance;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <h1>EcommerceBalance Dao 接口定义</h1>
 * */
public interface EcommerceBalanceRepository extends JpaRepository<EcommerceBalance, Long> {

    /** 根据 userId 查询 EcommerceBalance 对象 */
    EcommerceBalance findByUserId(Long userId);
}
