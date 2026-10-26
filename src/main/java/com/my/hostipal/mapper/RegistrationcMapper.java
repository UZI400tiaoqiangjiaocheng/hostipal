package com.my.hostipal.mapper;

import com.my.hostipal.pojo.entity.Appointment;
import com.my.hostipal.pojo.entity.Department;
import com.my.hostipal.pojo.entity.Doctor;
import com.my.hostipal.pojo.entity.Schedule;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RegistrationcMapper {

    List<Schedule> selectScheduleList(LocalDate startDate, LocalDate endDate);

    //查询医生表
    @Select("select * from doctor")
    List<Doctor> queryDoctor();

    //查询科室表
    @Select("select * from department")
    List<Department> queryDepartment();

    //更新已预约号源数
    @Update("update schedule set used_quota = used_quota + 1 where id = #{scheduleId}")
    void updateUsedQuota(Long scheduleId);

    //插入预约挂号记录
    void insert(Appointment appointment);

    //查询预约挂号记录
    @Select("select * from appointment where patient_id = #{userid}")
    List<Appointment> selectAppointment(Long userid);

    //更新预约状态
    @Update("update appointment set status = #{status} where id = #{id}")
    void updateStatus(Long id, String status);

    //根据id查询预约挂号记录
    @Select("select * from appointment where id = #{id}")
    Appointment selectById(Long id);

    @Select("select registration_fee from doctor where id = #{doctorId}")
    double selectPrice(Long doctorId);

    //更新排班号源数
    @Update("update schedule set used_quota = used_quota - #{i} where id = #{id}")
    void updateQuota(Long id, int i);
}
