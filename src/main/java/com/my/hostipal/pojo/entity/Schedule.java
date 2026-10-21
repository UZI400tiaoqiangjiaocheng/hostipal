package com.my.hostipal.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("schedule")
public class Schedule {
    @TableId(type = IdType.AUTO)
    private Long id; //排班id
    private Long doctorId; //医生id
    private Long departmentId; //科室id
    private LocalDate scheduleDate; //出诊日期
    private String timeSlot; //出诊时间段
    private Integer totalQuota; //总号源数
    private Integer usedQuota; //已预约号源数
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //更新时间
    private Long deleted; //删除状态 0-未删除 1-已删除
    private String departmentName; //科室名称
}
