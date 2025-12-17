package abtree.model;

import java.util.*;

/**
 * Represents the road network graph G = (V, E).
 * Provides efficient graph operations and shortest path calculation.
 */
public class RoadNetwork {
    private final Map<Long, Node> nodes;
    private final Map<Long, List<Edge>> adjacencyList;
    private final Map<String, Set<Long>> keywordIndex;  // keyword → node IDs
    private final Map<String, Double> distanceCache;    // "from-to" → distance
    
    public RoadNetwork() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.keywordIndex = new HashMap<>();
        this.distanceCache = new HashMap<>();
    }
    
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        
        // Index keywords
        for (String keyword : node.getKeywords()) {
            keywordIndex.computeIfAbsent(keyword, k -> new HashSet<>()).add(node.getId());
        }
    }
    
    public void addEdge(Edge edge) {
        adjacencyList.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge);
    }
    
    public Node getNode(long id) { return nodes.get(id); }
    public Map<Long, Node> getNodes() { return nodes; }
    public List<Edge> getEdges(long nodeId) { return adjacencyList.getOrDefault(nodeId, Collections.emptyList()); }
    public Map<Long, List<Edge>> getAdjacencyList() { return adjacencyList; }
    public Set<Long> getNodesWithKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }
    public Map<String, Set<Long>> getKeywordIndex() { return keywordIndex; }
    
    /**
     * Calculate Haversine distance between two coordinates (meters).
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
    
    /**
     * Get network distance using Dijkstra's algorithm with caching.
     */
    public double getNetworkDistance(long from, long to) {
        if (from == to) return 0;
        
        String cacheKey = from + "-" + to;
        if (distanceCache.containsKey(cacheKey)) {
            return distanceCache.get(cacheKey);
        }
        
        // Dijkstra's algorithm
        Map<Long, Double> distances = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
        Set<Long> visited = new HashSet<>();
        
        distances.put(from, 0.0);
        pq.offer(new long[]{from, Double.doubleToLongBits(0.0)});
        
        while (!pq.isEmpty()) {
            long[] current = pq.poll();
            long node = current[0];
            double dist = Double.longBitsToDouble(current[1]);
            
            if (node == to) {
                distanceCache.put(cacheKey, dist);
                distanceCache.put(to + "-" + from, dist);
                return dist;
            }
            
            if (visited.contains(node)) continue;
            visited.add(node);
            
            for (Edge edge : getEdges(node)) {
                if (!visited.contains(edge.getTo())) {
                    double newDist = dist + edge.getWeight();
                    if (newDist < distances.getOrDefault(edge.getTo(), Double.MAX_VALUE)) {
                        distances.put(edge.getTo(), newDist);
                        pq.offer(new long[]{edge.getTo(), Double.doubleToLongBits(newDist)});
                    }
                }
            }
        }
        
        distanceCache.put(cacheKey, Double.MAX_VALUE);
        return Double.MAX_VALUE;
    }
    
    /**
     * Compute all distances from a source vertex (for building AB-Tree).
     * Returns map: vertex ID → distance from source
     */
    public Map<Long, Double> computeAllDistancesFrom(long source) {
        Map<Long, Double> distances = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1])));
        Set<Long> visited = new HashSet<>();
        
        distances.put(source, 0.0);
        pq.offer(new long[]{source, Double.doubleToLongBits(0.0)});
        
        while (!pq.isEmpty()) {
            long[] current = pq.poll();
            long node = current[0];
            double dist = Double.longBitsToDouble(current[1]);
            
            if (visited.contains(node)) continue;
            visited.add(node);
            distances.put(node, dist);
            
            // Cache distance
            String cacheKey = source + "-" + node;
            distanceCache.put(cacheKey, dist);
            distanceCache.put(node + "-" + source, dist);
            
            for (Edge edge : getEdges(node)) {
                if (!visited.contains(edge.getTo())) {
                    double newDist = dist + edge.getWeight();
                    if (newDist < distances.getOrDefault(edge.getTo(), Double.MAX_VALUE)) {
                        distances.put(edge.getTo(), newDist);
                        pq.offer(new long[]{edge.getTo(), Double.doubleToLongBits(newDist)});
                    }
                }
            }
        }
        
        return distances;
    }
    
    /**
     * Remove isolated nodes (nodes with no edges).
     */
    public void removeIsolatedNodes() {
        Set<Long> connectedNodes = new HashSet<>();
        for (Map.Entry<Long, List<Edge>> entry : adjacencyList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                connectedNodes.add(entry.getKey());
                for (Edge e : entry.getValue()) {
                    connectedNodes.add(e.getTo());
                }
            }
        }
        nodes.keySet().retainAll(connectedNodes);
        adjacencyList.keySet().retainAll(connectedNodes);
    }
    
    public int getNodeCount() { return nodes.size(); }
    public int getEdgeCount() {
        return adjacencyList.values().stream().mapToInt(List::size).sum();
    }
    
    public void printStatistics() {
        System.out.println("=== Road Network Statistics ===");
        System.out.println("Nodes: " + getNodeCount());
        System.out.println("Edges: " + getEdgeCount());
        System.out.println("Keywords: " + keywordIndex.size());
        
        System.out.println("\nTop keywords:");
        keywordIndex.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
            .limit(10)
            .forEach(e -> System.out.println("  " + e.getKey() + ": " + e.getValue().size() + " nodes"));
    }
}
