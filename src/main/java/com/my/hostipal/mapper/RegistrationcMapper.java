package com.my.hostipal.mapper;

import com.my.hostipal.pojo.entity.Department;
import com.my.hostipal.pojo.entity.Doctor;
import com.my.hostipal.pojo.entity.Schedule;
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
}
