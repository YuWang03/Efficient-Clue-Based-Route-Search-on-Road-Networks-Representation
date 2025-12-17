package crs;

import crs.model.*;
import crs.parser.*;
import crs.utils.*;
import java.util.*;

/**
 * 快速診斷工具 - 檢查路徑穿越問題
 */
public class QuickDiagnostics {
    
    public static void main(String[] args) {
        try {
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║     CDP 路徑穿越問題快速診斷工具               ║");
            System.out.println("╚════════════════════════════════════════════════╝\n");
            
            // 1. 解析 OSM
            String osmFile = args.length > 0 ? args[0] : "map.osm";
            System.out.println("📂 解析 OSM 文件: " + osmFile);
            
            OSMParser parser = new OSMParser();
            RoadNetwork network = parser.parse(osmFile);
            
            // 2. 基本統計
            System.out.println("\n【基本統計】");
            System.out.println("  節點總數: " + network.getNodeCount());
            System.out.println("  邊總數: " + network.getEdgeCount());
            
            if (network.getNodeCount() == 0) {
                System.err.println("\n❌ 錯誤: 沒有節點！OSM 文件可能為空或格式不正確");
                return;
            }
            
            if (network.getEdgeCount() == 0) {
                System.err.println("\n❌ 錯誤: 沒有邊！路網沒有連接");
                return;
            }
            
            // 3. 完整診斷
            NetworkDiagnostics diagnostics = new NetworkDiagnostics(network);
            diagnostics.printFullDiagnostics();
            
            // 4. 檢查隨機節點對的連通性
            System.out.println("\n【隨機連通性測試】");
            List<Node> nodeList = new ArrayList<>(network.getAllNodes());
            Random rand = new Random(42);
            
            int testCount = Math.min(10, nodeList.size() / 2);
            int connectedCount = 0;
            
            for (int i = 0; i < testCount; i++) {
                Node n1 = nodeList.get(rand.nextInt(nodeList.size()));
                Node n2 = nodeList.get(rand.nextInt(nodeList.size()));
                
                if (n1.getId() == n2.getId()) continue;
                
                boolean connected = diagnostics.areConnected(n1, n2);
                if (connected) connectedCount++;
                
                System.out.println("  測試 " + (i+1) + ": " + 
                    n1.getName() + " → " + n2.getName() + 
                    " : " + (connected ? "✓ 連通" : "✗ 不連通"));
            }
            
            System.out.println("\n連通率: " + connectedCount + "/" + testCount + 
                             " (" + String.format("%.1f", connectedCount * 100.0 / testCount) + "%)");
            
            // 5. 檢查最大連通分量
            Set<Node> largestComponent = diagnostics.getLargestComponent();
            System.out.println("\n【最大連通分量】");
            System.out.println("  大小: " + largestComponent.size() + " 個節點");
            System.out.println("  比例: " + String.format("%.1f", 
                largestComponent.size() * 100.0 / network.getNodeCount()) + "%");
            
            if (largestComponent.size() < network.getNodeCount() * 0.8) {
                System.out.println("  ⚠️ 警告: 最大連通分量 < 80%，網絡高度不連通！");
            }
            
            // 6. 邊長度分析
            System.out.println("\n【邊長度分析】");
            List<Double> edgeLengths = new ArrayList<>();
            
            for (Node node : network.getAllNodes()) {
                for (Edge edge : network.getEdges(node.getId())) {
                    edgeLengths.add(edge.getWeight());
                }
            }
            
            if (!edgeLengths.isEmpty()) {
                Collections.sort(edgeLengths);
                double min = edgeLengths.get(0);
                double max = edgeLengths.get(edgeLengths.size() - 1);
                double avg = edgeLengths.stream().mapToDouble(d -> d).average().orElse(0);
                double median = edgeLengths.get(edgeLengths.size() / 2);
                
                System.out.println("  最短邊: " + String.format("%.2f", min) + "m");
                System.out.println("  最長邊: " + String.format("%.2f", max) + "m");
                System.out.println("  平均長度: " + String.format("%.2f", avg) + "m");
                System.out.println("  中位數: " + String.format("%.2f", median) + "m");
                
                if (max > 500) {
                    System.out.println("  ⚠️ 警告: 存在超長邊 (>" + String.format("%.0f", max) + "m)");
                    System.out.println("     這可能導致看起來穿越建築物");
                }
            }
            
            // 7. 節點密度分析
            System.out.println("\n【節點密度分析】");
            double totalArea = calculateBoundingBoxArea(nodeList);
            double density = network.getNodeCount() / totalArea;
            
            System.out.println("  覆蓋區域: " + String.format("%.6f", totalArea) + " km²");
            System.out.println("  節點密度: " + String.format("%.1f", density * 1000000) + " 個/km²");
            
            if (density * 1000000 < 100) {
                System.out.println("  ⚠️ 警告: 節點密度太低，道路網絡可能不完整");
            }
            
            // 8. POI 關鍵字統計
            System.out.println("\n【POI 關鍵字統計】");
            Map<String, Integer> keywordCount = new HashMap<>();
            
            for (Node node : network.getAllNodes()) {
                for (String kw : node.getKeywords()) {
                    keywordCount.put(kw, keywordCount.getOrDefault(kw, 0) + 1);
                }
            }
            
            if (keywordCount.isEmpty()) {
                System.out.println("  ⚠️ 警告: 沒有任何 POI 關鍵字！");
            } else {
                System.out.println("  關鍵字種類: " + keywordCount.size());
                List<Map.Entry<String, Integer>> sorted = new ArrayList<>(keywordCount.entrySet());
                sorted.sort((a, b) -> b.getValue() - a.getValue());
                
                int show = Math.min(10, sorted.size());
                System.out.println("  前 " + show + " 個關鍵字:");
                for (int i = 0; i < show; i++) {
                    Map.Entry<String, Integer> entry = sorted.get(i);
                    System.out.println("    " + (i+1) + ". " + entry.getKey() + ": " + entry.getValue() + " 個");
                }
            }
            
            // 9. 建議
            System.out.println("\n【診斷建議】");
            List<String> suggestions = new ArrayList<>();
            
            if (network.getEdgeCount() < network.getNodeCount()) {
                suggestions.add("❌ 邊數少於節點數，網絡嚴重不連通");
            }
            
            if (largestComponent.size() < network.getNodeCount() * 0.5) {
                suggestions.add("❌ 最大連通分量 < 50%，請檢查 OSM 文件完整性");
            }
            
            if (!edgeLengths.isEmpty() && Collections.max(edgeLengths) > 500) {
                suggestions.add("⚠️ 存在超長邊，可能導致路徑看起來穿越建築物");
            }
            
            if (density * 1000000 < 100) {
                suggestions.add("⚠️ 節點密度太低，考慮使用更詳細的 OSM 數據");
            }
            
            if (keywordCount.isEmpty()) {
                suggestions.add("⚠️ 沒有 POI，需要手動添加或使用包含 POI 的 OSM 文件");
            }
            
            if (suggestions.isEmpty()) {
                System.out.println("  ✅ 網絡結構看起來正常");
                System.out.println("  建議：");
                System.out.println("    1. 運行完整的 CDP 算法並查看詳細日誌");
                System.out.println("    2. 檢查可視化中的路徑是否真的穿越建築物");
                System.out.println("    3. 在 OSM 地圖上驗證節點位置");
            } else {
                for (String suggestion : suggestions) {
                    System.out.println("  " + suggestion);
                }
            }
            
            System.out.println("\n════════════════════════════════════════");
            System.out.println("診斷完成");
            System.out.println("════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("\n❌ 診斷過程出錯: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static double calculateBoundingBoxArea(List<Node> nodes) {
        if (nodes.isEmpty()) return 0;
        
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        
        for (Node node : nodes) {
            minLat = Math.min(minLat, node.getLat());
            maxLat = Math.max(maxLat, node.getLat());
            minLon = Math.min(minLon, node.getLon());
            maxLon = Math.max(maxLon, node.getLon());
        }
        
        // 近似計算面積 (平方公里)
        double latDiff = (maxLat - minLat) * 111.0; // 1度緯度約 111km
        double lonDiff = (maxLon - minLon) * 111.0 * Math.cos(Math.toRadians((minLat + maxLat) / 2));
        
        return latDiff * lonDiff;
    }
}
