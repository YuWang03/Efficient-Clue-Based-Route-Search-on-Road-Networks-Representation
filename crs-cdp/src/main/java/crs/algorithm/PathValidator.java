package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * 路徑驗證器 - 確保路徑沿著實際道路網絡
 */
public class PathValidator {
    
    private final RoadNetwork network;
    
    public PathValidator(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * 驗證路徑是否有效（所有相鄰節點之間都有邊連接）
     */
    public ValidationResult validatePath(List<Node> path) {
        if (path == null || path.isEmpty()) {
            return new ValidationResult(false, "路徑為空", Collections.emptyList());
        }
        
        if (path.size() == 1) {
            return new ValidationResult(true, "單節點路徑", Collections.emptyList());
        }
        
        List<String> issues = new ArrayList<>();
        boolean isValid = true;
        
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            
            // 檢查是否有直接邊連接
            boolean hasEdge = false;
            for (Edge edge : network.getEdges(from.getId())) {
                if (edge.getTo().getId() == to.getId()) {
                    hasEdge = true;
                    break;
                }
            }
            
            if (!hasEdge) {
                String issue = String.format("節點 %s -> %s 之間沒有直接邊連接", 
                                            from.getName(), to.getName());
                issues.add(issue);
                isValid = false;
            }
        }
        
        if (isValid) {
            return new ValidationResult(true, "路徑有效：所有相鄰節點都通過實際道路連接", issues);
        } else {
            return new ValidationResult(false, "路徑無效：存在穿越建築物的直線連接", issues);
        }
    }
    
    /**
     * 計算路徑的實際總距離
     */
    public double computePathDistance(List<Node> path) {
        if (path == null || path.size() < 2) {
            return 0.0;
        }
        
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            
            // 查找邊的權重
            for (Edge edge : network.getEdges(from.getId())) {
                if (edge.getTo().getId() == to.getId()) {
                    totalDistance += edge.getWeight();
                    break;
                }
            }
        }
        
        return totalDistance;
    }
    
    /**
     * 比較兩條路徑的差異
     */
    public PathComparison comparePaths(List<Node> keyPath, List<Node> fullPath) {
        double keyPathDirect = computeDirectDistance(keyPath);
        double fullPathActual = computePathDistance(fullPath);
        
        ValidationResult keyPathValidation = validatePath(keyPath);
        ValidationResult fullPathValidation = validatePath(fullPath);
        
        return new PathComparison(
            keyPath.size(),
            fullPath.size(),
            keyPathDirect,
            fullPathActual,
            keyPathValidation.isValid,
            fullPathValidation.isValid
        );
    }
    
    /**
     * 計算路徑的直線距離（Haversine）
     */
    private double computeDirectDistance(List<Node> path) {
        if (path == null || path.size() < 2) {
            return 0.0;
        }
        
        double totalDistance = 0.0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            totalDistance += RoadNetwork.haversineDistance(from, to);
        }
        
        return totalDistance;
    }
    
    /**
     * 驗證結果
     */
    public static class ValidationResult {
        public final boolean isValid;
        public final String message;
        public final List<String> issues;
        
        public ValidationResult(boolean isValid, String message, List<String> issues) {
            this.isValid = isValid;
            this.message = message;
            this.issues = issues;
        }
        
        public void printReport() {
            System.out.println("\n【路徑驗證報告】");
            System.out.println("狀態: " + (isValid ? "✓ 有效" : "✗ 無效"));
            System.out.println("說明: " + message);
            
            if (!issues.isEmpty()) {
                System.out.println("\n問題列表:");
                for (int i = 0; i < issues.size(); i++) {
                    System.out.println("  " + (i+1) + ". " + issues.get(i));
                }
            }
        }
    }
    
    /**
     * 路徑比較結果
     */
    public static class PathComparison {
        public final int keyNodeCount;
        public final int fullNodeCount;
        public final double keyPathDistance;
        public final double fullPathDistance;
        public final boolean keyPathValid;
        public final boolean fullPathValid;
        
        public PathComparison(int keyNodeCount, int fullNodeCount, 
                            double keyPathDistance, double fullPathDistance,
                            boolean keyPathValid, boolean fullPathValid) {
            this.keyNodeCount = keyNodeCount;
            this.fullNodeCount = fullNodeCount;
            this.keyPathDistance = keyPathDistance;
            this.fullPathDistance = fullPathDistance;
            this.keyPathValid = keyPathValid;
            this.fullPathValid = fullPathValid;
        }
        
        public void printReport() {
            System.out.println("\n【路徑比較報告】");
            System.out.println("─────────────────────────────────────");
            System.out.println("關鍵節點路徑:");
            System.out.println("  節點數: " + keyNodeCount);
            System.out.println("  直線距離: " + String.format("%.1f", keyPathDistance) + "m");
            System.out.println("  有效性: " + (keyPathValid ? "✓" : "✗ (穿越建築物)"));
            
            System.out.println("\n完整實際路徑:");
            System.out.println("  節點數: " + fullNodeCount);
            System.out.println("  實際距離: " + String.format("%.1f", fullPathDistance) + "m");
            System.out.println("  有效性: " + (fullPathValid ? "✓ (沿著道路)" : "✗"));
            
            System.out.println("\n差異分析:");
            System.out.println("  節點數增加: " + (fullNodeCount - keyNodeCount) + 
                             " (+" + String.format("%.1f", (fullNodeCount - keyNodeCount) * 100.0 / keyNodeCount) + "%)");
            System.out.println("  距離增加: " + String.format("%.1f", fullPathDistance - keyPathDistance) + "m" +
                             " (+" + String.format("%.1f", (fullPathDistance - keyPathDistance) * 100.0 / keyPathDistance) + "%)");
            System.out.println("─────────────────────────────────────");
        }
    }
}
