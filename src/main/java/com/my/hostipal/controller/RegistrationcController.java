package com.my.hostipal.controller;

import cn.hutool.core.util.StrUtil;
import com.my.hostipal.pojo.entity.Appointment;
import com.my.hostipal.pojo.entity.Result;
import com.my.hostipal.pojo.entity.Schedule;
import com.my.hostipal.pojo.vo.AppointmentVO;
import com.my.hostipal.service.RegistrationcSerrvice;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
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
    public Result notify(HttpServletRequest request) throws Exception {
        String status = registrationcService.payCallback(request);
        AppointmentVO appointmentVO = new AppointmentVO();
        log.info("支付回调结果：{}", status);
        //判断是否支付成功
        if (StrUtil.equals(status, "success")) {
            //扣减挂号数量并生成挂号记录
            String orderNo = request.getParameter("out_trade_no");
            appointmentVO.setOrderNo(orderNo);
            appointmentVO.setStatus(status);
            //测试,以json的形式输出appointmentVO
            log.info("appointmentVO: {}", appointmentVO);
            return Result.success(appointmentVO);
        }
        return Result.error("支付失败");
    }

    //生成挂号记录
    @PostMapping("/addAppointment")
    public Result addAppointment(@RequestBody AppointmentVO appointmentVO) {
        registrationcService.RegistrationRecord(appointmentVO.getOrderNo());
        return Result.success();
    }

    //查询预约挂号记录
    @GetMapping("/queryAppointment")
    public Result queryAppointment() {
        //查询预约挂号记录
        List<Appointment> appointments = registrationcService.queryAppointment();
        return Result.success(appointments);
    }

    //取消预约挂号
    @Update("/cancelAppointment")
    public Result cancelAppointment(@RequestBody AppointmentVO appointmentVO) throws Exception {
        registrationcService.cancelAppointment(appointmentVO);
        return Result.success();
    }
}
