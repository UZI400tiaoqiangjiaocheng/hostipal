package com.my.hostipal.utils;

public class RedisConstans {
    //token缓存key
    public static final String TOKEN_KEY = "token:";
    //排班信息缓存key
    public static final String SCHEDULE_KEY = "registration:schedule:";
    //redisson锁key前缀
    public static final String REDIS_LOCK_KEY = "lock:qrcode:";
    //医生缓存key前缀
    public static final String DOCTOR_KEY = "doctor:";
    //科室缓存key前缀
    public static final String DEPARTMENT_KEY = "department:";
    //减少总号数的redisson锁key前缀
    public static final String TOTAL_LOCK_KEY = "lock:total:";
    //用户选择的排班信息缓存key
    public static final String USER_SCHEDULE_KEY = "user:schedule:";
    //订单号 -> 排班信息映射key（支付回调时按订单号反查排班）
    public static final String ORDER_KEY = "order:";
}
