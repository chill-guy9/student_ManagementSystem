package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class SystemInfoVO {

    private String appName;
    private String version;
    private String javaVersion;
    private String osName;
    private String osArch;
    private long uptime;
    private int activeConnections;
    private String serverTime;
}
