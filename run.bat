@echo off
chcp 65001 >nul
title Lkboard - Р—Р°РїСѓСЃРє РїСЂРёР»РѕР¶РµРЅРёСЏ

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)

echo ==============================================
echo           Lkboard Desktop Launcher
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Р—Р°РїСѓСЃРє РїСЂРёР»РѕР¶РµРЅРёСЏ...
echo.

call gradlew.bat run

if %ERRORLEVEL% neq 0 (
    echo.
    echo [РћРЁРР‘РљРђ] РџСЂРёР»РѕР¶РµРЅРёРµ Р·Р°РІРµСЂС€РёР»РѕСЃСЊ СЃ РєРѕРґРѕРј РѕС€РёР±РєРё %ERRORLEVEL%.
    pause
)
