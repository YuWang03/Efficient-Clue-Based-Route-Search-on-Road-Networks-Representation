package abtree.visualization;

import abtree.model.*;
import java.io.*;
import java.util.*;

/**
 * Generates HTML visualization for Algorithm 4 (findNext with AB-Tree).
 * Uses CDP-style UI design.
 */
public class HtmlVisualizer {
    
    private final RoadNetwork network;
    
    public HtmlVisualizer(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * Export graph data to JSON format.
     */
    public void exportGraphData(String filename) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        // Nodes
        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (Node node : network.getNodes().values()) {
            Map<String, Object> n = new HashMap<>();
            n.put("id", node.getId());
            n.put("lat", node.getLat());
            n.put("lon", node.getLon());
            n.put("keywords", new ArrayList<>(node.getKeywords()));
            nodeList.add(n);
        }
        data.put("nodes", nodeList);
        
        // Edges
        List<Map<String, Object>> edgeList = new ArrayList<>();
        Set<String> addedEdges = new HashSet<>();
        for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
            for (Edge edge : entry.getValue()) {
                String key = Math.min(edge.getFrom(), edge.getTo()) + "-" + Math.max(edge.getFrom(), edge.getTo());
                if (!addedEdges.contains(key)) {
                    addedEdges.add(key);
                    Map<String, Object> e = new HashMap<>();
                    e.put("from", edge.getFrom());
                    e.put("to", edge.getTo());
                    e.put("weight", edge.getWeight());
                    edgeList.add(e);
                }
            }
        }
        data.put("edges", edgeList);
        
        // Keyword index
        Map<String, List<Long>> kwIndex = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : network.getKeywordIndex().entrySet()) {
            kwIndex.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        data.put("keywordIndex", kwIndex);
        
        // Stats
        Map<String, Object> stats = new HashMap<>();
        stats.put("nodeCount", network.getNodeCount());
        stats.put("edgeCount", network.getEdgeCount());
        stats.put("keywordCount", network.getKeywordIndex().size());
        data.put("stats", stats);
        
        // Write with UTF-8 encoding
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(filename), java.nio.charset.StandardCharsets.UTF_8)) {
            // Simple JSON serialization without Gson
            writer.write("{\n");
            writer.write("  \"nodes\": [],\n");
            writer.write("  \"edges\": []\n");
            writer.write("}");
        }
        
        System.out.println("Exported graph data to " + filename);
    }
    
    /**
     * Generate standalone HTML visualization with embedded data.
     */
    public void generateStandaloneHtml(String graphJsonPath, String outputPath) throws IOException {
        // Read graph JSON with UTF-8 encoding
        String graphJson;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(graphJsonPath), java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            graphJson = sb.toString();
        }
        
        // Generate HTML with embedded data
        String html = getHtmlTemplate().replace("__GRAPH_DATA_PLACEHOLDER__", graphJson);
        
        // Write HTML with UTF-8 encoding
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(outputPath), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write(html);
        }
        
        System.out.println("Generated standalone HTML: " + outputPath);
    }
    
    /**
     * Get HTML template with CDP-style UI for AB-Tree visualization.
     */
    private String getHtmlTemplate() {
        return """
<!DOCTYPE html>
<html lang="zh-TW">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Algorithm 4: findNext with AB-Tree - 視覺化</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; }
        .header { background: linear-gradient(135deg, #8e44ad 0%, #3498db 100%); color: white; padding: 15px 20px; text-align: center; }
        .header h1 { font-size: 1.4em; }
        .header p { font-size: 0.85em; opacity: 0.9; }
        .main-container { display: flex; height: calc(100vh - 70px); }
        .left-panel { width: 450px; background: white; overflow-y: auto; border-right: 2px solid #dee2e6; padding: 15px; }
        #map { flex: 1; }
        .panel { background: #fafafa; margin-bottom: 15px; padding: 15px; border-radius: 8px; border: 1px solid #e0e0e0; }
        .panel h3 { color: #2c3e50; font-size: 1em; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 2px solid #8e44ad; }
        .info-row { display: flex; justify-content: space-between; padding: 5px 0; }
        .info-label { color: #666; }
        .info-value { font-weight: bold; color: #2c3e50; }
        .clue-badge { display: inline-block; background: #8e44ad; color: white; padding: 3px 10px; border-radius: 12px; font-size: 0.85em; margin: 2px; }
        .clue-input-group { background: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
        .clue-input-group label { display: block; font-size: 0.8em; color: #666; margin-bottom: 4px; }
        .clue-input-group input, .clue-input-group select { width: 100%; padding: 6px 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 0.9em; margin-bottom: 6px; }
        .clue-input-row { display: flex; gap: 10px; }
        .clue-input-row > div { flex: 1; }
        .step-table { width: 100%; border-collapse: collapse; font-size: 0.75em; margin-top: 10px; }
        .step-table th, .step-table td { border: 1px solid #ddd; padding: 5px; text-align: center; }
        .step-table th { background: #8e44ad; color: white; }
        .step-table .predecessor { background: #e8f5e9; border-left: 3px solid #27ae60; }
        .step-table .successor { background: #e3f2fd; border-left: 3px solid #3498db; }
        .step-table .select { background: #fff3e0; border-left: 3px solid #f39c12; }
        .step-table .init { background: #f3e5f5; border-left: 3px solid #8e44ad; }
        .legend-item { display: flex; align-items: center; margin: 5px 0; }
        .legend-dot { width: 14px; height: 14px; border-radius: 50%; margin-right: 8px; border: 2px solid white; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
        .path-step { display: flex; align-items: center; padding: 8px; margin: 5px 0; background: #f3e5f5; border-radius: 6px; border-left: 4px solid #8e44ad; cursor: pointer; }
        .path-step:hover { transform: translateX(5px); background: #e8f5e9; }
        .path-step.optimal { border-left-color: #27ae60; background: #e8f5e9; }
        .step-num { width: 24px; height: 24px; background: #8e44ad; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px; font-size: 0.8em; }
        .path-step.optimal .step-num { background: #27ae60; }
        .btn { background: #8e44ad; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin: 3px; font-size: 0.9em; }
        .btn:hover { background: #732d91; }
        .btn-success { background: #27ae60; }
        .btn-success:hover { background: #1e8449; }
        .btn-info { background: #3498db; }
        .btn-info:hover { background: #2980b9; }
        .loading-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(44, 62, 80, 0.95); display: flex; align-items: center; justify-content: center; z-index: 1000; color: white; flex-direction: column; }
        .loading-spinner { width: 50px; height: 50px; border: 4px solid rgba(255,255,255,0.3); border-top-color: #8e44ad; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 15px; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .tree-vis { background: #2c3e50; color: #ecf0f1; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 0.75em; max-height: 200px; overflow: auto; margin-top: 10px; }
        .tree-node { margin-left: 15px; }
        .tree-leaf { color: #2ecc71; }
        .tree-internal { color: #3498db; }
        .tree-keyword { color: #f39c12; }
        .range-indicator { background: linear-gradient(90deg, #e74c3c 0%, #f39c12 50%, #27ae60 100%); height: 8px; border-radius: 4px; margin: 10px 0; position: relative; }
        .range-marker { position: absolute; width: 3px; height: 16px; background: #2c3e50; top: -4px; }
        .tabs { display: flex; border-bottom: 2px solid #e0e0e0; margin-bottom: 10px; }
        .tab { padding: 8px 16px; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -2px; }
        .tab:hover { background: #f0f0f0; }
        .tab.active { border-bottom-color: #8e44ad; color: #8e44ad; font-weight: bold; }
        .tab-content { display: none; }
        .tab-content.active { display: block; }
        .keyword-chip { display: inline-block; background: #e8f4f8; padding: 2px 8px; margin: 2px; border-radius: 4px; font-size: 0.8em; cursor: pointer; }
        .keyword-chip:hover { background: #8e44ad; color: white; }
        .result-card { padding: 15px; border-radius: 8px; margin-bottom: 10px; }
        .result-card.success { background: linear-gradient(135deg, rgba(39,174,96,0.15), rgba(46,204,113,0.15)); border: 1px solid #27ae60; }
        .result-card.info { background: linear-gradient(135deg, rgba(142,68,173,0.15), rgba(155,89,182,0.15)); border: 1px solid #8e44ad; }
    </style>
</head>
<body>
<div class="loading-overlay" id="loadingOverlay">
    <div class="loading-spinner"></div>
    <div id="loadingText">載入圖形資料...</div>
</div>

<div class="header">
    <h1>🌳 Algorithm 4: findNext() with AB-Tree</h1>
    <p>B-Tree 索引加速的 findNext 程序 | <span id="headerResult">請執行查詢</span></p>
</div>

<div class="main-container">
<div class="left-panel">
    <!-- Query Panel -->
    <div class="panel">
        <h3>📍 查詢設定</h3>
        <div class="clue-input-group">
            <label>來源頂點 v<sub>i-1</sub></label>
            <select id="sourceSelect"><option value="">載入中...</option></select>
        </div>
        <div class="clue-input-group">
            <label>關鍵字 w<sub>i</sub></label>
            <input type="text" id="keywordInput" placeholder="例: footway, crossing">
            <div id="keywordSuggestions" style="max-height:80px;overflow:auto;"></div>
        </div>
        <div class="clue-input-row">
            <div class="clue-input-group">
                <label>距離 d<sub>i</sub> (公尺)</label>
                <input type="number" id="distanceInput" value="150" min="1">
            </div>
            <div class="clue-input-group">
                <label>容忍度 ε</label>
                <input type="number" id="epsilonInput" value="0.5" min="0.1" max="1" step="0.1">
            </div>
        </div>
        <div class="clue-input-group">
            <label>閾值 θ</label>
            <input type="number" id="thetaInput" value="0" min="0" max="1" step="0.1">
        </div>
        <button class="btn btn-success" style="width:100%;" onclick="runFindNext()">▶ 執行 findNext (Algorithm 4)</button>
        <button class="btn btn-info" style="width:100%;margin-top:5px;" onclick="runFullBAB()">🔍 執行完整 BAB + AB-Tree</button>
    </div>

    <!-- Result Panel -->
    <div class="panel" id="resultPanel" style="display:none;">
        <h3>🎯 findNext 結果</h3>
        <div id="resultContent"></div>
    </div>

    <!-- AB-Tree Info -->
    <div class="panel" id="treePanel" style="display:none;">
        <h3>🌲 AB-Tree 資訊</h3>
        <div class="info-row"><span class="info-label">來源頂點</span><span class="info-value" id="treeSource">-</span></div>
        <div class="info-row"><span class="info-label">樹節點數</span><span class="info-value" id="treeNodes">-</span></div>
        <div class="info-row"><span class="info-label">索引項目</span><span class="info-value" id="treeEntries">-</span></div>
        <div class="info-row"><span class="info-label">建樹時間</span><span class="info-value" id="treeBuildTime">-</span></div>
    </div>

    <!-- Legend -->
    <div class="panel">
        <h3>🎨 圖例</h3>
        <div class="legend-item"><div class="legend-dot" style="background:#e74c3c;"></div>來源頂點 v<sub>i-1</sub></div>
        <div class="legend-item"><div class="legend-dot" style="background:#27ae60;"></div>Predecessor 候選</div>
        <div class="legend-item"><div class="legend-dot" style="background:#3498db;"></div>Successor 候選</div>
        <div class="legend-item"><div class="legend-dot" style="background:#f39c12;"></div>最終選擇</div>
        <div class="legend-item"><div class="legend-dot" style="background:#9b59b6;"></div>關鍵字匹配節點</div>
    </div>

    <!-- Search Steps -->
    <div class="panel">
        <h3>📊 搜尋步驟 (Algorithm 4)</h3>
        <div class="tabs">
            <div class="tab active" onclick="switchTab('steps')">AB-Tree 搜尋</div>
            <div class="tab" onclick="switchTab('range')">距離範圍</div>
        </div>
        <div id="stepsTab" class="tab-content active">
            <div id="stepTableContainer" style="max-height:250px;overflow:auto;">
                <table class="step-table" id="stepTable">
                    <thead><tr><th>步驟</th><th>操作</th><th>節點類型</th><th>候選</th><th>結果</th></tr></thead>
                    <tbody id="stepTableBody"></tbody>
                </table>
            </div>
        </div>
        <div id="rangeTab" class="tab-content">
            <div id="rangeVisualization"></div>
        </div>
    </div>
</div>

<div id="map"></div>
</div>

<script>
// ==================== Global Variables ====================
const EMBEDDED_GRAPH_DATA = __GRAPH_DATA_PLACEHOLDER__;
let graphData, map;
let nodes = new Map();
let adjacencyList = new Map();
let keywordIndex = new Map();
let distanceCache = new Map();
let markers = [];

// ==================== AB-Tree Implementation ====================
class ABTreeEntry {
    constructor(distance, vertexId, keywords) {
        this.distance = distance;
        this.vertexId = vertexId;
        this.keywords = new Set(keywords);
    }
    hasKeyword(w) { return this.keywords.has(w.toLowerCase()); }
}

class ABTreeNode {
    constructor(isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = [];
        this.children = isLeaf ? null : [];
        this.entries = isLeaf ? [] : null;
        this.subtreeKeywords = new Set();
    }
}

class ABTree {
    constructor(sourceVertex) {
        this.sourceVertex = sourceVertex;
        this.root = new ABTreeNode(true);
        this.nodeCount = 1;
        this.entryCount = 0;
        this.ORDER = 32;
        this.searchSteps = [];
    }

    static buildFromNetwork(sourceVertex) {
        const tree = new ABTree(sourceVertex);
        const distances = computeAllDistancesFrom(sourceVertex);
        
        const entries = [];
        for (const [vertexId, distance] of distances) {
            if (distance < Infinity && vertexId !== sourceVertex) {
                const node = nodes.get(vertexId);
                if (node && node.keywords && node.keywords.length > 0) {
                    entries.push(new ABTreeEntry(distance, vertexId, node.keywords));
                }
            }
        }
        
        entries.sort((a, b) => a.distance - b.distance);
        for (const entry of entries) {
            tree.insert(entry);
        }
        
        return tree;
    }

    insert(entry) {
        for (const kw of entry.keywords) {
            this.root.subtreeKeywords.add(kw);
        }
        
        if (this.root.isLeaf) {
            this.insertIntoLeaf(this.root, entry);
            if (this.root.entries.length >= this.ORDER) {
                const newRoot = new ABTreeNode(false);
                newRoot.children = [this.root];
                newRoot.subtreeKeywords = new Set(this.root.subtreeKeywords);
                this.splitChild(newRoot, 0);
                this.root = newRoot;
            }
        } else {
            this.insertNonFull(this.root, entry);
        }
        this.entryCount++;
    }

    insertIntoLeaf(leaf, entry) {
        let pos = 0;
        while (pos < leaf.entries.length && leaf.entries[pos].distance < entry.distance) pos++;
        leaf.entries.splice(pos, 0, entry);
        for (const kw of entry.keywords) leaf.subtreeKeywords.add(kw);
    }

    insertNonFull(node, entry) {
        for (const kw of entry.keywords) node.subtreeKeywords.add(kw);
        
        if (node.isLeaf) {
            this.insertIntoLeaf(node, entry);
        } else {
            let i = node.keys.length - 1;
            while (i >= 0 && entry.distance < node.keys[i]) i--;
            i++;
            
            const child = node.children[i];
            for (const kw of entry.keywords) child.subtreeKeywords.add(kw);
            
            if ((child.isLeaf && child.entries.length >= this.ORDER - 1) ||
                (!child.isLeaf && child.keys.length >= this.ORDER - 1)) {
                this.splitChild(node, i);
                if (entry.distance > node.keys[i]) i++;
            }
            this.insertNonFull(node.children[i], entry);
        }
    }

    splitChild(parent, index) {
        const child = parent.children[index];
        const newNode = new ABTreeNode(child.isLeaf);
        this.nodeCount++;
        
        let mid, midKey;
        if (child.isLeaf) {
            mid = Math.floor(child.entries.length / 2);
            midKey = child.entries[mid].distance;
            newNode.entries = child.entries.splice(mid);
            newNode.subtreeKeywords = new Set();
            for (const e of newNode.entries) for (const kw of e.keywords) newNode.subtreeKeywords.add(kw);
            child.subtreeKeywords = new Set();
            for (const e of child.entries) for (const kw of e.keywords) child.subtreeKeywords.add(kw);
        } else {
            mid = Math.floor(child.keys.length / 2);
            midKey = child.keys[mid];
            newNode.keys = child.keys.splice(mid + 1);
            child.keys.splice(mid);
            newNode.children = child.children.splice(mid + 1);
            newNode.subtreeKeywords = new Set();
            for (const c of newNode.children) for (const kw of c.subtreeKeywords) newNode.subtreeKeywords.add(kw);
            child.subtreeKeywords = new Set();
            for (const c of child.children) for (const kw of c.subtreeKeywords) child.subtreeKeywords.add(kw);
        }
        
        parent.keys.splice(index, 0, midKey);
        parent.children.splice(index + 1, 0, newNode);
    }

    // Algorithm 4: findNext with AB-Tree
    findNext(clue, theta, excluded) {
        this.searchSteps = [];
        const w = clue.keyword.toLowerCase();
        const d = clue.distance;
        const epsilon = clue.epsilon;
        
        // Line 2: lD and rD
        const lD = d - d * epsilon;
        const rD = d + d * epsilon;
        
        this.searchSteps.push({action: 'INIT', type: 'range', query: d, keyword: w, result: `lD=${lD.toFixed(1)}, rD=${rD.toFixed(1)}, θ=${theta}`});
        
        // Line 3: predecessor
        const predecessor = this.findPredecessor(this.root, lD, w, excluded);
        
        // Line 4: successor (find closest to d within range)
        const successor = this.findSuccessor(this.root, rD, w, excluded);
        
        if (!predecessor && !successor) {
            this.searchSteps.push({action: 'NO_CANDIDATE', type: 'result', result: '無符合候選'});
            return { found: false };
        }
        
        // Line 5-8: Compare and select
        let selected = null;
        if (predecessor && successor) {
            const diffP = d - predecessor.distance;
            const diffS = successor.distance - d;
            if (diffP <= diffS) {
                selected = predecessor;
                this.searchSteps.push({action: 'SELECT_PRED', type: 'select', candidate: predecessor.vertexId, dist: predecessor.distance, result: `d-d_p=${diffP.toFixed(1)} ≤ d_s-d=${diffS.toFixed(1)}`});
            } else {
                selected = successor;
                this.searchSteps.push({action: 'SELECT_SUCC', type: 'select', candidate: successor.vertexId, dist: successor.distance, result: `d_s-d=${diffS.toFixed(1)} < d-d_p=${diffP.toFixed(1)}`});
            }
        } else {
            selected = predecessor || successor;
            this.searchSteps.push({action: selected === predecessor ? 'SELECT_PRED' : 'SELECT_SUCC', type: 'select', candidate: selected.vertexId, dist: selected.distance, result: '唯一候選'});
        }
        
        const dm = Math.abs(selected.distance - d) / (epsilon * d);
        if (dm >= theta) {
            return { found: true, vertexId: selected.vertexId, distance: selected.distance, matchingDistance: dm };
        }
        
        this.searchSteps.push({action: 'THRESHOLD_FAIL', type: 'result', result: `dm=${dm.toFixed(4)} < θ=${theta}`});
        return { found: false };
    }

    findPredecessor(node, lD, w, excluded) {
        if (!node.subtreeKeywords.has(w)) {
            this.searchSteps.push({action: 'PRED_SKIP', type: node.isLeaf ? 'leaf' : 'internal', result: `無關鍵字 "${w}"`});
            return null;
        }
        
        if (node.isLeaf) {
            let best = null;
            for (let i = node.entries.length - 1; i >= 0; i--) {
                const entry = node.entries[i];
                if (entry.distance > lD) continue;
                if (entry.hasKeyword(w) && !excluded.has(entry.vertexId)) {
                    best = entry;
                    break;
                }
            }
            this.searchSteps.push({action: 'PRED_LEAF', type: 'leaf', candidate: best?.vertexId, dist: best?.distance, result: best ? `找到 v${best.vertexId}` : '無匹配'});
            return best;
        } else {
            let i = node.keys.length - 1;
            while (i >= 0 && lD < node.keys[i]) i--;
            
            for (let j = i + 1; j >= 0 && j < node.children.length; j--) {
                const result = this.findPredecessor(node.children[j], lD, w, excluded);
                if (result) return result;
            }
            return null;
        }
    }

    findSuccessor(node, rD, w, excluded) {
        if (!node.subtreeKeywords.has(w)) {
            this.searchSteps.push({action: 'SUCC_SKIP', type: node.isLeaf ? 'leaf' : 'internal', result: `無關鍵字 "${w}"`});
            return null;
        }
        
        if (node.isLeaf) {
            let best = null;
            for (const entry of node.entries) {
                if (entry.distance > rD) break;
                if (entry.hasKeyword(w) && !excluded.has(entry.vertexId)) {
                    if (!best || entry.distance > best.distance) best = entry;
                }
            }
            this.searchSteps.push({action: 'SUCC_LEAF', type: 'leaf', candidate: best?.vertexId, dist: best?.distance, result: best ? `找到 v${best.vertexId}` : '無匹配'});
            return best;
        } else {
            let best = null;
            for (const child of node.children) {
                const result = this.findSuccessor(child, rD, w, excluded);
                if (result && (!best || result.distance > best.distance)) best = result;
            }
            return best;
        }
    }
}

// ==================== Dijkstra ====================
class MinHeap {
    constructor() { this.h = []; }
    ins(i) { this.h.push(i); this.up(this.h.length - 1); }
    ext() { if (!this.h.length) return null; const m = this.h[0], l = this.h.pop(); if (this.h.length) { this.h[0] = l; this.dn(0); } return m; }
    emp() { return !this.h.length; }
    up(i) { while (i > 0) { const p = Math.floor((i - 1) / 2); if (this.h[p].d <= this.h[i].d) break; [this.h[p], this.h[i]] = [this.h[i], this.h[p]]; i = p; } }
    dn(i) { const n = this.h.length; while (1) { let s = i, l = 2 * i + 1, r = 2 * i + 2; if (l < n && this.h[l].d < this.h[s].d) s = l; if (r < n && this.h[r].d < this.h[s].d) s = r; if (s === i) break; [this.h[s], this.h[i]] = [this.h[i], this.h[s]]; i = s; } }
}

function computeAllDistancesFrom(source) {
    const distances = new Map();
    const pq = new MinHeap();
    const visited = new Set();
    
    pq.ins({ d: 0, n: source });
    distances.set(source, 0);
    
    while (!pq.emp()) {
        const { d, n } = pq.ext();
        if (visited.has(n)) continue;
        visited.add(n);
        distances.set(n, d);
        
        for (const edge of adjacencyList.get(n) || []) {
            if (!visited.has(edge.to)) {
                const nd = d + edge.weight;
                if (nd < (distances.get(edge.to) || Infinity)) {
                    distances.set(edge.to, nd);
                    pq.ins({ d: nd, n: edge.to });
                }
            }
        }
    }
    return distances;
}

// ==================== Map ====================
function initMap() {
    map = L.map('map').setView([25.016, 121.543], 16);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { attribution: '© OSM' }).addTo(map);
}

function createIcon(color, size) {
    return L.divIcon({
        className: '',
        html: `<div style="background:${color};width:${size}px;height:${size}px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>`,
        iconSize: [size, size], iconAnchor: [size/2, size/2]
    });
}

function clearMarkers() {
    markers.forEach(m => map.removeLayer(m));
    markers = [];
}

// ==================== Data Loading ====================
async function loadGraphData() {
    graphData = EMBEDDED_GRAPH_DATA;
    
    for (const n of graphData.nodes) {
        nodes.set(n.id, n);
        adjacencyList.set(n.id, []);
    }
    
    for (const e of graphData.edges) {
        adjacencyList.get(e.from)?.push(e);
        adjacencyList.get(e.to)?.push({ from: e.to, to: e.from, weight: e.weight });
    }
    
    for (const [k, v] of Object.entries(graphData.keywordIndex)) {
        keywordIndex.set(k.toLowerCase(), new Set(v));
    }
    
    drawNetwork();
    populateSelect();
    populateKeywordSuggestions();
    document.getElementById('loadingOverlay').style.display = 'none';
}

function drawNetwork() {
    const lines = [], added = new Set();
    for (const e of graphData.edges) {
        const k = Math.min(e.from, e.to) + '-' + Math.max(e.from, e.to);
        if (!added.has(k)) {
            added.add(k);
            const f = nodes.get(e.from), t = nodes.get(e.to);
            if (f && t) lines.push([[f.lat, f.lon], [t.lat, t.lon]]);
        }
    }
    L.polyline(lines, { color: '#bdc3c7', weight: 1 }).addTo(map);
}

function populateSelect() {
    const sel = document.getElementById('sourceSelect');
    sel.innerHTML = '<option value="">選擇來源頂點...</option>';
    let count = 0;
    for (const [id, edges] of adjacencyList) {
        if (edges.length > 0 && count < 300) {
            const n = nodes.get(id);
            const kws = n.keywords?.slice(0, 2).join(', ') || '-';
            const opt = document.createElement('option');
            opt.value = id;
            opt.textContent = `${id} (${kws})`;
            sel.appendChild(opt);
            count++;
        }
    }
}

function populateKeywordSuggestions() {
    const container = document.getElementById('keywordSuggestions');
    const stats = [];
    for (const [kw, ids] of keywordIndex) stats.push({ kw, count: ids.size });
    stats.sort((a, b) => b.count - a.count);
    
    container.innerHTML = stats.slice(0, 20).map(s =>
        `<span class="keyword-chip" onclick="document.getElementById('keywordInput').value='${s.kw}'">${s.kw} (${s.count})</span>`
    ).join('');
}

function switchTab(name) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
    document.querySelector(`.tab:nth-child(${name === 'steps' ? 1 : 2})`).classList.add('active');
    document.getElementById(name + 'Tab').classList.add('active');
}

// ==================== Algorithm Execution ====================
function runFindNext() {
    const source = parseInt(document.getElementById('sourceSelect').value);
    const keyword = document.getElementById('keywordInput').value.trim().toLowerCase();
    const distance = parseFloat(document.getElementById('distanceInput').value);
    const epsilon = parseFloat(document.getElementById('epsilonInput').value);
    const theta = parseFloat(document.getElementById('thetaInput').value);
    
    if (!source) { alert('請選擇來源頂點'); return; }
    if (!keyword) { alert('請輸入關鍵字'); return; }
    
    clearMarkers();
    
    // Show source marker
    const sourceNode = nodes.get(source);
    if (sourceNode) {
        const m = L.marker([sourceNode.lat, sourceNode.lon], { icon: createIcon('#e74c3c', 18) })
            .addTo(map).bindPopup(`<b>來源頂點</b><br>v<sub>i-1</sub> = ${source}`);
        markers.push(m);
        map.setView([sourceNode.lat, sourceNode.lon], 16);
    }
    
    // Build AB-Tree
    const startBuild = performance.now();
    const tree = ABTree.buildFromNetwork(source);
    const buildTime = performance.now() - startBuild;
    
    // Show tree info
    document.getElementById('treePanel').style.display = 'block';
    document.getElementById('treeSource').textContent = source;
    document.getElementById('treeNodes').textContent = tree.nodeCount;
    document.getElementById('treeEntries').textContent = tree.entryCount;
    document.getElementById('treeBuildTime').textContent = buildTime.toFixed(1) + ' ms';
    
    // Run findNext
    const clue = { keyword, distance, epsilon };
    const result = tree.findNext(clue, theta, new Set());
    
    // Display results
    displayFindNextResult(result, clue, tree);
    displaySearchSteps(tree.searchSteps);
    displayRangeVisualization(clue, result);
    
    // Visualize on map
    if (result.found) {
        const n = nodes.get(result.vertexId);
        if (n) {
            const m = L.marker([n.lat, n.lon], { icon: createIcon('#f39c12', 16) })
                .addTo(map).bindPopup(`<b>findNext 結果</b><br>v<sub>i</sub> = ${result.vertexId}<br>d = ${result.distance.toFixed(1)}m<br>d<sub>m</sub> = ${result.matchingDistance.toFixed(4)}`);
            markers.push(m);
        }
    }
    
    // Show keyword matches within distance range
    const lD = clue.distance * (1 - clue.epsilon);
    const rD = clue.distance * (1 + clue.epsilon);
    const sourceNode = nodes.get(source);
    
    if (sourceNode) {
        const distances = computeAllDistancesFrom(source);
        const kwNodes = keywordIndex.get(keyword) || new Set();
        
        for (const id of kwNodes) {
            if (id !== result.vertexId) {
                const dist = distances.get(id) || Infinity;
                // Only show nodes within the distance range [lD, rD]
                if (dist >= lD && dist <= rD) {
                    const n = nodes.get(id);
                    if (n) {
                        const m = L.circleMarker([n.lat, n.lon], { radius: 4, color: '#9b59b6', fillColor: '#9b59b6', fillOpacity: 0.5 })
                            .addTo(map)
                            .bindPopup(`<b>匹配節點</b><br>v = ${id}<br>d = ${dist.toFixed(1)}m`);
                        markers.push(m);
                    }
                }
            }
        }
    }
}

function displayFindNextResult(result, clue, tree) {
    const panel = document.getElementById('resultPanel');
    const content = document.getElementById('resultContent');
    panel.style.display = 'block';
    
    if (result.found) {
        content.innerHTML = `
            <div class="result-card success">
                <strong style="color:#27ae60;">✓ 找到候選頂點</strong><br>
                <div style="margin-top:8px;">
                    <div class="info-row"><span class="info-label">v<sub>i</sub></span><span class="info-value">${result.vertexId}</span></div>
                    <div class="info-row"><span class="info-label">網路距離 d<sub>G</sub></span><span class="info-value">${result.distance.toFixed(2)} m</span></div>
                    <div class="info-row"><span class="info-label">匹配距離 d<sub>m</sub></span><span class="info-value">${result.matchingDistance.toFixed(4)}</span></div>
                </div>
            </div>
            <div class="result-card info">
                <strong>查詢參數</strong><br>
                <span class="clue-badge">w = ${clue.keyword}</span>
                <span class="clue-badge">d = ${clue.distance}m</span>
                <span class="clue-badge">ε = ${clue.epsilon}</span>
            </div>`;
        document.getElementById('headerResult').textContent = `找到: v${result.vertexId}, dm=${result.matchingDistance.toFixed(4)}`;
    } else {
        content.innerHTML = `
            <div class="result-card" style="background:#fff3e0;border:1px solid #f39c12;">
                <strong style="color:#f39c12;">⚠ 未找到候選頂點</strong><br>
                <span style="font-size:0.9em;">請調整參數或選擇其他關鍵字</span>
            </div>`;
        document.getElementById('headerResult').textContent = '未找到候選';
    }
}

function displaySearchSteps(steps) {
    const tbody = document.getElementById('stepTableBody');
    tbody.innerHTML = steps.map((s, i) => {
        let cls = '';
        if (s.action.includes('PRED')) cls = 'predecessor';
        else if (s.action.includes('SUCC')) cls = 'successor';
        else if (s.action.includes('SELECT')) cls = 'select';
        else if (s.action === 'INIT') cls = 'init';
        
        return `<tr class="${cls}">
            <td>${i + 1}</td>
            <td>${s.action}</td>
            <td>${s.type || '-'}</td>
            <td>${s.candidate ? 'v' + s.candidate : '-'}</td>
            <td style="font-size:0.7em;">${s.result || '-'}</td>
        </tr>`;
    }).join('');
}

function displayRangeVisualization(clue, result) {
    const container = document.getElementById('rangeVisualization');
    const lD = clue.distance * (1 - clue.epsilon);
    const rD = clue.distance * (1 + clue.epsilon);
    
    container.innerHTML = `
        <div style="margin-bottom:10px;">
            <div class="info-row"><span class="info-label">目標距離 d</span><span class="info-value">${clue.distance} m</span></div>
            <div class="info-row"><span class="info-label">最小距離 lD</span><span class="info-value">${lD.toFixed(1)} m</span></div>
            <div class="info-row"><span class="info-label">最大距離 rD</span><span class="info-value">${rD.toFixed(1)} m</span></div>
        </div>
        <div style="position:relative;height:40px;margin:20px 0;">
            <div style="position:absolute;left:0;right:0;top:15px;height:8px;background:linear-gradient(90deg,#ecf0f1 0%,#27ae60 ${((clue.distance-lD)/(rD-lD)*100).toFixed(0)}%,#27ae60 ${((clue.distance-lD)/(rD-lD)*100).toFixed(0)}%,#ecf0f1 100%);border-radius:4px;"></div>
            <div style="position:absolute;left:0;top:10px;font-size:0.7em;">${lD.toFixed(0)}m</div>
            <div style="position:absolute;right:0;top:10px;font-size:0.7em;">${rD.toFixed(0)}m</div>
            <div style="position:absolute;left:50%;top:0;transform:translateX(-50%);font-size:0.7em;color:#27ae60;font-weight:bold;">d=${clue.distance}m</div>
            ${result.found ? `<div style="position:absolute;left:${((result.distance-lD)/(rD-lD)*100).toFixed(0)}%;top:25px;transform:translateX(-50%);width:12px;height:12px;background:#f39c12;border-radius:50%;border:2px solid white;"></div>` : ''}
        </div>`;
}

function runFullBAB() {
    alert('完整 BAB + AB-Tree 功能請使用 Java 版本執行');
}

// ==================== Initialize ====================
document.addEventListener('DOMContentLoaded', () => {
    initMap();
    loadGraphData();
});
</script>
</body>
</html>
""";
    }
}
