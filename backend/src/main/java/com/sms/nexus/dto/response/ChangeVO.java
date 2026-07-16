package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class ChangeVO {

    private Long id;
    private String logId;
    private String fieldName;
    private String fieldLabel;
    private String oldValue;
    private String newValue;
}
