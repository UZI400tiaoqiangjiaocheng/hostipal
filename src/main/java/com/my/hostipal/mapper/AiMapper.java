package com.my.hostipal.mapper;

import com.my.hostipal.pojo.entity.Conversation;
import com.my.hostipal.pojo.vo.DepartmentVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AiMapper {
    @Insert("insert into conversation (user_id, conversation_id) values (#{userId}, #{conversationId})")
    void insert(Conversation conversation);

    @Select("select conversation_id from conversation where user_id = #{userId}")
    List<String> selectConversationId(Long userId);

    @Select("select name,description from department")
    List<DepartmentVO> selectDepartment();
}
