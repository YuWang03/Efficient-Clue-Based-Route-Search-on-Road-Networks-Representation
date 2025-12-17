package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * Algorithm 1: Procedure findNextMin()
 * 論文第 4 頁 - 貪婪線索搜尋算法的核心程序
 * 
 * 輸入: 源頂點 u 和線索 m(w, d, ε)
 * 輸出: min{dm(m, s)} 和匹配頂點 v
 */
public class FindNextMinAlgorithm {
    
    private final RoadNetwork network;
    private List<TraversalStep> traversalHistory;  // 記錄遍歷過程，用於可視化
    
    public FindNextMinAlgorithm(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * 遍歷步驟記錄 - 用於可視化
     */
    public static class TraversalStep {
        public final Node node;
        public final double distance;
        public final boolean isCandidate;
        public final boolean isSelected;
        public final String reason;
        
        public TraversalStep(Node node, double distance, boolean isCandidate, 
                           boolean isSelected, String reason) {
            this.node = node;
            this.distance = distance;
            this.isCandidate = isCandidate;
            this.isSelected = isSelected;
            this.reason = reason;
        }
    }
    
    /**
     * Algorithm 1 實現
     * 
     * @param source 源頂點 u
     * @param clue 線索 m(w, d, ε)
     * @return SearchResult 包含匹配頂點和匹配距離
     */
    public SearchResult findNextMin(Node source, Clue clue) {
        traversalHistory = new ArrayList<>();
        
        String keyword = clue.getKeyword();
        double targetDistance = clue.getDistance();
        double minDist = clue.getMinDistance();
        double maxDist = clue.getMaxDistance();
        
        System.out.println("\n========== findNextMin 開始 ==========");
        System.out.println("源頂點: " + source.getName() + " (ID: " + source.getId() + ")");
        System.out.println("線索: " + clue);
        System.out.println("距離區間: [" + String.format("%.2f", minDist) + ", " + 
                          String.format("%.2f", maxDist) + "] 公尺");
        
        // 使用 Dijkstra 進行網路遍歷
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(
            Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
        
        dist.put(source.getId(), 0.0);
        pq.offer(new long[]{source.getId(), Double.doubleToLongBits(0.0)});
        
        Node bestMatch = null;
        double bestMatchingDistance = Double.MAX_VALUE;
        double bestNetworkDistance = 0;
        @SuppressWarnings("unused")
        boolean foundFirstMatchBeyondTarget = false;  // 論文優化：記錄是否找到超過目標距離的第一個匹配
        
        int visitCount = 0;
        final int MIN_VISIT_COUNT = 200;  // 增加目標：遍歷 200+ 節點
        final double MAX_SEARCH_DISTANCE = 50000.0;  // 增加到 50km（遍歷整個連通組件）
        
        // Line 1: From u, do network traversal
        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long nodeId = curr[0];
            double currentDist = Double.longBitsToDouble(curr[1]);
            
            if (currentDist > dist.getOrDefault(nodeId, Double.MAX_VALUE)) continue;
            
            Node currentNode = network.getNode(nodeId);
            if (currentNode == null) continue;
            
            visitCount++;
            
            // Line 2-3: if a match vertex v is found (contains keyword w)
            // 檢查當前節點或其相鄰的 POI 節點是否包含關鍵字
            boolean containsKeyword = currentNode.containsKeyword(keyword);
            Node matchNode = currentNode;
            
            // 如果當前節點沒有關鍵字，檢查相鄰的 POI 節點（檢查邊的兩端）
            if (!containsKeyword) {
                for (Edge edge : network.getEdges(currentNode.getId())) {
                    // 檢查邊的 to 節點
                    Node neighbor = edge.getTo();
                    if (neighbor.containsKeyword(keyword)) {
                        containsKeyword = true;
                        matchNode = neighbor;  // 使用 POI 節點作為匹配節點
                        if (visitCount % 100 == 1) {
                            System.out.println("  [Debug] 在 " + currentNode.getId() + " 的鄰居 " + neighbor.getName() + " 找到關鍵字: " + keyword);
                        }
                        break;
                    }
                    // 檢查邊的 from 節點
                    Node fromNeighbor = edge.getFrom();
                    if (fromNeighbor.getId() != currentNode.getId() && fromNeighbor.containsKeyword(keyword)) {
                        containsKeyword = true;
                        matchNode = fromNeighbor;  // 使用 POI 節點作為匹配節點
                        if (visitCount % 100 == 1) {
                            System.out.println("  [Debug] 在 " + currentNode.getId() + " 的鄰居 (from) " + fromNeighbor.getName() + " 找到關鍵字: " + keyword);
                        }
                        break;
                    }
                }
            }
            
            boolean inRange = clue.isDistanceInRange(currentDist);
            
            // 記錄遍歷步驟
            String reason;
            boolean isCandidate = containsKeyword && inRange;
            
            if (containsKeyword) {
                if (inRange) {
                    reason = "✓ 包含關鍵字且距離在範圍內";
                } else if (currentDist < minDist) {
                    reason = "包含關鍵字但距離不足 (" + String.format("%.1f", currentDist) + "m < " + 
                             String.format("%.1f", minDist) + "m)";
                } else {
                    reason = "包含關鍵字但距離超出 (" + String.format("%.1f", currentDist) + "m > " + 
                             String.format("%.1f", maxDist) + "m)";
                }
            } else {
                reason = "不包含關鍵字 '" + keyword + "'";
            }
            
            // Line 4-9: 尋找最佳匹配（論文算法 1）
            if (containsKeyword && inRange) {
                double dm = clue.computeMatchingDistance(currentDist);
                
                System.out.println("  [候選] " + matchNode.getName() + 
                    " (ID:" + matchNode.getId() + ", 位置:" + 
                    String.format("%.6f", matchNode.getLat()) + "," + 
                    String.format("%.6f", matchNode.getLon()) + ")" +
                    " - 網路距離: " + String.format("%.2f", currentDist) + "m" +
                    ", 匹配距離: " + String.format("%.4f", dm) +
                    ", 關鍵字: " + matchNode.getKeywords());
                
                // 論文 Line 4-5: v is the first visited match vertex and dG(u,v) > d
                // 為了遍歷更多節點，不在第一個匹配時立即終止
                if (bestMatch == null && currentDist > targetDistance) {
                    bestMatch = matchNode;
                    bestMatchingDistance = dm;
                    bestNetworkDistance = currentDist;
                    foundFirstMatchBeyondTarget = true;
                    
                    traversalHistory.add(new TraversalStep(matchNode, currentDist, 
                        true, true, reason + " [第一個匹配且 dG > d]"));
                    
                    System.out.println("  → 找到第一個匹配，距離(" + String.format("%.2f", currentDist) + 
                                     "m) > 目標距離(" + String.format("%.2f", targetDistance) + "m)，繼續搜索...");
                    // 不 break，繼續搜索
                }
                
                // 論文 Line 6-9: 比較並更新最佳匹配
                if (bestMatch == null) {
                    // 第一個匹配（且 dG <= d）
                    bestMatch = matchNode;
                    bestMatchingDistance = dm;
                    bestNetworkDistance = currentDist;
                } else {
                    // 論文 Line 7: If dG(u,v') <= d
                    if (currentDist <= targetDistance) {
                        // v' 距離更小，更新最佳匹配
                        if (dm < bestMatchingDistance) {
                            bestMatch = matchNode;
                            bestMatchingDistance = dm;
                            bestNetworkDistance = currentDist;
                        }
                    } else {
                        // 論文 Line 8-9: dG(u,v') > d，比較 |d - dG(u,v)| 與 |dG(u,v') - d|
                        double prevDiff = Math.abs(targetDistance - bestNetworkDistance);
                        double currDiff = Math.abs(currentDist - targetDistance);
                        
                        System.out.println("    比較差值: 前匹配 |" + String.format("%.2f", targetDistance) + 
                                         " - " + String.format("%.2f", bestNetworkDistance) + "| = " + 
                                         String.format("%.2f", prevDiff) + "m vs 當前 |" + 
                                         String.format("%.2f", currentDist) + " - " + 
                                         String.format("%.2f", targetDistance) + "| = " + 
                                         String.format("%.2f", currDiff) + "m");
                        
                        if (currDiff < prevDiff) {
                            // 當前匹配更接近目標距離
                            bestMatch = matchNode;
                            bestMatchingDistance = dm;
                            bestNetworkDistance = currentDist;
                            System.out.println("    → 更新最佳匹配（當前距離更接近目標）");
                        }
                        
                        // 為了遍歷更多節點，不在比較後終止
                        traversalHistory.add(new TraversalStep(matchNode, currentDist, 
                            true, matchNode == bestMatch, reason + " [比較後繼續]"));
                    }
                }
            }
            
            traversalHistory.add(new TraversalStep(currentNode, currentDist, 
                isCandidate, currentNode == bestMatch, reason));
            
            // 檢查是否達到目標遍歷數量
            if (bestMatch != null && visitCount >= MIN_VISIT_COUNT) {
                System.out.println("  → 已遍歷 " + visitCount + " 個節點且找到匹配，停止搜尋");
                break;
            }
            
            // 安全邊界：超過最大搜索距離停止搜尋（避免無窮遍歷）
            if (currentDist > MAX_SEARCH_DISTANCE) {
                System.out.println("  → 到達安全邊界（" + String.format("%.0f", MAX_SEARCH_DISTANCE) + "m），停止搜尋");
                break;
            }
            
            // 展開鄰居
            for (Edge edge : network.getEdges(nodeId)) {
                long nextId = edge.getTo().getId();
                double newDist = currentDist + edge.getWeight();
                
                if (newDist < dist.getOrDefault(nextId, Double.MAX_VALUE)) {
                    dist.put(nextId, newDist);
                    prev.put(nextId, nodeId);
                    pq.offer(new long[]{nextId, Double.doubleToLongBits(newDist)});
                }
            }
        }
        
        System.out.println("遍歷節點數: " + visitCount);
        
        // Line 10: return min{dm(m, s)} and v
        if (bestMatch != null) {
            // 建構路徑
            List<Node> path = reconstructPath(source, bestMatch, prev);
            
            System.out.println("========== 結果 ==========");
            System.out.println("最佳匹配: " + bestMatch.getName());
            System.out.println("網路距離: " + String.format("%.2f", bestNetworkDistance) + " 公尺");
            System.out.println("匹配距離: " + String.format("%.4f", bestMatchingDistance));
            System.out.println("路徑長度: " + path.size() + " 個節點");
            
            return new SearchResult(bestMatch, bestMatchingDistance, bestNetworkDistance, path, clue);
        } else {
            System.out.println("========== 無匹配結果 ==========");
            return new SearchResult(null, Double.MAX_VALUE, 0, Collections.emptyList(), clue);
        }
    }
    
    /**
     * 從前驅節點重建路徑
     */
    private List<Node> reconstructPath(Node source, Node target, Map<Long, Long> prev) {
        List<Node> path = new ArrayList<>();
        Long current = target.getId();
        
        while (current != null) {
            Node node = network.getNode(current);
            if (node != null) path.add(node);
            if (current.equals(source.getId())) break;
            current = prev.get(current);
        }
        
        Collections.reverse(path);
        return path;
    }
    
    /**
     * 取得遍歷歷史（用於可視化）
     */
    public List<TraversalStep> getTraversalHistory() {
        return traversalHistory;
    }
}
