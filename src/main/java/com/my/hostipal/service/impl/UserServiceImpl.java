package com.my.hostipal.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.my.hostipal.mapper.UserMapper;
import com.my.hostipal.pojo.dto.UserDTO;
import com.my.hostipal.pojo.entity.Result;
import com.my.hostipal.pojo.entity.User;
import com.my.hostipal.pojo.vo.UserVO;
import com.my.hostipal.service.Userservice;
import com.my.hostipal.utils.CurrentHolder;
import com.my.hostipal.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.my.hostipal.utils.RedisConstans.TOKEN_KEY;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements Userservice {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result login(UserDTO userDTO) {
        //从数据库查询用户
        UserVO userVO = userMapper.selectByUsername(userDTO.getUsername());
        //判断用户是否存在
        if (userVO == null || !userVO.getPassword().equals(userDTO.getPassword())) {
            return Result.error("用户或密码错误");
        }

        //获取token
        String token = JwtUtil.generateJwt(userVO.getId());
        //将token存入redis
        redisTemplate.opsForValue().set(
                TOKEN_KEY + userVO.getId(),
                token,
                60L,
                TimeUnit.MINUTES
        );
        //将token存入userVO
        userVO.setToken(token);
        return Result.success(userVO);
    }

    //用户注册
    @Override
    public void register(User user) {
        //将用户存入数据库
        save(user);
    }
}
