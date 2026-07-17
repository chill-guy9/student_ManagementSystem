-- SMS Nexus - Seed Data
-- V2__seed_data.sql

-- Admin users (BCrypt hashed passwords - change on first login)
INSERT INTO `admins` (`admin_id`, `username`, `password`, `real_name`, `role`, `email`, `phone`, `status`) VALUES
('adm-001', 'admin',   '$2a$10$wsTOh3ER4p2Lt36k6ErvzulBvJZOQ9w1.L73oc1t/UAs8AUp2kWjq', '系统管理员', 'super_admin',  'admin@smsnexus.com',   '13800000001', 1),
('adm-002', 'usermgr', '$2a$10$wsTOh3ER4p2Lt36k6ErvzulBvJZOQ9w1.L73oc1t/UAs8AUp2kWjq', '用户管理员', 'user_admin',   'usermgr@smsnexus.com', '13800000002', 1),
('adm-003', 'auditor', '$2a$10$wsTOh3ER4p2Lt36k6ErvzulBvJZOQ9w1.L73oc1t/UAs8AUp2kWjq', '日志审计员', 'log_auditor',  'auditor@smsnexus.com', '13800000003', 1),
('adm-004', 'viewer',  '$2a$10$wsTOh3ER4p2Lt36k6ErvzulBvJZOQ9w1.L73oc1t/UAs8AUp2kWjq', '只读用户',   'read_only',    'viewer@smsnexus.com',  '13800000004', 1);

-- Sample students
INSERT INTO `students` (`student_id`, `name`, `gender`, `birth_date`, `phone`, `email`, `address`, `class_name`, `major`, `grade`, `enrollment_date`, `status`) VALUES
('stu-001', '张三',   1, '2003-05-15', '13900000001', 'zhangsan@edu.cn',    '北京市海淀区',   '计科2101', '计算机科学与技术', '2021', '2021-09-01', 'active'),
('stu-002', '李四',   0, '2003-08-22', '13900000002', 'lisi@edu.cn',        '上海市浦东新区', '计科2101', '计算机科学与技术', '2021', '2021-09-01', 'active'),
('stu-003', '王五',   1, '2002-11-03', '13900000003', 'wangwu@edu.cn',      '广州市天河区',   '软工2102', '软件工程',         '2021', '2021-09-01', 'active'),
('stu-004', '赵六',   0, '2003-02-14', '13900000004', 'zhaoliu@edu.cn',     '深圳市南山区',   '软工2102', '软件工程',         '2021', '2021-09-01', 'active'),
('stu-005', '孙七',   1, '2004-01-20', '13900000005', 'sunqi@edu.cn',       '杭州市西湖区',   '数据2101', '数据科学',         '2022', '2022-09-01', 'active'),
('stu-006', '周八',   0, '2004-06-30', '13900000006', 'zhouba@edu.cn',      '成都市武侯区',   '数据2101', '数据科学',         '2022', '2022-09-01', 'active'),
('stu-007', '吴九',   1, '2003-09-12', '13900000007', 'wujiu@edu.cn',       '南京市鼓楼区',   '计科2201', '计算机科学与技术', '2022', '2022-09-01', 'active'),
('stu-008', '郑十',   0, '2004-04-05', '13900000008', 'zhengshi@edu.cn',    '武汉市洪山区',   '计科2201', '计算机科学与技术', '2022', '2022-09-01', 'active'),
('stu-009', '陈晓明', 1, '2003-12-25', '13900000009', 'chenxm@edu.cn',      '西安市雁塔区',   '软工2203', '软件工程',         '2022', '2022-09-01', 'suspended'),
('stu-010', '林小红', 0, '2004-07-18', '13900000010', 'linxh@edu.cn',       '重庆市渝中区',   '数据2202', '数据科学',         '2022', '2022-09-01', 'graduated');

-- Sample teachers
INSERT INTO `teachers` (`teacher_id`, `name`, `gender`, `birth_date`, `phone`, `email`, `department`, `title`, `status`) VALUES
('tch-001', '刘教授', 1, '1975-03-10', '13700000001', 'liuprof@edu.cn', '计算机学院', '教授',   1),
('tch-002', '陈副教授', 0, '1980-07-22', '13700000002', 'chenprof@edu.cn', '计算机学院', '副教授', 1),
('tch-003', '王讲师', 1, '1985-11-05', '13700000003', 'wanglect@edu.cn', '软件学院',   '讲师',   1),
('tch-004', '李教授', 0, '1972-09-18', '13700000004', 'liprof@edu.cn',   '数据学院',   '教授',   1),
('tch-005', '赵讲师', 1, '1988-04-30', '13700000005', 'zhalect@edu.cn',  '软件学院',   '讲师',   1);

-- Sample teacher courses
INSERT INTO `teacher_courses` (`teacher_id`, `course_name`, `course_code`, `semester`, `hours`) VALUES
('tch-001', '数据结构与算法', 'CS201', '2024-1', 64),
('tch-001', '编译原理',       'CS301', '2024-1', 48),
('tch-002', '操作系统',       'CS202', '2024-1', 64),
('tch-002', '计算机网络',     'CS203', '2024-2', 48),
('tch-003', '软件工程',       'SE201', '2024-1', 48),
('tch-003', '设计模式',       'SE301', '2024-2', 32),
('tch-004', '数据库原理',     'DS201', '2024-1', 64),
('tch-004', '大数据技术',     'DS301', '2024-2', 48),
('tch-005', 'Web开发技术',    'SE202', '2024-1', 48),
('tch-005', '移动应用开发',   'SE302', '2024-2', 32);

-- Default notification configs
INSERT INTO `notification_configs` (`config_key`, `config_value`, `description`) VALUES
('login_notification',      'true', '登录通知'),
('operation_notification',  'true', '操作通知'),
('backup_notification',     'true', '备份通知'),
('system_alert',            'true', '系统告警');

-- Default system settings
INSERT INTO `system_settings` (`setting_key`, `setting_value`, `description`) VALUES
('max_login_attempts',    '5',  '最大登录尝试次数'),
('login_lock_minutes',    '15', '登录锁定时长(分钟)'),
('session_timeout',       '1440', '会话超时时间(分钟)'),
('log_retention_days',    '90', '日志保留天数'),
('auto_backup_enabled',   'false', '是否启用自动备份'),
('backup_retention_days', '30', '备份保留天数');
