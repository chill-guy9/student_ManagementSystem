package com.sms.nexus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sms.nexus.entity.Log;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LogMapper extends BaseMapper<Log> {

    List<Map<String, Object>> countByOperationType(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    List<Map<String, Object>> countByDay(@Param("startDate") String startDate,
                                          @Param("endDate") String endDate);

    List<Map<String, Object>> countByLevel(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate);

    List<Map<String, Object>> countByHour(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);

    List<Map<String, Object>> topOperators(@Param("startDate") String startDate,
                                            @Param("endDate") String endDate,
                                            @Param("limit") int limit);
}
