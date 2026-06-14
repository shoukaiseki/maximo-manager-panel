@echo off
echo.
echo [信息] 启动 Solon 后端服务 loc
echo.

setLocal
%~d0
cd %~dp0

cd ..\server

set JAVA_HOME=D:\usr\java\jdk-17.0.19x64
if not defined JAVA_HOME (
    set JAVA_HOME=D:\usr\java\jdk-17.0.19x64
    echo [信息] 使用默认 JAVA_HOME: %JAVA_HOME%
) else (
    echo [信息] 使用系统 JAVA_HOME: %JAVA_HOME%
)
set PATH=%JAVA_HOME%\bin;%PATH%

echo [信息] 编译打包...
call D:\usr\apache\apache-maven-3.9.7\bin\mvn.cmd package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [错误] 编译失败，请检查错误信息
    pause
    exit /b 1
)

echo [信息] 启动 Solon 服务...
java -jar target\solon-server-1.0.0.jar --env=my

endLocal