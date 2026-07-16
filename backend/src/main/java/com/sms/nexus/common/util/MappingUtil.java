package com.sms.nexus.common.util;

import com.sms.nexus.common.enums.Gender;
import com.sms.nexus.common.enums.StudentStatus;
import com.sms.nexus.dto.response.StudentVO;
import com.sms.nexus.dto.response.TeacherVO;
import com.sms.nexus.entity.Student;
import com.sms.nexus.entity.Teacher;

import java.util.ArrayList;
import java.util.List;

public class MappingUtil {

    public static StudentVO toStudentVO(Student student) {
        StudentVO vo = new StudentVO();
        vo.setStudentId(student.getStudentId());
        vo.setName(student.getName());
        vo.setGender(student.getGender());
        vo.setGenderLabel(Gender.getLabel(student.getGender()));
        vo.setBirthDate(student.getBirthDate());
        vo.setPhone(student.getPhone());
        vo.setEmail(student.getEmail());
        vo.setAddress(student.getAddress());
        vo.setClassName(student.getClassName());
        vo.setMajor(student.getMajor());
        vo.setGrade(student.getGrade());
        vo.setEnrollmentDate(student.getEnrollmentDate());
        vo.setStatus(student.getStatus());
        vo.setStatusLabel(StudentStatus.fromValue(student.getStatus()).getLabel());
        vo.setAvatar(student.getAvatar());
        vo.setCreatedAt(student.getCreatedAt());
        vo.setUpdatedAt(student.getUpdatedAt());
        return vo;
    }

    public static List<StudentVO> toStudentVOList(List<Student> students) {
        List<StudentVO> list = new ArrayList<>();
        for (Student s : students) {
            list.add(toStudentVO(s));
        }
        return list;
    }

    public static TeacherVO toTeacherVO(Teacher teacher) {
        TeacherVO vo = new TeacherVO();
        vo.setTeacherId(teacher.getTeacherId());
        vo.setName(teacher.getName());
        vo.setGender(teacher.getGender());
        vo.setGenderLabel(Gender.getLabel(teacher.getGender()));
        vo.setBirthDate(teacher.getBirthDate());
        vo.setPhone(teacher.getPhone());
        vo.setEmail(teacher.getEmail());
        vo.setDepartment(teacher.getDepartment());
        vo.setTitle(teacher.getTitle());
        vo.setAvatar(teacher.getAvatar());
        vo.setStatus(teacher.getStatus());
        vo.setCreatedAt(teacher.getCreatedAt());
        vo.setUpdatedAt(teacher.getUpdatedAt());
        return vo;
    }
}
