@echo off
chcp 65001 >nul
title Lkboard - Р—Р°РїСѓСЃРє С‚РµСЃС‚РѕРІ

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)

echo ==============================================
echo            Lkboard Test Runner
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo Р—Р°РїСѓСЃРє СЋРЅРёС‚-С‚РµСЃС‚РѕРІ (jvmTest)...
echo.

call gradlew.bat jvmTest

if %ERRORLEVEL% equ 0 (
    echo.
    echo ==============================================
    echo [РЈРЎРџР•РҐ] Р’СЃРµ С‚РµСЃС‚С‹ РїСЂРѕР№РґРµРЅС‹ Р±РµР· РѕС€РёР±РѕРє!
    echo РћС‚С‡РµС‚ Рѕ С‚РµСЃС‚Р°С…: build\reports\tests\jvmTest\index.html
    echo ==============================================
) else (
    echo.
    echo [РћРЁРР‘РљРђ] РћР±РЅР°СЂСѓР¶РµРЅС‹ РѕС€РёР±РєРё РїСЂРё РІС‹РїРѕР»РЅРµРЅРёРё С‚РµСЃС‚РѕРІ.
)

echo.
pause
