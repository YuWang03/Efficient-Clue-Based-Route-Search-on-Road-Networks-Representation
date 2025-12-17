package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * 路徑修復器 - 修復包含不連通節點的路徑
 * 當路徑中相鄰節點沒有直接邊連接時，使用Dijkstra找到實際道路連接
 */
public class PathRepairer {
    
    private final RoadNetwork network;
    
    public PathRepairer(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * 修復路徑，確保所有相鄰節點都通過實際道路連接
     */
    public RepairResult repairPath(List<Node> path) {
        if (path == null || path.size() < 2) {
            return new RepairResult(path, true, "路徑無需修復");
        }
        
        List<Node> repairedPath = new ArrayList<>();
        List<String> repairs = new ArrayList<>();
        boolean needsRepair = false;
        
        repairedPath.add(path.get(0));
        
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            
            // 檢查是否有直接邊連接
            if (hasDirectEdge(from, to)) {
                repairedPath.add(to);
            } else {
                // 沒有直接連接，需要修復
                needsRepair = true;
                String repairMsg = String.format("修復 %s -> %s (無直接連接)", 
                                                from.getName(), to.getName());
                repairs.add(repairMsg);
                
                // 使用Dijkstra找到實際路徑
                RoadNetwork.PathResult pathResult = network.computeShortestPath(from, to);
                
                if (pathResult.isValid()) {
                    // 添加中間節點（跳過第一個，因為已經在repairedPath中）
                    for (int j = 1; j < pathResult.path.size(); j++) {
                        repairedPath.add(pathResult.path.get(j));
                    }
                    repairs.add(String.format("  → 插入 %d 個中間節點", pathResult.path.size() - 2));
                } else {
                    // 無法找到路徑，保留原始連接（可能會穿越建築物）
                    repairedPath.add(to);
                    repairs.add("  → 警告：無法找到有效路徑，保留原始連接");
                }
            }
        }
        
        String message = needsRepair ? 
            "路徑已修復: " + repairs.size() + " 處問題" : 
            "路徑有效，無需修復";
        
        return new RepairResult(repairedPath, !needsRepair, message, repairs);
    }
    
    /**
     * 檢查兩個節點之間是否有直接邊連接
     */
    private boolean hasDirectEdge(Node from, Node to) {
        for (Edge edge : network.getEdges(from.getId())) {
            if (edge.getTo().getId() == to.getId()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 修復結果
     */
    public static class RepairResult {
        public final List<Node> path;
        public final boolean wasValid;
        public final String message;
        public final List<String> repairs;
        
        public RepairResult(List<Node> path, boolean wasValid, String message) {
            this(path, wasValid, message, Collections.emptyList());
        }
        
        public RepairResult(List<Node> path, boolean wasValid, String message, List<String> repairs) {
            this.path = path;
            this.wasValid = wasValid;
            this.message = message;
            this.repairs = repairs;
        }
        
        public void printReport() {
            System.out.println("\n【路徑修復報告】");
            System.out.println("狀態: " + (wasValid ? "✓ 原始路徑有效" : "⚠ 已修復"));
            System.out.println("說明: " + message);
            
            if (!repairs.isEmpty()) {
                System.out.println("\n修復詳情:");
                for (String repair : repairs) {
                    System.out.println("  " + repair);
                }
            }
            
            System.out.println("最終節點數: " + path.size());
        }
    }
}
