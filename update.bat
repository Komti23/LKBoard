@echo off
chcp 65001 >nul
title Lkboard - РћС‡РёСЃС‚РєР° Рё РїРµСЂРµСЃР±РѕСЂРєР°

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)

echo ==============================================
echo        Lkboard Clean & Rebuild
echo ==============================================
echo РћС‡РёСЃС‚РєР° РєСЌС€Р° СЃР±РѕСЂРєРё...
call gradlew.bat clean

echo РљРѕРјРїРёР»СЏС†РёСЏ РїСЂРѕРµРєС‚Р°...
call gradlew.bat compileKotlinJvm

if %ERRORLEVEL% equ 0 (
    echo.
    echo [РЈРЎРџР•РҐ] РџСЂРѕРµРєС‚ СѓСЃРїРµС€РЅРѕ РїРµСЂРµСЃРѕР±СЂР°РЅ!
) else (
    echo.
    echo [РћРЁРР‘РљРђ] РћС€РёР±РєР° РєРѕРјРїРёР»СЏС†РёРё.
)

echo.
pause
