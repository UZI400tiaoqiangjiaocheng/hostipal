package com.my.hostipal.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long id; //预约挂号记录id
    private Long scheduleId; //排班id
    private Long patientId; //患者id
    private String patientName; //患者姓名
    private String patientPhone; //患者手机号
    private String idCard; //患者身份证号
    private String status; //预约状态
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //更新时间
    private Long deleted; //删除状态 0-未删除 1-已删除
}
