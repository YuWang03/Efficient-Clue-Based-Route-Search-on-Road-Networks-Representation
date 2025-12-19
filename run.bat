@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ==========================================
:: CRS 論文復現專案執行腳本 (Windows 版本)
:: Clue-based Route Searching (CRS) Project
:: 簡化版本 - 直接運行 Java 程序，輸出數據結果
:: ==========================================

title CRS Project - Java Execution

:menu
echo Please select a mode:
echo.
echo   [1] GCS Algorithm
echo.
echo   [2] CDP Algorithm
echo.
echo   [3] BAB - AB-tree Version
echo.
echo   [4] BAB - PB-tree Version
echo.
echo   [5] Run All Algorithms
echo.
echo   [Q] Quit
echo.

set /p choice="Enter choice [1-5, Q]: "

if "%choice%"=="1" goto gcs
if "%choice%"=="2" goto cdp
if "%choice%"=="3" goto abtree
if "%choice%"=="4" goto pbtree
if "%choice%"=="5" goto all
if /i "%choice%"=="q" goto quit
echo Invalid choice!
timeout /t 2 >nul
goto header

:gcs
echo.
echo =============================================================
echo   GCS Algorithm (Java disabled)
echo =============================================================
echo.
echo Available evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.
pause
goto header

:cdp
echo.
echo =============================================================
echo   CDP Algorithm (Java disabled)
echo =============================================================
echo.
echo Available evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.
pause
goto header

:abtree
echo.
echo =============================================================
echo   BAB - AB-tree Version (Java disabled)
echo =============================================================
echo.
echo Available evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.
pause
goto header

:pbtree
echo.
echo =============================================================
echo   BAB - PB-tree Version (Java disabled)
echo =============================================================
echo.
echo Available evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.
pause
goto header

:all
echo.
echo ══════════════════════════════════════════════════════════════
echo   Running All Algorithms
echo ══════════════════════════════════════════════════════════════
echo.

echo [1/4] GCS - Java disabled. Showing evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.

echo [2/4] CDP - Java disabled. Showing evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.

echo [3/4] AB-tree - Java disabled. Showing evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.

echo [4/4] PB-tree - Java disabled. Showing evaluation files:
if exist "%~dp0evaluation\" (
    dir /b "%~dp0evaluation\"
) else (
    echo [WARN] No evaluation directory found at %~dp0evaluation
)
echo.

echo =============================================================
echo   All steps completed (Java disabled)
echo =============================================================
echo.
pause
goto header

:quit
echo.
echo Thank you for using CRS Evaluation Suite!
exit /b 0
