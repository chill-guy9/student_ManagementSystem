-- SMS Nexus - Initial Schema
-- V1__init_schema.sql

CREATE TABLE IF NOT EXISTS `admins` (
    `admin_id`      VARCHAR(20)     NOT NULL        COMMENT '管理员ID',
    `username`      VARCHAR(50)     NOT NULL        COMMENT '用户名',
    `password`      VARCHAR(255)    NOT NULL        COMMENT '密码(BCrypt)',
    `real_name`     VARCHAR(50)     DEFAULT NULL    COMMENT '真实姓名',
    `role`          VARCHAR(20)     NOT NULL        COMMENT '角色: super_admin/user_admin/log_auditor/read_only',
    `email`         VARCHAR(100)    DEFAULT NULL    COMMENT '邮箱',
    `phone`         VARCHAR(20)     DEFAULT NULL    COMMENT '手机号',
    `avatar`        VARCHAR(255)    DEFAULT NULL    COMMENT '头像URL',
    `status`        TINYINT         DEFAULT 1       COMMENT '状态: 1=启用 0=禁用',
    `last_login_at` DATETIME        DEFAULT NULL    COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(45)     DEFAULT NULL    COMMENT '最后登录IP',
    `created_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT         DEFAULT 0       COMMENT '逻辑删除: 0=未删除 1=已删除',
    PRIMARY KEY (`admin_id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

CREATE TABLE IF NOT EXISTS `students` (
    `student_id`      VARCHAR(20)   NOT NULL        COMMENT '学生ID',
    `name`            VARCHAR(50)   NOT NULL        COMMENT '姓名',
    `gender`          TINYINT       DEFAULT NULL    COMMENT '性别: 0=女 1=男',
    `birth_date`      DATE          DEFAULT NULL    COMMENT '出生日期',
    `phone`           VARCHAR(20)   DEFAULT NULL    COMMENT '手机号',
    `email`           VARCHAR(100)  DEFAULT NULL    COMMENT '邮箱',
    `address`         VARCHAR(255)  DEFAULT NULL    COMMENT '地址',
    `class_name`      VARCHAR(50)   DEFAULT NULL    COMMENT '班级',
    `major`           VARCHAR(100)  DEFAULT NULL    COMMENT '专业',
    `grade`           VARCHAR(20)   DEFAULT NULL    COMMENT '年级',
    `enrollment_date` DATE          DEFAULT NULL    COMMENT '入学日期',
    `status`          VARCHAR(20)   DEFAULT 'active' COMMENT '状态: active/suspended/graduated/dropped',
    `avatar`          VARCHAR(255)  DEFAULT NULL    COMMENT '头像URL',
    `created_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT       DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (`student_id`),
    KEY `idx_class` (`class_name`),
    KEY `idx_major` (`major`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生表';

CREATE TABLE IF NOT EXISTS `teachers` (
    `teacher_id`  VARCHAR(20)   NOT NULL        COMMENT '教师ID',
    `name`        VARCHAR(50)   NOT NULL        COMMENT '姓名',
    `gender`      TINYINT       DEFAULT NULL    COMMENT '性别: 0=女 1=男',
    `birth_date`  DATE          DEFAULT NULL    COMMENT '出生日期',
    `phone`       VARCHAR(20)   DEFAULT NULL    COMMENT '手机号',
    `email`       VARCHAR(100)  DEFAULT NULL    COMMENT '邮箱',
    `department`  VARCHAR(100)  DEFAULT NULL    COMMENT '院系',
    `title`       VARCHAR(50)   DEFAULT NULL    COMMENT '职称',
    `avatar`      VARCHAR(255)  DEFAULT NULL    COMMENT '头像URL',
    `status`      TINYINT       DEFAULT 1       COMMENT '状态: 1=启用 0=禁用',
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT       DEFAULT 0       COMMENT '逻辑删除',
    PRIMARY KEY (`teacher_id`),
    KEY `idx_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师表';

CREATE TABLE IF NOT EXISTS `teacher_courses` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `teacher_id`  VARCHAR(20)   NOT NULL        COMMENT '教师ID',
    `course_name` VARCHAR(100)  NOT NULL        COMMENT '课程名称',
    `course_code` VARCHAR(30)   DEFAULT NULL    COMMENT '课程代码',
    `semester`    VARCHAR(20)   DEFAULT NULL    COMMENT '学期',
    `hours`       INT           DEFAULT 0       COMMENT '学时',
    `created_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师课程表';

CREATE TABLE IF NOT EXISTS `logs` (
    `log_id`         VARCHAR(20)   NOT NULL        COMMENT '日志ID',
    `operator_id`    VARCHAR(20)   DEFAULT NULL    COMMENT '操作者ID',
    `operator_name`  VARCHAR(50)   DEFAULT NULL    COMMENT '操作者姓名',
    `operation_type` VARCHAR(30)   NOT NULL        COMMENT '操作类型: CREATE/UPDATE/DELETE/LOGIN/LOGOUT/EXPORT/IMPORT/BACKUP/RESTORE/SYSTEM',
    `target_type`    VARCHAR(30)   DEFAULT NULL    COMMENT '目标类型',
    `target_id`      VARCHAR(50)   DEFAULT NULL    COMMENT '目标ID',
    `target_name`    VARCHAR(100)  DEFAULT NULL    COMMENT '目标名称',
    `detail`         TEXT          DEFAULT NULL    COMMENT '操作详情',
    `ip`             VARCHAR(45)   DEFAULT NULL    COMMENT 'IP地址',
    `level`          VARCHAR(10)   DEFAULT 'INFO'  COMMENT '日志级别: INFO/WARN/ERROR',
    `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`log_id`),
    KEY `idx_operator` (`operator_id`),
    KEY `idx_type` (`operation_type`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS `log_changes` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `log_id`     VARCHAR(20)   NOT NULL        COMMENT '关联日志ID',
    `field_name` VARCHAR(50)   NOT NULL        COMMENT '字段名',
    `old_value`  TEXT          DEFAULT NULL    COMMENT '旧值',
    `new_value`  TEXT          DEFAULT NULL    COMMENT '新值',
    `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_id` (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段变更记录表';

CREATE TABLE IF NOT EXISTS `admin_sessions` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `admin_id`   VARCHAR(20)   NOT NULL        COMMENT '管理员ID',
    `token`      VARCHAR(500)  NOT NULL        COMMENT 'JWT Token',
    `ip`         VARCHAR(45)   DEFAULT NULL    COMMENT 'IP地址',
    `user_agent` VARCHAR(500)  DEFAULT NULL    COMMENT '浏览器UA',
    `expired_at` DATETIME      NOT NULL        COMMENT '过期时间',
    `created_at` DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_admin_id` (`admin_id`),
    KEY `idx_token` (`token`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员会话表';

CREATE TABLE IF NOT EXISTS `notification_configs` (
    `id`          INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key`  VARCHAR(50)   NOT NULL        COMMENT '配置键',
    `config_value` TEXT         DEFAULT NULL    COMMENT '配置值',
    `description` VARCHAR(255)  DEFAULT NULL    COMMENT '描述',
    `updated_at`  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知配置表';

CREATE TABLE IF NOT EXISTS `backup_records` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `file_name`     VARCHAR(255)  NOT NULL        COMMENT '文件名',
    `file_path`     VARCHAR(500)  NOT NULL        COMMENT '文件路径',
    `file_size`     BIGINT        DEFAULT 0       COMMENT '文件大小(字节)',
    `status`        VARCHAR(20)   DEFAULT 'PENDING' COMMENT '状态: PENDING/RUNNING/SUCCESS/FAILED',
    `trigger_type`  VARCHAR(20)   DEFAULT 'MANUAL' COMMENT '触发类型: MANUAL/SCHEDULED',
    `operator_id`   VARCHAR(20)   DEFAULT NULL    COMMENT '操作者ID',
    `operator_name` VARCHAR(50)   DEFAULT NULL    COMMENT '操作者姓名',
    `started_at`    DATETIME      DEFAULT NULL    COMMENT '开始时间',
    `finished_at`   DATETIME      DEFAULT NULL    COMMENT '完成时间',
    `error_message` TEXT          DEFAULT NULL    COMMENT '错误信息',
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='备份记录表';

CREATE TABLE IF NOT EXISTS `system_settings` (
    `id`           INT           NOT NULL AUTO_INCREMENT COMMENT '主键',
    `setting_key`  VARCHAR(50)   NOT NULL        COMMENT '设置键',
    `setting_value` TEXT         DEFAULT NULL    COMMENT '设置值',
    `description`  VARCHAR(255)  DEFAULT NULL    COMMENT '描述',
    `updated_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_setting_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';
