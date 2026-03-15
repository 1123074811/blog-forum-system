CREATE TABLE IF NOT EXISTS security_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trace_id VARCHAR(64),
    user_id BIGINT,
    ip VARCHAR(50),
    user_agent VARCHAR(255),
    method VARCHAR(10),
    path VARCHAR(255),
    operation VARCHAR(100),
    result VARCHAR(20),
    reason_code VARCHAR(50),
    detail TEXT,
    created_at VARCHAR(32),
    INDEX idx_ip (ip),
    INDEX idx_user (user_id),
    INDEX idx_created (created_at)
);
