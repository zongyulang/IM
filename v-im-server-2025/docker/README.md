# Docker 部署说明

本目录提供 v-im-server-2025 的一键容器化运行环境，依赖关系与项目 `readme.md` 中的说明保持一致（MySQL 主从数据源、Redis、MongoDB）。

## 预备步骤
- 确保数据库脚本存在于项目根目录的同级目录：`../../doc/v-im.sql` 与 `../../doc/sys.sql`。本仓库默认依赖该目录，无需手动拷贝。
- 首次启动会初始化数据库并自动导入上述 SQL。

## 启动
```bash
cd docker
docker compose up -d --build
```

服务启动后：
- 应用访问地址：`http://localhost:8080`
- MySQL 外部端口：`3306`
- Redis 外部端口：`6379`
- MongoDB 外部端口：`27017`

## 说明
- MySQL 创建了 `v-im-2025` 与 `ruoyi-vue-plus` 两个数据库，对应 README 中的 master/slave 数据源，并创建了同名账号及密码。
- 如果需要重新导入数据，清理 `docker_mysql-data` 数据卷后重新 `docker compose up` 即可。
- 如需调整端口或数据库账号，请编辑 `docker-compose.yml` 中的相关环境变量。
