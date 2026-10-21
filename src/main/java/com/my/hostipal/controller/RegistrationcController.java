package com.my.hostipal.controller;

import cn.hutool.core.util.StrUtil;
import com.my.hostipal.pojo.entity.Result;
import com.my.hostipal.pojo.entity.Schedule;
import com.my.hostipal.service.RegistrationcSerrvice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/registration")
public class RegistrationcController {

    @Autowired
    private RegistrationcSerrvice registrationcService;

    //点击预约查询科室信息
    @GetMapping("/queryDepartment")
    public Result queryDepartment() {
        //查询排班信息
        List<Schedule> schedules = registrationcService.queryDepartment();
        return Result.success(schedules);
    }

    //点击预约生成挂号订单
    @PostMapping("/generateOrder")
    public Result generateOrder(@RequestBody Schedule schedule) throws Exception {
        //生成支付的二维码
        String qrCode = registrationcService.generateQrCode(schedule);
        return Result.success(qrCode);
    }

    //接收支付成功的回调
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) throws Exception {
        String result = registrationcService.payCallback(request);
        log.info("支付回调结果：{}", result);
        //判断是否支付成功
        if (StrUtil.equals(result, "success")) {
            //扣减挂号数量并生成挂号记录
            registrationcService.RegistrationRecord();
        }
        return result;
    }
}
