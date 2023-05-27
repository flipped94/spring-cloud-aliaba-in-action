package com.flipped.learn.authoritycenter.service.impl;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.authoritycenter.constant.AuthorityConstant;
import com.flipped.learn.authoritycenter.entity.EcommerceUser;
import com.flipped.learn.authoritycenter.repository.EcommerceUserRepository;
import com.flipped.learn.authoritycenter.service.IJWTService;
import com.flipped.learn.common.constant.CommonConstant;
import com.flipped.learn.common.vo.LoginUserInfo;
import com.flipped.learn.common.vo.UsernameAndPassword;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * <h1>JWT 相关服务接口实现</h1>
 */
@Slf4j
@Service
public class JWTServiceImpl implements IJWTService {

    @Resource
    private EcommerceUserRepository ecommerceUserRepository;

    @Override
    public String generateToken(String username, String password) throws Exception {
        return generateToken(username, password, 0);
    }

    @Override
    public String generateToken(String username, String password, int expire) throws Exception {
        // 首先需要验证用户是否能够通过授权校验, 即输入的用户名和密码能否匹配数据表记录
        EcommerceUser ecommerceUser = ecommerceUserRepository.findByUsernameAndPassword(username, password);
        if (null == ecommerceUser) {
            log.error("can not find user: [{}], [{}]", username, password);
            return null;
        }

        // Token 中塞入对象, 即 JWT 中存储的信息, 后端拿到这些信息就可以知道是哪个用户在操作
        LoginUserInfo loginUserInfo = new LoginUserInfo(ecommerceUser.getId(), ecommerceUser.getUsername());

        if (expire <= 0) {
            expire = AuthorityConstant.DEFAULT_EXPIRE_DAY;
        }

        // 计算超时时间
        ZonedDateTime zdt = LocalDate.now().plus(expire, ChronoUnit.DAYS)
                .atStartOfDay(ZoneId.systemDefault());
        Date expireDate = Date.from(zdt.toInstant());

        return Jwts.builder()
                // jwt payload --> KV
                .claim(CommonConstant.JWT_USER_INFO_KEY, JSON.toJSONString(loginUserInfo))
                // jwt id
                .setId(UUID.randomUUID().toString())
                // jwt 过期时间
                .setExpiration(expireDate)
                // jwt 签名 --> 加密
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    @Override
    public String registerUserAndGenerateToken(UsernameAndPassword usernameAndPassword) throws Exception {
        // 先去校验用户名是否存在, 如果存在, 不能重复注册
        EcommerceUser oldUser = ecommerceUserRepository.findByUsername(usernameAndPassword.getUsername());
        if (null != oldUser) {
            log.error("username is registered: [{}]", oldUser.getUsername());
            return null;
        }

        EcommerceUser ecommerceUser = new EcommerceUser();
        ecommerceUser.setUsername(usernameAndPassword.getUsername());
        ecommerceUser.setPassword(usernameAndPassword.getPassword());   // MD5 编码以后
        ecommerceUser.setExtraInfo("{}");

        // 注册一个新用户, 写一条记录到数据表中
        ecommerceUser = ecommerceUserRepository.save(ecommerceUser);
        log.info("register user success: [{}], [{}]", ecommerceUser.getUsername(), ecommerceUser.getId());

        // 生成 token 并返回
        return generateToken(ecommerceUser.getUsername(), ecommerceUser.getPassword());
    }

    /**
     * <h2>根据本地存储的私钥获取到 PrivateKey 对象</h2>
     */
    private PrivateKey getPrivateKey() throws Exception {
        PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(
                Base64.getDecoder().decode(AuthorityConstant.PRIVATE_KEY));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(priPKCS8);

    }
}