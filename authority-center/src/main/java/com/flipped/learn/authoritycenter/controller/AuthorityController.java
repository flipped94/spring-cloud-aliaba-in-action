package com.flipped.learn.authoritycenter.controller;

import com.alibaba.fastjson.JSON;
import com.flipped.learn.authoritycenter.service.IJWTService;
import com.flipped.learn.common.vo.CommonResponse;
import com.flipped.learn.common.vo.JwtToken;
import com.flipped.learn.common.vo.UsernameAndPassword;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * <h1>对外暴露的授权服务接口</h1>
 */
@Slf4j
@RestController
@RequestMapping("/authority")
public class AuthorityController {

    @Resource
    private IJWTService ijwtService;

    /**
     * <h2>从授权中心获取 Token (其实就是登录功能)</h2>
     */
    @PostMapping("/token")
    public CommonResponse<JwtToken> token(@RequestBody UsernameAndPassword usernameAndPassword) throws Exception {
        log.info("request to get token with param: [{}]", JSON.toJSONString(usernameAndPassword));
        String token = ijwtService.generateToken(usernameAndPassword.getUsername(), usernameAndPassword.getPassword());
        JwtToken jwtToken = new JwtToken(token);
        return CommonResponse.success(jwtToken);
    }

    /**
     * <h2>注册用户并返回当前注册用户的 Token, 即通过授权中心创建用户</h2>
     */
    @PostMapping("/register")
    public CommonResponse<JwtToken> register(@RequestBody UsernameAndPassword usernameAndPassword) throws Exception {
        log.info("register user with param: [{}]", JSON.toJSONString(usernameAndPassword));
        String token = ijwtService.registerUserAndGenerateToken(usernameAndPassword);
        JwtToken jwtToken = new JwtToken(token);
        return CommonResponse.success(jwtToken);
    }
}
