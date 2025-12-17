# AB-Tree 路徑搜尋演算法

實作 **Algorithm 4: Procedure findNext() with AB-Tree**，用於以提示（clue）為基礎的路徑搜尋，提升搜尋效率。

## 演算法概述

AB-Tree 是一個以網路距離作為索引鍵的 B-Tree，能使 `findNext` 的查找由 O(n) 線性掃描降為 O(log n) 的對數時間。

### Algorithm 4: 使用 AB-Tree 的 findNext()

```
Input: Query vertex v_{i-1}, clue w_i and d_i, threshold θ
Output: Next candidate v_i with d_m(v_i)

1  Obtain BT(v_{i-1});
2  lD ← d_i - d_i·ε; rD ← d_i + d_i·ε;
3  v_p and d_p ← BT(v_{i-1}).predecessor(lD, w_i);
4  v_s and d_s ← BT(v_{i-1}).successor(rD, w_i);
5  if d_i - d_p ≤ d_s - d_i then
6      return v_p with d_m(v_p);
7  else
8      return v_s with d_m(v_s);
```

### Procedure Predecessor(lD, w, Node)

```
1  if Node is a leaf node then
2      Obtain v_p and d_p of current node;
3      if v_p contains w and d_p ≤ lD then
4          return v_p and d_p;
5  else
6      return false;
7  else
8      Generate H(w);
9      if H(w) ∧ H(Node) = ∅ then
10         return false;
11     if lD < Node.routing then
12         lNode ← index of its left subnode;
13         return Predecessor(lD, w, lNode);
14     else
15         rNode ← index of its right subnode;
16         lNode ← index of its left subnode;
17         if rNode exists then
18             if Predecessor(lD, w, rNode);
19             then
20                 return v_p and d_p
21             else
22                 return Predecessor(lD, w, lNode);
23     return Predecessor(lD, w, lNode);
```

## 專案結構

```
abtree-project/
├── src/main/java/abtree/
│   ├── model/
│   │   ├── Node.java           # 路網頂點
│   │   ├── Edge.java           # 路網邊
│   │   ├── Clue.java           # 提示 (m(w, d, ε))
│   │   ├── Query.java          # 查詢 Q = (vq, C)
│   │   ├── RoadNetwork.java    # 含 Dijkstra 的圖
│   │   └── ABTreeEntry.java    # AB-Tree 葉節點條目
│   ├── algorithm/
│   │   ├── ABTree.java         # B-Tree 實作
│   │   ├── BABWithABTree.java  # 使用 AB-Tree 的 BAB 演算法
│   │   └── SearchResult.java   # 搜尋結果容器
│   ├── parser/
│   │   └── OSMParser.java      # OpenStreetMap 解析器
│   ├── visualization/
│   │   └── HtmlVisualizer.java # HTML 視覺化產生器
│   └── Main.java               # 進入點
├── bin/                        # 編譯後類別檔
├── lib/                        # 依賴 (GSON)
├── resources/
├── run.sh                      # 建置與執行腳本
└── README.md
```

## 使用方式

### 命令列

```bash
# 編譯並執行
chmod +x run.sh
./run.sh map.osm

# 互動模式
./run.sh map.osm --interactive

# 基準測試模式
./run.sh map.osm --benchmark
```

bab <source> <kw1,d1,e1> <kw2,d2,e2> ...
tree <source>
quit
### 互動指令

```
findnext <source> <keyword> <distance> <epsilon> <theta>
bab <source> <kw1,d1,e1> <kw2,d2,e2> ...
tree <source>
quit
```

### Examples

```bash
# Find next vertex with keyword "crossing" at ~150m
findnext 123456 crossing 150 0.5 0

# Run full BAB search
bab 123456 footway,100,0.5 crossing,200,0.5

# Inspect AB-Tree structure
tree 123456
```

## 主要元件

### `ABTree.java`

主要的 B-Tree 實作，包含：
- `buildFromNetwork(network, source)` - 從路網建立樹
- `findNext(clue, theta, excluded)` - Algorithm 4 的實作
- `findPredecessor(lD, w, excluded)` - 前驅搜尋程序
- `findSuccessor(rD, w, excluded)` - 後繼搜尋程序
- `rangeQuery(minD, maxD, w)` - 範圍查詢支援

### `ABTreeEntry.java`

葉節點中存放的條目，包含：
- `distance` - 從來源的網路距離 (d_p)
- `vertexId` - 頂點 ID (v_p)
- `keywords` - 頂點的關鍵詞集合 H(v_p)

### `BABWithABTree.java`

使用 AB-Tree 的 BAB 演算法修改版：
- 延遲建立 AB-Tree（Lazy build）
- 快取已建立的樹以供重複查詢使用
- 記錄詳細的搜尋步驟以便分析與視覺化

## 視覺化

HTML 視覺化包含：
- 使用 Leaflet 的互動地圖
- AB-Tree 搜尋步驟顯示
- 距離範圍的視覺化（圈/帶）
- 關鍵詞配對高亮顯示

執行程式後可打開 `abtree_visualization.html` 檢視結果。

### 為何能將 AB-Tree 可視化

專案可以將 AB-Tree 可視化，原因在於程式碼將樹的條目、距離與搜尋步驟以可序列化的形式匯出，並由 HTML/JavaScript 將其渲染到地圖及距離導向的視圖中。關鍵點包括：

- **距離索引的條目：** `ABTreeEntry.java` 儲存每個葉條目的 `distance` 與 `vertexId`，提供可用於視覺化的數值（網路距離）軸。
- **網路幾何資訊：** `RoadNetwork.java` 與 `Node.java` 提供頂點座標與基於 Dijkstra 的網路距離，能將標記與距離圈繪製在正確的地圖位置上。
- **可序列化的資料：** `HtmlVisualizer.java` 使用 GSON 將 AB-Tree 條目與記錄的搜尋步驟序列化成 JSON，供生成的 HTML/JavaScript 使用。
- **互動式 HTML 與 Leaflet：** 生成的 HTML（例如 `abtree_visualization.html` 或 `abtree_standalone.html`）使用 Leaflet 畫出底圖與覆蓋層；JavaScript 讀取序列化資料以繪製標記、強調關鍵詞，並渲染對應 `lD`/`rD` 的距離範圍（圈或區段）。
- **逐步搜尋回放：** `BABWithABTree.java` 與 `ABTree.findNext()` 會記錄前驅/後繼的抉擇與距離範圍，視覺化器使用這些步驟資料做動畫或逐步播放，讓使用者看到會選取哪個節點以及原因。
- **獨立檢視：** 視覺化可以輸出自包含的 HTML 檔（或搭配 JS 資料），讓使用者在不執行完整 Java 程式的情況下直接以瀏覽器檢視（方便分享與除錯）。

如何產生與檢視：

1. 建置並執行程式以產生視覺化資料（範例）：

```bash
chmod +x run.sh
./run.sh map.osm
```

2. 在互動模式下可選擇輸出視覺化或使用 `--interactive` 觸發視覺化流程。

3. 打開生成的 `abtree_visualization.html` 或 `abtree_standalone.html`，檢視 AB-Tree 的佈局、距離範圍以及搜尋步驟。

建議檢視的檔案（視覺化流程）：`src/main/java/abtree/visualization/HtmlVisualizer.java`、`src/main/java/abtree/algorithm/ABTree.java`、`src/main/java/abtree/model/ABTreeEntry.java`。

## 效能

AB-Tree 的優化帶來：
- **findNext**：O(log n)（優於 O(n) 的線性掃描）
- **空間**：每個來源頂點需 O(n)
- **建置時間**：每棵樹 O(n log n)

當多個查詢使用相同來源頂點時，透過快取樹可達成攤還效能（amortized performance）。

## 相依套件

- Java 17+
- GSON 2.10.1（會自動下載）

## 參考資料

基於論文中的 Algorithm 4: Procedure findNext() with AB-Tree，用於高效率的提示式路徑搜尋。
