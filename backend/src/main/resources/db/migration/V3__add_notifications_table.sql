-- SMS Nexus - Add Notifications Table
-- V3__add_notifications_table.sql

CREATE TABLE IF NOT EXISTS `notifications` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    `notification_id` VARCHAR(20)    NOT NULL        COMMENT '通知ID',
    `admin_id`        VARCHAR(20)    NOT NULL        COMMENT '接收者管理员ID',
    `title`           VARCHAR(200)   NOT NULL        COMMENT '通知标题',
    `content`         TEXT           DEFAULT NULL    COMMENT '通知内容',
    `type`            VARCHAR(30)    DEFAULT 'SYSTEM' COMMENT '通知类型: SYSTEM/LOGIN/OPERATION/BACKUP/ALERT',
    `is_read`         TINYINT        DEFAULT 0       COMMENT '是否已读: 0=未读 1=已读',
    `created_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_notification_id` (`notification_id`),
    KEY `idx_admin_read` (`admin_id`, `is_read`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
