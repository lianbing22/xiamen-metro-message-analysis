-- 创建数据库和用户
CREATE DATABASE xiamen_metro_message;
CREATE USER metro_user WITH ENCRYPTED PASSWORD 'metro_password';
GRANT ALL PRIVILEGES ON DATABASE xiamen_metro_message TO metro_user;

-- 连接到数据库
\c xiamen_metro_message;

-- 创建扩展
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- 授权扩展
GRANT ALL ON SCHEMA public TO metro_user;