package crs.model;

import java.util.*;

/**
 * 道路網路圖 G = (V, E)
 * 論文 Section 2 的問題定義
 */
public class RoadNetwork {
    private final Map<Long, Node> nodes;                    // V - 頂點集合
    private final Map<Long, List<Edge>> adjacencyList;      // E - 邊集合（鄰接表）
    private final Map<String, Set<Long>> keywordIndex;      // 關鍵字 -> 節點集合的索引
    
    public RoadNetwork() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.keywordIndex = new HashMap<>();
    }
    
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        
        // 建立關鍵字索引
        for (String keyword : node.getKeywords()) {
            keywordIndex.computeIfAbsent(keyword, k -> new HashSet<>()).add(node.getId());
        }
    }
    
    public void addEdge(Node from, Node to, double weight, String wayName, String wayType) {
        Edge edge = new Edge(from, to, weight, wayName, wayType);
        adjacencyList.computeIfAbsent(from.getId(), k -> new ArrayList<>()).add(edge);
    }
    
    public void addBidirectionalEdge(Node from, Node to, double weight, String wayName, String wayType) {
        addEdge(from, to, weight, wayName, wayType);
        addEdge(to, from, weight, wayName, wayType);
    }
    
    public Node getNode(long id) {
        return nodes.get(id);
    }
    
    public List<Edge> getEdges(long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }
    
    public Set<Long> getNodesByKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }
    
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }
    
    public int getNodeCount() {
        return nodes.size();
    }
    
    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }
    
    /**
     * 使用 Haversine 公式計算兩點間的地理距離（公尺）
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // 地球半徑（公尺）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    public static double calculateDistance(Node n1, Node n2) {
        return calculateDistance(n1.getLat(), n1.getLon(), n2.getLat(), n2.getLon());
    }
    
    /**
     * 計算網路距離 dG(u, v) - 使用 Dijkstra 算法
     */
    public double getNetworkDistance(long fromId, long toId) {
        if (fromId == toId) return 0;
        
        Map<Long, Double> dist = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
        
        dist.put(fromId, 0.0);
        pq.offer(new long[]{fromId, Double.doubleToLongBits(0.0)});
        
        while (!pq.isEmpty()) {
            long[] curr = pq.poll();
            long u = curr[0];
            double d = Double.longBitsToDouble(curr[1]);
            
            if (u == toId) return d;
            if (d > dist.getOrDefault(u, Double.MAX_VALUE)) continue;
            
            for (Edge edge : getEdges(u)) {
                long v = edge.getTo().getId();
                double newDist = d + edge.getWeight();
                if (newDist < dist.getOrDefault(v, Double.MAX_VALUE)) {
                    dist.put(v, newDist);
                    pq.offer(new long[]{v, Double.doubleToLongBits(newDist)});
                }
            }
        }
        
        return Double.MAX_VALUE; // 不可達
    }
    
    @Override
    public String toString() {
        return String.format("RoadNetwork[nodes=%d, edges=%d, keywords=%d]", 
            getNodeCount(), getEdgeCount(), keywordIndex.size());
    }
}
