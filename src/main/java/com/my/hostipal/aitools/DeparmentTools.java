package com.my.hostipal.aitools;

import com.my.hostipal.pojo.vo.DepartmentVO;
import com.my.hostipal.service.AiService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeparmentTools {
    @Autowired
    private AiService aiService;

    @Tool(description = "查询所有科室")
    public List<DepartmentVO> queryDepartment(){
        return aiService.queryDepartment();
    }
}
