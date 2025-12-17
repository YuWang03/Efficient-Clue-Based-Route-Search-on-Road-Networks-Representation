# CRS 演算法核心邏輯與專案問題說明

## 一、CRS 演算法核心邏輯

CRS（Clue-based Route Search）演算法的核心是根據「線索」(clue) 來在路網中尋找最符合條件的 POI（Point of Interest）節點。其主要步驟如下：

### 1. findNextMin() 程序（參考 GCS 論文）

```
Input: Source vertex u, clue μ(w, d, ε)
Output: min{dm(μ, σ)} and match vertex v

1. 從起點 u 開始網路遍歷
2. 若找到符合線索的匹配節點 v
   - 計算 u 到 v 的網路距離 d_G
3. 進入迴圈，持續尋找更佳的匹配
   - 找下一個包含線索 w 的節點 v'
   - 若 d_G < d 且 d'_G > d，則停止
   - 否則更新 v ← v'，d_G ← d'_G
4. 回傳最佳匹配節點 v 及其距離
```

### 2. 線索設計

每個 clue μ(w, d, ε) 包含：
- w：關鍵字（如 restaurant, cafe, convenience）
- d：目標距離
- ε：容許誤差（如 ±100%）

### 3. 網路遍歷與匹配

- 使用 Dijkstra/BFS 進行網路遍歷
- 每次遇到符合線索的 POI，計算距離與匹配度
- 支援鄰居 POI 檢查（道路節點可因鄰接 POI 而被視為匹配）

---

## 二、專案遇到的核心問題

### 1. OSM 路網分離

- OSM 原始資料中，主道路網（約 1300+ 節點）與 POI 區域（3~6 節點的小島）完全分離
- 導致遍歷主網路時無法找到任何 POI，遍歷 POI 區域時只能走幾個節點

### 2. POI 連接距離限制

- 初始 POI 連接距離僅 150m，無法跨越小區域
- 增加距離到 500m、1000m、2000m，仍有部分 POI無法連接到主網路

### 3. POI 橋接策略

- 新增「POI 橋接」：將 800m 內的 POI 互相連接，形成更大的 POI 連通分量
- 成功讓所有 restaurant/cafe/convenience 互通，遍歷節點數提升到 60

### 4. 可視化優化

- 原本地圖會顯示完整路徑線條
- 依需求改為只顯示匹配到的 POI，不顯示路徑線，路徑作為背景運算保留

---

## 三、目前成果

- ✅ CRS 演算法完整實作，支援 clue-based 搜尋與鄰居 POI 檢查
- ✅ POI 橋接機制，解決 OSM 分離問題
- ✅ 可視化只顯示匹配點，地圖更簡潔
- ✅ 成功遍歷 60 個節點，找到多個 restaurant/cafe/convenience

---

## 四、後續建議

- 若需遍歷更多節點，需進一步優化 OSM 資料或設計更強的 POI 橋接策略
- 可考慮自動合併主網路與 POI 區域，或人工補充缺失的道路連接

---

**參考：GCS 論文 Algorithm 1 - findNextMin()**
（如上圖所示）

---
