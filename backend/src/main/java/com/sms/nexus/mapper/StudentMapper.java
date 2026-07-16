package com.sms.nexus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sms.nexus.entity.Student;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {
}
