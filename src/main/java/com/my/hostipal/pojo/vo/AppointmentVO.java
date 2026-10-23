package com.my.hostipal.pojo.vo;

import lombok.Data;

@Data
public class AppointmentVO {
    //支付是否成功
    private String status;
    //订单号
    private String orderNo;
}
