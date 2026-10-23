@echo off
chcp 65001 >nul
title LKBoard - Деплой сайта и обновлений

echo ==============================================
echo       LKBoard Release & Update Deployer
echo ==============================================
echo.

where py >nul 2>&1
if %ERRORLEVEL% equ 0 (
    py deploy_update.py
) else (
    where python >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        python deploy_update.py
    ) else (
        echo [ОШИБКА] Python не найден в системе.
    )
)

echo.
pause
