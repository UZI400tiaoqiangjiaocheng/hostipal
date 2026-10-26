package com.my.hostipal.service;

import com.my.hostipal.pojo.entity.Appointment;
import com.my.hostipal.pojo.entity.Schedule;
import com.my.hostipal.pojo.vo.AppointmentVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface RegistrationcSerrvice {
    List<Schedule> queryDepartment();

    //生成支付的二维码
    String generateQrCode(Schedule schedule) throws Exception;

    String payCallback(HttpServletRequest request) throws Exception;

    //生成挂号记录
    void RegistrationRecord(String orderNo);

    //查询预约挂号记录
    List<Appointment> queryAppointment();

    //取消预约挂号并完成退款
    void cancelAppointment(AppointmentVO appointmentVO);
}
