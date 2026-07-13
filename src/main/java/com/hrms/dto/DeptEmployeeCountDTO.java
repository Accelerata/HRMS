package com.hrms.dto;

import lombok.Data;

@Data
public class DeptEmployeeCountDTO {
    private Long deptId;
    private Integer status;
    private Integer count;
}

