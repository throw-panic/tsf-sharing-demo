package com.unionpay.zuulFilter.zuuldemo.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author MyPC
 */
@Component
public class AuthZuulFilter extends ZuulFilter {        // 继承 ZuulFilter

    /**
     * 外部请求 Header - token 认证令牌
     */
    private static final String DEFAULT_TOKEN_HEADER_NAME = "token";
    /**
     * 转发请求 Header - userId 用户编号
     */
    private static final String DEFAULT_HEADER_NAME = "user-id";

    /**
     * token 和 userId 的映射
     */
    private static Map<String, Integer> TOKENS = new HashMap<String, Integer>();

    static {
        TOKENS.put("rrmai", 1);     // todo: 塞一个token
    }

    public String filterType() {
        return FilterConstants.PRE_TYPE; // 前置过滤器
    }

    public int filterOrder() {
        return 0;
    }

    public boolean shouldFilter() {
        return true; // 需要过滤
    }

    public Object run() throws ZuulException {
        // 获取当前请求上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        // 获得 token
        HttpServletRequest request = ctx.getRequest();
        String token = request.getHeader(DEFAULT_TOKEN_HEADER_NAME);

        // todo: 策略(1) 如果没有 token，则不进行认证。因为可能是无需认证的 API 接口
        if (!StringUtils.hasText(token)) {
            return null;
        }

        // 进行认证
        // todo: 策略(2) 如果有 token，进行认证。
        Integer userId = TOKENS.get(token);

        // 通过 token 获取不到 userId，说明认证不通过
        if (userId == null) {
            ctx.setSendZuulResponse(false);
            // todo: 认证不通过 返回 401 （Unauthorized 未授权）
            ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value()); // 响应 401 状态码
            return null;
        }

        // TODO： 策略（3） 认证通过，将 userId 添加到 Header 中
        ctx.getZuulRequestHeaders().put(DEFAULT_HEADER_NAME, String.valueOf(userId));
        return null;
    }

}

/**
 *  todo：
 *      postman 测一下这个接口 即可 http://127.0.0.1:8888/archives
 *      header 塞一个token，是否有差异，filter 是否生效。
 */
