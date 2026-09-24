-- Spring AI 会话记忆表（MySQL 方言）
-- 与 Spring AI 2.0.0 内置的 schema-mysql.sql 完全一致
-- 数据库名：spring_ai（或你实际使用的库）
--
-- 使用方式：
--   1) 开发环境：应用启动时已自动建表（spring.ai.chat.memory.repository.jdbc.initialize-schema=always）
--   2) 手动执行：mysql -u root -p spring_ai < chat-memory-mysql.sql

CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    `conversation_id` VARCHAR(36) NOT NULL COMMENT '会话 ID',
    `content`         TEXT       NOT NULL COMMENT '消息内容（JSON 序列化）',
    `type`            ENUM('USER', 'ASSISTANT', 'SYSTEM', 'TOOL') NOT NULL COMMENT '消息类型',
    `timestamp`       TIMESTAMP  NOT NULL COMMENT '记录时间',
    `sequence_id`     BIGINT     NOT NULL COMMENT '同一会话内的顺序号',

    INDEX `SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX` (`conversation_id`, `timestamp`),
    INDEX `SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_SEQUENCE_ID_IDX` (`conversation_id`, `sequence_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
