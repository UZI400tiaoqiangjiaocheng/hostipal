package com.my.hostipal.controller;

import com.my.hostipal.pojo.dto.UserDTO;
import com.my.hostipal.pojo.entity.Result;

import com.my.hostipal.pojo.entity.User;
import com.my.hostipal.service.Userservice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Autowired
    private Userservice userService;

    @PostMapping("/login")
    public Result login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        userService.register(user);
        return Result.success();
    }
}
