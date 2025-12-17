@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ==========================================
:: CRS 論文復現專案執行腳本 (Windows 版本)
:: Clue-based Route Searching (CRS) Project
:: ==========================================

title CRS Evaluation Suite

:header
cls
echo ╔══════════════════════════════════════════════════════════════╗
echo ║     CRS - Clue-based Route Searching 論文復現專案           ║
echo ║     Performance Evaluation ^& Visualization Suite            ║
echo ╚══════════════════════════════════════════════════════════════╝
echo.

:: 檢查 Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found! Please install Python 3.
    pause
    exit /b 1
)
echo [OK] Python found.
echo.

:menu
echo 請選擇執行模式 (Select a mode):
echo.
echo   [1] Performance Benchmark - 生成所有效能分析圖表
echo       (Query Time, Keyword Frequency, Query Distance, Epsilon)
echo.
echo   [2] Visualization Demo - 開啟演算法視覺化網頁
echo       (GCS, CDP, BAB w/ AB-tree, BAB w/ PB-tree)
echo.
echo   [3] Accuracy Analysis - 生成 GCS 準確度分析圖表
echo       (Matching Ratio ^& Hitting Ratio)
echo.
echo   [4] Index Size Comparison - 生成索引大小比較圖表
echo       (AB-tree vs PB-tree 空間效率)
echo.
echo   [5] Run All - 執行所有分析
echo.
echo   [Q] 退出 (Quit)
echo.

set /p choice="Enter choice [1-5, Q]: "

if "%choice%"=="1" goto benchmark
if "%choice%"=="2" goto visual
if "%choice%"=="3" goto accuracy
if "%choice%"=="4" goto index
if "%choice%"=="5" goto all
if /i "%choice%"=="q" goto quit
echo Invalid choice!
timeout /t 2 >nul
goto header

:benchmark
echo.
echo ══════════════════════════════════════════════════════════════
echo   Running Performance Benchmark
echo   Algorithms: GCS, CDP, BAB (w/ AB-tree), BAB (w/ PB-tree)
echo ══════════════════════════════════════════════════════════════
echo.
cd /d "%~dp0"
echo Y | python QT.py
echo.
echo [DONE] Performance benchmark completed!
echo Generated: query_time_comparison.png, keyword_frequency_comparison.png
echo            query_distance_comparison.png, epsilon_comparison.png
echo.
pause
goto header

:visual
echo.
echo ══════════════════════════════════════════════════════════════
echo   Starting Visualization Server
echo ══════════════════════════════════════════════════════════════
echo.
cd /d "%~dp0..\visualize"
echo Available Visualizations:
echo   - GCS:        http://localhost:8000/gcs_visualization.html
echo   - CDP:        http://localhost:8000/cdp_visualization.html
echo   - AB-tree:    http://localhost:8000/abtree_visualization.html
echo   - PB-tree:    http://localhost:8000/pbtree_visualization.html
echo   - BAB:        http://localhost:8000/bab_visualization.html
echo.
echo Press Ctrl+C to stop the server
echo.
start http://localhost:8000/gcs_visualization.html
python -m http.server 8000
goto header

:accuracy
echo.
echo ══════════════════════════════════════════════════════════════
echo   Running GCS Accuracy Analysis
echo ══════════════════════════════════════════════════════════════
echo.
cd /d "%~dp0"
python Accuracy_of_GCS.py
echo.
echo [DONE] Generated: accuracy_of_gcs.png
echo.
pause
goto header

:index
echo.
echo ══════════════════════════════════════════════════════════════
echo   Running Index Size Comparison
echo ══════════════════════════════════════════════════════════════
echo.
cd /d "%~dp0"
python Index_Size_Comparison.py
echo.
echo [DONE] Generated: index_size_comparison.png, index_size_trend.png
echo                   space_savings_ratio.png
echo.
pause
goto header

:all
echo.
echo ══════════════════════════════════════════════════════════════
echo   Running Complete Analysis Suite
echo ══════════════════════════════════════════════════════════════
echo.
cd /d "%~dp0"

echo [1/3] Performance Benchmark...
echo Y | python QT.py
echo.

echo [2/3] Accuracy Analysis...
python Accuracy_of_GCS.py
echo.

echo [3/3] Index Size Comparison...
python Index_Size_Comparison.py
echo.

echo ══════════════════════════════════════════════════════════════
echo   All analyses completed!
echo ══════════════════════════════════════════════════════════════
echo.
set /p start_vis="Start visualization server? [y/N]: "
if /i "%start_vis%"=="y" goto visual
pause
goto header

:quit
echo.
echo Thank you for using CRS Evaluation Suite!
exit /b 0
