package pbtree.visualization;

import pbtree.model.*;
import pbtree.algorithm.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;

/**
 * Generates HTML visualization for findNext() with PB-Tree.
 * Uses CDP-style UI design.
 */
public class HtmlVisualizer {
    
    private final RoadNetwork network;
    private final Gson gson;
    
    public HtmlVisualizer(RoadNetwork network) {
        this.network = network;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
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
        
        try (FileWriter writer = new FileWriter(filename)) {
            gson.toJson(data, writer);
        }
        
        System.out.println("Exported graph data to " + filename);
    }
    
    /**
     * Generate standalone HTML visualization with embedded data.
     */
    public void generateStandaloneHtml(String graphJsonPath, String outputPath) throws IOException {
        // Read graph JSON
        String graphJson;
        try (BufferedReader reader = new BufferedReader(new FileReader(graphJsonPath))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            graphJson = sb.toString();
        }
        
        // Generate HTML with embedded data
        String html = getHtmlTemplate().replace("__GRAPH_DATA_PLACEHOLDER__", graphJson);
        
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(html);
        }
        
        System.out.println("Generated standalone HTML: " + outputPath);
    }
    
    /**
     * Get HTML template with CDP-style UI for PB-Tree visualization.
     */
    private String getHtmlTemplate() {
        return """
<!DOCTYPE html>
<html lang="zh-TW">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>findNext() with PB-Tree - 視覺化</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; }
        .header { background: linear-gradient(135deg, #27ae60 0%, #2980b9 100%); color: white; padding: 15px 20px; text-align: center; }
        .header h1 { font-size: 1.4em; }
        .header p { font-size: 0.85em; opacity: 0.9; }
        .main-container { display: flex; height: calc(100vh - 70px); }
        .left-panel { width: 450px; background: white; overflow-y: auto; border-right: 2px solid #dee2e6; padding: 15px; }
        #map { flex: 1; }
        .panel { background: #fafafa; margin-bottom: 15px; padding: 15px; border-radius: 8px; border: 1px solid #e0e0e0; }
        .panel h3 { color: #2c3e50; font-size: 1em; margin-bottom: 10px; padding-bottom: 8px; border-bottom: 2px solid #27ae60; }
        .info-row { display: flex; justify-content: space-between; padding: 5px 0; }
        .info-label { color: #666; }
        .info-value { font-weight: bold; color: #2c3e50; }
        .clue-badge { display: inline-block; background: #27ae60; color: white; padding: 3px 10px; border-radius: 12px; font-size: 0.85em; margin: 2px; }
        .clue-input-group { background: #f8f9fa; border: 1px solid #e0e0e0; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
        .clue-input-group label { display: block; font-size: 0.8em; color: #666; margin-bottom: 4px; }
        .clue-input-group input, .clue-input-group select { width: 100%; padding: 6px 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 0.9em; margin-bottom: 6px; }
        .clue-input-row { display: flex; gap: 10px; }
        .clue-input-row > div { flex: 1; }
        .step-table { width: 100%; border-collapse: collapse; font-size: 0.75em; margin-top: 10px; }
        .step-table th, .step-table td { border: 1px solid #ddd; padding: 5px; text-align: center; }
        .step-table th { background: #27ae60; color: white; }
        .step-table .predecessor { background: #e8f5e9; border-left: 3px solid #27ae60; }
        .step-table .successor { background: #e3f2fd; border-left: 3px solid #3498db; }
        .step-table .select { background: #fff3e0; border-left: 3px solid #f39c12; }
        .step-table .init { background: #e8f5e9; border-left: 3px solid #27ae60; }
        .legend-item { display: flex; align-items: center; margin: 5px 0; }
        .legend-dot { width: 14px; height: 14px; border-radius: 50%; margin-right: 8px; border: 2px solid white; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
        .path-step { display: flex; align-items: center; padding: 8px; margin: 5px 0; background: #e8f5e9; border-radius: 6px; border-left: 4px solid #27ae60; cursor: pointer; }
        .path-step:hover { transform: translateX(5px); background: #d4edda; }
        .path-step.optimal { border-left-color: #2980b9; background: #e3f2fd; }
        .step-num { width: 24px; height: 24px; background: #27ae60; color: white; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 10px; font-size: 0.8em; }
        .path-step.optimal .step-num { background: #2980b9; }
        .btn { background: #27ae60; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; margin: 3px; font-size: 0.9em; }
        .btn:hover { background: #1e8449; }
        .btn-info { background: #2980b9; }
        .btn-info:hover { background: #2471a3; }
        .loading-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(44, 62, 80, 0.95); display: flex; align-items: center; justify-content: center; z-index: 1000; color: white; flex-direction: column; }
        .loading-spinner { width: 50px; height: 50px; border: 4px solid rgba(255,255,255,0.3); border-top-color: #27ae60; border-radius: 50%; animation: spin 1s linear infinite; margin-bottom: 15px; }
        @keyframes spin { to { transform: rotate(360deg); } }
        .tree-vis { background: #2c3e50; color: #ecf0f1; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 0.75em; max-height: 200px; overflow: auto; margin-top: 10px; }
        .tree-node { margin-left: 15px; }
        .tree-leaf { color: #2ecc71; }
        .tree-internal { color: #3498db; }
        .tree-keyword { color: #f39c12; }
        .pivot-badge { display: inline-block; background: #2980b9; color: white; padding: 2px 8px; border-radius: 10px; font-size: 0.8em; margin-left: 5px; }
        .result-card { padding: 15px; border-radius: 8px; margin-bottom: 10px; }
        .result-card.success { background: linear-gradient(135deg, rgba(39,174,96,0.15), rgba(46,204,113,0.15)); border: 1px solid #27ae60; }
        .result-card.info { background: linear-gradient(135deg, rgba(41,128,185,0.15), rgba(52,152,219,0.15)); border: 1px solid #2980b9; }
        .keyword-chip { display: inline-block; background: #e8f4f8; padding: 2px 8px; margin: 2px; border-radius: 4px; font-size: 0.8em; cursor: pointer; }
        .keyword-chip:hover { background: #27ae60; color: white; }
    </style>
</head>
<body>
<div class="loading-overlay" id="loadingOverlay">
    <div class="loading-spinner"></div>
    <div id="loadingText">載入圖形資料...</div>
</div>

<div class="header">
    <h1>🌳 findNext() with PB-Tree (Pivot reverse Binary Tree)</h1>
    <p>2-Hop Label 與 Pivot 索引加速 | 空間效率優化方案 | <span id="headerResult">請執行查詢</span></p>
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
            <label>目標關鍵字 w<sub>i</sub></label>
            <select id="keywordSelect"><option value="">載入中...</option></select>
        </div>
        <div class="clue-input-row">
            <div class="clue-input-group">
                <label>期望距離 d (公尺)</label>
                <input type="number" id="distanceInput" value="200" min="1">
            </div>
            <div class="clue-input-group">
                <label>容差 ε</label>
                <input type="number" id="epsilonInput" value="0.5" min="0.01" max="1" step="0.1">
            </div>
        </div>
        <div class="clue-input-row">
            <div class="clue-input-group">
                <label>閾值 θ</label>
                <input type="number" id="thetaInput" value="0" min="0">
            </div>
            <div class="clue-input-group">
                <label>上界 UB</label>
                <input type="number" id="ubInput" value="1" min="0" max="1" step="0.1">
            </div>
        </div>
        <button class="btn" onclick="runFindNext()">🔍 執行 findNext()</button>
        <button class="btn btn-info" onclick="runDemo()">📊 執行 Demo</button>
    </div>

    <!-- Algorithm Info -->
    <div class="panel">
        <h3>📐 PB-Tree 演算法說明</h3>
        <div style="font-size: 0.85em; line-height: 1.6;">
            <p><strong>核心概念：</strong>利用 2-hop label 的樞紐(Pivot)來壓縮索引空間</p>
            <p style="margin-top: 8px;"><strong>距離計算：</strong></p>
            <p>d<sub>G</sub>(u, v) = d<sub>G</sub>(u, o) + d<sub>G</sub>(o, v)</p>
            <p style="margin-top: 8px;"><strong>優勢：</strong></p>
            <ul style="padding-left: 20px;">
                <li>空間複雜度: O(|L| × h) vs AB-tree 的 O(|V|²)</li>
                <li>可放入主記憶體，避免 I/O 開銷</li>
                <li>保持高效的前驅/後繼查詢</li>
            </ul>
        </div>
    </div>

    <!-- Results Panel -->
    <div class="panel" id="resultsPanel" style="display: none;">
        <h3>📊 查詢結果</h3>
        <div id="resultsContent"></div>
    </div>

    <!-- Legend -->
    <div class="panel">
        <h3>📋 圖例</h3>
        <div class="legend-item"><div class="legend-dot" style="background: #e74c3c;"></div> 來源頂點 v<sub>i-1</sub></div>
        <div class="legend-item"><div class="legend-dot" style="background: #27ae60;"></div> 候選頂點 (符合條件)</div>
        <div class="legend-item"><div class="legend-dot" style="background: #2980b9;"></div> 樞紐頂點 (Pivot)</div>
        <div class="legend-item"><div class="legend-dot" style="background: #f39c12;"></div> 含有關鍵字的頂點</div>
    </div>
</div>

<div id="map"></div>
</div>

<script>
const EMBEDDED_GRAPH_DATA = __GRAPH_DATA_PLACEHOLDER__;

let map, graphData, nodeMarkers = {}, edgeLines = [];

// Initialize map and load data
window.onload = async function() {
    document.getElementById('loadingText').textContent = '初始化地圖...';
    
    // Load graph data
    graphData = EMBEDDED_GRAPH_DATA;
    
    // Initialize map
    const bounds = calculateBounds(graphData.nodes);
    map = L.map('map').fitBounds(bounds);
    
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);
    
    document.getElementById('loadingText').textContent = '繪製網路圖...';
    await drawNetwork();
    
    // Populate dropdowns
    populateDropdowns();
    
    document.getElementById('loadingOverlay').style.display = 'none';
};

function calculateBounds(nodes) {
    let minLat = Infinity, maxLat = -Infinity;
    let minLon = Infinity, maxLon = -Infinity;
    
    nodes.forEach(n => {
        minLat = Math.min(minLat, n.lat);
        maxLat = Math.max(maxLat, n.lat);
        minLon = Math.min(minLon, n.lon);
        maxLon = Math.max(maxLon, n.lon);
    });
    
    return [[minLat, minLon], [maxLat, maxLon]];
}

async function drawNetwork() {
    // Draw edges
    graphData.edges.forEach(edge => {
        const from = graphData.nodes.find(n => n.id === edge.from);
        const to = graphData.nodes.find(n => n.id === edge.to);
        if (from && to) {
            const line = L.polyline([[from.lat, from.lon], [to.lat, to.lon]], {
                color: '#bdc3c7',
                weight: 2,
                opacity: 0.6
            }).addTo(map);
            edgeLines.push(line);
        }
    });
    
    // Draw nodes
    graphData.nodes.forEach(node => {
        const hasKeywords = node.keywords && node.keywords.length > 0;
        const marker = L.circleMarker([node.lat, node.lon], {
            radius: hasKeywords ? 6 : 4,
            fillColor: hasKeywords ? '#f39c12' : '#3498db',
            color: '#fff',
            weight: 1,
            fillOpacity: 0.8
        }).addTo(map);
        
        marker.bindPopup(`<b>Node ${node.id}</b><br>Keywords: ${node.keywords.join(', ') || 'none'}`);
        nodeMarkers[node.id] = marker;
    });
}

function populateDropdowns() {
    // Source vertex dropdown
    const sourceSelect = document.getElementById('sourceSelect');
    sourceSelect.innerHTML = '';
    graphData.nodes.filter(n => n.keywords && n.keywords.length > 0).slice(0, 100).forEach(node => {
        const opt = document.createElement('option');
        opt.value = node.id;
        opt.textContent = `v${node.id} (${node.keywords.slice(0, 2).join(', ')})`;
        sourceSelect.appendChild(opt);
    });
    
    // Keyword dropdown
    const keywordSelect = document.getElementById('keywordSelect');
    keywordSelect.innerHTML = '';
    const keywords = Object.keys(graphData.keywordIndex || {}).sort((a, b) => 
        (graphData.keywordIndex[b]?.length || 0) - (graphData.keywordIndex[a]?.length || 0)
    );
    keywords.slice(0, 50).forEach(kw => {
        const opt = document.createElement('option');
        opt.value = kw;
        opt.textContent = `${kw} (${graphData.keywordIndex[kw]?.length || 0} nodes)`;
        keywordSelect.appendChild(opt);
    });
}

function runFindNext() {
    const sourceId = parseInt(document.getElementById('sourceSelect').value);
    const keyword = document.getElementById('keywordSelect').value;
    const distance = parseFloat(document.getElementById('distanceInput').value);
    const epsilon = parseFloat(document.getElementById('epsilonInput').value);
    const theta = parseFloat(document.getElementById('thetaInput').value);
    const ub = parseFloat(document.getElementById('ubInput').value);
    
    // Reset markers
    Object.values(nodeMarkers).forEach(m => {
        m.setStyle({ fillColor: m.options.fillColor === '#e74c3c' || m.options.fillColor === '#27ae60' || m.options.fillColor === '#2980b9' ? '#3498db' : m.options.fillColor });
    });
    
    // Highlight source
    if (nodeMarkers[sourceId]) {
        nodeMarkers[sourceId].setStyle({ fillColor: '#e74c3c', radius: 10 });
        map.panTo([graphData.nodes.find(n => n.id === sourceId).lat, graphData.nodes.find(n => n.id === sourceId).lon]);
    }
    
    // Find candidates with keyword
    const lD = distance * (1 - epsilon) + theta;
    const rD = distance * (1 + epsilon) - theta;
    const lB = distance - distance * epsilon * ub;
    const rB = distance + distance * epsilon * ub;
    
    let resultHtml = `
        <div class="result-card info">
            <strong>查詢參數</strong><br>
            <small>
                來源: v${sourceId}<br>
                關鍵字: "${keyword}"<br>
                距離範圍 [lD, rD]: [${lD.toFixed(1)}, ${rD.toFixed(1)}]m<br>
                剪枝範圍 [lB, rB]: [${lB.toFixed(1)}, ${rB.toFixed(1)}]m
            </small>
        </div>
    `;
    
    // Highlight keyword nodes
    const keywordNodes = graphData.keywordIndex[keyword] || [];
    keywordNodes.forEach(nodeId => {
        if (nodeMarkers[nodeId]) {
            nodeMarkers[nodeId].setStyle({ fillColor: '#f39c12' });
        }
    });
    
    resultHtml += `
        <div class="result-card success">
            <strong>PB-Tree 查詢步驟</strong><br>
            <small>
                1. 檢查樞紐 (Pivots) in L(v${sourceId})<br>
                2. 對每個 PB(o) 執行 predecessor/successor 查詢<br>
                3. 驗證最短路徑並計算 matching distance<br>
                <br>
                找到 ${keywordNodes.length} 個含有 "${keyword}" 的頂點
            </small>
        </div>
    `;
    
    document.getElementById('resultsContent').innerHTML = resultHtml;
    document.getElementById('resultsPanel').style.display = 'block';
    document.getElementById('headerResult').textContent = `找到 ${keywordNodes.length} 個候選`;
}

function runDemo() {
    // Auto-select first available source and keyword
    const sourceSelect = document.getElementById('sourceSelect');
    const keywordSelect = document.getElementById('keywordSelect');
    
    if (sourceSelect.options.length > 0) {
        sourceSelect.selectedIndex = 0;
    }
    if (keywordSelect.options.length > 1) {
        keywordSelect.selectedIndex = 1;
    }
    
    document.getElementById('distanceInput').value = 200;
    document.getElementById('epsilonInput').value = 0.5;
    document.getElementById('thetaInput').value = 0;
    document.getElementById('ubInput').value = 0.4;
    
    runFindNext();
}
</script>
</body>
</html>
""";
    }
}
