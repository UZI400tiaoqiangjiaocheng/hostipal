package com.my.hostipal.service;

import com.my.hostipal.pojo.vo.DepartmentVO;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface AiService {
    void save(String conversationId);

    List<String> list();

    List<Message> getMessages(String conversationId);

    //查询所有科室
    List<DepartmentVO> queryDepartment();

    void delete(String conversationId);
}
