package com.sms.nexus.dto.request;

import lombok.Data;

@Data
public class StudentQueryRequest {

    private String keyword;
    private String className;
    private String major;
    private String grade;
    private String status;
    private Integer page = 1;
    private Integer pageSize = 10;
}
