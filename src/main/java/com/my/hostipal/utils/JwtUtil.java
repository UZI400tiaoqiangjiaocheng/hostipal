package com.my.hostipal.utils;

import java.util.UUID;

public class JwtUtil {
    //使用uuid来生成token
    public static String generateJwt(Long id){
        return UUID.randomUUID().toString();
    }
}
