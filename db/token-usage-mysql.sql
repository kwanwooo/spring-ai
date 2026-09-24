-- AI Token 用量与成本记录表（MySQL 方言）
--
-- 使用方式（手动执行）：
--   mysql -u root -p spring_ai < token-usage-mysql.sql

CREATE TABLE IF NOT EXISTS AI_TOKEN_USAGE (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `conversation_id`   VARCHAR(36)    NULL     COMMENT '会话 ID（单轮可为空）',
    `model`             VARCHAR(64)    NOT NULL COMMENT '模型名',
    `prompt_tokens`     INT            NOT NULL DEFAULT 0 COMMENT '输入 token 数',
    `completion_tokens` INT            NOT NULL DEFAULT 0 COMMENT '输出 token 数',
    `total_tokens`      INT            NOT NULL DEFAULT 0 COMMENT '总 token 数',
    `cost`              DECIMAL(14, 8) NOT NULL DEFAULT 0 COMMENT '成本（元）',
    `created_at`        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',

    INDEX `IDX_USAGE_MODEL` (`model`),
    INDEX `IDX_USAGE_CREATED` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI Token 用量与成本记录';
