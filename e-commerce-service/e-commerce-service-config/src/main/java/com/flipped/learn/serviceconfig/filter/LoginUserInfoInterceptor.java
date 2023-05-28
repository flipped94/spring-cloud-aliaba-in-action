package com.flipped.learn.serviceconfig.filter;


import com.flipped.learn.common.constant.CommonConstant;
import com.flipped.learn.common.exception.BusinessException;
import com.flipped.learn.common.exception.enums.GlobalErrorCodeConstants;
import com.flipped.learn.common.util.TokenParseUtil;
import com.flipped.learn.common.vo.LoginUserInfo;
import com.flipped.learn.serviceconfig.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <h1>用户身份统一登录拦截</h1>
 */

@Slf4j
@Component
public class LoginUserInfoInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 部分请求不需要带有身份信息, 即白名单
        if (checkWhiteListUrl(request.getRequestURI())) {
            return true;
        }

        // 先尝试从 http header 里面拿到 token
        String token = request.getHeader(CommonConstant.JWT_USER_INFO_KEY);
        LoginUserInfo loginUserInfo = null;
        try {
            loginUserInfo = TokenParseUtil.parseUserInfoFromToken(token);
        } catch (Exception ex) {
            log.error("parse login user info error: [{}]", ex.getMessage(), ex);
        }

        // 如果程序走到这里, 说明 header 中没有 token 信息
        if (null == loginUserInfo) {
            throw new BusinessException(GlobalErrorCodeConstants.FORBIDDEN);
        }

        log.info("set login user info: [{}]", request.getRequestURI());
        // 设置当前请求上下文, 把用户信息填充进去
        UserContextHolder.setLoginUserInfo(loginUserInfo);

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * <h2>在请求完全结束后调用, 常用于清理资源等工作</h2>
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        if (null != UserContextHolder.getLoginUserInfo()) {
            UserContextHolder.clearLoginUserInfo();
        }
    }

    /**
     * <h2>校验是否是白名单接口</h2>
     * swagger2 接口
     */
    private boolean checkWhiteListUrl(String url) {
        return StringUtils.containsAny(url, "springfox", "swagger", "v2", "webjars", "doc.html");
    }
}
