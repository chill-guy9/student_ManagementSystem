package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sms.nexus.common.enums.StudentStatus;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.util.MappingUtil;
import com.sms.nexus.common.util.SystemInfoUtil;
import com.sms.nexus.dto.response.*;
import com.sms.nexus.entity.Log;
import com.sms.nexus.entity.Student;
import com.sms.nexus.entity.Teacher;
import com.sms.nexus.entity.Admin;
import com.sms.nexus.mapper.*;
import com.sms.nexus.service.DashboardService;
import com.sms.nexus.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final AdminMapper adminMapper;
    private final LogMapper logMapper;
    private final DashboardMapper dashboardMapper;

    @Override
    public ApiResponse<DashboardStatsVO> getStats() {
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setTotalStudents(studentMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setTotalTeachers(teacherMapper.selectCount(new LambdaQueryWrapper<>()));
        vo.setTotalAdmins(adminMapper.selectCount(new LambdaQueryWrapper<>()));

        LambdaQueryWrapper<Log> logWrapper = new LambdaQueryWrapper<>();
        vo.setTotalLogs(logMapper.selectCount(logWrapper));

        // Today's logs
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<Log> todayLogWrapper = new LambdaQueryWrapper<>();
        todayLogWrapper.ge(Log::getCreatedAt, todayStart);
        vo.setTodayLogs(logMapper.selectCount(todayLogWrapper));

        // Active students
        LambdaQueryWrapper<Student> activeStudentWrapper = new LambdaQueryWrapper<>();
        activeStudentWrapper.eq(Student::getStatus, StudentStatus.ACTIVE.getValue());
        vo.setActiveStudents(studentMapper.selectCount(activeStudentWrapper));

        // Active teachers
        LambdaQueryWrapper<Teacher> activeTeacherWrapper = new LambdaQueryWrapper<>();
        activeTeacherWrapper.eq(Teacher::getStatus, 1);
        vo.setActiveTeachers(teacherMapper.selectCount(activeTeacherWrapper));

        return ApiResponse.success(vo);
    }

    @Override
    public ApiResponse<List<UserGrowthVO>> getUserGrowth(int days) {
        int safeDays = Math.min(Math.max(days, 1), 365);
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(safeDays - 1);

        List<UserGrowthVO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            UserGrowthVO vo = new UserGrowthVO();
            vo.setDate(date.toString());
            result.add(vo);
        }

        // Query students growth using a single GROUP BY query
        QueryWrapper<Student> studentWrapper = new QueryWrapper<>();
        studentWrapper.select("DATE(created_at) as date, COUNT(*) as cnt")
                .ge("created_at", startDate.atStartOfDay())
                .groupBy("DATE(created_at)");
        List<Map<String, Object>> studentCounts = studentMapper.selectMaps(studentWrapper);
        Map<String, Long> studentByDate = new HashMap<>();
        for (Map<String, Object> row : studentCounts) {
            String d = String.valueOf(row.get("date"));
            Long cnt = ((Number) row.get("cnt")).longValue();
            studentByDate.put(d, cnt);
        }

        // Query teachers growth
        QueryWrapper<Teacher> teacherWrapper = new QueryWrapper<>();
        teacherWrapper.select("DATE(created_at) as date, COUNT(*) as cnt")
                .ge("created_at", startDate.atStartOfDay())
                .groupBy("DATE(created_at)");
        List<Map<String, Object>> teacherCounts = teacherMapper.selectMaps(teacherWrapper);
        Map<String, Long> teacherByDate = new HashMap<>();
        for (Map<String, Object> row : teacherCounts) {
            String d = String.valueOf(row.get("date"));
            Long cnt = ((Number) row.get("cnt")).longValue();
            teacherByDate.put(d, cnt);
        }

        // Query admins growth
        QueryWrapper<Admin> adminWrapper = new QueryWrapper<>();
        adminWrapper.select("DATE(created_at) as date, COUNT(*) as cnt")
                .ge("created_at", startDate.atStartOfDay())
                .groupBy("DATE(created_at)");
        List<Map<String, Object>> adminCounts = adminMapper.selectMaps(adminWrapper);
        Map<String, Long> adminByDate = new HashMap<>();
        for (Map<String, Object> row : adminCounts) {
            String d = String.valueOf(row.get("date"));
            Long cnt = ((Number) row.get("cnt")).longValue();
            adminByDate.put(d, cnt);
        }

        // Compute cumulative counts from base counts before start date
        long cumStudents = studentMapper.selectCount(
                new LambdaQueryWrapper<Student>().lt(Student::getCreatedAt, startDate.atStartOfDay()));
        long cumTeachers = teacherMapper.selectCount(
                new LambdaQueryWrapper<Teacher>().lt(Teacher::getCreatedAt, startDate.atStartOfDay()));
        long cumAdmins = adminMapper.selectCount(
                new LambdaQueryWrapper<Admin>().lt(Admin::getCreatedAt, startDate.atStartOfDay()));

        for (UserGrowthVO vo : result) {
            cumStudents += studentByDate.getOrDefault(vo.getDate(), 0L);
            cumTeachers += teacherByDate.getOrDefault(vo.getDate(), 0L);
            cumAdmins += adminByDate.getOrDefault(vo.getDate(), 0L);
            vo.setStudents((int) cumStudents);
            vo.setTeachers((int) cumTeachers);
            vo.setAdmins((int) cumAdmins);
        }

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<List<LogDistributionVO>> getLogDistribution(int days) {
        String startDate = LocalDate.now().minusDays(days).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<Map<String, Object>> raw = logMapper.countByOperationType(startDate, endDate);
        List<LogDistributionVO> result = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            LogDistributionVO vo = new LogDistributionVO();
            vo.setOperationType(String.valueOf(row.get("operation_type")));
            vo.setCount(Long.parseLong(String.valueOf(row.get("cnt"))));
            result.add(vo);
        }

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<SystemLoadVO> getSystemLoad() {
        return ApiResponse.success(SystemInfoUtil.getSystemLoad());
    }

    @Override
    public ApiResponse<List<LogVO>> getRecentLogs(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        LambdaQueryWrapper<Log> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Log::getCreatedAt);
        wrapper.last("LIMIT " + safeLimit);
        List<Log> logs = logMapper.selectList(wrapper);

        List<LogVO> voList = logs.stream().map(this::toLogVO).collect(Collectors.toList());
        return ApiResponse.success(voList);
    }

    private LogVO toLogVO(Log logEntry) {
        LogVO vo = new LogVO();
        vo.setLogId(logEntry.getLogId());
        vo.setOperatorId(logEntry.getOperatorId());
        vo.setOperatorName(logEntry.getOperatorName());
        vo.setOperationType(logEntry.getOperationType());
        try {
            vo.setOperationTypeLabel(OperationType.fromValue(logEntry.getOperationType()).getLabel());
        } catch (Exception e) {
            vo.setOperationTypeLabel(logEntry.getOperationType());
        }
        vo.setTargetType(logEntry.getTargetType());
        vo.setTargetId(logEntry.getTargetId());
        vo.setTargetName(logEntry.getTargetName());
        vo.setDetail(logEntry.getDetail());
        vo.setIp(logEntry.getIp());
        vo.setLevel(logEntry.getLevel());
        vo.setCreatedAt(logEntry.getCreatedAt());
        return vo;
    }
}
