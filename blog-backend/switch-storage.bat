@echo off
REM ============================================
REM 存储方式切换脚本 (Windows)
REM ============================================
REM 用法：
REM   switch-storage.bat minio   # 切换到 MinIO
REM   switch-storage.bat oss     # 切换到阿里云 OSS
REM ============================================

setlocal enabledelayedexpansion

set STORAGE_TYPE=%1
set ENV_FILE=.env

if "%STORAGE_TYPE%"=="" (
    echo 错误：请指定存储类型
    echo 用法: %0 [minio^|oss]
    exit /b 1
)

if not "%STORAGE_TYPE%"=="minio" if not "%STORAGE_TYPE%"=="oss" (
    echo 错误：存储类型必须是 'minio' 或 'oss'
    exit /b 1
)

if not exist "%ENV_FILE%" (
    echo 错误：找不到 .env 文件
    echo 请先从 .env.example 复制并配置 .env 文件
    exit /b 1
)

REM 备份原配置
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
copy "%ENV_FILE%" "%ENV_FILE%.backup.%TIMESTAMP%" >nul
echo 已备份原配置到 %ENV_FILE%.backup.%TIMESTAMP%

REM 创建临时文件
set TEMP_FILE=%ENV_FILE%.tmp
if exist "%TEMP_FILE%" del "%TEMP_FILE%"

REM 更新存储类型
set FOUND=0
for /f "usebackq delims=" %%a in ("%ENV_FILE%") do (
    set LINE=%%a
    echo !LINE! | findstr /b "STORAGE_TYPE=" >nul
    if !errorlevel! equ 0 (
        echo STORAGE_TYPE=%STORAGE_TYPE%>>"%TEMP_FILE%"
        set FOUND=1
    ) else (
        echo %%a>>"%TEMP_FILE%"
    )
)

REM 如果没找到，则添加
if !FOUND! equ 0 (
    echo STORAGE_TYPE=%STORAGE_TYPE%>>"%TEMP_FILE%"
)

REM 替换原文件
move /y "%TEMP_FILE%" "%ENV_FILE%" >nul

echo √ 已切换存储类型为: %STORAGE_TYPE%

if "%STORAGE_TYPE%"=="minio" (
    echo.
    echo 当前使用 MinIO 存储
    echo 请确保以下配置正确：
    echo   - MINIO_ENDPOINT
    echo   - MINIO_ACCESS_KEY
    echo   - MINIO_SECRET_KEY
    echo   - MINIO_BUCKET
) else (
    echo.
    echo 当前使用阿里云 OSS 存储
    echo 请确保以下配置正确：
    echo   - ALIYUN_OSS_ENDPOINT
    echo   - ALIYUN_OSS_ACCESS_KEY_ID
    echo   - ALIYUN_OSS_ACCESS_KEY_SECRET
    echo   - ALIYUN_OSS_BUCKET
    echo   - ALIYUN_OSS_CUSTOM_DOMAIN (可选)
)

echo.
echo 请重启应用以使配置生效

endlocal
