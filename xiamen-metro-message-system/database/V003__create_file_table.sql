-- 创建文件表
CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_extension VARCHAR(10) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100),
    storage_path VARCHAR(500),
    file_hash VARCHAR(32),
    upload_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    process_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    data_row_count INTEGER,
    valid_message_count INTEGER,
    invalid_message_count INTEGER,
    uploaded_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_files_file_hash ON files(file_hash);
CREATE INDEX idx_files_upload_status ON files(upload_status);
CREATE INDEX idx_files_process_status ON files(process_status);
CREATE INDEX idx_files_file_type ON files(file_type);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);
CREATE INDEX idx_files_created_at ON files(created_at);

-- 创建注释
COMMENT ON TABLE files IS '文件管理表';
COMMENT ON COLUMN files.id IS '文件ID';
COMMENT ON COLUMN files.file_name IS '文件名';
COMMENT ON COLUMN files.original_file_name IS '原始文件名';
COMMENT ON COLUMN files.file_extension IS '文件扩展名';
COMMENT ON COLUMN files.file_size IS '文件大小（字节）';
COMMENT ON COLUMN files.file_type IS '文件类型 (EXCEL, CSV)';
COMMENT ON COLUMN files.mime_type IS 'MIME类型';
COMMENT ON COLUMN files.storage_path IS 'MinIO存储路径';
COMMENT ON COLUMN files.file_hash IS '文件哈希值（MD5）';
COMMENT ON COLUMN files.upload_status IS '上传状态 (PENDING, UPLOADING, COMPLETED, FAILED)';
COMMENT ON COLUMN files.process_status IS '处理状态 (PENDING, PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN files.error_message IS '处理错误信息';
COMMENT ON COLUMN files.data_row_count IS '数据行数';
COMMENT ON COLUMN files.valid_message_count IS '有效报文数量';
COMMENT ON COLUMN files.invalid_message_count IS '无效报文数量';
COMMENT ON COLUMN files.uploaded_by IS '上传用户ID';
COMMENT ON COLUMN files.created_at IS '创建时间';
COMMENT ON COLUMN files.updated_at IS '更新时间';