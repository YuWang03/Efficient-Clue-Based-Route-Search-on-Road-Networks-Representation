#!/bin/bash

# ==========================================
# CRS 論文復現專案執行腳本
# Clue-based Route Searching (CRS) Project
# 
# 包含：
# - 四種演算法視覺化演示 (GCS, CDP, BAB w/ AB-tree, BAB w/ PB-tree)
# - 五組效能分析圖表生成
# ==========================================

# 顏色定義 (美化輸出)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 取得腳本所在目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
EVAL_DIR="$SCRIPT_DIR"
VIS_DIR="$PROJECT_ROOT/visualize"

# 預設 HTTP Server Port
HTTP_PORT=8000

# ==========================================
# 函數定義
# ==========================================

print_header() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║     CRS - Clue-based Route Searching 論文復現專案           ║"
    echo "║     Performance Evaluation & Visualization Suite            ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_menu() {
    echo -e "${YELLOW}請選擇執行模式 (Select a mode):${NC}"
    echo ""
    echo -e "  ${GREEN}[1]${NC} 📊 Performance Benchmark - 生成所有效能分析圖表"
    echo -e "      (Query Time, Keyword Frequency, Query Distance, Epsilon, Index Size)"
    echo ""
    echo -e "  ${GREEN}[2]${NC} 🗺️  Visualization Demo - 開啟演算法視覺化網頁"
    echo -e "      (GCS, CDP, BAB w/ AB-tree, BAB w/ PB-tree)"
    echo ""
    echo -e "  ${GREEN}[3]${NC} 📈 Accuracy Analysis - 生成 GCS 準確度分析圖表"
    echo -e "      (Matching Ratio & Hitting Ratio)"
    echo ""
    echo -e "  ${GREEN}[4]${NC} 💾 Index Size Comparison - 生成索引大小比較圖表"
    echo -e "      (AB-tree vs PB-tree 空間效率)"
    echo ""
    echo -e "  ${GREEN}[5]${NC} 🚀 Run All - 執行所有分析並開啟視覺化"
    echo ""
    echo -e "  ${GREEN}[q]${NC} 退出 (Quit)"
    echo ""
}

check_python() {
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        echo -e "${RED}Error: Python not found! Please install Python 3.${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Using Python: $($PYTHON_CMD --version)${NC}"
}

run_performance_benchmark() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running Performance Benchmark (效能分析)"
    echo "  Algorithms: GCS, CDP, BAB (w/ AB-tree), BAB (w/ PB-tree)"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    cd "$EVAL_DIR"
    
    echo -e "${YELLOW}執行 QT.py - 查詢時間比較分析...${NC}"
    echo ""
    
    # 使用 echo 自動輸入 'Y' 來使用 demo 數據
    echo "Y" | $PYTHON_CMD QT.py
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Performance benchmark completed!${NC}"
        echo -e "${GREEN}  Generated files:${NC}"
        echo -e "    - query_time_comparison.png"
        echo -e "    - keyword_frequency_comparison.png"
        echo -e "    - query_distance_comparison.png"
        echo -e "    - epsilon_comparison.png"
    else
        echo -e "${RED}✗ Error running performance benchmark${NC}"
    fi
}

run_accuracy_analysis() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running GCS Accuracy Analysis (GCS 準確度分析)"
    echo "  Metrics: Matching Ratio, Hitting Ratio"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    cd "$EVAL_DIR"
    
    echo -e "${YELLOW}執行 Accuracy_of_GCS.py...${NC}"
    $PYTHON_CMD Accuracy_of_GCS.py
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Accuracy analysis completed!${NC}"
        echo -e "${GREEN}  Generated file: accuracy_of_gcs.png${NC}"
    else
        echo -e "${RED}✗ Error running accuracy analysis${NC}"
    fi
}

run_index_size_comparison() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running Index Size Comparison (索引大小比較)"
    echo "  Comparing: AB-tree vs PB-tree space efficiency"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    cd "$EVAL_DIR"
    
    echo -e "${YELLOW}執行 Index_Size_Comparison.py...${NC}"
    $PYTHON_CMD Index_Size_Comparison.py
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ Index size comparison completed!${NC}"
        echo -e "${GREEN}  Generated files:${NC}"
        echo -e "    - index_size_comparison.png"
        echo -e "    - index_size_trend.png"
        echo -e "    - space_savings_ratio.png"
    else
        echo -e "${RED}✗ Error running index size comparison${NC}"
    fi
}

start_visualization_server() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Starting Visualization Server (視覺化演示)"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    cd "$VIS_DIR"
    
    echo -e "${YELLOW}Starting HTTP server on port $HTTP_PORT...${NC}"
    echo ""
    echo -e "${GREEN}Available Visualizations:${NC}"
    echo -e "  🔹 GCS Algorithm:        http://localhost:$HTTP_PORT/gcs_visualization.html"
    echo -e "  🔹 CDP Algorithm:        http://localhost:$HTTP_PORT/cdp_visualization.html"
    echo -e "  🔹 BAB (AB-tree):        http://localhost:$HTTP_PORT/abtree_visualization.html"
    echo -e "  🔹 BAB (PB-tree):        http://localhost:$HTTP_PORT/pbtree_visualization.html"
    echo -e "  🔹 BAB Visualization:    http://localhost:$HTTP_PORT/bab_visualization.html"
    echo ""
    echo -e "${CYAN}Press Ctrl+C to stop the server${NC}"
    echo ""
    # Open browser automatically (cross-platform)
    URL="http://localhost:$HTTP_PORT/gcs_visualization.html"
    if command -v xdg-open &> /dev/null; then
        xdg-open "$URL" &
    elif command -v open &> /dev/null; then
        open "$URL" &
    elif command -v start &> /dev/null; then
        start "$URL"
    else
        echo -e "${YELLOW}Please open $URL in your browser manually.${NC}"
    fi

    $PYTHON_CMD -m http.server $HTTP_PORT
}

run_all() {
    echo -e "${PURPLE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║              Running Complete Analysis Suite                 ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # 1. Performance Benchmark
    run_performance_benchmark
    echo ""
    
    # 2. Accuracy Analysis
    run_accuracy_analysis
    echo ""
    
    # 3. Index Size Comparison
    run_index_size_comparison
    echo ""
    
    # 4. Summary
    echo -e "${GREEN}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  All analyses completed! Generated files in: $EVAL_DIR"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    echo -e "${YELLOW}是否要啟動視覺化伺服器？ (Start visualization server?) [y/N]${NC}"
    read -p "" start_vis
    
    if [[ "$start_vis" =~ ^[Yy]$ ]]; then
        start_visualization_server
    fi
}

list_generated_files() {
    echo -e "${CYAN}Generated Analysis Files:${NC}"
    echo ""
    if [ -d "$EVAL_DIR" ]; then
        ls -la "$EVAL_DIR"/*.png 2>/dev/null || echo "  No PNG files found yet."
    fi
}

# ==========================================
# 主程式
# ==========================================

main() {
    print_header
    check_python
    echo ""
    
    # 如果有命令列參數，直接執行對應模式
    if [ $# -gt 0 ]; then
        case "$1" in
            benchmark|1)
                run_performance_benchmark
                ;;
            visual|2)
                start_visualization_server
                ;;
            accuracy|3)
                run_accuracy_analysis
                ;;
            index|4)
                run_index_size_comparison
                ;;
            all|5)
                run_all
                ;;
            *)
                echo -e "${RED}Unknown argument: $1${NC}"
                echo "Usage: $0 [benchmark|visual|accuracy|index|all]"
                exit 1
                ;;
        esac
        exit 0
    fi
    
    # 互動式選單
    while true; do
        print_menu
        read -p "Enter choice [1-5, q]: " choice
        echo ""
        
        case "$choice" in
            1)
                run_performance_benchmark
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            2)
                start_visualization_server
                ;;
            3)
                run_accuracy_analysis
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            4)
                run_index_size_comparison
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            5)
                run_all
                ;;
            q|Q)
                echo -e "${GREEN}Thank you for using CRS Evaluation Suite!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Invalid choice. Please enter 1-5 or q.${NC}"
                echo ""
                ;;
        esac
    done
}

# 執行主程式
main "$@"
