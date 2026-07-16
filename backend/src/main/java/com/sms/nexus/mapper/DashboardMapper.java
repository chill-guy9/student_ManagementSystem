package com.sms.nexus.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface DashboardMapper {

    Map<String, Object> getStats();

    int countStudentsByDay(@Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    int countAdminsByDay(@Param("startDate") String startDate,
                         @Param("endDate") String endDate);

    int countTeachersByDay(@Param("startDate") String startDate,
                           @Param("endDate") String endDate);

    int countLogsByDay(@Param("startDate") String startDate,
                       @Param("endDate") String endDate);
}
