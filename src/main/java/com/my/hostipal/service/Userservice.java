package com.my.hostipal.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.my.hostipal.pojo.dto.UserDTO;
import com.my.hostipal.pojo.entity.Result;
import com.my.hostipal.pojo.entity.User;

public interface Userservice extends IService<User> {
    //登录
    Result login(UserDTO userDTO);

    //注册
    void register(User user);
}
