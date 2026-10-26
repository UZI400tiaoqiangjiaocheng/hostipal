package com.my.hostipal.service.impl;

import cn.hutool.core.lang.UUID;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.alipay.easysdk.payment.common.models.AlipayTradeRefundResponse;
import com.my.hostipal.mapper.RegistrationcMapper;
import com.my.hostipal.mapper.UserMapper;
import com.my.hostipal.pojo.entity.*;
import com.my.hostipal.pojo.vo.AppointmentVO;
import com.my.hostipal.service.RegistrationcSerrvice;
import com.my.hostipal.utils.CurrentHolder;
import com.my.hostipal.utils.PayUtil;
import com.my.hostipal.utils.RedisConstans;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RegistrationcServiceImpl implements RegistrationcSerrvice {

    @Autowired
    private RegistrationcMapper registrationcMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private Config config;

    @Autowired
    private UserMapper userMapper;

    @Override
    public List<Schedule> queryDepartment() {
        List<Schedule> scheduleList = new ArrayList<>();
        //先从redis查询已缓存的排班信息
        Map<Object, Object> scheduleCache = redisTemplate.opsForHash().entries(RedisConstans.SCHEDULE_KEY);
        if (scheduleCache != null) {
            for (Object value : scheduleCache.values()) {
                if (value instanceof Schedule schedule) {
                    scheduleList.add(schedule);
                }
            }
        }
        //获取当前几点
        LocalDateTime currentTime = LocalDateTime.now();
        //获取当前日期
        LocalDate currentDate = LocalDate.now();

        //如果redis没有排班信息，从数据库查询排班信息
        if (scheduleList.isEmpty()) {
            //从数据库查询从今天开始到5天后的排班信息
            scheduleList = registrationcMapper.selectScheduleList(currentDate, currentDate.plusDays(5));
            if (scheduleList == null) {
                scheduleList = new ArrayList<>();
            }
            //并将排班信息缓存到redis hash并设置过期时间为1天
            Map<Long, Schedule> scheduleMap = scheduleList.stream()
                    .collect(Collectors.toMap(Schedule::getId, schedule -> schedule));
            redisTemplate.opsForHash().putAll(RedisConstans.SCHEDULE_KEY, scheduleMap);
            redisTemplate.expire(RedisConstans.SCHEDULE_KEY, 1, TimeUnit.DAYS);
        }
        //如果当前时间在12点之前则不需要遍历排班信息直接返回
        if (currentTime.getHour() < 12) {
            return scheduleList;
        }
        //12点到18点之间：删除今天上午时段和小于今天的排班信息
        if (currentTime.getHour() < 18) {
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().isBefore(currentDate)
                    || (schedule.getScheduleDate().equals(currentDate) && "上午".equals(schedule.getTimeSlot())));
        } else {
            //18点之后：删除今天上午和下午时段的排班信息
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().isBefore(currentDate)
                    || (schedule.getScheduleDate().equals(currentDate)
                    && ("上午".equals(schedule.getTimeSlot()) || "下午".equals(schedule.getTimeSlot()))));
        }
        //遍历排班信息，筛选出当前日期后的排班信息
        return scheduleList;
    }

    //生成支付的二维码
    @Override
    public String generateQrCode(Schedule schedule) throws Exception {
        //判断号源是否充足
        if (schedule.getTotalQuota() <= schedule.getUsedQuota()) {
            return "号源不足";
        }
        Integer userId = CurrentHolder.getCurrentId();
        if (userId == null) {
            return "用户未登录";
        }
        //判断用户是否已预约该排班
        Appointment appointment = (Appointment) redisTemplate.opsForHash().get(RedisConstans.APPOINTMENT_KEY+userId, schedule.getId());
        if (appointment != null) {
            return "您已预约该排班";
        }

        //生成通过医生id+科室id+uuid来组成订单号
        String orderNo = schedule.getDoctorId() + "-" + schedule.getDepartmentId() + "-" + UUID.randomUUID().toString();
        //将订单号与排班信息关联，供支付回调后扣减号源；15分钟未支付自动失效
        redisTemplate.opsForValue().set(RedisConstans.ORDER_KEY + orderNo, schedule, 15, TimeUnit.MINUTES);
        //从redis中查询医生姓名和科室名称
        Doctor doctor = (Doctor) redisTemplate.opsForHash().get(RedisConstans.DOCTOR_KEY, schedule.getDoctorId());
        Department department = (Department) redisTemplate.opsForHash().get(RedisConstans.DEPARTMENT_KEY, schedule.getDepartmentId());
        if (doctor == null || department == null) {
            //如果redis中没有医生姓名和科室名称，从数据库查询医生姓名和科室名称并缓存到redis
            RLock lock = redissonClient.getLock(RedisConstans.REDIS_LOCK_KEY + userId);
            //获取锁
            boolean locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (locked) {
                try {
                    //双重检查，防止并发时重复查询数据库
                    doctor = (Doctor) redisTemplate.opsForHash().get(RedisConstans.DOCTOR_KEY, schedule.getDoctorId());
                    department = (Department) redisTemplate.opsForHash().get(RedisConstans.DEPARTMENT_KEY, schedule.getDepartmentId());
                    if (doctor == null || department == null) {
                        //查询数据库的doctor表
                        List<Doctor> doctorList = registrationcMapper.queryDoctor();
                        //查询数据库的department表
                        List<Department> departmentList = registrationcMapper.queryDepartment();
                        //将这两个表以hashmap的形式缓存到redis中
                        Map<Long, Doctor> doctorMap = doctorList.stream()
                                .collect(Collectors.toMap(Doctor::getId, doctor1 -> doctor1));
                        Map<Long, Department> departmentMap = departmentList.stream()
                                .collect(Collectors.toMap(Department::getId, department1 -> department1));
                        redisTemplate.opsForHash().putAll(RedisConstans.DOCTOR_KEY, doctorMap);
                        redisTemplate.opsForHash().putAll(RedisConstans.DEPARTMENT_KEY, departmentMap);
                        //设置过期时间为30分钟
                        redisTemplate.expire(RedisConstans.DOCTOR_KEY, 30, TimeUnit.MINUTES);
                        redisTemplate.expire(RedisConstans.DEPARTMENT_KEY, 30, TimeUnit.MINUTES);
                        //查询完成后重新赋值，避免后续使用空值
                        doctor = doctorMap.get(schedule.getDoctorId());
                        department = departmentMap.get(schedule.getDepartmentId());
                    }
                } finally {
                    //释放锁
                    lock.unlock();
                }
            } else {
                //如果获取锁失败，说明有其他线程正在查询医生姓名和科室名称，等待1秒后重试
                Thread.sleep(1000);
                return generateQrCode(schedule);
            }
        }
        if (doctor == null || department == null) {
            return "医生或科室信息不存在";
        }
        //生成支付二维码
        String qrCode = PayUtil.pay(config, doctor.getName() + department.getName() + "挂号", orderNo,
                String.format("%.2f", doctor.getRegistrationFee()));
        return qrCode;
    }

    //接收支付回调
    @Override
    public String payCallback(HttpServletRequest request) throws Exception {
        //从request中获取orderNo
        String orderNo = request.getParameter("out_trade_no");
        if (orderNo == null || orderNo.isEmpty()) {
            log.warn("支付回调缺少订单号 out_trade_no");
            return "fail";
        }
        log.info("支付成功，订单号为：{}", orderNo);
        return "success";
    }

    //生成挂号记录
    @Override
    public void RegistrationRecord(String orderNo) {
        //支付回调无登录态，按订单号从redis中反查排班信息
        Object obj = redisTemplate.opsForValue().get(RedisConstans.ORDER_KEY + orderNo);
        if (!(obj instanceof Schedule userSchedule)) {
            log.warn("未找到订单号对应的排班信息，无法生成挂号记录：{}", orderNo);
            return;
        }
        Long scheduleId = userSchedule.getId();
        //将数据库中的已预约号数+1
        registrationcMapper.updateUsedQuota(scheduleId);
        //同步更新redis中排班缓存的已预约数量
        Object cached = redisTemplate.opsForHash().get(RedisConstans.SCHEDULE_KEY, scheduleId);
        if (cached instanceof Schedule schedule && schedule.getUsedQuota() != null) {
            schedule.setUsedQuota(schedule.getUsedQuota() + 1);
            redisTemplate.opsForHash().put(RedisConstans.SCHEDULE_KEY, scheduleId, schedule);
        }
        //删除已处理的订单映射，避免重复回调重复扣减号源
        redisTemplate.delete(RedisConstans.ORDER_KEY + orderNo);
        //生成预约挂号记录
        //获取当前登录用户id
        Long userid = Long.valueOf(CurrentHolder.getCurrentId());
        //获取预约时间
        LocalDate scheduleDate = userSchedule.getScheduleDate();
        String timeSlot = userSchedule.getTimeSlot();
        LocalDateTime appointmentTime = null;
        //判断时间段
        if (timeSlot.equals("上午")) {
            appointmentTime = scheduleDate.atTime(12, 0);
        } else if (timeSlot.equals("下午")) {
            appointmentTime = scheduleDate.atTime(18, 0);
        }else if (timeSlot.equals("晚上")){
            appointmentTime = scheduleDate.atTime(22, 0);
        }
        //通过用户id查询用户信息
        User user = userMapper.selectById(userid);
        Appointment appointment = new Appointment();
        appointment.setScheduleId(scheduleId);
        appointment.setPatientId(userid);
        appointment.setPatientName(user.getRealName());
        appointment.setPatientPhone(user.getPhone());
        appointment.setIdCard(user.getIdCard());
        appointment.setStatus("已预约");
        appointment.setCreateTime(LocalDateTime.now());
        appointment.setUpdateTime(LocalDateTime.now());
        appointment.setDeleted(0L);
        appointment.setAppointmentTime(appointmentTime);
        //将预约挂号记录插入到数据库
        registrationcMapper.insert(appointment);
        //将预约挂号记录缓存到redis中
        redisTemplate.opsForHash().put(RedisConstans.APPOINTMENT_KEY+userid, scheduleId, appointment);
        //设置过期时间为预约时间30分钟
        redisTemplate.expire(RedisConstans.APPOINTMENT_KEY+userid,30, TimeUnit.MINUTES);
    }

    //查询预约挂号记录
    @Override
    public List<Appointment> queryAppointment() {
        //1.先从redis中查询缓存
        Long userid = Long.valueOf(CurrentHolder.getCurrentId());
        List<Appointment> appointments = redisTemplate.opsForHash()
                .entries(RedisConstans.APPOINTMENT_KEY + userid)
                .values()
                .stream()
                .map(obj -> (Appointment) obj)
                .collect(Collectors.toList());
        //2.如果缓存中没有，从数据库中查询
        if (appointments.isEmpty() || appointments == null) {
            //2.1从数据库查询数据
            appointments = registrationcMapper.selectAppointment(userid);
            //2.2将查询结果缓存到redis中
            appointments.forEach(appointment -> {
                //判断是否已经超过预约时间
                if (appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
                    redisTemplate.opsForHash().put(RedisConstans.APPOINTMENT_KEY + userid, appointment.getScheduleId(), appointment);
                }else {
                    //修改预约状态为已过期
                    registrationcMapper.updateStatus(appointment.getId(), "已过期");
                }
            });
            //2.3设置过期时间为预约时间30分钟
            redisTemplate.expire(RedisConstans.APPOINTMENT_KEY + userid, 30, TimeUnit.MINUTES);
        }
        return appointments;
    }

    //取消预约挂号并完成退款
    @Override
    public String cancelAppointment(AppointmentVO appointmentVO) throws Exception {
        Long userid = Long.valueOf(CurrentHolder.getCurrentId());
        //从数据库中查询预约挂号记录
        Appointment appointment = registrationcMapper.selectById(userid);
        //从redis中查询排班信息
        Schedule schedule = (Schedule) redisTemplate.opsForHash().get(RedisConstans.SCHEDULE_KEY, appointment.getScheduleId());
        LocalDate appointmentDate = appointment.getAppointmentTime().toLocalDate();
        LocalDate today = LocalDate.now();

        if (!appointmentDate.isBefore(today)) {
            // 预约日期晚于今天
            return "预约日期到了，不能行退款";
        }
        //对用户进行退款
        //生成退款订单号
        String outRequestNo = appointment.getId()+"_"+userid+"_"+UUID.randomUUID();
        //通过dockerid查询挂号金额
        double price = registrationcMapper.selectPrice(schedule.getDoctorId());
        AlipayTradeRefundResponse response = Factory.Payment.Common()
                .optional("out_request_no", outRequestNo)
                .optional("refund_reason", "取消预约挂号")
                .refund(appointmentVO.getOrderNo(), String.valueOf(price));
        if (!"10000".equals(response.code)){
            return "退款失败";
        }
        //退款成功过后，让排班号源数减少1
        registrationcMapper.updateQuota(schedule.getId(), 1);
        //更新redis中排班信息
        schedule.setUsedQuota(schedule.getUsedQuota()-1);
        redisTemplate.opsForHash().put(RedisConstans.SCHEDULE_KEY, schedule.getId(), schedule);
        //更改预约状态为已退款
        registrationcMapper.updateStatus(appointment.getId(), "已退款");
        //删除redis中预约挂号记录缓存
        redisTemplate.opsForHash().delete(RedisConstans.APPOINTMENT_KEY+userid, appointment.getId());
        return "退款成功";
    }
}
