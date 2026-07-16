package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.enums.StudentStatus;
import com.sms.nexus.common.exception.BusinessException;
import com.sms.nexus.common.exception.ResourceNotFoundException;
import com.sms.nexus.common.util.CsvExporter;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.common.util.MappingUtil;
import com.sms.nexus.dto.request.CreateStudentRequest;
import com.sms.nexus.dto.request.StudentQueryRequest;
import com.sms.nexus.dto.request.UpdateStudentRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.StudentVO;
import com.sms.nexus.entity.Student;
import com.sms.nexus.mapper.StudentMapper;
import com.sms.nexus.service.LogService;
import com.sms.nexus.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final IdGenerator idGenerator;
    private final LogService logService;

    @Override
    public ApiResponse<PageResult<StudentVO>> listStudents(StudentQueryRequest request) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(Student::getName, request.getKeyword())
                    .or().like(Student::getStudentId, request.getKeyword())
                    .or().like(Student::getPhone, request.getKeyword())
                    .or().like(Student::getEmail, request.getKeyword())
            );
        }
        if (StringUtils.hasText(request.getClassName())) {
            wrapper.eq(Student::getClassName, request.getClassName());
        }
        if (StringUtils.hasText(request.getMajor())) {
            wrapper.eq(Student::getMajor, request.getMajor());
        }
        if (StringUtils.hasText(request.getGrade())) {
            wrapper.eq(Student::getGrade, request.getGrade());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Student::getStatus, request.getStatus());
        }

        wrapper.orderByDesc(Student::getCreatedAt);

        Page<Student> page = studentMapper.selectPage(
                new Page<>(request.getPage(), request.getPageSize()), wrapper);

        List<StudentVO> voList = MappingUtil.toStudentVOList(page.getRecords());
        PageResult<StudentVO> result = new PageResult<>(voList, page.getTotal(),
                page.getCurrent(), page.getSize());

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<StudentVO> getStudent(String studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new ResourceNotFoundException("学生不存在: " + studentId);
        }
        return ApiResponse.success(MappingUtil.toStudentVO(student));
    }

    @Override
    public ApiResponse<StudentVO> createStudent(CreateStudentRequest request, String operatorId, String operatorName) {
        Student student = new Student();
        student.setStudentId(idGenerator.nextStudentId());
        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setBirthDate(request.getBirthDate());
        student.setPhone(request.getPhone());
        student.setEmail(request.getEmail());
        student.setAddress(request.getAddress());
        student.setClassName(request.getClassName());
        student.setMajor(request.getMajor());
        student.setGrade(request.getGrade());
        student.setEnrollmentDate(request.getEnrollmentDate());
        student.setStatus(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE.getValue());
        student.setAvatar(request.getAvatar());

        studentMapper.insert(student);

        logService.createLog(operatorId, operatorName, OperationType.CREATE.getValue(),
                "student", student.getStudentId(), student.getName(),
                "创建学生: " + student.getName(), null, "INFO");

        return ApiResponse.success(MappingUtil.toStudentVO(student));
    }

    @Override
    public ApiResponse<StudentVO> updateStudent(String studentId, UpdateStudentRequest request,
                                                 String operatorId, String operatorName) {
        Student existing = studentMapper.selectById(studentId);
        if (existing == null) {
            throw new ResourceNotFoundException("学生不存在: " + studentId);
        }

        // Track changes
        List<ChangeVO> changes = computeChanges(existing, request);

        // Apply updates
        if (request.getName() != null) existing.setName(request.getName());
        if (request.getGender() != null) existing.setGender(request.getGender());
        if (request.getBirthDate() != null) existing.setBirthDate(request.getBirthDate());
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getAddress() != null) existing.setAddress(request.getAddress());
        if (request.getClassName() != null) existing.setClassName(request.getClassName());
        if (request.getMajor() != null) existing.setMajor(request.getMajor());
        if (request.getGrade() != null) existing.setGrade(request.getGrade());
        if (request.getEnrollmentDate() != null) existing.setEnrollmentDate(request.getEnrollmentDate());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());
        if (request.getAvatar() != null) existing.setAvatar(request.getAvatar());

        studentMapper.updateById(existing);

        logService.createLogWithChanges(operatorId, operatorName, OperationType.UPDATE.getValue(),
                "student", studentId, existing.getName(),
                "更新学生: " + existing.getName(), null, "INFO", changes);

        return ApiResponse.success(MappingUtil.toStudentVO(existing));
    }

    @Override
    public ApiResponse<Void> deleteStudent(String studentId, String operatorId, String operatorName) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new ResourceNotFoundException("学生不存在: " + studentId);
        }

        studentMapper.deleteById(studentId);

        logService.createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                "student", studentId, student.getName(),
                "删除学生: " + student.getName(), null, "WARN");

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> batchDeleteStudents(List<String> studentIds, String operatorId, String operatorName) {
        int deletedCount = 0;
        for (String studentId : studentIds) {
            Student student = studentMapper.selectById(studentId);
            if (student != null) {
                studentMapper.deleteById(studentId);
                deletedCount++;
            }
        }

        logService.createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                "student", "batch", deletedCount + "名学生",
                "批量删除学生: " + deletedCount + "名", null, "WARN");

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> exportStudents(StudentQueryRequest request, HttpServletResponse response) {
        // Fetch all matching students (no pagination for export)
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                    .like(Student::getName, request.getKeyword())
                    .or().like(Student::getStudentId, request.getKeyword())
            );
        }
        if (StringUtils.hasText(request.getClassName())) {
            wrapper.eq(Student::getClassName, request.getClassName());
        }
        if (StringUtils.hasText(request.getMajor())) {
            wrapper.eq(Student::getMajor, request.getMajor());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(Student::getStatus, request.getStatus());
        }

        List<Student> students = studentMapper.selectList(wrapper);

        String[] headers = {"学号", "姓名", "性别", "出生日期", "手机号", "邮箱", "地址", "班级", "专业", "年级", "入学日期", "状态"};
        List<String[]> data = new ArrayList<>();
        for (Student s : students) {
            data.add(new String[]{
                    s.getStudentId(), s.getName(),
                    com.sms.nexus.common.enums.Gender.getLabel(s.getGender()),
                    s.getBirthDate() != null ? s.getBirthDate().toString() : "",
                    s.getPhone(), s.getEmail(), s.getAddress(),
                    s.getClassName(), s.getMajor(), s.getGrade(),
                    s.getEnrollmentDate() != null ? s.getEnrollmentDate().toString() : "",
                    StudentStatus.fromValue(s.getStatus()).getLabel()
            });
        }

        try {
            CsvExporter.export(response, "students_export.csv", headers, data);
        } catch (IOException e) {
            throw new BusinessException("导出失败: " + e.getMessage());
        }

        return ApiResponse.success();
    }

    private List<ChangeVO> computeChanges(Student existing, UpdateStudentRequest request) {
        List<ChangeVO> changes = new ArrayList<>();

        addChangeIfDifferent(changes, "name", "姓名", existing.getName(), request.getName());
        addChangeIfDifferent(changes, "gender", "性别",
                existing.getGender() != null ? String.valueOf(existing.getGender()) : null,
                request.getGender() != null ? String.valueOf(request.getGender()) : null);
        addChangeIfDifferent(changes, "birthDate", "出生日期",
                existing.getBirthDate() != null ? existing.getBirthDate().toString() : null,
                request.getBirthDate() != null ? request.getBirthDate().toString() : null);
        addChangeIfDifferent(changes, "phone", "手机号", existing.getPhone(), request.getPhone());
        addChangeIfDifferent(changes, "email", "邮箱", existing.getEmail(), request.getEmail());
        addChangeIfDifferent(changes, "address", "地址", existing.getAddress(), request.getAddress());
        addChangeIfDifferent(changes, "className", "班级", existing.getClassName(), request.getClassName());
        addChangeIfDifferent(changes, "major", "专业", existing.getMajor(), request.getMajor());
        addChangeIfDifferent(changes, "grade", "年级", existing.getGrade(), request.getGrade());
        addChangeIfDifferent(changes, "enrollmentDate", "入学日期",
                existing.getEnrollmentDate() != null ? existing.getEnrollmentDate().toString() : null,
                request.getEnrollmentDate() != null ? request.getEnrollmentDate().toString() : null);
        addChangeIfDifferent(changes, "status", "状态", existing.getStatus(), request.getStatus());
        addChangeIfDifferent(changes, "avatar", "头像", existing.getAvatar(), request.getAvatar());

        return changes;
    }

    private void addChangeIfDifferent(List<ChangeVO> changes, String fieldName, String fieldLabel,
                                       String oldValue, String newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            ChangeVO change = new ChangeVO();
            change.setFieldName(fieldName);
            change.setFieldLabel(fieldLabel);
            change.setOldValue(oldValue);
            change.setNewValue(newValue);
            changes.add(change);
        }
    }
}
