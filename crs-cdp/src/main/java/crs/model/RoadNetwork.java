package crs.model;

import java.util.*;

/**
 * 道路網路圖 G = (V, E)
 */
public class RoadNetwork {
    private final Map<Long, Node> nodes = new HashMap<>();
    private final Map<Long, List<Edge>> adjacency = new HashMap<>();
    private final Map<String, Set<Long>> keywordIndex = new HashMap<>();
    
    // 距離緩存 - 用於 CDP 算法
    private final Map<String, Double> distanceCache = new HashMap<>();
    // 路徑緩存 - 儲存實際路徑節點序列
    private final Map<String, List<Long>> pathCache = new HashMap<>();
    
    /**
     * 路徑結果類 - 包含距離和完整路徑
     */
    public static class PathResult {
        public final double distance;
        public final List<Node> path;
        
        public PathResult(double distance, List<Node> path) {
            this.distance = distance;
            this.path = path;
        }
        
        public boolean isValid() {
            return distance < Double.MAX_VALUE && !path.isEmpty();
        }
    }
    
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        for (String kw : node.getKeywords()) {
            keywordIndex.computeIfAbsent(kw, k -> new HashSet<>()).add(node.getId());
        }
    }
    
    /**
     * 重新建立關鍵字索引
     * 當節點的關鍵字在添加到network後被修改時需要調用此方法
     */
    public void rebuildKeywordIndex() {
        keywordIndex.clear();
        for (Node node : nodes.values()) {
            for (String kw : node.getKeywords()) {
                keywordIndex.computeIfAbsent(kw, k -> new HashSet<>()).add(node.getId());
            }
        }
    }
    
    public void addEdge(Node from, Node to, double weight, String wayName) {
        adjacency.computeIfAbsent(from.getId(), k -> new ArrayList<>())
                 .add(new Edge(from, to, weight, wayName));
    }
    
    public void addBidirectionalEdge(Node n1, Node n2, double weight, String wayName) {
        addEdge(n1, n2, weight, wayName);
        addEdge(n2, n1, weight, wayName);
    }
    
    public Node getNode(long id) { return nodes.get(id); }
    public List<Edge> getEdges(long nodeId) { 
        return adjacency.getOrDefault(nodeId, Collections.emptyList()); 
    }
    public Collection<Node> getAllNodes() { return nodes.values(); }
    public int getNodeCount() { return nodes.size(); }
    public int getEdgeCount() { 
        return adjacency.values().stream().mapToInt(List::size).sum(); 
    }
    
    /**
     * 取得包含特定關鍵字的所有節點ID - 返回Long集合
     */
    public Set<Long> getNodesByKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }
    
    /**
     * 取得包含特定關鍵字的所有節點對象 - 返回Node集合
     */
    public Set<Node> getNodeObjectsByKeyword(String keyword) {
        Set<Long> ids = keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
        Set<Node> result = new HashSet<>();
        for (Long id : ids) {
            Node node = nodes.get(id);
            if (node != null) result.add(node);
        }
        return result;
    }
    
    /**
     * 取得包含特定關鍵字的所有節點ID - 返回Long集合
     */
    public Set<Long> getNodeIdsByKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }
    }
    
    /**
     * 計算網路距離 dG(u, v) - 使用 Dijkstra 算法 (帶緩存)
     * 只返回距離值，用於快速查詢
     */
    public double getNetworkDistance(Node from, Node to) {
        if (from.getId() == to.getId()) return 0;
        
        String key = from.getId() + "-" + to.getId();
        if (distanceCache.containsKey(key)) {
            return distanceCache.get(key);
        }
        
        PathResult result = computeShortestPath(from, to);
        distanceCache.put(key, result.distance);
        return result.distance;
    }
    
    /**
     * 計算最短路徑 - 返回距離和完整路徑
     * 使用 Dijkstra 算法，確保路徑沿著實際道路網絡
     */
    public PathResult computeShortestPath(Node from, Node to) {
        if (from.getId() == to.getId()) {
            return new PathResult(0, List.of(from));
        }
        
        // 檢查起點和終點的連接性
        List<Edge> fromEdges = getEdges(from.getId());
        
        if (fromEdges.isEmpty()) {
            System.err.println("警告: 起點 " + from.getName() + " (" + from.getId() + ") 無出邊");
            return new PathResult(Double.MAX_VALUE, Collections.emptyList());
        }
        
        // 檢查緩存
        String key = from.getId() + "-" + to.getId();
        if (distanceCache.containsKey(key) && pathCache.containsKey(key)) {
            List<Node> cachedPath = new ArrayList<>();
            for (Long nodeId : pathCache.get(key)) {
                cachedPath.add(nodes.get(nodeId));
            }
            return new PathResult(distanceCache.get(key), cachedPath);
        }
        
        // Dijkstra 算法
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        Set<Long> visited = new HashSet<>();
        
        dist.put(from.getId(), 0.0);
        pq.offer(new double[]{0.0, from.getId()});
        
        while (!pq.isEmpty()) {
            double[] curr = pq.poll();
            double d = curr[0];
            long u = (long) curr[1];
            
            // 找到目標節點
            if (u == to.getId()) {
                // 重建路徑
                List<Long> pathIds = new ArrayList<>();
                List<Node> path = new ArrayList<>();
                long current = to.getId();
                
                while (current != from.getId()) {
                    pathIds.add(0, current);
                    path.add(0, nodes.get(current));
                    current = previous.get(current);
                }
                pathIds.add(0, from.getId());
                path.add(0, from);
                
                // 緩存結果
                distanceCache.put(key, d);
                pathCache.put(key, pathIds);
                
                return new PathResult(d, path);
            }
            
            if (visited.contains(u)) continue;
            visited.add(u);
            
            if (d > dist.getOrDefault(u, Double.MAX_VALUE)) continue;
            
            // 探索鄰居節點
            for (Edge edge : getEdges(u)) {
                long v = edge.getTo().getId();
                double newDist = d + edge.getWeight();
                
                if (newDist < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, newDist);
                    previous.put(v, u);
                    pq.offer(new double[]{newDist, v});
                }
            }
        }
        
        // 無法到達
        distanceCache.put(key, Double.MAX_VALUE);
        return new PathResult(Double.MAX_VALUE, Collections.emptyList());
    }
    
    /**
     * 清除距離和路徑緩存
     */
    public void clearCache() {
        distanceCache.clear();
        pathCache.clear();
    }
    
    /**
     * Haversine 公式計算地理距離（公尺）
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
    
    public static double haversineDistance(Node n1, Node n2) {
        return haversineDistance(n1.getLat(), n1.getLon(), n2.getLat(), n2.getLon());
    }
}
