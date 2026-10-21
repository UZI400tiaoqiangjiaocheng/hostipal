package com.my.hostipal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.my.hostipal.pojo.entity.Schedule;
import com.my.hostipal.pojo.entity.User;
import com.my.hostipal.pojo.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select id,username,password from user where username = #{username}")
    UserVO selectByUsername(String username);
}
