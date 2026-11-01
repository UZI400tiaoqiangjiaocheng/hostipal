package com.my.hostipal.pojo.vo;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;
@Data
public class DepartmentVO {
    @ToolParam(description = "科室名称")
    private String name;
    @ToolParam(description = "科室详细介绍")
    private String description;
}
