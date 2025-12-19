# CRS Java 項目文件概要

**項目類型**: Java 線索型路徑搜尋演算法實現與最佳化  
**最後更新**: 2025年12月19日

---

## 📋 項目結構概覽

```
crs_java_project/
├── abtree-project/          # AB-Tree 演算法實現
├── bab-project/             # BAB (分支定界) 演算法實現
├── crs-cdp/                 # CDP 演算法優化版本（主要項目）
├── gcs-project/             # GCS 演算法實現
├── pbtree-project/          # PB-Tree 演算法實現
├── evaluation/              # 評估與測試工具
├── visualize/               # 可視化資源
├── .venv/                   # Python 虛擬環境
└── Readme.md               # 主項目說明（空）
```

---

## 🎯 核心項目詳解

### 1. **CRS-CDP 項目** (主要優化項目)
**位置**: `crs-cdp/`  
**目標**: 解決 CDP 算法路徑穿越建築物問題

#### 📁 源代碼結構
```
crs-cdp/src/main/java/crs/
├── algorithm/
│   ├── CDPAlgorithm.java        # CDP 動態規劃核心演算法
│   ├── PathValidator.java       # 路徑驗證與分析
│   └── PathRepairer.java        # 路徑修復工具
├── model/
│   ├── Node.java                # 節點數據結構
│   ├── Edge.java                # 邊數據結構
│   ├── Clue.java                # 線索數據結構
│   └── RoadNetwork.java         # 道路網絡圖數據結構
├── parser/
│   └── [OSM 文件解析器]
├── utils/
│   └── [工具類]
├── visualization/
│   └── CDPVisualizer.java       # 地圖可視化
├── CDPMain.java                 # 主程序入口
├── QuickDiagnostics.java        # 快速診斷工具
└── SimpleTest.java              # 簡單測試用例
```

#### 🔑 關鍵文件說明

| 文件 | 職責 | 主要方法/類 |
|------|------|-----------|
| **CDPAlgorithm.java** | 核心路徑搜尋演算法 | `findPath()`、`CDPResult` |
| **RoadNetwork.java** | 圖數據結構 | `computeShortestPath()`、`getAdjacentNodes()` |
| **PathValidator.java** | 路徑有效性驗證 | `validatePath()`、`comparePathQuality()` |
| **Node.java** | 節點定義 | 位置、鄰接表 |
| **Edge.java** | 邊定義 | 端點、距離權重 |
| **Clue.java** | 線索定義 | 位置、距離、容忍度 |

#### 📊 主要優化改動

- ✅ **雙路徑表示**: 關鍵節點路徑 + 完整實際路徑
- ✅ **路徑緩存機制**: 距離與路徑雙重緩存
- ✅ **自動路徑驗證**: 檢查相鄰節點連接有效性
- ✅ **詳細可視化**: 藍色虛線(關鍵節點) + 綠色粗線(實際路徑)

#### 📄 文檔與報告

| 文件 | 內容 |
|------|------|
| `OPTIMIZATION_README.md` | 優化說明與技術細節 |
| `CDP_OPTIMIZATION_REPORT.md` | 詳細優化報告 |
| `QUICK_REFERENCE.md` | 快速參考指南 |
| `SOLUTION_SUMMARY.md` | 解決方案總結 |
| `TROUBLESHOOTING_GUIDE.md` | 故障排除指南 |

#### 🗂️ 資源文件

| 文件 | 用途 |
|------|------|
| `map.osm` | OSM 路網數據 |
| `graph_data.json` | 圖結構 JSON |
| `cdp_visualization.html` | 可視化結果 |
| `path_comparison.html` | 路徑對比 |
| `settings.json` | 配置文件 |

---

### 2. **AB-Tree 項目**
**位置**: `abtree-project/`  
**目標**: 實現基於 B-Tree 的高效路徑搜尋

#### 📁 結構
```
abtree-project/
├── src/main/java/abtree/
│   ├── algorithm/          # AB-Tree 演算法
│   ├── model/             # 數據結構
│   ├── parser/            # OSM 解析
│   └── visualization/     # 可視化
├── bin/                   # 編譯後的類文件
├── lib/                   # 依賴庫
├── resources/             # 資源文件
└── map.osm               # 測試路網
```

#### 🎯 功能特點
- **時間複雜度**: `findNext()` 從 O(n) 優化為 O(log n)
- **索引結構**: 以網絡距離為鍵的 B-Tree
- **前驅/後繼查詢**: 高效的範圍查詢
- **Algorithm 4**: 使用 AB-Tree 的 findNext() 實現

---

### 3. **BAB 項目** (分支定界)
**位置**: `bab-project/`  
**目標**: 實現組合優化的分支定界演算法

#### 📁 結構
```
bab-project/
├── src/main/java/bab/
│   ├── algorithm/          # BAB 搜尋演算法
│   ├── model/             # 數據模型
│   ├── parser/            # OSM 解析
│   └── visualization/     # 結果可視化
├── bin/                   # 編譯文件
└── resources/             # 資源
```

#### 🎯 演算法流程
1. **初始化**: 解析 OSM、建立索引
2. **分支**: 從起點逐層搜尋候選節點
3. **定界**: 利用上下界剪枝不可行分支
4. **路徑重建**: 使用 Dijkstra 確保路徑沿實際道路

---

### 4. **PB-Tree 項目** (樞軸反向二叉樹)
**位置**: `pbtree-project/`  
**目標**: 空間高效的路徑搜尋索引

#### 📁 結構
```
pbtree-project/
├── src/main/java/pbtree/
│   ├── algorithm/          # PB-Tree 演算法
│   ├── model/             # 2-Hop 標籤結構
│   ├── parser/            # OSM 解析
│   └── visualization/     # 可視化
├── bin/                   # 編譯文件
└── lib/                   # 依賴庫
```

#### 📊 特性對比

| 特性 | PB-Tree | AB-Tree |
|------|---------|---------|
| 空間複雜度 | O(\|L\| × h) | O(\|V\|²) |
| 查詢時間 | O(log \|L\|/\|V\|) | O(log n) |
| 索引方式 | 2-Hop 標籤 | B-Tree |
| **優勢** | 空間效率高 | 查詢速度快 |

---

### 5. **GCS 項目** (通用線索搜尋)
**位置**: `gcs-project/`  
**目標**: 通用的線索型路徑搜尋

#### 📁 結構
```
gcs-project/
├── src/main/java/crs/
│   ├── algorithm/          # GCS 演算法
│   ├── model/             # 數據結構
│   ├── parser/            # OSM 解析
│   └── visualization/     # 可視化
├── bin/                   # 編譯文件
└── resources/             # 資源
```

---

## 🧪 評估與測試

**位置**: `evaluation/`

### 📊 評估文件

| 文件 | 功能 |
|------|------|
| `TestRunner.java` | Java 性能測試框架 |
| `GCSAccuracyEvaluator.java` | GCS 精度評估 |
| `Accuracy_of_GCS.py` | Python 精度分析 |
| `gcs_accuracy_evaluator.py` | Python 評估工具 |
| `QT.py` | 查詢時間分析 |

### 📈 評估結果

| 文件 | 內容 |
|------|------|
| `query_time_results.json` | 查詢時間結果 |
| `*.png` | 性能對比圖表 |
|  - `query_time_comparison.png` | 查詢時間對比 |
|  - `query_distance_comparison.png` | 查詢距離對比 |
|  - `epsilon_comparison.png` | 容忍度影響 |
|  - `keyword_frequency_comparison.png` | 關鍵字頻率對比 |
|  - `Accuracy_of_GCS.png` | 精度評估 |
|  - `all_experiments.png` | 全實驗對比 |

---

## 🎨 可視化模塊

**位置**: `visualize/`

### 📄 可視化文件

| 文件 | 對應項目 | 功能 |
|------|---------|------|
| `abtree_visualization.html` | AB-Tree | 路徑搜尋可視化 |
| `bab_visualization.html` | BAB | 分支定界過程 |
| `cdp_visualization.html` | CDP | 路徑展示 |
| `crs_visualization.html` | CRS | 通用可視化 |
| `gcs_visualization.html` | GCS | GCS 結果展示 |
| `pbtree_visualization.html` | PB-Tree | 樹結構展示 |
| `abtree_extracted.js` | - | 提取的 JS 數據 |
| `graph_data.json` | - | 圖結構數據 |

---

## 🛠️ 構建與運行

### 腳本文件

| 文件 | 平台 | 功能 |
|------|------|------|
| `run.sh` | Linux/Mac | 編譯和運行主程序 |
| `run.bat` | Windows | 編譯和運行主程序 |

### 配置文件

| 文件 | 位置 | 用途 |
|------|------|------|
| `settings.json` | crs-cdp/ | CDP 運行配置 |

---

## 📦 依賴關係

### 核心依賴
- **Java**: 1.8+ (建議 11+)
- **JSON Processing**: 用於圖數據解析
- **HTML/JavaScript**: 用於可視化

### Python 依賴 (evaluation/)
- matplotlib
- json
- pandas (可能)

---

## 🔄 數據流向

```
OSM 文件
    ↓
OSM 解析器 (parser/)
    ↓
RoadNetwork (圖數據結構)
    ↓
演算法模塊 (CDPAlgorithm/BAB/etc.)
    ↓
PathValidator/PathRepairer
    ↓
Visualizer (可視化)
    ↓
HTML/PNG 輸出
```

---

## 📝 主要數據結構

### Node (節點)
```
- id: long
- latitude: double
- longitude: double
- keywords: Set<String>
- adjacentNodes: Set<Node>
```

### Edge (邊)
```
- from: Node
- to: Node
- distance: double
```

### Clue (線索)
```
- keyword: String
- distance: double
- tolerance: double
```

### CDPResult (結果)
```
- path: List<Node>          # 關鍵節點路徑
- fullPath: List<Node>      # 完整實際路徑
- distance: double          # 總距離
- matchingDistance: double  # 匹配距離
```

---

## 🎓 學習路徑

1. **入門**: 從 `CRS-CDP/CDPMain.java` 開始
2. **理論**: 閱讀各項目 README 文檔
3. **實現**: 研究 `algorithm/` 中的核心演算法
4. **優化**: 參考 `OPTIMIZATION_README.md`
5. **測試**: 使用 `evaluation/` 進行性能測試
6. **可視化**: 查看 `visualize/` 中的結果

---

## 📊 項目對比

| 項目 | 演算法 | 複雜度 | 優勢 | 應用場景 |
|------|--------|--------|------|---------|
| **CDP** | 動態規劃 | O(k·n) | 精確、可靠 | 標準線索搜尋 |
| **AB-Tree** | B-Tree 索引 | O(log n) | 查詢快速 | 大規模圖 |
| **BAB** | 分支定界 | O(b^d) | 剪枝高效 | 組合優化 |
| **PB-Tree** | 2-Hop 標籤 | O(log n) | 空間省 | 受限環境 |
| **GCS** | 通用算法 | 可變 | 靈活適應 | 通用查詢 |

---

## 🚀 快速開始

### 編譯 (Windows)
```bash
run.bat
```

### 編譯 (Linux/Mac)
```bash
./run.sh
```

### 運行主程序
```bash
cd crs-cdp
java -cp bin crs.CDPMain
```

### 查看可視化
在瀏覽器打開 `visualize/` 下的 HTML 文件

---

## 📞 關鍵代碼位置速查

| 需求 | 文件位置 |
|------|---------|
| 修改核心算法 | `crs-cdp/src/main/java/crs/algorithm/CDPAlgorithm.java` |
| 修改圖結構 | `crs-cdp/src/main/java/crs/model/RoadNetwork.java` |
| 修改可視化 | `crs-cdp/src/main/java/crs/visualization/CDPVisualizer.java` |
| 解析 OSM | `crs-cdp/src/main/java/crs/parser/` |
| 性能測試 | `evaluation/TestRunner.java` |
| 精度評估 | `evaluation/GCSAccuracyEvaluator.java` |

---

## 📋 文檔清單

### 項目文檔
- [crs-cdp/OPTIMIZATION_README.md](crs-cdp/OPTIMIZATION_README.md) - 優化詳解
- [crs-cdp/CDP_OPTIMIZATION_REPORT.md](crs-cdp/CDP_OPTIMIZATION_REPORT.md) - 優化報告
- [crs-cdp/QUICK_REFERENCE.md](crs-cdp/QUICK_REFERENCE.md) - 快速參考
- [abtree-project/README.md](abtree-project/README.md) - AB-Tree 說明
- [bab-project/README.md](bab-project/README.md) - BAB 說明
- [pbtree-project/README.md](pbtree-project/README.md) - PB-Tree 說明

### 數據文件
- `*.osm` - OpenStreetMap 路網數據
- `graph_data.json` - 圖結構 JSON
- `query_time_results.json` - 查詢結果

---

## 📅 版本歷史標記

| 時間 | 內容 |
|------|------|
| 2025-12-19 | 文件概要生成 |
| 2025-12-** | CDP 優化完成 |
| 2025-12-** | AB-Tree 實現 |
| 2025-12-** | BAB 實現 |
| 2025-12-** | PB-Tree 實現 |

---

*此文檔由自動化工具生成，用於快速理解項目結構與文件組織*
