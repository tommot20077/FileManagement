CREATE DATABASE IF NOT EXISTS file_management;

USE file_management;
CREATE TABLE IF NOT EXISTS users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    email         VARCHAR(50)  NOT NULL UNIQUE,
    role          VARCHAR(20)  NOT NULL,
    storage_limit BIGINT       NOT NULL,
    used_storage  BIGINT       NOT NULL DEFAULT 0,
    INDEX idx_user_name (username),
    INDEX idx_user_email (email)
);
CREATE TABLE IF NOT EXISTS tokens
(
    id                                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                             BIGINT   NOT NULL UNIQUE,
    jwt_token_version                   CHAR(18) NOT NULL,
    jwt_token_expire_time               DATETIME,
    reset_verification_code             VARCHAR(10),
    reset_verification_code_expire_time DATETIME,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id)
);
CREATE TABLE IF NOT EXISTS server_file_metadata
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_size        BIGINT              NOT NULL,
    mime_type        VARCHAR(255)        NULL,
    file_type        VARCHAR(30)         NOT NULL,
    upload_time      DATETIME            NOT NULL,
    last_access_time DATETIME            NOT NULL,
    grid_fs_id       VARCHAR(255)        NOT NULL UNIQUE,
    md5              CHAR(32)            NOT NULL,
    owners           JSON DEFAULT ('[]') NOT NULL,
    CONSTRAINT check_owners_json CHECK (JSON_VALID(owners)),
    INDEX idx_md5 (md5)
);
CREATE TABLE IF NOT EXISTS user_file_metadata
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    server_file_id   BIGINT,
    filename         VARCHAR(255) NOT NULL,
    parent_folder_id BIGINT                DEFAULT NULL,
    is_star          BOOLEAN      NOT NULL DEFAULT 0,
    file_type        VARCHAR(20)  NOT NULL,
    share_type       VARCHAR(10)  NOT NULL,
    upload_time      DATETIME     NOT NULL,
    last_access_time DATETIME     NOT NULL,
    is_deleted       BOOLEAN               DEFAULT 0 NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (server_file_id) REFERENCES server_file_metadata (id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES user_file_metadata (id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_parent_folder_id (parent_folder_id),
    INDEX idx_user_type (user_id, file_type),
    INDEX idx_user_star (user_id, is_star),
    INDEX idx_user_delete (user_id, is_deleted),
    INDEX idx_user_access (user_id, last_access_time),
    FULLTEXT INDEX idx_filename (filename) WITH PARSER ngram
);
CREATE TABLE IF NOT EXISTS transfers_task
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    transfer_task_id VARCHAR(255) NOT NULL,
    md5              CHAR(32)     NOT NULL,
    grid_fs_id       VARCHAR(255) UNIQUE,
    file_size        BIGINT       NOT NULL,
    start_time       DATETIME     NOT NULL,
    finish_time      DATETIME,
    message          VARCHAR(255),
    status           VARCHAR(20)  NOT NULL
);
CREATE TABLE IF NOT EXISTS user_online_file
(
    id                     BIGINT PRIMARY KEY,
    file_size              BIGINT NOT NULL,
    content                JSON   NOT NULL,
    last_modified_by       BIGINT NOT NULL,
    current_snapshot_count INT,
    last_history_version   BIGINT,
    is_match_history       BOOLEAN,
    FOREIGN KEY (id) REFERENCES user_file_metadata (id) ON DELETE CASCADE,
    CONSTRAINT check_content_json CHECK (JSON_VALID(content))
);
CREATE TABLE IF NOT EXISTS user_online_file_history
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id          BIGINT   NOT NULL,
    version          BIGINT   NOT NULL,
    previous_version BIGINT,
    diff             JSON,
    note             VARCHAR(1024),
    is_snapshot      BOOLEAN  NOT NULL,
    snapshot_content JSON,
    modified_time    DATETIME NOT NULL,
    modified_by      BIGINT   NOT NULL,
    FOREIGN KEY (file_id) REFERENCES user_online_file (id) ON DELETE CASCADE,
    CONSTRAINT check_diff_json CHECK (JSON_VALID(diff)),
    INDEX idx_file_id (file_id),
    INDEX idx_file_version (file_id, version),
    INDEX idx_file_p_version (file_id, previous_version)
);
CREATE TABLE IF NOT EXISTS file_trash_record
(
    file_id          BIGINT PRIMARY KEY,
    parent_folder_id BIGINT,
    user_id          BIGINT   NOT NULL,
    delete_time      DATETIME NOT NULL,
    FOREIGN KEY (file_id) REFERENCES user_file_metadata (id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS user_file_share_record
(
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    UNIQUE KEY unique_file_user (file_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_file_id (file_id),
    FOREIGN KEY (file_id) REFERENCES user_file_metadata (id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users (id)
);