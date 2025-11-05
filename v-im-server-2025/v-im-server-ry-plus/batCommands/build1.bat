
@父目录创建再启动测试
@echo off
cd /d e:\jianzhan\dockerProje\JavaBackend\IM\v-im-server-2025
echo 正在编译项目...
call mvn clean install -DskipTests

if %errorlevel% equ 0 (
    echo 编译成功,启动应用...
    cd v-im-server-ry-plus
    call mvn spring-boot:run -DskipTests
) else (
    echo 编译失败!
    pause
)