-- Create databases for v-im master and ruoyi slave data sources
CREATE DATABASE IF NOT EXISTS `v-im-2025` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `ruoyi-vue-plus` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application users with matching credentials from the README
CREATE USER IF NOT EXISTS 'v-im-2025'@'%' IDENTIFIED BY 'v-im-2025';
CREATE USER IF NOT EXISTS 'ruoyi-vue-plus'@'%' IDENTIFIED BY 'ruoyi-vue-plus';

GRANT ALL PRIVILEGES ON `v-im-2025`.* TO 'v-im-2025'@'%';
GRANT ALL PRIVILEGES ON `ruoyi-vue-plus`.* TO 'ruoyi-vue-plus'@'%';
FLUSH PRIVILEGES;

-- Import the provided schemas/data from the shared doc directory
USE `v-im-2025`;
SOURCE /docker-entrypoint-initdb.d/source/v-im.sql;

USE `ruoyi-vue-plus`;
SOURCE /docker-entrypoint-initdb.d/source/sys.sql;
