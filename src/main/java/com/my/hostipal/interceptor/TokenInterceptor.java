package com.my.hostipal.interceptor;

import com.my.hostipal.utils.CurrentHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.my.hostipal.utils.RedisConstans.TOKEN_KEY;

@Slf4j
@Component
public class TokenInterceptor implements HandlerInterceptor {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //获取请求路径
        String requestURI = request.getRequestURI();
        log.info("拦截器收到请求: method={}, uri={}", request.getMethod(), requestURI);

        //放行跨域预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //判断请求路径是否包含/login
        if(requestURI.contains("/login") || requestURI.contains("/register") || requestURI.contains("/notify")){
            log.info("请求路径包含/login或/register或/notify路径，放行");
            return true;
        }

        //获取请求头中的token
        String token = request.getHeader("token");

        //判断token是否为空
        if(token == null || token.isEmpty()){
            log.info("令牌为空，响应401");
            response.setStatus(401);
            return false;
        }

        //获取用户id
        Integer id = Integer.parseInt(request.getHeader("userId"));
        CurrentHolder.setCurrentId(id);

        //如果token不为空校验令牌是否有效
        try {
            //从redis中获取token
            Object redisToken = redisTemplate.opsForValue().get(TOKEN_KEY + id);
            if(redisToken == null||!redisToken.toString().equals(token)){
                //返回一个错误
                log.info("令牌校验失败，响应401");
                response.setStatus(401);
                return false;
            }

            //判断token是否与redis中的token一致

        } catch (Exception e) {
            log.error("令牌校验失败，响应401", e);
            response.setStatus(401);
            return false;
        }

        //如果验通过，放行
        return true;
    }
}
