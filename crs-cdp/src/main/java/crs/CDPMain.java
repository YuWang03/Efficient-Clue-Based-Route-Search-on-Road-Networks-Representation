package crs;

import crs.model.*;
import crs.parser.*;
import crs.algorithm.*;
import crs.visualization.*;
import java.util.*;

/**
 * CRS Algorithm 2: CDP (Clue-Based Dynamic Programming)
 * 論文 Section 4 實現
 */
public class CDPMain {
    
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  Algorithm 2: CDP - Clue-Based Dynamic Programming       ║");
            System.out.println("║  動態規劃精確求解線索式路徑搜尋                          ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
            
            // 1. 解析 OSM
            String osmFile = args.length > 0 ? args[0] : "/mnt/user-data/uploads/map.osm";
            System.out.println("正在解析 OSM 檔案: " + osmFile);
            
            OSMParser parser = new OSMParser();
            RoadNetwork network = parser.parse(osmFile);
            
            System.out.println("\n路網統計:");
            System.out.println("  - 節點數: " + network.getNodeCount());
            System.out.println("  - 邊數: " + network.getEdgeCount());
            
            // 檢查網絡連通性
            int connectedNodes = 0;
            int isolatedNodes = 0;
            for (Node node : network.getAllNodes()) {
                if (network.getEdges(node.getId()).isEmpty()) {
                    isolatedNodes++;
                } else {
                    connectedNodes++;
                }
            }
            System.out.println("  - 連通節點: " + connectedNodes);
            if (isolatedNodes > 0) {
                System.out.println("  - ⚠️ 孤立節點: " + isolatedNodes + " (無邊連接)");
            }
            
            // 2. 運行網絡診斷
            crs.utils.NetworkDiagnostics diagnostics = new crs.utils.NetworkDiagnostics(network);
            diagnostics.printFullDiagnostics();
            
            // 3. 添加模擬 POI（只在最大連通分量中）
            Set<Node> largestComponent = diagnostics.getLargestComponent();
            System.out.println("\n最大連通分量: " + largestComponent.size() + " 個節點");
            addSimulatedPOIs(network, largestComponent);
            
            // 4. 選擇起點（必須在最大連通分量中）
            Node source = findStartNode(network, largestComponent);
            System.out.println("\n選擇起點: " + source.getName());
            System.out.println("  位置: (" + source.getLat() + ", " + source.getLon() + ")");
            System.out.println("  出邊數: " + network.getEdges(source.getId()).size());
            
            // 5. 建立線索（只在最大連通分量中選擇候選）
            List<Clue> clues = createClues(network, largestComponent);
            System.out.println("\n線索序列:");
            for (int i = 0; i < clues.size(); i++) {
                Clue c = clues.get(i);
                Set<Node> candidates = network.getNodeObjectsByKeyword(c.getKeyword());
                // 過濾出連通分量中的候選
                long connectedCount = candidates.stream()
                    .filter(largestComponent::contains)
                    .count();
                System.out.println("  " + (i+1) + ". " + c + " - 候選數: " + 
                                 connectedCount + "/" + candidates.size() + " (在連通分量中)");
            }
            
            // 6. 執行 CDP 算法
            CDPAlgorithm cdp = new CDPAlgorithm(network);
            CDPAlgorithm.CDPResult result = cdp.solve(source, clues);
            
            // 7. 驗證路徑
            System.out.println("\n正在驗證路徑...");
            PathValidator validator = new PathValidator(network);
            
            // 驗證關鍵節點路徑
            PathValidator.ValidationResult keyPathValidation = validator.validatePath(result.path);
            keyPathValidation.printReport();
            
            // 驗證完整實際路徑
            PathValidator.ValidationResult fullPathValidation = validator.validatePath(result.fullPath);
            fullPathValidation.printReport();
            
            // 比較兩條路徑
            PathValidator.PathComparison comparison = validator.comparePaths(result.path, result.fullPath);
            comparison.printReport();
            
            // 8. 生成可視化
            System.out.println("\n正在生成可視化...");
            CDPVisualizer visualizer = new CDPVisualizer();
            String outputPath = "..\\cdp_visualization.html";
            visualizer.generateVisualization(network, source, clues, result, 
                                            cdp.getDPSteps(), outputPath);
            
            System.out.println("\n════════════════════════════════════════");
            System.out.println("✓ 完成！最優匹配距離: " + String.format("%.4f", result.matchingDistance));
            System.out.println("✓ 完整實際路徑: " + result.fullPath.size() + " 個節點");
            System.out.println("✓ 路徑驗證: " + (fullPathValidation.isValid ? "沿著實際道路" : "警告：可能穿越建築物"));
            System.out.println("════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("Error loading OSM data: " + e.getMessage());
        }
    }
    
    private static void addSimulatedPOIs(@SuppressWarnings("unused") RoadNetwork network, Set<Node> validNodes) {
        String[] pois = {"restaurant", "cafe", "convenience", "bank", "pharmacy",
                        "school", "hospital", "park", "station", "shop"};
        Random rand = new Random(42);
        int count = 0;
        
        // 只為連通分量中的節點添加POI
        for (Node node : validNodes) {
            if (rand.nextDouble() < 0.3) {  // 提高POI密度
                node.addKeyword(pois[rand.nextInt(pois.length)]);
                count++;
            }
        }
        System.out.println("  - 添加了 " + count + " 個 POI（在連通分量中）");
    }
    
    private static Node findStartNode(RoadNetwork network, Set<Node> validNodes) {
        // 優先選擇度數較高的節點作為起點
        Node best = null;
        int maxDegree = 0;
        
        for (Node node : validNodes) {
            int degree = network.getEdges(node.getId()).size();
            if (degree > maxDegree) {
                maxDegree = degree;
                best = node;
            }
        }
        
        if (best != null) {
            return best;
        }
        
        // 備選：任意連通節點
        for (Node node : validNodes) {
            if (!network.getEdges(node.getId()).isEmpty()) {
                return node;
            }
        }
        
        return validNodes.iterator().next();
    }
    
    private static List<Clue> createClues(@SuppressWarnings("unused") RoadNetwork network, Set<Node> validNodes) {
        List<Clue> clues = new ArrayList<>();
        
        // 統計所有可用的關鍵字
        Map<String, Integer> keywordCount = new HashMap<>();
        for (Node node : validNodes) {
            for (String kw : node.getKeywords()) {
                keywordCount.put(kw, keywordCount.getOrDefault(kw, 0) + 1);
            }
        }
        
        // 選擇候選數量最多的前3個關鍵字
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(keywordCount.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());
        
        System.out.println("\n可用關鍵字（前10個）:");
        for (int i = 0; i < Math.min(10, sorted.size()); i++) {
            System.out.println("  " + (i+1) + ". " + sorted.get(i).getKey() + 
                             ": " + sorted.get(i).getValue() + " 個");
        }
        
        // 選擇在連通分量中有足夠候選的關鍵字
        for (Map.Entry<String, Integer> entry : sorted) {
            if (clues.size() >= 3) break;
            
            String keyword = entry.getKey();
            int candidateCount = entry.getValue();
            
            // 至少需要5個候選
            if (candidateCount < 5) continue;
            
            // 使用不同的距離和誤差
            int i = clues.size();
            double distance = 150 + i * 50;  // 150m, 200m, 250m
            double epsilon = 0.5 + i * 0.1;   // 0.5, 0.6, 0.7
            clues.add(new Clue(keyword, distance, epsilon));
        }
        
        if (clues.isEmpty()) {
            System.out.println("  警告: 沒有可用的關鍵字，使用默認線索");
            clues.add(new Clue("road", 200, 0.5));
        }
        
        return clues;
    }
}
