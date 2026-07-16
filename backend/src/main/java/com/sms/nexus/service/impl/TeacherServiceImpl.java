package com.sms.nexus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sms.nexus.common.enums.OperationType;
import com.sms.nexus.common.exception.BusinessException;
import com.sms.nexus.common.exception.ResourceNotFoundException;
import com.sms.nexus.common.util.CsvExporter;
import com.sms.nexus.common.util.IdGenerator;
import com.sms.nexus.common.util.MappingUtil;
import com.sms.nexus.dto.request.AddCourseRequest;
import com.sms.nexus.dto.request.CreateTeacherRequest;
import com.sms.nexus.dto.request.UpdateTeacherRequest;
import com.sms.nexus.dto.response.ApiResponse;
import com.sms.nexus.dto.response.ChangeVO;
import com.sms.nexus.dto.response.PageResult;
import com.sms.nexus.dto.response.TeacherVO;
import com.sms.nexus.entity.Teacher;
import com.sms.nexus.entity.TeacherCourse;
import com.sms.nexus.mapper.TeacherCourseMapper;
import com.sms.nexus.mapper.TeacherMapper;
import com.sms.nexus.service.LogService;
import com.sms.nexus.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private final TeacherMapper teacherMapper;
    private final TeacherCourseMapper teacherCourseMapper;
    private final IdGenerator idGenerator;
    private final LogService logService;

    @Override
    public ApiResponse<PageResult<TeacherVO>> listTeachers(String keyword, String department, String title,
                                                            Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Teacher::getName, keyword)
                    .or().like(Teacher::getTeacherId, keyword)
                    .or().like(Teacher::getPhone, keyword)
                    .or().like(Teacher::getEmail, keyword)
            );
        }
        if (StringUtils.hasText(department)) {
            wrapper.eq(Teacher::getDepartment, department);
        }
        if (StringUtils.hasText(title)) {
            wrapper.eq(Teacher::getTitle, title);
        }
        if (status != null) {
            wrapper.eq(Teacher::getStatus, status);
        }

        wrapper.orderByDesc(Teacher::getCreatedAt);

        Page<Teacher> pageResult = teacherMapper.selectPage(
                new Page<>(page, pageSize), wrapper);

        List<TeacherVO> voList = new ArrayList<>();
        for (Teacher t : pageResult.getRecords()) {
            TeacherVO vo = MappingUtil.toTeacherVO(t);
            vo.setCourses(getTeacherCoursesList(t.getTeacherId()));
            voList.add(vo);
        }

        PageResult<TeacherVO> result = new PageResult<>(voList, pageResult.getTotal(),
                pageResult.getCurrent(), pageResult.getSize());

        return ApiResponse.success(result);
    }

    @Override
    public ApiResponse<TeacherVO> getTeacher(String teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }
        TeacherVO vo = MappingUtil.toTeacherVO(teacher);
        vo.setCourses(getTeacherCoursesList(teacherId));
        return ApiResponse.success(vo);
    }

    @Override
    public ApiResponse<TeacherVO> createTeacher(CreateTeacherRequest request, String operatorId, String operatorName) {
        Teacher teacher = new Teacher();
        teacher.setTeacherId(idGenerator.nextTeacherId());
        teacher.setName(request.getName());
        teacher.setGender(request.getGender());
        teacher.setBirthDate(request.getBirthDate());
        teacher.setPhone(request.getPhone());
        teacher.setEmail(request.getEmail());
        teacher.setDepartment(request.getDepartment());
        teacher.setTitle(request.getTitle());
        teacher.setAvatar(request.getAvatar());
        teacher.setStatus(1);

        teacherMapper.insert(teacher);

        logService.createLog(operatorId, operatorName, OperationType.CREATE.getValue(),
                "teacher", teacher.getTeacherId(), teacher.getName(),
                "创建教师: " + teacher.getName(), null, "INFO");

        return ApiResponse.success(MappingUtil.toTeacherVO(teacher));
    }

    @Override
    public ApiResponse<TeacherVO> updateTeacher(String teacherId, UpdateTeacherRequest request,
                                                 String operatorId, String operatorName) {
        Teacher existing = teacherMapper.selectById(teacherId);
        if (existing == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }

        List<ChangeVO> changes = computeTeacherChanges(existing, request);

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getGender() != null) existing.setGender(request.getGender());
        if (request.getBirthDate() != null) existing.setBirthDate(request.getBirthDate());
        if (request.getPhone() != null) existing.setPhone(request.getPhone());
        if (request.getEmail() != null) existing.setEmail(request.getEmail());
        if (request.getDepartment() != null) existing.setDepartment(request.getDepartment());
        if (request.getTitle() != null) existing.setTitle(request.getTitle());
        if (request.getAvatar() != null) existing.setAvatar(request.getAvatar());
        if (request.getStatus() != null) existing.setStatus(request.getStatus());

        teacherMapper.updateById(existing);

        logService.createLogWithChanges(operatorId, operatorName, OperationType.UPDATE.getValue(),
                "teacher", teacherId, existing.getName(),
                "更新教师: " + existing.getName(), null, "INFO", changes);

        return ApiResponse.success(MappingUtil.toTeacherVO(existing));
    }

    @Override
    public ApiResponse<Void> deleteTeacher(String teacherId, String operatorId, String operatorName) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }

        teacherMapper.deleteById(teacherId);

        logService.createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                "teacher", teacherId, teacher.getName(),
                "删除教师: " + teacher.getName(), null, "WARN");

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<Void> exportTeachers(String keyword, String department, String title, Integer status, HttpServletResponse response) {
        LambdaQueryWrapper<Teacher> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Teacher::getName, keyword)
                    .or().like(Teacher::getTeacherId, keyword)
            );
        }
        if (StringUtils.hasText(department)) wrapper.eq(Teacher::getDepartment, department);
        if (StringUtils.hasText(title)) wrapper.eq(Teacher::getTitle, title);
        if (status != null) wrapper.eq(Teacher::getStatus, status);
        wrapper.orderByDesc(Teacher::getCreatedAt);

        List<Teacher> teachers = teacherMapper.selectList(wrapper);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=teachers_export.csv");

        try (PrintWriter writer = response.getWriter()) {
            writer.write("﻿"); // BOM for Excel
            writer.println("工号,姓名,性别,院系,职称,电话,邮箱,状态");
            for (Teacher t : teachers) {
                String genderLabel = t.getGender() != null ? (t.getGender() == 1 ? "男" : "女") : "";
                String statusLabel = t.getStatus() != null ? (t.getStatus() == 1 ? "在职" : "离职") : "";
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                        t.getTeacherId(), t.getName(), genderLabel,
                        t.getDepartment(), t.getTitle(), t.getPhone(), t.getEmail(), statusLabel);
            }
        } catch (IOException e) {
            throw new BusinessException("导出教师CSV失败: " + e.getMessage());
        }

        return ApiResponse.success();
    }

    @Override
    public ApiResponse<TeacherVO.CourseInfoList> getTeacherCourses(String teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }
        TeacherVO.CourseInfoList list = new TeacherVO.CourseInfoList();
        list.setCourses(getTeacherCoursesList(teacherId));
        return ApiResponse.success(list);
    }

    @Override
    public ApiResponse<TeacherVO.CourseInfo> addTeacherCourse(String teacherId, AddCourseRequest request,
                                                               String operatorId, String operatorName) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }

        TeacherCourse course = new TeacherCourse();
        course.setTeacherId(teacherId);
        course.setCourseName(request.getCourseName());
        course.setCourseCode(request.getCourseCode());
        course.setSemester(request.getSemester());
        course.setHours(request.getHours() != null ? request.getHours() : 0);
        teacherCourseMapper.insert(course);

        logService.createLog(operatorId, operatorName, OperationType.CREATE.getValue(),
                "teacher_course", String.valueOf(course.getId()), request.getCourseName(),
                "添加教师课程: " + teacher.getName() + " - " + request.getCourseName(), null, "INFO");

        TeacherVO.CourseInfo info = new TeacherVO.CourseInfo();
        info.setId(course.getId());
        info.setCourseName(course.getCourseName());
        info.setCourseCode(course.getCourseCode());
        info.setSemester(course.getSemester());
        info.setHours(course.getHours());
        return ApiResponse.success(info);
    }

    @Override
    public ApiResponse<Void> removeTeacherCourse(String teacherId, Long courseId,
                                                  String operatorId, String operatorName) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new ResourceNotFoundException("教师不存在: " + teacherId);
        }
        TeacherCourse course = teacherCourseMapper.selectById(courseId);
        if (course == null || !course.getTeacherId().equals(teacherId)) {
            throw new ResourceNotFoundException("课程不存在: " + courseId);
        }

        teacherCourseMapper.deleteById(courseId);

        logService.createLog(operatorId, operatorName, OperationType.DELETE.getValue(),
                "teacher_course", String.valueOf(courseId), course.getCourseName(),
                "删除教师课程: " + teacher.getName() + " - " + course.getCourseName(), null, "WARN");

        return ApiResponse.success();
    }

    private List<TeacherVO.CourseInfo> getTeacherCoursesList(String teacherId) {
        LambdaQueryWrapper<TeacherCourse> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TeacherCourse::getTeacherId, teacherId);
        List<TeacherCourse> courses = teacherCourseMapper.selectList(wrapper);

        List<TeacherVO.CourseInfo> result = new ArrayList<>();
        for (TeacherCourse c : courses) {
            TeacherVO.CourseInfo info = new TeacherVO.CourseInfo();
            info.setId(c.getId());
            info.setCourseName(c.getCourseName());
            info.setCourseCode(c.getCourseCode());
            info.setSemester(c.getSemester());
            info.setHours(c.getHours());
            result.add(info);
        }
        return result;
    }

    private List<ChangeVO> computeTeacherChanges(Teacher existing, UpdateTeacherRequest request) {
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
        addChangeIfDifferent(changes, "department", "院系", existing.getDepartment(), request.getDepartment());
        addChangeIfDifferent(changes, "title", "职称", existing.getTitle(), request.getTitle());
        addChangeIfDifferent(changes, "avatar", "头像", existing.getAvatar(), request.getAvatar());
        addChangeIfDifferent(changes, "status", "状态",
                existing.getStatus() != null ? String.valueOf(existing.getStatus()) : null,
                request.getStatus() != null ? String.valueOf(request.getStatus()) : null);

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
