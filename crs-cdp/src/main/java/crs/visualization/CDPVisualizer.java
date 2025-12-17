package crs.visualization;

import crs.model.*;
import crs.algorithm.CDPAlgorithm.*;
import java.io.*;
import java.util.*;

/**
 * CDP 算法 HTML 可視化生成器
 * 生成互動式地圖 + DP 表格視覺化
 */
public class CDPVisualizer {
    
    public void generateVisualization(
            RoadNetwork network,
            Node source,
            List<Clue> clues,
            CDPResult result,
            List<DPStep> dpSteps,
            String outputPath) throws IOException {
        
        // 計算地圖中心
        double centerLat = 0, centerLon = 0;
        int count = 0;
        for (Node node : network.getAllNodes()) {
            centerLat += node.getLat();
            centerLon += node.getLon();
            count++;
        }
        centerLat /= count;
        centerLon /= count;
        
        StringBuilder html = new StringBuilder();
        html.append(generateHeader());
        html.append(generateBody(source, clues, result, dpSteps, centerLat, centerLon));
        html.append(generateScript(source, clues, result, dpSteps, centerLat, centerLon));
        html.append("</body></html>");
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.print(html.toString());
        }
        
        System.out.println("\n可視化 HTML 已生成: " + outputPath);
    }
    
    private String generateHeader() {
        return """
<!DOCTYPE html>
<html lang="zh-TW">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>CDP Algorithm Visualization - 動態規劃路徑搜尋</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; }
        .header {
            background: linear-gradient(135deg, #2c3e50 0%, #3498db 100%);
            color: white; padding: 15px 20px; text-align: center;
        }
        .header h1 { font-size: 1.4em; }
        .header p { font-size: 0.85em; opacity: 0.9; }
        .main-container { display: flex; height: calc(100vh - 70px); }
        .left-panel {
            width: 420px; background: white; overflow-y: auto;
            border-right: 2px solid #dee2e6; padding: 15px;
        }
        #map { flex: 1; }
        .panel {
            background: #fafafa; margin-bottom: 15px; padding: 15px;
            border-radius: 8px; border: 1px solid #e0e0e0;
        }
        .panel h3 {
            color: #2c3e50; font-size: 1em; margin-bottom: 10px;
            padding-bottom: 8px; border-bottom: 2px solid #3498db;
        }
        .info-row { display: flex; justify-content: space-between; padding: 5px 0; }
        .info-label { color: #666; }
        .info-value { font-weight: bold; color: #2c3e50; }
        .clue-badge {
            display: inline-block; background: #3498db; color: white;
            padding: 3px 10px; border-radius: 12px; font-size: 0.85em; margin: 2px;
        }
        .dp-table { width: 100%; border-collapse: collapse; font-size: 0.8em; margin-top: 10px; }
        .dp-table th, .dp-table td {
            border: 1px solid #ddd; padding: 6px; text-align: center;
        }
        .dp-table th { background: #3498db; color: white; }
        .dp-table .optimal { background: #27ae60; color: white; font-weight: bold; }
        .dp-table .candidate { background: #f39c12; color: white; }
        .legend-item { display: flex; align-items: center; margin: 5px 0; }
        .legend-dot {
            width: 14px; height: 14px; border-radius: 50%; margin-right: 8px;
            border: 2px solid white; box-shadow: 0 1px 3px rgba(0,0,0,0.3);
        }
        .path-step {
            display: flex; align-items: center; padding: 8px;
            margin: 5px 0; background: #e8f4f8; border-radius: 6px;
            border-left: 4px solid #3498db; cursor: pointer;
            transition: all 0.2s;
        }
        .path-step:hover { transform: translateX(5px); background: #d4edda; }
        .path-step.optimal { border-left-color: #27ae60; background: #d4edda; }
        .step-num {
            width: 24px; height: 24px; background: #3498db; color: white;
            border-radius: 50%; display: flex; align-items: center;
            justify-content: center; margin-right: 10px; font-size: 0.8em;
        }
        .path-step.optimal .step-num { background: #27ae60; }
        .btn {
            background: #3498db; color: white; border: none;
            padding: 8px 16px; border-radius: 4px; cursor: pointer; margin: 3px;
        }
        .btn:hover { background: #2980b9; }
        .btn-success { background: #27ae60; }
        .btn-success:hover { background: #219a52; }
    </style>
</head>
""";
    }
    
    private String generateBody(Node source, List<Clue> clues, CDPResult result,
                               List<DPStep> dpSteps, double centerLat, double centerLon) {
        StringBuilder sb = new StringBuilder();
        sb.append("<body>\n");
        sb.append("<div class=\"header\">\n");
        sb.append("    <h1>🧮 Algorithm 2: CDP - 動態規劃線索路徑搜尋</h1>\n");
        sb.append("    <p>Clue-Based Dynamic Programming | 最優匹配距離: ");
        sb.append(String.format("%.4f", result.matchingDistance)).append("</p>\n");
        sb.append("</div>\n");
        
        sb.append("<div class=\"main-container\">\n");
        sb.append("<div class=\"left-panel\">\n");
        
        // 查詢資訊
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>📍 查詢資訊</h3>\n");
        sb.append("    <div class=\"info-row\"><span class=\"info-label\">起點</span>");
        sb.append("<span class=\"info-value\">").append(source.getName()).append("</span></div>\n");
        sb.append("    <div class=\"info-row\"><span class=\"info-label\">線索數量</span>");
        sb.append("<span class=\"info-value\">").append(clues.size()).append("</span></div>\n");
        sb.append("    <div class=\"info-row\"><span class=\"info-label\">最優匹配距離</span>");
        sb.append("<span class=\"info-value\">").append(String.format("%.4f", result.matchingDistance));
        sb.append("</span></div>\n");
        sb.append("    <div style=\"margin-top:10px;\">線索序列:</div>\n");
        for (int i = 0; i < clues.size(); i++) {
            Clue c = clues.get(i);
            sb.append("    <span class=\"clue-badge\">").append(i+1).append(". ");
            sb.append(c.getKeyword()).append(" ~").append((int)c.getDistance()).append("m</span>\n");
        }
        sb.append("</div>\n");
        
        // 最優路徑
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>🛤️ 最優路徑 FP<sub>cdp</sub></h3>\n");
        for (int i = 0; i < result.path.size(); i++) {
            Node node = result.path.get(i);
            String label = i == 0 ? "起點" : "匹配 " + i;
            sb.append("    <div class=\"path-step optimal\" onclick=\"focusNode(");
            sb.append(node.getLat()).append(",").append(node.getLon()).append(")\">\n");
            sb.append("        <div class=\"step-num\">").append(i).append("</div>\n");
            sb.append("        <div><strong>").append(label).append("</strong><br>");
            sb.append("<small>").append(node.getName()).append("</small></div>\n");
            sb.append("    </div>\n");
        }
        sb.append("</div>\n");
        
        // 圖例
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>🎨 圖例</h3>\n");
        sb.append("    <div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#e74c3c;\"></div>起點</div>\n");
        sb.append("    <div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#27ae60;\"></div>最優路徑節點</div>\n");
        sb.append("    <div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#f39c12;\"></div>DP 候選節點</div>\n");
        sb.append("    <div class=\"legend-item\"><div class=\"legend-dot\" style=\"background:#3498db;width:30px;height:4px;border-radius:2px;\"></div>最優路徑</div>\n");
        sb.append("</div>\n");
        
        // DP 表格
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>📊 動態規劃表 D(w<sub>i</sub>, u)</h3>\n");
        sb.append("    <button class=\"btn btn-success\" onclick=\"animateDP()\">▶ 動畫演示</button>\n");
        sb.append("    <button class=\"btn\" onclick=\"resetDP()\">⟲ 重置</button>\n");
        sb.append("    <div id=\"dp-table-container\" style=\"max-height:250px;overflow:auto;margin-top:10px;\">\n");
        sb.append(generateDPTable(clues, result, dpSteps));
        sb.append("    </div>\n");
        sb.append("</div>\n");
        
        sb.append("</div>\n"); // left-panel
        sb.append("<div id=\"map\"></div>\n");
        sb.append("</div>\n"); // main-container
        
        return sb.toString();
    }
    
    private String generateDPTable(List<Clue> clues, CDPResult result, List<DPStep> dpSteps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"dp-table\">\n");
        sb.append("<tr><th>Level</th><th>節點</th><th>D值</th><th>前驅</th></tr>\n");
        
        Set<Long> optimalNodes = new HashSet<>();
        for (Node n : result.path) optimalNodes.add(n.getId());
        
        for (DPStep step : dpSteps) {
            boolean isOpt = optimalNodes.contains(step.node.getId());
            String cls = isOpt ? "optimal" : "candidate";
            sb.append("<tr class=\"").append(cls).append("\">");
            sb.append("<td>").append(step.level).append("</td>");
            sb.append("<td>").append(step.node.getName().substring(0, 
                Math.min(15, step.node.getName().length()))).append("</td>");
            sb.append("<td>").append(String.format("%.3f", step.dpValue)).append("</td>");
            sb.append("<td>").append(step.prevNode != null ? 
                step.prevNode.getName().substring(0, Math.min(10, step.prevNode.getName().length())) : "-")
                .append("</td>");
            sb.append("</tr>\n");
        }
        
        sb.append("</table>\n");
        return sb.toString();
    }
    
    private String generateScript(Node source, List<Clue> clues, CDPResult result,
                                 List<DPStep> dpSteps, double centerLat, double centerLon) {
        StringBuilder js = new StringBuilder();
        js.append("<script>\n");
        
        // 初始化地圖
        js.append("var map = L.map('map').setView([").append(centerLat).append(",");
        js.append(centerLon).append("], 15);\n");
        js.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{");
        js.append("attribution:'© OpenStreetMap'}).addTo(map);\n\n");
        
        // 圖標函數
        js.append("""
function createIcon(color, size) {
    return L.divIcon({
        className: 'custom-icon',
        html: '<div style="background:'+color+';width:'+size+'px;height:'+size+'px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>',
        iconSize: [size, size], iconAnchor: [size/2, size/2]
    });
}
var dpMarkers = [];
var animIndex = 0;
var animInterval = null;

""");
        
        // 起點
        js.append("L.marker([").append(source.getLat()).append(",").append(source.getLon());
        js.append("],{icon:createIcon('#e74c3c',20)}).addTo(map).bindPopup('<b>起點</b><br>");
        js.append(escapeJs(source.getName())).append("');\n\n");
        
        // 完整實際路徑（綠色粗線）
        js.append("// 完整實際路徑 - 沿著真實道路網絡\n");
        js.append("var fullPathCoords = [\n");
        for (Node node : result.fullPath) {
            js.append("  [").append(node.getLat()).append(",").append(node.getLon()).append("],\n");
        }
        js.append("];\n");
        js.append("L.polyline(fullPathCoords,{color:'#27ae60',weight:5,opacity:0.8}).addTo(map);\n\n");
        
        // 關鍵節點路徑（藍色虛線，用於對比）
        js.append("// 關鍵節點連接（藍色虛線）\n");
        js.append("var keyPathCoords = [\n");
        for (Node node : result.path) {
            js.append("  [").append(node.getLat()).append(",").append(node.getLon()).append("],\n");
        }
        js.append("];\n");
        js.append("L.polyline(keyPathCoords,{color:'#3498db',weight:2,opacity:0.5,dashArray:'5,10'}).addTo(map);\n\n");
        
        // 最優路徑關鍵節點（較大的標記）
        for (int i = 1; i < result.path.size(); i++) {
            Node node = result.path.get(i);
            js.append("L.marker([").append(node.getLat()).append(",").append(node.getLon());
            js.append("],{icon:createIcon('#27ae60',16)}).addTo(map).bindPopup('<b>匹配 ").append(i);
            js.append("</b><br>").append(escapeJs(node.getName())).append("');\n");
        }
        
        // DP 步驟數據
        js.append("\nvar dpSteps = [\n");
        for (DPStep step : dpSteps) {
            js.append("  {lat:").append(step.node.getLat());
            js.append(",lon:").append(step.node.getLon());
            js.append(",name:'").append(escapeJs(step.node.getName()));
            js.append("',level:").append(step.level);
            js.append(",dpValue:").append(step.dpValue);
            js.append(",isOptimal:").append(step.isOptimal).append("},\n");
        }
        js.append("];\n\n");
        
        // 動畫函數
        js.append("""
function animateDP() {
    resetDP();
    animInterval = setInterval(function() {
        if (animIndex >= dpSteps.length) { clearInterval(animInterval); return; }
        var step = dpSteps[animIndex];
        var color = step.isOptimal ? '#27ae60' : '#f39c12';
        var marker = L.marker([step.lat, step.lon], {icon: createIcon(color, 12)}).addTo(map);
        marker.bindPopup('<b>Level '+step.level+'</b><br>'+step.name+'<br>D='+step.dpValue.toFixed(4));
        dpMarkers.push(marker);
        
        // 高亮表格行
        var rows = document.querySelectorAll('.dp-table tr');
        if (animIndex + 1 < rows.length) {
            rows[animIndex + 1].style.outline = '3px solid #e74c3c';
        }
        animIndex++;
    }, 300);
}

function resetDP() {
    if (animInterval) clearInterval(animInterval);
    dpMarkers.forEach(m => map.removeLayer(m));
    dpMarkers = [];
    animIndex = 0;
    document.querySelectorAll('.dp-table tr').forEach(r => r.style.outline = '');
}

function focusNode(lat, lon) {
    map.setView([lat, lon], 17);
    L.popup().setLatLng([lat, lon]).setContent('選中位置').openOn(map);
}
""");
        
        js.append("</script>\n");
        return js.toString();
    }
    
    private String escapeJs(String s) {
        return s.replace("'", "\\'").replace("\n", "\\n");
    }
}
