package com.my.hostipal.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("doctor")
public class Doctor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long departmentId;
    private String name;
    private String title;
    private String introduction;
    private double registrationFee;
    private Long status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long deleted;
}
