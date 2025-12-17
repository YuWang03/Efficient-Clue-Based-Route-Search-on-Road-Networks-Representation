package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * Algorithm 2: Clue-Based Dynamic Programming (CDP)
 * 論文 Section 4
 * 
 * 使用動態規劃精確求解 CRS 問題
 * 時間複雜度: O(Σ|Vw_{i-1}| × |Vw_i|)
 */
public class CDPAlgorithm {
    
    private final RoadNetwork network;
    private List<DPStep> dpSteps;  // 記錄 DP 過程用於可視化
    
    public CDPAlgorithm(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * DP 步驟記錄 - 用於可視化
     */
    public static class DPStep {
        public final int level;           // 第幾個關鍵字 (i)
        public final Node node;           // 當前節點 u
        public final Node prevNode;       // 前驅節點 v
        public final double networkDist;  // dG(v, u)
        public final double matchingDist; // dm(mi, s(v→u))
        public final double dpValue;      // D(wi, u)
        public final boolean isOptimal;   // 是否在最優路徑上
        
        public DPStep(int level, Node node, Node prevNode, double networkDist,
                     double matchingDist, double dpValue, boolean isOptimal) {
            this.level = level;
            this.node = node;
            this.prevNode = prevNode;
            this.networkDist = networkDist;
            this.matchingDist = matchingDist;
            this.dpValue = dpValue;
            this.isOptimal = isOptimal;
        }
    }
    
    /**
     * CDP 結果
     */
    public static class CDPResult {
        public final List<Node> path;           // 關鍵節點路徑 FP_cdp
        public final List<Node> fullPath;       // 完整實際路徑（沿著道路網絡）
        public final double matchingDistance;   // dm(C, FP_cdp)
        public final Map<Integer, Map<Long, Double>> dpTable;  // D(wi, u) 表
        public final Map<Integer, Map<Long, Long>> backtrack;  // 回溯表
        
        public CDPResult(List<Node> path, List<Node> fullPath, double matchingDistance,
                        Map<Integer, Map<Long, Double>> dpTable,
                        Map<Integer, Map<Long, Long>> backtrack) {
            this.path = path;
            this.fullPath = fullPath;
            this.matchingDistance = matchingDistance;
            this.dpTable = dpTable;
            this.backtrack = backtrack;
        }
    }
    
    /**
     * 執行 Algorithm 2: CDP
     * 
     * @param source 起始頂點 vq
     * @param clues 線索序列 C = {(w1,d1), ..., (wk,dk)}
     * @return CDPResult 包含最優路徑和匹配距離
     */
    public CDPResult solve(Node source, List<Clue> clues) {
        dpSteps = new ArrayList<>();
        int k = clues.size();
        
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║    Algorithm 2: CDP (Dynamic Programming)      ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("起點: " + source.getName());
        System.out.println("線索數量: " + k);
        
        // D(wi, u) - 儲存到達節點 u 時的最小匹配距離
        // dpTable[i][u] = D(wi, u)
        Map<Integer, Map<Long, Double>> dpTable = new HashMap<>();
        
        // backtrack[i][u] = 最優前驅節點的 ID
        Map<Integer, Map<Long, Long>> backtrack = new HashMap<>();
        
        // ========== Line 1-2: 初始化 D(w1, u) ==========
        System.out.println("\n【Level 1】初始化 D(w1, u)");
        Clue clue1 = clues.get(0);
        Set<Node> Vw1 = network.getNodeObjectsByKeyword(clue1.getKeyword());
        
        System.out.println("  關鍵字: " + clue1.getKeyword() + ", 候選數: " + Vw1.size());
        
        Map<Long, Double> dp1 = new HashMap<>();
        Map<Long, Long> bt1 = new HashMap<>();
        
        for (Node u : Vw1) {
            // 計算 dG(vq, u)
            double dG = network.getNetworkDistance(source, u);
            
            // 檢查是否在距離範圍內
            if (!clue1.isDistanceInRange(dG)) continue;
            
            // dm(m1, s(vq → u))
            double dm = clue1.computeMatchingDistance(dG);
            dp1.put(u.getId(), dm);
            bt1.put(u.getId(), source.getId());
            
            System.out.println("    " + u.getName() + ": dG=" + 
                String.format("%.1f", dG) + "m, dm=" + String.format("%.4f", dm));
            
            dpSteps.add(new DPStep(1, u, source, dG, dm, dm, false));
        }
        
        dpTable.put(1, dp1);
        backtrack.put(1, bt1);
        
        // ========== Line 3-11: 迭代計算 D(wi, u) ==========
        for (int i = 2; i <= k; i++) {
            System.out.println("\n【Level " + i + "】計算 D(w" + i + ", u)");
            
            Clue clueI = clues.get(i - 1);
            Clue cluePrev = clues.get(i - 2);
            
            Set<Node> Vwi = network.getNodeObjectsByKeyword(clueI.getKeyword());
            Set<Node> VwPrev = network.getNodeObjectsByKeyword(cluePrev.getKeyword());
            
            Map<Long, Double> dpI = new HashMap<>();
            Map<Long, Long> btI = new HashMap<>();
            Map<Long, Double> dpPrev = dpTable.get(i - 1);
            
            System.out.println("  關鍵字: " + clueI.getKeyword() + ", 候選數: " + Vwi.size());
            System.out.println("  前一層級有效節點數: " + dpPrev.size());
            
            int processedCount = 0;
            // Line 4: for each u ∈ Vw_i
            for (Node u : Vwi) {
                // Line 5: Initial intermediate vector iv(u)
                List<Double> iv = new ArrayList<>();
                long bestPrev = -1;
                double minIV = Double.MAX_VALUE;
                
                int validPathCount = 0;
                // Line 6: for each v ∈ Vw_{i-1}
                for (Node v : VwPrev) {
                    if (!dpPrev.containsKey(v.getId())) continue;
                    validPathCount++;
                    
                    double dPrev = dpPrev.get(v.getId());  // D(w_{i-1}, v)
                    double dG = network.getNetworkDistance(v, u);
                    
                    // 檢查距離範圍
                    if (!clueI.isDistanceInRange(dG)) continue;
                    
                    double dm = clueI.computeMatchingDistance(dG);  // dm(μi, σ(v→u))
                    
                    double intermediate;
                    // Line 7-10: if dm < D(w_{i-1}, v) then insert D else insert dm
                    if (dm < dPrev) {
                        intermediate = dPrev;  // Line 8
                    } else {
                        intermediate = dm;     // Line 10
                    }
                    
                    iv.add(intermediate);
                    
                    // 記錄最小值及其來源
                    if (intermediate < minIV) {
                        minIV = intermediate;
                        bestPrev = v.getId();
                    }
                }
                
                // Line 11: D(wi, u) ← min{iv(u)}
                if (!iv.isEmpty()) {
                    dpI.put(u.getId(), minIV);
                    btI.put(u.getId(), bestPrev);
                    
                    Node prevNode = network.getNode(bestPrev);
                    double dG = network.getNetworkDistance(prevNode, u);
                    double dm = clueI.computeMatchingDistance(dG);
                    
                    System.out.println("    " + u.getName() + ": D=" + 
                        String.format("%.4f", minIV) + " (from " + prevNode.getName() + 
                        ", dG=" + String.format("%.1f", dG) + "m, paths_checked=" + validPathCount + ")");
                    
                    dpSteps.add(new DPStep(i, u, prevNode, dG, dm, minIV, false));
                    processedCount++;
                }
            }
            
            System.out.println("  → 本層級找到 " + processedCount + " 個有效節點");
            
            dpTable.put(i, dpI);
            backtrack.put(i, btI);
        }
        
        // ========== Line 12-13: 找最小 D(wk, u) 並回溯 ==========
        System.out.println("\n【回溯最優路徑】");
        
        Map<Long, Double> dpK = dpTable.get(k);
        if (dpK == null || dpK.isEmpty()) {
            System.out.println("　無可行路徑！");
            return new CDPResult(Collections.emptyList(), Collections.emptyList(), 
                               Double.MAX_VALUE, dpTable, backtrack);
        }
        
        // Line 12: Find min{D(wk, u)}
        double minDm = Double.MAX_VALUE;
        long bestEndNode = -1;
        for (Map.Entry<Long, Double> entry : dpK.entrySet()) {
            if (entry.getValue() < minDm) {
                minDm = entry.getValue();
                bestEndNode = entry.getKey();
            }
        }
        
        // Line 13: 回溯構建路徑
        List<Node> path = new ArrayList<>();
        long current = bestEndNode;
        
        for (int i = k; i >= 1; i--) {
            Node node = network.getNode(current);
            path.add(node);
            
            // 標記最優路徑上的節點
            for (DPStep step : dpSteps) {
                if (step.level == i && step.node.getId() == current) {
                    // 更新為最優
                    int idx = dpSteps.indexOf(step);
                    dpSteps.set(idx, new DPStep(step.level, step.node, step.prevNode,
                        step.networkDist, step.matchingDist, step.dpValue, true));
                }
            }
            
            Map<Long, Long> bt = backtrack.get(i);
            if (bt != null && bt.containsKey(current)) {
                current = bt.get(current);
            }
        }
        path.add(source);
        Collections.reverse(path);
        
        // 構建完整的實際路徑（沿著道路網絡）
        System.out.println("\n構建完整實際路徑...");
        List<Node> fullPath = new ArrayList<>();
        double totalDistance = 0;
        int totalSegments = path.size() - 1;
        int successSegments = 0;
        int failedSegments = 0;
        
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            
            // 使用 Dijkstra 算法計算實際路徑
            RoadNetwork.PathResult pathResult = network.computeShortestPath(from, to);
            
            if (!pathResult.isValid()) {
                System.out.println("　❌ 警告: 無法從 " + from.getName() + " 到達 " + to.getName());
                System.out.println("　   起點邊數: " + network.getEdges(from.getId()).size());
                System.out.println("　   終點邊數: " + network.getEdges(to.getId()).size());
                failedSegments++;
                
                // 即使無法找到路徑，也要保持連續性
                if (i == 0) {
                    fullPath.add(from);
                }
                fullPath.add(to);
                continue;
            }
            
            // 添加中間路徑節點（除了最後一個，避免重複）
            if (i == 0) {
                fullPath.addAll(pathResult.path);
            } else {
                fullPath.addAll(pathResult.path.subList(1, pathResult.path.size()));
            }
            
            totalDistance += pathResult.distance;
            successSegments++;
            System.out.println("　✓ " + from.getName() + " → " + to.getName() + 
                             ": " + pathResult.path.size() + " 個節點, " + 
                             String.format("%.1f", pathResult.distance) + "m");
        }
        
        System.out.println("\n路徑段統計: " + successSegments + "/" + totalSegments + " 成功");
        if (failedSegments > 0) {
            System.out.println("警告: " + failedSegments + " 個路徑段無法找到道路連接，可能穿越建築物");
        }
        
        // 使用PathRepairer修復路徑中的斷點
        if (failedSegments > 0 || fullPath.isEmpty()) {
            System.out.println("\n嘗試修復路徑...");
            PathRepairer repairer = new PathRepairer(network);
            PathRepairer.RepairResult repairResult = repairer.repairPath(fullPath.isEmpty() ? path : fullPath);
            repairResult.printReport();
            fullPath = repairResult.path;
        }
        
        System.out.println("\n========== CDP 結果 ==========");
        System.out.println("最優匹配距離 dm(C, FP_cdp) = " + String.format("%.4f", minDm));
        System.out.println("關鍵節點路徑 (" + path.size() + " 個節點):");
        for (int i = 0; i < path.size(); i++) {
            System.out.println("  " + i + ". " + path.get(i).getName());
        }
        System.out.println("\n完整實際路徑 (" + fullPath.size() + " 個節點, 總距離: " + 
                         String.format("%.1f", totalDistance) + "m):");
        if (fullPath.size() <= 20) {
            for (int i = 0; i < fullPath.size(); i++) {
                System.out.println("  " + i + ". " + fullPath.get(i).getName());
            }
        } else {
            System.out.println("  (過多節點，略過詳細顯示)");
        }
        
        return new CDPResult(path, fullPath, minDm, dpTable, backtrack);
    }
    
    /**
     * 取得 DP 步驟記錄（用於可視化）
     */
    public List<DPStep> getDPSteps() {
        return dpSteps;
    }
}
