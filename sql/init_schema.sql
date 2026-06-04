-- 百味智厨 PostgreSQL 初始化脚本
-- 使用方式（容器已启动时）：
--   docker exec -i postgres-pgvector psql -U root -d mydb < sql/init_schema.sql
-- 或在 psql 内：
--   \i /path/to/init_schema.sql
-- Windows PowerShell 示例：
--   Get-Content sql\init_schema.sql | docker exec -i postgres-pgvector psql -U root -d mydb
-- PS C:\WINDOWS\system32> docker run -d `
-- >>   --name postgres-pgvector `
-- >>   -e POSTGRES_USER=root `
-- >>   -e POSTGRES_PASSWORD=123456 `
-- >>   -e POSTGRES_DB=mydb `
-- >>   -p 5432:5432 `
-- >>   -v E:/postgres-data:/var/lib/postgresql/data `
-- >>   pgvector/pgvector:pg17

-- docker exec -it postgres-pgvector psql -U myuser -d mydb

-- RAG 向量扩展（pgvector 镜像一般已自带，执行一次无妨）
CREATE EXTENSION IF NOT EXISTS vector;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    email           VARCHAR(128),
    phone           VARCHAR(32),
    password_hash   VARCHAR(256),
    status          SMALLINT     NOT NULL DEFAULT 1,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.status IS '0-禁用 1-正常';

-- 会话表（conversation_id 对应接口参数 chatId）
CREATE TABLE IF NOT EXISTS chat_conversation (
    id                BIGSERIAL PRIMARY KEY,
    conversation_id   VARCHAR(64)  NOT NULL,
    user_id           BIGINT,
    title             VARCHAR(128),
    scene             VARCHAR(32)  NOT NULL DEFAULT 'chef_app',
    model             VARCHAR(32),
    message_count     INT          NOT NULL DEFAULT 0,
    last_message_at   TIMESTAMP,
    is_deleted        SMALLINT     NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_conversation_id UNIQUE (conversation_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_conversation_user_id ON chat_conversation (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversation_last_msg ON chat_conversation (last_message_at DESC);

COMMENT ON TABLE chat_conversation IS '聊天会话';
COMMENT ON COLUMN chat_conversation.scene IS 'chef_app / chef_manus / rag 等';

-- 消息表（对话记忆持久化）
CREATE TABLE IF NOT EXISTS chat_message (
    id                BIGSERIAL PRIMARY KEY,
    conversation_id   VARCHAR(64)  NOT NULL,
    role              VARCHAR(16)  NOT NULL,
    content           TEXT         NOT NULL,
    token_usage       INT,
    metadata          JSONB,
    is_deleted        SMALLINT     NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_message_conversation_id ON chat_message (conversation_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at ON chat_message (conversation_id, created_at);

COMMENT ON TABLE chat_message IS '会话消息';
COMMENT ON COLUMN chat_message.role IS 'user / assistant / system / tool';
