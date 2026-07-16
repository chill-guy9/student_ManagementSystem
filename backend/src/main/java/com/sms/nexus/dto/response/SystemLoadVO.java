package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class SystemLoadVO {

    private double cpuUsage;
    private double memoryUsage;
    private long totalMemory;
    private long usedMemory;
    private long freeMemory;
    private long diskTotal;
    private long diskUsed;
    private long diskFree;
    private double diskUsage;
}
