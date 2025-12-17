package crs;

import crs.model.*;
import crs.parser.*;
import crs.algorithm.*;
import crs.visualization.*;
import java.util.*;

/**
 * 簡化測試 - 使用OSM實際道路名稱
 */
public class SimpleTest {
    
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║     CDP 簡化測試 - 使用實際道路名稱      ║");
            System.out.println("╚══════════════════════════════════════════╝\n");
            
            // 1. 解析 OSM
            String osmFile = args.length > 0 ? args[0] : "c:\\Users\\user\\Desktop\\cdp_java_project\\map.osm";
            OSMParser parser = new OSMParser();
            RoadNetwork network = parser.parse(osmFile);
            
            System.out.println("\n網絡統計:");
            System.out.println("  節點: " + network.getNodeCount());
            System.out.println("  邊: " + network.getEdgeCount());
            
            // 2. 找最大連通分量
            crs.utils.NetworkDiagnostics diag = new crs.utils.NetworkDiagnostics(network);
            Set<Node> connected = diag.getLargestComponent();
            System.out.println("  最大連通分量: " + connected.size());
            
            // 3. 統計關鍵字（直接從network獲取）
            System.out.println("\n測試關鍵字索引:");
            Map<String, List<Node>> keywordNodes = new HashMap<>();
            for (Node node : connected) {
                for (String kw : node.getKeywords()) {
                    keywordNodes.computeIfAbsent(kw, k -> new ArrayList<>()).add(node);
                }
            }
            
            System.out.println("\n可用關鍵字（>= 5 個節點）:");
            List<Map.Entry<String, List<Node>>> sorted = new ArrayList<>(keywordNodes.entrySet());
            sorted.sort((a, b) -> b.getValue().size() - a.getValue().size());
            
            List<String> goodKeywords = new ArrayList<>();
            for (Map.Entry<String, List<Node>> entry : sorted) {
                if (entry.getValue().size() >= 5 && !entry.getKey().isEmpty()) {
                    String kw = entry.getKey();
                    int manualCount = entry.getValue().size();
                    int networkCount = network.getNodesByKeyword(kw).size();
                    
                    System.out.println("  - \"" + kw + "\": " + manualCount + 
                                     " (network查詢:" + networkCount + ")");
                    goodKeywords.add(kw);
                    if (goodKeywords.size() >= 10) break;
                }
            }
            
            if (goodKeywords.size() < 3) {
                System.out.println("\n❌ 錯誤: 沒有足夠的關鍵字（至少需要3個）");
                return;
            }
            
            // 4. 選擇起點（連通分量中度數最高的節點）
            Node source = null;
            int maxDegree = 0;
            for (Node node : connected) {
                int degree = network.getEdges(node.getId()).size();
                if (degree > maxDegree) {
                    maxDegree = degree;
                    source = node;
                }
            }
            
            System.out.println("\n起點: " + source.getName());
            System.out.println("  度數: " + maxDegree);
            
            // 5. 創建線索（使用crossing作為所有線索，測試連通性）
            List<Clue> clues = new ArrayList<>();
            clues.add(new Clue("crossing", 150, 0.9));
            clues.add(new Clue("crossing", 300, 0.9));
            clues.add(new Clue("crossing", 450, 0.9));
            
            System.out.println("\n線索:");
            for (int i = 0; i < clues.size(); i++) {
                Clue c = clues.get(i);
                int count = keywordNodes.get(c.getKeyword()).size();
                System.out.println("  " + (i+1) + ". " + c.getKeyword() + 
                                 " (~" + c.getDistance() + "m, ε=" + c.getEpsilon() + 
                                 ") - " + count + " 個候選");
            }
            
            // 6. 執行 CDP
            System.out.println("\n執行 CDP 算法...");
            CDPAlgorithm cdp = new CDPAlgorithm(network);
            CDPAlgorithm.CDPResult result = cdp.solve(source, clues);
            
            // 7. 顯示結果
            System.out.println("\n【結果】");
            if (result.matchingDistance < Double.MAX_VALUE) {
                System.out.println("✓ 找到路徑！");
                System.out.println("  匹配距離: " + String.format("%.4f", result.matchingDistance));
                System.out.println("  關鍵節點: " + result.path.size());
                System.out.println("  完整路徑: " + result.fullPath.size());
                
                System.out.println("\n路徑:");
                for (int i = 0; i < result.path.size(); i++) {
                    System.out.println("  " + i + ". " + result.path.get(i).getName());
                }
                
                // 生成可視化
                CDPVisualizer vis = new CDPVisualizer();
                vis.generateVisualization(network, source, clues, result, 
                                        cdp.getDPSteps(), "c:\\Users\\user\\Desktop\\cdp_java_project\\cdp_visualization.html");
                
                System.out.println("\n✓ 完成！可視化已生成");
            } else {
                System.out.println("✗ 沒有找到可行路徑");
            }
            
        } catch (Exception e) {
            System.err.println("\n錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
