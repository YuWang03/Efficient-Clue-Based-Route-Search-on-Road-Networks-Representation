package crs.utils;

import crs.model.*;
import java.util.*;

/**
 * 網絡診斷工具 - 用於診斷道路網絡的連通性問題
 */
public class NetworkDiagnostics {
    
    private final RoadNetwork network;
    
    public NetworkDiagnostics(RoadNetwork network) {
        this.network = network;
    }
    
    /**
     * 完整診斷報告
     */
    public void printFullDiagnostics() {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║         道路網絡診斷報告                       ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        
        // 基本統計
        printBasicStats();
        
        // 連通性分析
        printConnectivityAnalysis();
        
        // 節點度數分布
        printDegreeDistribution();
        
        // 孤立節點
        printIsolatedNodes();
    }
    
    private void printBasicStats() {
        System.out.println("\n【基本統計】");
        System.out.println("  節點總數: " + network.getNodeCount());
        System.out.println("  邊總數: " + network.getEdgeCount());
        System.out.println("  平均度數: " + String.format("%.2f", 
            (double) network.getEdgeCount() / network.getNodeCount()));
    }
    
    private void printConnectivityAnalysis() {
        System.out.println("\n【連通性分析】");
        
        // 使用BFS找出所有連通分量
        Set<Long> visited = new HashSet<>();
        List<Set<Long>> components = new ArrayList<>();
        
        for (Node node : network.getAllNodes()) {
            if (!visited.contains(node.getId())) {
                Set<Long> component = bfs(node.getId(), visited);
                if (!component.isEmpty()) {
                    components.add(component);
                }
            }
        }
        
        System.out.println("  連通分量數: " + components.size());
        
        if (components.size() > 1) {
            System.out.println("  ⚠️ 警告: 網絡不連通！存在 " + components.size() + " 個獨立的子網絡");
            System.out.println("\n  各連通分量大小:");
            components.sort((a, b) -> b.size() - a.size());
            for (int i = 0; i < Math.min(5, components.size()); i++) {
                System.out.println("    " + (i+1) + ". " + components.get(i).size() + " 個節點");
            }
            
            if (components.size() > 5) {
                System.out.println("    ... 還有 " + (components.size() - 5) + " 個分量");
            }
            
            System.out.println("\n  💡 建議: 檢查OSM文件是否完整，或者調整起點選擇策略");
        } else {
            System.out.println("  ✓ 網絡完全連通");
        }
    }
    
    private void printDegreeDistribution() {
        System.out.println("\n【節點度數分布】");
        
        Map<Integer, Integer> degreeCount = new HashMap<>();
        int maxDegree = 0;
        
        for (Node node : network.getAllNodes()) {
            int degree = network.getEdges(node.getId()).size();
            degreeCount.put(degree, degreeCount.getOrDefault(degree, 0) + 1);
            maxDegree = Math.max(maxDegree, degree);
        }
        
        System.out.println("  度數範圍: 0 - " + maxDegree);
        System.out.println("  度數為0 (孤立): " + degreeCount.getOrDefault(0, 0) + " 個節點");
        System.out.println("  度數為1 (端點): " + degreeCount.getOrDefault(1, 0) + " 個節點");
        System.out.println("  度數為2 (普通): " + degreeCount.getOrDefault(2, 0) + " 個節點");
        
        int highDegree = 0;
        for (int d = 3; d <= maxDegree; d++) {
            highDegree += degreeCount.getOrDefault(d, 0);
        }
        System.out.println("  度數≥3 (交叉): " + highDegree + " 個節點");
    }
    
    private void printIsolatedNodes() {
        System.out.println("\n【孤立節點檢查】");
        
        List<Node> isolated = new ArrayList<>();
        for (Node node : network.getAllNodes()) {
            if (network.getEdges(node.getId()).isEmpty()) {
                isolated.add(node);
            }
        }
        
        if (isolated.isEmpty()) {
            System.out.println("  ✓ 沒有孤立節點");
        } else {
            System.out.println("  ⚠️ 發現 " + isolated.size() + " 個孤立節點");
            System.out.println("  前5個孤立節點:");
            for (int i = 0; i < Math.min(5, isolated.size()); i++) {
                Node node = isolated.get(i);
                System.out.println("    - " + node.getName() + " (ID: " + node.getId() + 
                                 ", 關鍵字: " + node.getKeywords() + ")");
            }
        }
    }
    
    /**
     * BFS尋找連通分量
     */
    private Set<Long> bfs(long startNodeId, Set<Long> visited) {
        Set<Long> component = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        
        queue.offer(startNodeId);
        visited.add(startNodeId);
        component.add(startNodeId);
        
        while (!queue.isEmpty()) {
            long nodeId = queue.poll();
            
            for (Edge edge : network.getEdges(nodeId)) {
                long neighborId = edge.getTo().getId();
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    component.add(neighborId);
                    queue.offer(neighborId);
                }
            }
        }
        
        return component;
    }
    
    /**
     * 檢查兩個節點是否在同一連通分量中
     */
    public boolean areConnected(Node from, Node to) {
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        
        queue.offer(from.getId());
        visited.add(from.getId());
        
        while (!queue.isEmpty()) {
            long nodeId = queue.poll();
            
            if (nodeId == to.getId()) {
                return true;
            }
            
            for (Edge edge : network.getEdges(nodeId)) {
                long neighborId = edge.getTo().getId();
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    queue.offer(neighborId);
                }
            }
        }
        
        return false;
    }
    
    /**
     * 找出最大連通分量中的所有節點
     */
    public Set<Node> getLargestComponent() {
        Set<Long> visited = new HashSet<>();
        Set<Long> largestComponent = new HashSet<>();
        
        for (Node node : network.getAllNodes()) {
            if (!visited.contains(node.getId())) {
                Set<Long> component = bfs(node.getId(), visited);
                if (component.size() > largestComponent.size()) {
                    largestComponent = component;
                }
            }
        }
        
        Set<Node> result = new HashSet<>();
        for (Long nodeId : largestComponent) {
            result.add(network.getNode(nodeId));
        }
        return result;
    }
}
