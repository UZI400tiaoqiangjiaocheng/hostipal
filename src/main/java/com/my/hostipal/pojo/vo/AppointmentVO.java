package com.my.hostipal.pojo.vo;

import lombok.Data;

@Data
public class AppointmentVO {
    //支付是否成功
    private String status;
    //预约挂号记录id
    private Long appointmentId;
    //订单号
    private String orderNo;
}
