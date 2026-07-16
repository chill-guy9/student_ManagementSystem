package com.sms.nexus.dto.response;

import lombok.Data;

@Data
public class DashboardStatsVO {

    private long totalStudents;
    private long totalTeachers;
    private long totalAdmins;
    private long totalLogs;
    private long todayLogs;
    private long activeStudents;
    private long activeTeachers;
}
