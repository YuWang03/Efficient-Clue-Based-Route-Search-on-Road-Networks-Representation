package crs;

import crs.algorithm.*;
import crs.model.*;
import crs.parser.*;
import crs.visualization.*;
import java.util.*;

/**
 * CRS (Clue-Based Route Search) 主程式
 * 
 * 實現論文 "Efficient Clue-Based Route Search on Road Networks" 中的
 * Algorithm 1: findNextMin()
 */
public class Main {
    
    public static void main(String[] args) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║  CRS - Clue-Based Route Search 線索式路徑搜尋系統        ║");
            System.out.println("║  Algorithm 1: findNextMin() 實現與可視化                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");
            
            // 1. 解析 OSM 檔案
            String osmFile = args.length > 0 ? args[0] : "c:\\Users\\user\\Desktop\\crs_java_project\\map.osm";
            System.out.println("正在解析 OSM 檔案: " + osmFile);
            
            OSMParser parser = new OSMParser();
            RoadNetwork network = parser.parse(osmFile);
            
            System.out.println("\n路網統計:");
            System.out.println("  - 節點數: " + network.getNodeCount());
            System.out.println("  - 邊數: " + network.getEdgeCount());
            
            // 2. 為節點添加模擬的 POI 關鍵字
            addSimulatedKeywords(network);
            
            // 3. 定義線索序列（提前創建，用於多起點搜索）
            List<Clue> clues = createDemoClues(network);
            System.out.println("\n線索序列:");
            for (int i = 0; i < clues.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + clues.get(i));
            }
            
            // 4. 尋找既有 POI 又連通性好的起點
            System.out.println("\n尋找包含 POI 且連通性最佳的區域...");
            
            // 收集所有 restaurant POI 連接的道路節點
            Set<Long> restaurantRoadNodes = new HashSet<>();
            for (Node poi : network.getAllNodes()) {
                if (poi.containsKeyword("restaurant") || poi.containsKeyword("cafe") || poi.containsKeyword("convenience")) {
                    for (Edge edge : network.getEdges(poi.getId())) {
                        long roadNodeId = edge.getTo().getId();
                        if (roadNodeId != poi.getId()) {
                            restaurantRoadNodes.add(roadNodeId);
                        }
                        roadNodeId = edge.getFrom().getId();
                        if (roadNodeId != poi.getId()) {
                            restaurantRoadNodes.add(roadNodeId);
                        }
                    }
                }
            }
            
            System.out.println("  POI 連接的道路節點數: " + restaurantRoadNodes.size());
            
            // 在這些道路節點中找到可達節點數最多的
            Node bestStart = null;
            int maxReachable = 0;
            int maxDegree = 0;
            
            for (Long roadNodeId : restaurantRoadNodes) {
                Node candidate = network.getNode(roadNodeId);
                if (candidate == null) continue;
                
                // BFS 計算可達節點數
                Set<Long> visited = new HashSet<>();
                java.util.Queue<Long> queue = new java.util.LinkedList<>();
                queue.offer(candidate.getId());
                visited.add(candidate.getId());
                
                while (!queue.isEmpty()) {
                    long currentId = queue.poll();
                    for (Edge edge : network.getEdges(currentId)) {
                        long neighborId = edge.getTo().getId();
                        if (!visited.contains(neighborId)) {
                            visited.add(neighborId);
                            queue.offer(neighborId);
                        }
                    }
                }
                
                int degree = network.getEdges(candidate.getId()).size();
                if (visited.size() > maxReachable || (visited.size() == maxReachable && degree > maxDegree)) {
                    maxReachable = visited.size();
                    maxDegree = degree;
                    bestStart = candidate;
                }
            }
            
            if (bestStart == null) {
                System.err.println("錯誤：找不到合適的起點");
                return;
            }
            
            System.out.println("  最佳起點: Node-" + bestStart.getId());
            System.out.println("  節點名稱: " + (bestStart.getName().isEmpty() ? "未命名道路節點" : bestStart.getName()));
            System.out.println("  位置: (" + bestStart.getLat() + ", " + bestStart.getLon() + ")");
            System.out.println("  度數: " + maxDegree);
            System.out.println("  可達節點數: " + maxReachable);
            
            // 檢查可達節點中是否包含 POI
            Set<Long> reachable = new HashSet<>();
            java.util.Queue<Long> q = new java.util.LinkedList<>();
            q.offer(bestStart.getId());
            reachable.add(bestStart.getId());
            int poiCount = 0;
            
            while (!q.isEmpty()) {
                long curr = q.poll();
                Node node = network.getNode(curr);
                if (!node.getKeywords().isEmpty()) {
                    poiCount++;
                }
                for (Edge edge : network.getEdges(curr)) {
                    long next = edge.getTo().getId();
                    if (!reachable.contains(next)) {
                        reachable.add(next);
                        q.offer(next);
                    }
                }
            }
            
            System.out.println("  可達節點中的 POI 數量: " + poiCount);
            
            // 檢查 restaurant POI 連接的道路節點是否可達
            int restaurantCount = 0;
            int reachableRestaurantRoadNodes = 0;
            for (Node poi : network.getAllNodes()) {
                if (poi.containsKeyword("restaurant")) {
                    restaurantCount++;
                    for (Edge edge : network.getEdges(poi.getId())) {
                        long roadNodeId = edge.getTo().getId();
                        if (roadNodeId != poi.getId() && reachable.contains(roadNodeId)) {
                            reachableRestaurantRoadNodes++;
                            System.out.println("  ✓ Restaurant POI " + poi.getName() + " 連接的道路節點 " + roadNodeId + " 可達！");
                            break;
                        }
                    }
                }
            }
            System.out.println("  Restaurant 總數: " + restaurantCount + ", 連接到可達道路的: " + reachableRestaurantRoadNodes);
            
            // 如果沒有 restaurant 可達，列出一些 restaurant 連接的道路節點
            if (reachableRestaurantRoadNodes == 0) {
                System.out.println("  [問題] 所有 restaurant 都在不同的連通分量！");
                System.out.println("  前 3 個 restaurant 連接的道路節點:");
                int count = 0;
                for (Node poi : network.getAllNodes()) {
                    if (poi.containsKeyword("restaurant") && count++ < 3) {
                        for (Edge edge : network.getEdges(poi.getId())) {
                            long roadNodeId = edge.getTo().getId();
                            if (roadNodeId != poi.getId()) {
                                System.out.println("    - " + poi.getName() + " -> 道路節點 " + roadNodeId + 
                                                 " (在主網路: " + reachable.contains(roadNodeId) + ")");
                                break;
                            }
                        }
                    }
                }
            }
            
            System.out.println("  ※ 將透過鄰居檢查來匹配 POI 節點");
            
            GreedyClueSearch gcs = new GreedyClueSearch(network);
            Node source = bestStart;
            
            System.out.println("\n執行搜尋...\n");
            GreedyClueSearch.FeasiblePath result = gcs.search(source, clues);
            int traversed = gcs.getLastTraversalHistory().size();
            
            // 計算有效匹配數
            int matchCount = 0;
            if (result != null) {
                for (SearchResult match : result.getMatches()) {
                    if (match.isValid()) matchCount++;
                }
            }
            
            System.out.println("\n遍歷節點數: " + traversed);
            System.out.println("找到 " + matchCount + " 個有效匹配");
            if (result != null && matchCount > 0) {
                System.out.println("路徑長度: " + result.getFullPath().size() + " 個節點");
            }
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║  搜尋結果                              ║");
            System.out.println("╚════════════════════════════════════════╝");
            
            // 6. 生成可視化
            System.out.println("\n正在生成可視化...");
            HtmlVisualizer visualizer = new HtmlVisualizer();
            String outputPath = "c:\\Users\\user\\Desktop\\crs_java_project\\crs_visualization.html";
            visualizer.generateVisualization(
                network, source, clues, result, 
                gcs.getLastTraversalHistory(), outputPath
            );
            
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("✓ 完成！請開啟以下檔案查看可視化結果:");
            System.out.println("  " + outputPath);
            System.out.println("═══════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
        }
    }
    
    /**
     * 統計並顯示 OSM 中的真實 POI 關鍵字
     * OSM 的 amenity/shop/tourism 等標籤已在解析時自動加入節點
     */
    private static void addSimulatedKeywords(RoadNetwork network) {
        // 統計真實 POI 關鍵字
        Map<String, Integer> keywordStats = new HashMap<>();
        Map<String, List<Node>> keywordNodes = new HashMap<>();
        int totalNodesWithKeywords = 0;
        
        for (Node node : network.getAllNodes()) {
            if (!node.getKeywords().isEmpty()) {
                totalNodesWithKeywords++;
                for (String keyword : node.getKeywords()) {
                    keywordStats.put(keyword, keywordStats.getOrDefault(keyword, 0) + 1);
                    keywordNodes.computeIfAbsent(keyword, k -> new ArrayList<>()).add(node);
                }
            }
        }
        
        System.out.println("  - 從 OSM 解析到 " + totalNodesWithKeywords + " 個包含關鍵字的節點");
        System.out.println("  - POI 類型統計（前 20 種）：");
        
        // 按數量排序並顯示
        keywordStats.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .limit(20)
            .forEach(entry -> {
                System.out.println("    • " + entry.getKey() + ": " + entry.getValue() + " 個");
            });
        
        // 顯示真實 POI 的詳細信息
        System.out.println("\n  - 真實 POI 節點（amenity, shop 等）：");
        String[] poiKeywords = {"cafe", "restaurant", "convenience", "food_court", "fast_food"};
        for (String keyword : poiKeywords) {
            List<Node> nodes = keywordNodes.get(keyword);
            if (nodes != null && !nodes.isEmpty()) {
                System.out.println("    • " + keyword + ": " + nodes.size() + " 個");
                for (Node node : nodes.subList(0, Math.min(3, nodes.size()))) {
                    System.out.println("      - ID: " + node.getId() + ", Name: " + node.getName() + 
                                     ", 位置: (" + String.format("%.6f", node.getLat()) + ", " + 
                                     String.format("%.6f", node.getLon()) + ")");
                }
            }
        }
        
        // 如果 POI 太少，提示用戶
        if (totalNodesWithKeywords < 10) {
            System.out.println("  ⚠️  警告：POI 數量較少，可能影響搜尋結果");
        }
    }
    
    /**
     * 找出全局連通性最好的節點（度數最高）
     */
    @SuppressWarnings("unused")
    private static Node findMostConnectedNode(RoadNetwork network) {
        Node bestNode = null;
        int maxDegree = 0;
        for (Node node : network.getAllNodes()) {
            int degree = network.getEdges(node.getId()).size();
            if (degree > maxDegree) {
                maxDegree = degree;
                bestNode = node;
            }
        }
        return bestNode;
    }
    
    /**
     * 找出所有候選起點：返回連接目標 POI 的道路節點，按連通性排序
     */
    @SuppressWarnings("unused")
    private static List<Node> findAllCandidateStarts(RoadNetwork network, List<Clue> clues) {
        Set<String> targetKeywords = new HashSet<>();
        for (Clue clue : clues) {
            targetKeywords.add(clue.getKeyword());
        }
        
        // 找出所有連接了目標 POI 的道路節點
        Map<Long, Integer> candidateDegrees = new HashMap<>();
        for (Node node : network.getAllNodes()) {
            for (String keyword : node.getKeywords()) {
                if (targetKeywords.contains(keyword)) {
                    // 這是一個目標 POI 節點，找它連接的道路節點
                    for (Edge edge : network.getEdges(node.getId())) {
                        long neighborId = edge.getTo().getId();
                        Node neighbor = network.getNode(neighborId);
                        if (neighbor != null) {
                            int degree = network.getEdges(neighborId).size();
                            candidateDegrees.put(neighborId, degree);
                        }
                    }
                    break; // 一個節點只需處理一次
                }
            }
        }
        
        // 轉換成列表並按度數排序（從高到低）
        List<Node> candidates = new ArrayList<>();
        candidateDegrees.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> {
                Node node = network.getNode(entry.getKey());
                if (node != null) {
                    candidates.add(node);
                }
            });
        
        System.out.println("  （找到 " + candidates.size() + " 個連接目標 POI 的道路節點）");
        
        // 如果沒有找到連接 POI 的節點，返回全局度數最高的幾個節點
        if (candidates.isEmpty()) {
            System.out.println("  （未找到連接 POI 的節點，使用全局度數最高的節點）");
            List<Map.Entry<Node, Integer>> allNodes = new ArrayList<>();
            for (Node node : network.getAllNodes()) {
                int degree = network.getEdges(node.getId()).size();
                if (degree > 0) {
                    allNodes.add(new AbstractMap.SimpleEntry<>(node, degree));
                }
            }
            allNodes.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));
            for (int i = 0; i < Math.min(10, allNodes.size()); i++) {
                candidates.add(allNodes.get(i).getKey());
            }
        }
        
        return candidates;
    }
    
    /**
     * 找到一個適合作為起點的節點
     * 優先選擇靠近目標 POI (cafe/restaurant/convenience) 的連通分量
     * （此方法已被 findAllCandidateStarts 取代，但保留用於向後兼容）
     */
    @SuppressWarnings("unused")
    private static Node findGoodStartNode(RoadNetwork network) {
        // 策略：找到包含最多目標 POI 的連通分量的起點
        // 先找所有 cafe, restaurant, convenience 的連接道路節點
        Set<Long> targetRoadNodes = new HashSet<>();
        
        for (Node node : network.getAllNodes()) {
            if (node.containsKeyword("cafe") || 
                node.containsKeyword("restaurant") || 
                node.containsKeyword("convenience")) {
                // 找這個 POI 連接的道路節點
                for (Edge edge : network.getEdges(node.getId())) {
                    targetRoadNodes.add(edge.getTo().getId());
                }
            }
        }
        
        System.out.println("  （找到 " + targetRoadNodes.size() + " 個連接目標 POI 的道路節點）");
        
        // 在這些道路節點中找度數最高的，並顯示前 5 個候選
        List<Map.Entry<Long, Integer>> candidates = new ArrayList<>();
        
        for (Long nodeId : targetRoadNodes) {
            Node node = network.getNode(nodeId);
            if (node != null) {
                int degree = network.getEdges(nodeId).size();
                candidates.add(new AbstractMap.SimpleEntry<>(nodeId, degree));
            }
        }
        
        // 按度數降序排序
        candidates.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        System.out.println("  前 5 個候選起點（按連通性排序）：");
        for (int i = 0; i < Math.min(5, candidates.size()); i++) {
            Map.Entry<Long, Integer> entry = candidates.get(i);
            Node node = network.getNode(entry.getKey());
            System.out.println("    " + (i+1) + ". Node-" + entry.getKey() + 
                             " (度數: " + entry.getValue() + 
                             ", 位置: " + String.format("%.6f, %.6f", node.getLat(), node.getLon()) + ")");
        }
        
        // 選擇度數最高的
        Node bestNode = null;
        int maxDegree = 0;
        if (!candidates.isEmpty()) {
            bestNode = network.getNode(candidates.get(0).getKey());
            maxDegree = candidates.get(0).getValue();
        }
        
        // 比較 POI 附近節點 vs 全局最佳節點
        int poiMaxDegree = maxDegree;
        Node poiBestNode = bestNode;
        
        // 找全局度數最高的節點
        Node globalBestNode = null;
        int globalMaxDegree = 0;
        for (Node node : network.getAllNodes()) {
            int degree = network.getEdges(node.getId()).size();
            if (degree > globalMaxDegree) {
                globalMaxDegree = degree;
                globalBestNode = node;
            }
        }
        
        if (globalBestNode != null) {
            System.out.println("  全局最佳節點: Node-" + globalBestNode.getId() + 
                             " (度數: " + globalMaxDegree + ")");
        }
        
        // 如果全局最佳節點的度數遠大於 POI 附近節點,優先使用全局最佳
        // 這樣可以遍歷更多節點,增加找到目標的機會
        if (globalMaxDegree > poiMaxDegree * 2) {
            System.out.println("  （選擇全局連通性最好的節點以遍歷更多節點，有 " + globalMaxDegree + " 個鄰居）");
            return globalBestNode;
        } else if (poiBestNode != null) {
            System.out.println("  （選擇目標 POI 附近連通性最好的節點，有 " + poiMaxDegree + " 個鄰居）");
            return poiBestNode;
        } else {
            System.out.println("  （選擇全局連通性最好的節點，有 " + globalMaxDegree + " 個鄰居）");
            return globalBestNode;
        }
    }
    
    /**
     * 建立示範用的線索序列
     */
    private static List<Clue> createDemoClues(RoadNetwork network) {
        List<Clue> clues = new ArrayList<>();
        
        // 找出網路中實際存在的關鍵字
        Set<String> availableKeywords = new HashSet<>();
        for (Node node : network.getAllNodes()) {
            availableKeywords.addAll(node.getKeywords());
        }
        
        System.out.println("  - 可用關鍵字: " + availableKeywords);
        
        // 建立線索（大幅增加搜索範圍以跨越道路）
        if (availableKeywords.contains("restaurant")) {
            clues.add(new Clue("restaurant", 2000, 1.0));  // 先找餐廳，2000m ± 100%（範圍 0-4000m）
        }
        if (availableKeywords.contains("cafe")) {
            clues.add(new Clue("cafe", 2000, 1.0));  // 再找咖啡廳，2000m ± 100%（範圍 0-4000m）
        }
        if (availableKeywords.contains("convenience")) {
            clues.add(new Clue("convenience", 2000, 1.0));  // 找便利商店，2000m ± 100%（範圍 0-4000m）
        }
        
        // 如果沒有足夠的關鍵字，添加一些預設的
        if (clues.isEmpty()) {
            // 使用道路名稱作為關鍵字
            for (Node node : network.getAllNodes()) {
                for (String keyword : node.getKeywords()) {
                    if (!keyword.isEmpty() && clues.size() < 3) {
                        clues.add(new Clue(keyword, 200 + clues.size() * 100, 0.5));
                    }
                }
                if (clues.size() >= 3) break;
            }
        }
        
        return clues;
    }
}
