@echo off
chcp 65001 >nul
title Lkboard - РЎР±РѕСЂРєР° Windows .exe

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\ms-21.0.11" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\ms-21.0.11"
    )
)

echo ==============================================
echo        Lkboard Windows .exe Builder
echo ==============================================
echo JAVA_HOME: %JAVA_HOME%
echo.
echo РЎР±РѕСЂРєР° РґРёСЃС‚СЂРёР±СѓС‚РёРІР° СЃРѕ РІСЃС‚СЂРѕРµРЅРЅС‹Рј JDK...
echo РџРѕР¶Р°Р»СѓР№СЃС‚Р°, РїРѕРґРѕР¶РґРёС‚Рµ (1-2 РјРёРЅСѓС‚С‹)...
echo.

call gradlew.bat packageDistributionForCurrentOS

if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo [РЈРЎРџР•РҐ] РЎР±РѕСЂРєР° .exe Рё .msi СѓСЃРїРµС€РЅРѕ Р·Р°РІРµСЂС€РµРЅР°!
    echo Р¤Р°Р№Р»С‹ РЅР°С…РѕРґСЏС‚СЃСЏ РІ РїР°РїРєРµ: build\compose\binaries\main\exe
    echo ========================================================
    echo РћС‚РєСЂС‹РІР°СЋ РїР°РїРєСѓ СЃ РіРѕС‚РѕРІС‹Рј СѓСЃС‚Р°РЅРѕРІС‰РёРєРѕРј...
    explorer.exe "build\compose\binaries\main\exe"
) else (
    echo.
    echo [РћРЁРР‘РљРђ] РџСЂРѕРёР·РѕС€Р»Р° РѕС€РёР±РєР° РїСЂРё СЃР±РѕСЂРєРµ РґРёСЃС‚СЂРёР±СѓС‚РёРІР°.
)

echo.
pause
