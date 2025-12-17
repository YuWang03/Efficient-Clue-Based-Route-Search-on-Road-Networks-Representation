package crs.visualization;

import crs.model.*;
import crs.algorithm.*;
import java.io.*;
import java.util.*;

/**
 * HTML 可視化生成器
 * 使用 Leaflet.js 生成互動式地圖
 */
public class HtmlVisualizer {
    
    /**
     * 生成完整的可視化 HTML
     */
    public void generateVisualization(
            RoadNetwork network,
            Node source,
            List<Clue> clues,
            GreedyClueSearch.FeasiblePath result,
            List<FindNextMinAlgorithm.TraversalStep> traversalHistory,
            String outputPath) throws IOException {
        
        StringBuilder html = new StringBuilder();
        
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
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-TW\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>CRS Algorithm Visualization - 線索式路徑搜尋可視化</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.css\" />\n");
        html.append("    <script src=\"https://unpkg.com/leaflet@1.9.4/dist/leaflet.js\"></script>\n");
        html.append("    <style>\n");
        html.append(getCSS());
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 標題和控制面板
        html.append("    <div class=\"header\">\n");
        html.append("        <h1>🗺️ CRS Algorithm - 線索式路徑搜尋可視化</h1>\n");
        html.append("        <p>Algorithm 1: findNextMin() 網路遍歷與路徑搜尋</p>\n");
        html.append("    </div>\n");
        
        // 主容器
        html.append("    <div class=\"container\">\n");
        
        // 左側面板
        html.append("        <div class=\"left-panel\">\n");
        html.append(generateInfoPanel(source, clues, result));
        html.append(generateLegend());
        html.append(generateTraversalPanel(traversalHistory));
        html.append("        </div>\n");
        
        // 地圖容器
        html.append("        <div id=\"map\"></div>\n");
        
        html.append("    </div>\n");
        
        // JavaScript
        html.append("    <script>\n");
        html.append(generateJavaScript(network, source, clues, result, traversalHistory, centerLat, centerLon));
        html.append("    </script>\n");
        
        html.append("</body>\n");
        html.append("</html>\n");
        
        // 寫入檔案
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.print(html.toString());
        }
        
        System.out.println("可視化 HTML 已生成: " + outputPath);
    }
    
    private String getCSS() {
        return """
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { 
                font-family: 'Segoe UI', 'Microsoft JhengHei', Arial, sans-serif;
                background: #f5f6fa;
            }
            .header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 20px;
                text-align: center;
                box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            }
            .header h1 { 
                font-size: 1.8em; 
                margin-bottom: 8px;
                text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
            }
            .header p { 
                font-size: 1em; 
                opacity: 0.95;
                font-weight: 300;
            }
            .container { 
                display: flex; 
                height: calc(100vh - 100px);
                gap: 0;
            }
            .left-panel {
                width: 380px;
                background: #ffffff;
                overflow-y: auto;
                border-right: 1px solid #e1e8ed;
                box-shadow: 2px 0 10px rgba(0,0,0,0.05);
            }
            .left-panel::-webkit-scrollbar { width: 8px; }
            .left-panel::-webkit-scrollbar-track { background: #f1f1f1; }
            .left-panel::-webkit-scrollbar-thumb { 
                background: #888; 
                border-radius: 4px;
            }
            .left-panel::-webkit-scrollbar-thumb:hover { background: #555; }
            
            #map { 
                flex: 1;
                position: relative;
            }
            .map-controls {
                position: absolute;
                top: 10px;
                right: 10px;
                z-index: 1000;
                background: white;
                padding: 10px;
                border-radius: 8px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            }
            .panel {
                background: white;
                margin: 15px;
                padding: 18px;
                border-radius: 12px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.08);
                transition: transform 0.2s, box-shadow 0.2s;
            }
            .panel:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0,0,0,0.12);
            }
            .panel h3 {
                color: #2c3e50;
                border-bottom: 3px solid #667eea;
                padding-bottom: 10px;
                margin-bottom: 15px;
                font-size: 1.1em;
                display: flex;
                align-items: center;
                gap: 8px;
            }
            .info-item {
                display: flex;
                justify-content: space-between;
                padding: 10px 0;
                border-bottom: 1px solid #f0f0f0;
                transition: background 0.2s;
            }
            .info-item:hover { background: #f8f9fa; }
            .info-item:last-child { border-bottom: none; }
            .info-label { 
                color: #666;
                font-size: 0.95em;
            }
            .info-value { 
                font-weight: 600; 
                color: #2c3e50;
                font-size: 1em;
            }
            .clue-item {
                background: linear-gradient(135deg, #e8f4f8 0%, #f0f7fa 100%);
                padding: 12px 14px;
                margin: 10px 0;
                border-radius: 8px;
                border-left: 5px solid #3498db;
                transition: all 0.3s;
                cursor: pointer;
            }
            .clue-item:hover {
                transform: translateX(5px);
                box-shadow: 0 3px 10px rgba(0,0,0,0.1);
            }
            .clue-keyword { 
                font-weight: bold; 
                color: #2980b9;
                font-size: 1.1em;
                display: inline-block;
                margin-bottom: 5px;
            }
            .match-success { 
                border-left-color: #27ae60; 
                background: linear-gradient(135deg, #e8f8f0 0%, #f0faf5 100%);
            }
            .match-fail { 
                border-left-color: #e74c3c; 
                background: linear-gradient(135deg, #fdf2f2 0%, #fef5f5 100%);
            }
            .legend-item {
                display: flex;
                align-items: center;
                margin: 10px 0;
                padding: 5px;
                border-radius: 6px;
                transition: background 0.2s;
            }
            .legend-item:hover { background: #f8f9fa; }
            .legend-color {
                width: 24px;
                height: 24px;
                border-radius: 50%;
                margin-right: 12px;
                border: 3px solid #fff;
                box-shadow: 0 2px 5px rgba(0,0,0,0.3);
            }
            .traversal-step {
                padding: 10px 12px;
                margin: 6px 0;
                border-radius: 8px;
                font-size: 0.9em;
                cursor: pointer;
                transition: all 0.2s;
                border-left: 3px solid transparent;
            }
            .traversal-step:hover { 
                background: #e8e8e8;
                transform: translateX(3px);
                border-left-color: #667eea;
            }
            .step-visited { 
                background: #fff3cd;
                border-left-color: #ffc107;
            }
            .step-candidate { 
                background: #d4edda;
                border-left-color: #28a745;
            }
            .step-selected { 
                background: #cfe2ff;
                border: 2px solid #0d6efd;
                font-weight: 600;
            }
            .btn {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                border: none;
                padding: 10px 20px;
                border-radius: 6px;
                cursor: pointer;
                margin: 5px 3px;
                font-size: 0.95em;
                font-weight: 500;
                transition: all 0.3s;
                box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            }
            .btn:hover { 
                transform: translateY(-2px);
                box-shadow: 0 4px 8px rgba(0,0,0,0.3);
            }
            .btn:active { transform: translateY(0); }
            .btn-play { 
                background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
            }
            .btn-pause {
                background: linear-gradient(135deg, #f2994a 0%, #f2c94c 100%);
            }
            .btn-stop {
                background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
            }
            .speed-control {
                margin: 10px 0;
                padding: 10px;
                background: #f8f9fa;
                border-radius: 6px;
            }
            .speed-control label {
                display: block;
                margin-bottom: 5px;
                font-size: 0.9em;
                color: #666;
            }
            .speed-control input[type="range"] {
                width: 100%;
            }
            .stats-grid {
                display: grid;
                grid-template-columns: 1fr 1fr;
                gap: 10px;
                margin-top: 10px;
            }
            .stat-box {
                background: #f8f9fa;
                padding: 12px;
                border-radius: 8px;
                text-align: center;
            }
            .stat-box .value {
                font-size: 1.5em;
                font-weight: bold;
                color: #667eea;
            }
            .stat-box .label {
                font-size: 0.85em;
                color: #666;
                margin-top: 5px;
            }
            .progress-bar {
                width: 100%;
                height: 6px;
                background: #e0e0e0;
                border-radius: 3px;
                margin: 10px 0;
                overflow: hidden;
            }
            .progress-bar-fill {
                height: 100%;
                background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
                width: 0%;
                transition: width 0.3s;
            }
            """;
    }
    
    private String generateInfoPanel(Node source, List<Clue> clues, GreedyClueSearch.FeasiblePath result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>📍 查詢資訊</h3>\n");
        
        sb.append("    <div class=\"info-item\">\n");
        sb.append("        <span class=\"info-label\">起點</span>\n");
        sb.append("        <span class=\"info-value\">").append(source.getName()).append("</span>\n");
        sb.append("    </div>\n");
        
        sb.append("    <div class=\"info-item\">\n");
        sb.append("        <span class=\"info-label\">線索數量</span>\n");
        sb.append("        <span class=\"info-value\">").append(clues.size()).append("</span>\n");
        sb.append("    </div>\n");
        
        sb.append("    <div class=\"info-item\">\n");
        sb.append("        <span class=\"info-label\">最大匹配距離</span>\n");
        sb.append("        <span class=\"info-value\">");
        
        // 修正：計算已成功匹配的線索中的最大距離
        double maxValidDistance = -1;
        for (SearchResult match : result.getMatches()) {
            if (match.isValid()) {
                maxValidDistance = Math.max(maxValidDistance, match.getMatchingDistance());
            }
        }
        
        if (maxValidDistance < 0) {
            sb.append("N/A (無有效匹配)");
        } else {
            sb.append(String.format("%.4f", maxValidDistance));
        }
        
        sb.append("</span>\n");
        sb.append("    </div>\n");
        
        sb.append("    <h4 style=\"margin-top:15px;\">線索與匹配結果</h4>\n");
        
        List<SearchResult> matches = result.getMatches();
        for (int i = 0; i < clues.size(); i++) {
            Clue clue = clues.get(i);
            SearchResult match = i < matches.size() ? matches.get(i) : null;
            boolean success = match != null && match.isValid();
            
            // 檢查是否是正確的匹配（有對應關鍵字）
            boolean hasCorrectKeyword = false;
            if (success) {
                Node matchNode = match.getMatchVertex();
                String targetKeyword = clue.getKeyword().toLowerCase();
                hasCorrectKeyword = matchNode.getKeywords().stream()
                    .anyMatch(k -> k.toLowerCase().contains(targetKeyword) || 
                                  targetKeyword.contains(k.toLowerCase()));
            }
            
            String cssClass = !success ? "match-fail" : (hasCorrectKeyword ? "match-success" : "match-fail");
            String emoji = !success ? "❌" : (hasCorrectKeyword ? "✅" : "⚠️");
            
            sb.append("    <div class=\"clue-item ").append(cssClass)
              .append("\" onclick=\"focusOnClue(").append(i).append(")\" title=\"點擊查看詳情\">\n");
            sb.append("        <div style=\"display:flex; justify-content:space-between; align-items:center;\">\n");
            sb.append("            <span class=\"clue-keyword\">").append(i + 1).append(". ").append(clue.getKeyword()).append("</span>\n");
            sb.append("            <span style=\"font-size:1.2em;\">").append(emoji).append("</span>\n");
            sb.append("        </div>\n");
            sb.append("        <div style=\"font-size:0.9em; margin:5px 0; color:#666;\">\n");
            sb.append("            目標: ").append(String.format("%.0f", clue.getDistance())).append("m ± ");
            sb.append(String.format("%.0f", clue.getDistance() * clue.getEpsilon())).append("m</div>\n");
            if (success) {
                Node matchNode = match.getMatchVertex();
                String color = hasCorrectKeyword ? "#27ae60" : "#e74c3c";
                String prefix = hasCorrectKeyword ? "✓" : "⚠";
                
                sb.append("        <div style=\"font-size:0.95em; color:").append(color).append("; font-weight:500;\">\n");
                sb.append("            ").append(prefix).append(" ").append(matchNode.getName()).append("</div>\n");
                sb.append("        <div style=\"font-size:0.85em; color:#666; margin:3px 0;\">\n");
                sb.append("            ID: ").append(matchNode.getId()).append("</div>\n");
                
                String keywordsDisplay = matchNode.getKeywords().isEmpty() ? 
                    "<span style='color:#e74c3c;'>❌ 無關鍵字</span>" : 
                    matchNode.getKeywords().toString();
                if (!hasCorrectKeyword && !matchNode.getKeywords().isEmpty()) {
                    keywordsDisplay = "<span style='color:#ff9800;'>" + keywordsDisplay + "</span>";
                }
                
                sb.append("        <div style=\"font-size:0.85em; color:#666; margin:3px 0;\">\n");
                sb.append("            關鍵字: ").append(keywordsDisplay).append("</div>\n");
                
                if (!hasCorrectKeyword) {
                    sb.append("        <div style=\"font-size:0.8em; color:#e74c3c; margin:3px 0; font-weight:500;\">\n");
                    sb.append("            ⚠️ 不包含 \"").append(clue.getKeyword()).append("\"</div>\n");
                }
                
                sb.append("        <div style=\"display:flex; justify-content:space-between; font-size:0.85em; margin-top:5px;\">\n");
                sb.append("            <span>距離: ").append(String.format("%.1f", match.getNetworkDistance())).append("m</span>\n");
                sb.append("            <span>dm: ").append(String.format("%.3f", match.getMatchingDistance())).append("</span>\n");
                sb.append("        </div>\n");
            } else {
                sb.append("        <div style=\"color:#e74c3c; font-weight:500;\">✗ 未找到匹配</div>\n");
            }
            sb.append("    </div>\n");
        }
        
        sb.append("</div>\n");
        return sb.toString();
    }
    
    private String generateLegend() {
        return """
            <div class="panel">
                <h3>🎨 圖例</h3>
                <div class="legend-item">
                    <div class="legend-color" style="background:#e74c3c;"></div>
                    <span>起點</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background:#27ae60;"></div>
                    <span>✓ 正確匹配（有關鍵字）</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background:#ff6b6b;"></div>
                    <span>✗ 錯誤匹配（無關鍵字）</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background:#f39c12;"></div>
                    <span>候選節點（遍歷時）</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background:#95a5a6;"></div>
                    <span>已遍歷節點</span>
                </div>
                <div class="legend-item">
                    <div class="legend-color" style="background:#3498db;width:30px;height:4px;border-radius:2px;"></div>
                    <span>最終路徑</span>
                </div>
            </div>
            """;
    }
    
    private String generateTraversalPanel(List<FindNextMinAlgorithm.TraversalStep> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"panel\">\n");
        sb.append("    <h3>🔍 遍歷過程</h3>\n");
        sb.append("    <div style=\"display:flex; gap:5px; flex-wrap:wrap;\">\n");
        sb.append("        <button class=\"btn btn-play\" onclick=\"playAnimation()\" id=\"playBtn\">▶ 播放</button>\n");
        sb.append("        <button class=\"btn btn-pause\" onclick=\"pauseAnimation()\" id=\"pauseBtn\" style=\"display:none;\">⏸ 暫停</button>\n");
        sb.append("        <button class=\"btn btn-stop\" onclick=\"resetAnimation()\">⏹ 重置</button>\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"speed-control\">\n");
        sb.append("        <label>動畫速度: <span id=\"speedValue\">100ms</span></label>\n");
        sb.append("        <input type=\"range\" id=\"speedSlider\" min=\"10\" max=\"500\" value=\"100\" oninput=\"updateSpeed(this.value)\">\n");
        sb.append("    </div>\n");
        sb.append("    <div class=\"progress-bar\">\n");
        sb.append("        <div class=\"progress-bar-fill\" id=\"progressBar\"></div>\n");
        sb.append("    </div>\n");
        sb.append("    <div style=\"text-align:center; margin:5px 0; font-size:0.9em; color:#666;\" id=\"stepCounter\">步驟 0 / ");
        sb.append(history.size()).append("</div>\n");
        sb.append("    <div id=\"traversal-list\" style=\"max-height:300px;overflow-y:auto;margin-top:10px;\">\n");
        
        int displayCount = Math.min(history.size(), 50);
        for (int i = 0; i < displayCount; i++) {
            FindNextMinAlgorithm.TraversalStep step = history.get(i);
            String stepClass = step.isSelected ? "step-selected" : 
                              (step.isCandidate ? "step-candidate" : "step-visited");
            
            sb.append("        <div class=\"traversal-step ").append(stepClass)
              .append("\" onclick=\"highlightStep(").append(i).append(")\">\n");
            sb.append("            <strong>#").append(i + 1).append("</strong> ");
            sb.append(step.node.getName().substring(0, Math.min(15, step.node.getName().length())));
            sb.append(" (").append(String.format("%.1f", step.distance)).append("m)\n");
            sb.append("            <div style=\"font-size:0.8em;color:#666;\">").append(step.reason).append("</div>\n");
            sb.append("        </div>\n");
        }
        
        if (history.size() > 50) {
            sb.append("        <div style=\"padding:10px;color:#666;\">... 還有 ")
              .append(history.size() - 50).append(" 個步驟</div>\n");
        }
        
        sb.append("    </div>\n");
        sb.append("</div>\n");
        return sb.toString();
    }
    
    private String generateJavaScript(
            RoadNetwork network,
            Node source,
            List<Clue> clues,
            GreedyClueSearch.FeasiblePath result,
            List<FindNextMinAlgorithm.TraversalStep> history,
            double centerLat, double centerLon) {
        
        StringBuilder js = new StringBuilder();
        
        // 初始化地圖
        js.append("var map = L.map('map').setView([").append(centerLat).append(", ")
          .append(centerLon).append("], 16);\n");
        js.append("L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n");
        js.append("    attribution: '© OpenStreetMap contributors'\n");
        js.append("}).addTo(map);\n\n");
        
        // 定義標記樣式
        js.append("""
            var icons = {
                source: L.divIcon({className: 'custom-icon', html: '<div style="background:#e74c3c;width:20px;height:20px;border-radius:50%;border:3px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>'}),
                match: L.divIcon({className: 'custom-icon', html: '<div style="background:#27ae60;width:16px;height:16px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"></div>'}),
                wrongMatch: L.divIcon({className: 'custom-icon', html: '<div style="background:#ff6b6b;width:16px;height:16px;border-radius:50%;border:2px solid white;box-shadow:0 2px 5px rgba(0,0,0,0.3);"><div style="color:white;font-size:10px;text-align:center;line-height:16px;">✗</div></div>'}),
                candidate: L.divIcon({className: 'custom-icon', html: '<div style="background:#f39c12;width:12px;height:12px;border-radius:50%;border:2px solid white;"></div>'}),
                visited: L.divIcon({className: 'custom-icon', html: '<div style="background:#95a5a6;width:8px;height:8px;border-radius:50%;"></div>'})
            };
            var markers = [];
            var pathLine = null;
            var animationIndex = 0;
            var animationInterval = null;
            
            """);
        
        // 添加起點
        js.append("// 起點\n");
        String sourceKeywords = source.getKeywords().isEmpty() ? "無" : escapeJs(source.getKeywords().toString());
        js.append("L.marker([").append(source.getLat()).append(", ").append(source.getLon())
          .append("], {icon: icons.source})\n");
        js.append("    .addTo(map)\n");
        js.append("    .bindPopup('<div style=\"min-width:200px;\">");
        js.append("<b style=\"font-size:1.1em;\">🎯 起點</b>");
        js.append("<hr style=\"margin:8px 0; border:none; border-top:1px solid #ddd;\">");
        js.append("<div style=\"margin:5px 0;\"><b>節點名稱:</b> ").append(escapeJs(source.getName())).append("</div>");
        js.append("<div style=\"margin:5px 0;\"><b>節點 ID:</b> ").append(source.getId()).append("</div>");
        js.append("<div style=\"margin:5px 0;\"><b>座標:</b> (").append(String.format("%.6f", source.getLat()))
          .append(", ").append(String.format("%.6f", source.getLon())).append(")</div>");
        js.append("<div style=\"margin:5px 0;\"><b>關鍵字:</b> ").append(sourceKeywords).append("</div>");
        js.append("</div>');\n\n");
        
        // 添加匹配節點（區分正確和錯誤的匹配）
        js.append("// 匹配節點\n");
        int matchIndex = 0;
        for (SearchResult match : result.getMatches()) {
            if (match.isValid()) {
                matchIndex++;
                Node node = match.getMatchVertex();
                
                // 檢查節點是否有與線索關鍵字匹配的關鍵字
                String targetKeyword = match.getClue().getKeyword().toLowerCase();
                boolean hasMatchingKeyword = node.getKeywords().stream()
                    .anyMatch(k -> k.toLowerCase().contains(targetKeyword) || 
                                  targetKeyword.contains(k.toLowerCase()));
                
                // 使用不同的圖標：綠色表示正確，紅色表示錯誤
                String iconType = hasMatchingKeyword ? "icons.match" : "icons.wrongMatch";
                String statusEmoji = hasMatchingKeyword ? "✅" : "⚠️";
                String statusColor = hasMatchingKeyword ? "#27ae60" : "#e74c3c";
                
                String keywordsStr = node.getKeywords().isEmpty() ? 
                    "<span style=\\'color:#e74c3c;\\'>❌ 無關鍵字（錯誤匹配！）</span>" : 
                    escapeJs(node.getKeywords().toString());
                
                if (!hasMatchingKeyword && !node.getKeywords().isEmpty()) {
                    keywordsStr = "<span style=\\'color:#ff9800;\\'>" + keywordsStr + "</span><br>" +
                                "<span style=\\'color:#e74c3c;\\'>⚠️ 不包含 \"" + escapeJs(targetKeyword) + "\"</span>";
                }
                
                js.append("L.marker([").append(node.getLat()).append(", ").append(node.getLon())
                  .append("], {icon: ").append(iconType).append("})\n");
                js.append("    .addTo(map)\n");
                js.append("    .bindPopup('<div style=\"min-width:250px;\">");
                js.append("<b style=\"font-size:1.1em; color:").append(statusColor).append(";\">")
                  .append(statusEmoji).append(" 匹配 #").append(matchIndex).append(": ")
                  .append(escapeJs(match.getClue().getKeyword())).append("</b>");
                
                if (!hasMatchingKeyword) {
                    js.append("<div style=\"background:#fff3cd; padding:5px; margin:5px 0; border-left:3px solid #ffc107;\">");
                    js.append("<b style=\"color:#856404;\">⚠️ 警告：此節點可能不是正確的匹配！</b></div>");
                }
                
                js.append("<hr style=\"margin:8px 0; border:none; border-top:1px solid #ddd;\">");
                js.append("<div style=\"margin:5px 0;\"><b>節點名稱:</b> ").append(escapeJs(node.getName())).append("</div>");
                js.append("<div style=\"margin:5px 0;\"><b>節點 ID:</b> ").append(node.getId()).append("</div>");
                js.append("<div style=\"margin:5px 0;\"><b>座標:</b> (").append(String.format("%.6f", node.getLat()))
                  .append(", ").append(String.format("%.6f", node.getLon())).append(")</div>");
                js.append("<div style=\"margin:5px 0;\"><b>關鍵字:</b> ").append(keywordsStr).append("</div>");
                js.append("<hr style=\"margin:8px 0; border:none; border-top:1px solid #ddd;\">");
                js.append("<div style=\"margin:5px 0;\"><b>網路距離:</b> ").append(String.format("%.1f", match.getNetworkDistance())).append("m</div>");
                js.append("<div style=\"margin:5px 0;\"><b>匹配距離 dm:</b> ").append(String.format("%.4f", match.getMatchingDistance())).append("</div>");
                js.append("<div style=\"margin:5px 0;\"><b>預期距離:</b> ").append(String.format("%.0f", match.getClue().getDistance()))
                  .append("m ± ").append(String.format("%.0f", match.getClue().getDistance() * match.getClue().getEpsilon())).append("m</div>");
                js.append("</div>');\n");
            }
        }
        
        // 添加匹配節點座標資料
        js.append("\n// 匹配節點座標資料\n");
        js.append("var clueMatches = [\n");
        for (SearchResult match : result.getMatches()) {
            if (match.isValid()) {
                Node node = match.getMatchVertex();
                js.append("    {lat: ").append(node.getLat())
                  .append(", lon: ").append(node.getLon())
                  .append(", name: '").append(escapeJs(node.getName()))
                  .append("', keyword: '").append(escapeJs(match.getClue().getKeyword()))
                  .append("', keywords: '").append(escapeJs(node.getKeywords().toString()))
                  .append("'},\n");
            } else {
                js.append("    null,\n");
            }
        }
        js.append("];\n\n");
        
        // 添加遍歷步驟資料
        js.append("// 遍歷步驟資料\n");
        js.append("var traversalSteps = [\n");
        for (FindNextMinAlgorithm.TraversalStep step : history) {
            js.append("    {lat: ").append(step.node.getLat())
              .append(", lon: ").append(step.node.getLon())
              .append(", name: '").append(escapeJs(step.node.getName()))
              .append("', distance: ").append(step.distance)
              .append(", isCandidate: ").append(step.isCandidate)
              .append(", isSelected: ").append(step.isSelected)
              .append("},\n");
        }
        js.append("];\n\n");
        
        // 路徑作為背景運算保留，不在地圖上繪製
        js.append("// 路徑計算完成，但不在地圖上顯示（作為背景運算）\n\n");
        
        // 動畫函數
        js.append("""
            var animationSpeed = 100;
            var isPaused = false;
            
            function playAnimation() {
                document.getElementById('playBtn').style.display = 'none';
                document.getElementById('pauseBtn').style.display = 'inline-block';
                
                if (isPaused) {
                    isPaused = false;
                    animateStep();
                    return;
                }
                
                resetAnimation();
                animationIndex = 0;
                animateStep();
            }
            
            function animateStep() {
                if (animationIndex >= traversalSteps.length || isPaused) {
                    if (animationIndex >= traversalSteps.length) {
                        document.getElementById('playBtn').style.display = 'inline-block';
                        document.getElementById('pauseBtn').style.display = 'none';
                    }
                    return;
                }
                
                var step = traversalSteps[animationIndex];
                var icon = step.isSelected ? icons.match : (step.isCandidate ? icons.candidate : icons.visited);
                var marker = L.marker([step.lat, step.lon], {icon: icon}).addTo(map);
                marker.bindPopup('<b>' + step.name + '</b><br>距離: ' + step.distance.toFixed(1) + 'm');
                markers.push(marker);
                
                animationIndex++;
                var progress = (animationIndex / traversalSteps.length) * 100;
                document.getElementById('progressBar').style.width = progress + '%';
                document.getElementById('stepCounter').textContent = '步驟 ' + animationIndex + ' / ' + traversalSteps.length;
                
                animationInterval = setTimeout(animateStep, animationSpeed);
            }
            
            function pauseAnimation() {
                isPaused = true;
                document.getElementById('playBtn').style.display = 'inline-block';
                document.getElementById('pauseBtn').style.display = 'none';
                if (animationInterval) clearTimeout(animationInterval);
            }
            
            function resetAnimation() {
                isPaused = false;
                if (animationInterval) clearTimeout(animationInterval);
                markers.forEach(function(m) { map.removeLayer(m); });
                markers = [];
                animationIndex = 0;
                document.getElementById('progressBar').style.width = '0%';
                document.getElementById('stepCounter').textContent = '步驟 0 / ' + traversalSteps.length;
                document.getElementById('playBtn').style.display = 'inline-block';
                document.getElementById('pauseBtn').style.display = 'none';
            }
            
            function updateSpeed(value) {
                animationSpeed = parseInt(value);
                document.getElementById('speedValue').textContent = value + 'ms';
            }
            
            function highlightStep(index) {
                if (index >= traversalSteps.length) return;
                var step = traversalSteps[index];
                map.setView([step.lat, step.lon], 18);
                var statusIcon = step.isSelected ? '✓' : (step.isCandidate ? '⭐' : '•');
                L.popup()
                    .setLatLng([step.lat, step.lon])
                    .setContent('<b>' + statusIcon + ' ' + step.name + '</b><br>距離: ' + step.distance.toFixed(1) + 'm<br>位置: (' + step.lat.toFixed(6) + ', ' + step.lon.toFixed(6) + ')')
                    .openOn(map);
            }
            
            function focusOnClue(index) {
                if (index >= clueMatches.length || !clueMatches[index]) {
                    alert('線索 #' + (index + 1) + ' 沒有匹配結果');
                    return;
                }
                var match = clueMatches[index];
                map.setView([match.lat, match.lon], 18);
                
                // 顯示詳細信息
                var hasKeywords = match.keywords && match.keywords !== '[]' && match.keywords !== '';
                var keywordWarning = hasKeywords ? '' : '<br><span style=\"color:#e74c3c;\">⚠️ 此節點無關鍵字，可能是錯誤匹配！</span>';
                
                L.popup()
                    .setLatLng([match.lat, match.lon])
                    .setContent('<div style=\"min-width:200px;\"><b style=\"font-size:1.1em;\">線索 #' + (index + 1) + ': ' + match.keyword + '</b><hr style=\"margin:5px 0;\">' +
                               '<b>節點:</b> ' + match.name + '<br>' +
                               '<b>關鍵字:</b> ' + (hasKeywords ? match.keywords : '<span style=\"color:#999;\">無</span>') +
                               keywordWarning + '</div>')
                    .openOn(map);
            }
            """);
        
        return js.toString();
    }
    
    private String escapeJs(String s) {
        return s.replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }
}
