#!/bin/bash

# ==========================================
# CRS 論文復現專案執行腳本 (Linux/Unix 版本)
# Clue-based Route Searching (CRS) Project
# 簡化版本 - 直接運行 Java 程序，輸出數據結果
# ==========================================

# 顏色定義 (美化輸出)
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 取得腳本所在目錄
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# ==========================================
# Functions
# ==========================================

print_header() {
    echo -e "${CYAN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║     CRS - Clue-based Route Searching Project                ║"
    echo "║     Execution & Data Listing Suite                          ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

print_menu() {
    echo -e "${YELLOW}Please select a mode:${NC}"
    echo ""
    echo -e "  ${GREEN}[1]${NC} GCS Algorithm"
    echo ""
    echo -e "  ${GREEN}[2]${NC} CDP Algorithm"
    echo ""
    echo -e "  ${GREEN}[3]${NC} BAB - AB-tree Version"
    echo ""
    echo -e "  ${GREEN}[4]${NC} BAB - PB-tree Version"
    echo ""
    echo -e "  ${GREEN}[5]${NC} Run All Algorithms"
    echo ""
    echo -e "  ${GREEN}[6]${NC} Open Visualization (Web Server)"
    echo ""
    echo -e "  ${GREEN}[q]${NC} Quit"
    echo ""
}

check_java() {
    # Java execution disabled by user request. Do not attempt to run Java.
    echo -e "${YELLOW}Note: Java execution is disabled. Scripts will not run Java programs.${NC}"
}

run_gcs() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running GCS Algorithm"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    echo -e "${YELLOW}Java execution disabled. Showing available evaluation data for GCS:${NC}"
    if [ -d "$PROJECT_ROOT/evaluation" ]; then
        ls -1 "$PROJECT_ROOT/evaluation" | sed -n '1,200p' || echo "  (no files)"
    else
        echo -e "  ${RED}No evaluation directory found at $PROJECT_ROOT/evaluation${NC}"
    fi
}

run_cdp() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running CDP Algorithm"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    echo -e "${YELLOW}Java execution disabled. Showing available evaluation data for CDP:${NC}"
    if [ -d "$PROJECT_ROOT/evaluation" ]; then
        ls -1 "$PROJECT_ROOT/evaluation" | sed -n '1,200p' || echo "  (no files)"
    else
        echo -e "  ${RED}No evaluation directory found at $PROJECT_ROOT/evaluation${NC}"
    fi
}

run_abtree() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running BAB - AB-tree Version"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    echo -e "${YELLOW}Java execution disabled. Showing available evaluation data for AB-tree:${NC}"
    if [ -d "$PROJECT_ROOT/evaluation" ]; then
        ls -1 "$PROJECT_ROOT/evaluation" | sed -n '1,200p' || echo "  (no files)"
    else
        echo -e "  ${RED}No evaluation directory found at $PROJECT_ROOT/evaluation${NC}"
    fi
}

run_pbtree() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Running BAB - PB-tree Version"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
    
    echo -e "${YELLOW}Java execution disabled. Showing available evaluation data for PB-tree:${NC}"
    if [ -d "$PROJECT_ROOT/evaluation" ]; then
        ls -1 "$PROJECT_ROOT/evaluation" | sed -n '1,200p' || echo "  (no files)"
    else
        echo -e "  ${RED}No evaluation directory found at $PROJECT_ROOT/evaluation${NC}"
    fi
}

open_visualization() {
    echo -e "${BLUE}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  Visualization Web Server"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"

    DEFAULT_PORT=8000
    PORT=
    TARGET=

    # 解析參數：支持 open_visualization [target] [port] 或 open_visualization [port]
    if [ $# -gt 0 ]; then
        if [[ "$1" =~ ^[0-9]+$ ]]; then
            PORT=$1
        else
            TARGET=$1
            if [ $# -gt 1 ] && [[ "$2" =~ ^[0-9]+$ ]]; then
                PORT=$2
            fi
        fi
    fi

    if [ -z "$PORT" ]; then
        echo -e "${YELLOW}Enter port number (default: $DEFAULT_PORT):${NC}"
        read -p "Port: " PORT
        PORT=${PORT:-$DEFAULT_PORT}
    fi

    if ! [[ "$PORT" =~ ^[0-9]+$ ]] || [ "$PORT" -lt 1024 ] || [ "$PORT" -gt 65535 ]; then
        echo -e "${RED}Invalid port number! Using default port $DEFAULT_PORT${NC}"
        PORT=$DEFAULT_PORT
    fi

    VISUALIZE_DIR="$PROJECT_ROOT/visualize"

    if [ ! -d "$VISUALIZE_DIR" ]; then
        echo -e "${RED}Visualization directory not found at $VISUALIZE_DIR${NC}"
        return 1
    fi

    echo -e "${YELLOW}Available visualizations:${NC}"
    ls -1 "$VISUALIZE_DIR"/*.html 2>/dev/null | xargs -n1 basename || echo "  (no HTML files found)"
    echo ""

    if [ -z "$TARGET" ]; then
        echo -e "${YELLOW}Enter visualization to open (name or alias, e.g. cdp or cdp_visualization.html). Leave blank to open root:${NC}"
        read -p "Visual: " TARGET
    fi

    case "$TARGET" in
        gcs) TARGET="gcs_visualization.html" ;;
        cdp) TARGET="cdp_visualization.html" ;;
        abtree) TARGET="abtree_visualization.html" ;;
        bab) TARGET="bab_visualization.html" ;;
        pbtree) TARGET="pbtree_visualization.html" ;;
        "") TARGET="" ;;
        *) ;; # assume user typed a valid filename
    esac

    if [ -n "$TARGET" ] && [ ! -f "$VISUALIZE_DIR/$TARGET" ]; then
        echo -e "${RED}Visualization '$TARGET' not found in $VISUALIZE_DIR${NC}"
        return 1
    fi

    if [ -n "$TARGET" ]; then
        URL="http://localhost:${PORT}/${TARGET}"
    else
        URL="http://localhost:${PORT}/"
    fi

    echo -e "${GREEN}Starting Python HTTP server on port ${PORT}...${NC}"
    echo -e "${CYAN}Access: ${URL}${NC}"
    echo ""

    cd "$VISUALIZE_DIR" || return 1

    # 嘗試在不同平台自動打開瀏覽器（支援 Linux/macOS/Windows/WSL）
    if command -v xdg-open >/dev/null 2>&1; then
        xdg-open "$URL" 2>/dev/null &
        echo -e "${GREEN}Opened browser (xdg-open)${NC}"
    elif command -v open >/dev/null 2>&1; then
        open "$URL" 2>/dev/null &
        echo -e "${GREEN}Opened browser (open)${NC}"
    elif command -v powershell.exe >/dev/null 2>&1; then
        powershell.exe -NoProfile -Command "Start-Process '$URL'" >/dev/null 2>&1 &
        echo -e "${GREEN}Opened browser (powershell)${NC}"
    elif command -v cmd.exe >/dev/null 2>&1; then
        cmd.exe /C start "" "$URL" >/dev/null 2>&1 &
        echo -e "${GREEN}Opened browser (cmd start)${NC}"
    elif command -v explorer.exe >/dev/null 2>&1; then
        explorer.exe "$URL" >/dev/null 2>&1 &
        echo -e "${GREEN}Opened browser (explorer)${NC}"
    else
        echo -e "${YELLOW}Unable to auto-open browser. Please visit ${URL} manually.${NC}"
    fi

    # 啟動HTTP服務器
    if command -v python3 &> /dev/null; then
        PYTHON_CMD="python3"
    elif command -v python &> /dev/null; then
        PYTHON_CMD="python"
    else
        echo -e "${RED}Python not found! Please install Python to run the web server.${NC}"
        return 1
    fi

    $PYTHON_CMD -m http.server $PORT
}

run_all() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "  Running All Algorithms"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # 1. GCS
    run_gcs
    echo ""
    
    # 2. CDP
    run_cdp
    echo ""
    
    # 3. AB-tree
    run_abtree
    echo ""
    
    # 4. PB-tree
    run_pbtree
    echo ""
    
    # Summary
    echo -e "${GREEN}"
    echo "══════════════════════════════════════════════════════════════"
    echo "  All algorithms executed!"
    echo "══════════════════════════════════════════════════════════════"
    echo -e "${NC}"
}

# ==========================================
# 主程式
# ==========================================

main() {
    print_header
    check_java
    echo ""
    
    # 如果有命令列參數，直接執行對應模式
    if [ $# -gt 0 ]; then
        case "$1" in
            gcs|1)
                run_gcs
                ;;
            cdp|2)
                run_cdp
                ;;
            abtree|3)
                run_abtree
                ;;
            pbtree|4)
                run_pbtree
                ;;
            all|5)
                run_all
                ;;
            viz|visualize|6)
                open_visualization "$2" "$3"
                ;;
            *)
                echo -e "${RED}Unknown argument: $1${NC}"
                echo "Usage: $0 [gcs|cdp|abtree|pbtree|all|visualize [port]]"
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
                run_gcs
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            2)
                run_cdp
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            3)
                run_abtree
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            4)
                run_pbtree
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            5)
                run_all
                echo ""
                read -p "Press Enter to continue..."
                clear
                print_header
                ;;
            6)
                open_visualization
                break
                ;;
            q|Q)
                echo -e "${GREEN}Thank you for using CRS Evaluation Suite!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Invalid choice. Please enter 1-6 or q.${NC}"
                echo ""
                ;;
        esac
    done
}

# 執行主程式
main "$@"
