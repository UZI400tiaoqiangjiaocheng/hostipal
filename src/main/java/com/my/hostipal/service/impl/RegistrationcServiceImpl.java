package com.my.hostipal.service.impl;

import cn.hutool.core.lang.UUID;
import com.alipay.easysdk.kernel.Config;
import com.my.hostipal.mapper.RegistrationcMapper;
import com.my.hostipal.pojo.entity.Department;
import com.my.hostipal.pojo.entity.Doctor;
import com.my.hostipal.pojo.entity.Schedule;
import com.my.hostipal.service.RegistrationcSerrvice;
import com.my.hostipal.utils.CurrentHolder;
import com.my.hostipal.utils.PayUtil;
import com.my.hostipal.utils.RedisConstans;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private Config config;


    @Override
    public List<Schedule> queryDepartment() {
        //先从redis查询从今天开始和之后的所有排班信息
        List<Schedule> scheduleList = null;
//        Map<Long,Schedule> scheduleMap = new HashMap<>();
        Map<Object, Object> scheduleCache = redisTemplate.opsForHash().entries(RedisConstans.SCHEDULE_KEY);
        for (Map.Entry<Object, Object> entry : scheduleCache.entrySet()) {
            scheduleList.add((Schedule) entry.getValue());
        }
        //获取当前几点
        LocalDateTime currentTime = LocalDateTime.now();
        //获取当前日期
        LocalDate currentDate = LocalDate.now();

        //如果redis没有排班信息，从数据库查询排班信息
        if (scheduleList == null || scheduleList.isEmpty()) {
            //从数据库查询从今天开始到5天后的排班信息
            scheduleList = registrationcMapper.selectScheduleList(currentDate, currentDate.plusDays(5));
            //并将排班信息缓存到redis hash并设置过期时间为1天
            Map<Long,Schedule> scheduleMap = scheduleList.stream().collect(Collectors.toMap(Schedule::getId, schedule1 -> schedule1));
            redisTemplate.opsForHash().putAll(RedisConstans.SCHEDULE_KEY, scheduleMap);
            redisTemplate.expire(RedisConstans.SCHEDULE_KEY, 1, TimeUnit.DAYS);
        }
        //如果当前时间在12点之前则不需要遍历排班信息直接返回
        if (currentTime.getHour() < 12) {
            return scheduleList;
        }
        if (currentTime.getHour()>=12 && currentTime.getHour()<18) {
            //从scheduleList中删除今天上午时段和小于今天的排班信息
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().isBefore(currentDate));
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().equals(currentDate) && schedule.getTimeSlot().equals("上午"));
        }
        if (currentTime.getHour()>=18) {
            //从scheduleList中删除今天上午和下午时段的排班信息
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().isBefore(currentDate));
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().equals(currentDate) && schedule.getTimeSlot().equals("上午"));
            scheduleList.removeIf(schedule -> schedule.getScheduleDate().equals(currentDate) && schedule.getTimeSlot().equals("下午"));
        }
        //遍历排班信息，筛选出当前日期后的排班信息
        return scheduleList;
    }

    //生成支付的二维码
    @Override
    public String generateQrCode(Schedule schedule) throws Exception {
        //判断号源是否充足
        if (schedule.getTotalQuota()<=schedule.getUsedQuota()){
            return "号源不足";
        }
        //将schedule存入redis中
        redisTemplate.opsForHash().put(RedisConstans.USER_SCHEDULE_KEY+CurrentHolder.getCurrentId(),CurrentHolder.getCurrentId(),schedule);
        //生成通过医生id+科室id+uuid来组成订单号
        String orderNo = schedule.getDoctorId() + schedule.getDepartmentId() + UUID.randomUUID().toString();
        //从redis中查询医生姓名和科室名称
        Doctor doctor = (Doctor) redisTemplate.opsForHash().get(RedisConstans.DOCTOR_KEY, schedule.getDoctorId());
        Department department = (Department) redisTemplate.opsForHash().get(RedisConstans.DEPARTMENT_KEY, schedule.getDepartmentId());
        if (doctor == null || department == null) {
            //如果redis中没有医生姓名和科室名称，从数据库查询医生姓名和科室名称并缓存到redis
            RLock lock = redissonClient.getLock(RedisConstans.REDIS_LOCK_KEY + CurrentHolder.getCurrentId());
            //获取锁
            boolean locked = lock.tryLock(1,TimeUnit.SECONDS);
            if(locked){
                try {
                    //查询数据库的doctor表
                    List<Doctor> doctorList = registrationcMapper.queryDoctor();
                    //查询数据库的department表
                    List<Department> departmentList = registrationcMapper.queryDepartment();
                    //将这两个表以hashmap的形式缓存到redis中
                    Map<Long, Doctor> doctorMap = doctorList.stream().collect(Collectors.toMap(Doctor::getId, doctor1 -> doctor));
                    Map<Long, Department> departmentMap = departmentList.stream().collect(Collectors.toMap(Department::getId, department1 -> department1));
                    redisTemplate.opsForHash().putAll(RedisConstans.DOCTOR_KEY, doctorMap);
                    redisTemplate.opsForHash().putAll(RedisConstans.DEPARTMENT_KEY, departmentMap);
                    //设置过期时间为30分钟
                    redisTemplate.expire(RedisConstans.DOCTOR_KEY, 30, TimeUnit.MINUTES);
                    redisTemplate.expire(RedisConstans.DEPARTMENT_KEY, 30, TimeUnit.MINUTES);
                } finally {
                    //释放锁
                    lock.unlock();
                }
            }else {
                //如果获取锁失败，说明有其他线程正在查询医生姓名和科室名称，等待1秒后重试
                Thread.sleep(1000);
                return generateQrCode(schedule);
            }
        }
        //生成支付二维码
        String qrCode = PayUtil.pay(config, doctor.getName()+department.getName()+"挂号", orderNo, String.valueOf(doctor.getRegistrationFee()));
        return qrCode;
    }

    //接收支付回调
    @Override
    public String payCallback(HttpServletRequest request) throws Exception {
        //从request中获取orderNo
        String orderNo = request.getParameter("out_trade_no");
        if (orderNo == null || orderNo.isEmpty()) {
            orderNo = PayUtil.query(config, orderNo);
        }
        log.info("支付成功，订单号为：{}", orderNo);
        return "success";
    }

    //生成挂号记录
    @Override
    public void RegistrationRecord() throws InterruptedException {
        //减少预约号源数
        RLock lock = redissonClient.getLock(RedisConstans.TOTAL_LOCK_KEY + CurrentHolder.getCurrentId());
        //获取锁
        boolean locked = lock.tryLock(1,TimeUnit.SECONDS);
        if (locked){
            try {
                //从redis中获取已预约数量并增加1
                Schedule userSchedule = (Schedule) redisTemplate.opsForHash().get(RedisConstans.USER_SCHEDULE_KEY+CurrentHolder.getCurrentId(),CurrentHolder.getCurrentId());
                //从数据库中获取总的预约号源数和已预约号源数

            }finally {
                //释放锁
                lock.unlock();
            }
        }
    }
}
