package bab.model;

import java.util.*;

/**
 * Road Network G = (V, E)
 * 
 * Manages the graph structure with:
 * - Nodes (vertices) with coordinates and keywords
 * - Edges with weights (distances)
 * - Keyword index for efficient lookup
 * - Network distance calculation using Dijkstra's algorithm
 */
public class RoadNetwork {
    private final Map<Long, Node> nodes;
    private final Map<Long, List<Edge>> adjacencyList;
    private final Map<String, Set<Long>> keywordIndex;
    private final Map<String, Double> distanceCache;

    public RoadNetwork() {
        this.nodes = new HashMap<>();
        this.adjacencyList = new HashMap<>();
        this.keywordIndex = new HashMap<>();
        this.distanceCache = new HashMap<>();
    }

    // ==================== Node Operations ====================

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacencyList.putIfAbsent(node.getId(), new ArrayList<>());
        
        // Index keywords
        for (String keyword : node.getKeywords()) {
            keywordIndex.computeIfAbsent(keyword, k -> new HashSet<>()).add(node.getId());
        }
    }

    public Node getNode(long id) {
        return nodes.get(id);
    }

    public boolean hasNode(long id) {
        return nodes.containsKey(id);
    }

    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public int getNodeCount() {
        return nodes.size();
    }

    // ==================== Edge Operations ====================

    public void addEdge(Edge edge) {
        adjacencyList.computeIfAbsent(edge.getFrom(), k -> new ArrayList<>()).add(edge);
    }

    public void addBidirectionalEdge(long from, long to, double weight) {
        addEdge(new Edge(from, to, weight));
        addEdge(new Edge(to, from, weight));
    }

    public List<Edge> getOutgoingEdges(long nodeId) {
        return adjacencyList.getOrDefault(nodeId, Collections.emptyList());
    }

    public int getEdgeCount() {
        int count = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count / 2;  // Bidirectional edges counted once
    }

    // ==================== Keyword Index ====================

    public void indexNodeKeyword(long nodeId, String keyword) {
        String kw = keyword.toLowerCase();
        keywordIndex.computeIfAbsent(kw, k -> new HashSet<>()).add(nodeId);
        
        Node node = nodes.get(nodeId);
        if (node != null) {
            node.addKeyword(kw);
        }
    }

    public Set<Long> getNodesWithKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }

    public Set<String> getAllKeywords() {
        return Collections.unmodifiableSet(keywordIndex.keySet());
    }

    public int getKeywordCount() {
        return keywordIndex.size();
    }

    public Map<String, Integer> getKeywordStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Set<Long>> entry : keywordIndex.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }

    // ==================== Distance Calculation ====================

    /**
     * Calculate Haversine distance between two coordinates
     * @return distance in meters
     */
    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Earth's radius in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Get network distance using Dijkstra's algorithm with caching
     * dG(u, v) in the paper notation
     * 
     * @return shortest path distance in meters, or Double.MAX_VALUE if unreachable
     */
    public double getNetworkDistance(long from, long to) {
        if (from == to) return 0;

        String key = from + "-" + to;
        if (distanceCache.containsKey(key)) {
            return distanceCache.get(key);
        }

        // Dijkstra's algorithm with priority queue
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        Map<Long, Double> distances = new HashMap<>();
        Set<Long> visited = new HashSet<>();

        pq.offer(new double[]{0, from});
        distances.put(from, 0.0);

        while (!pq.isEmpty()) {
            double[] current = pq.poll();
            double dist = current[0];
            long node = (long) current[1];

            if (node == to) {
                distanceCache.put(key, dist);
                distanceCache.put(to + "-" + from, dist);  // Symmetric caching
                return dist;
            }

            if (visited.contains(node)) continue;
            visited.add(node);

            for (Edge edge : getOutgoingEdges(node)) {
                if (!visited.contains(edge.getTo())) {
                    double newDist = dist + edge.getWeight();
                    if (newDist < distances.getOrDefault(edge.getTo(), Double.MAX_VALUE)) {
                        distances.put(edge.getTo(), newDist);
                        pq.offer(new double[]{newDist, edge.getTo()});
                    }
                }
            }
        }

        distanceCache.put(key, Double.MAX_VALUE);
        return Double.MAX_VALUE;
    }

    /**
     * Clear distance cache (useful for testing or memory management)
     */
    public void clearDistanceCache() {
        distanceCache.clear();
    }

    public int getCacheSize() {
        return distanceCache.size();
    }

    // ==================== Graph Connectivity ====================

    /**
     * Remove isolated nodes (nodes with no edges)
     */
    public void removeIsolatedNodes() {
        Set<Long> connectedNodes = new HashSet<>();
        for (Map.Entry<Long, List<Edge>> entry : adjacencyList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                connectedNodes.add(entry.getKey());
                for (Edge edge : entry.getValue()) {
                    connectedNodes.add(edge.getTo());
                }
            }
        }
        nodes.keySet().retainAll(connectedNodes);
    }

    /**
     * Get a list of nodes that have outgoing edges
     */
    public List<Long> getConnectedNodeIds() {
        List<Long> connected = new ArrayList<>();
        for (Map.Entry<Long, List<Edge>> entry : adjacencyList.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                connected.add(entry.getKey());
            }
        }
        return connected;
    }

    // ==================== Statistics ====================

    @Override
    public String toString() {
        return String.format("RoadNetwork(nodes=%d, edges=%d, keywords=%d)", 
                             getNodeCount(), getEdgeCount(), getKeywordCount());
    }

    public void printStatistics() {
        System.out.println("=== Road Network Statistics ===");
        System.out.println("Nodes: " + getNodeCount());
        System.out.println("Edges: " + getEdgeCount());
        System.out.println("Keywords: " + getKeywordCount());
        System.out.println("Distance cache entries: " + getCacheSize());
    }

    public void printTopKeywords(int limit) {
        System.out.println("\n=== Top " + limit + " Keywords ===");
        getKeywordStatistics().entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .forEach(e -> System.out.printf("  %s: %d nodes%n", e.getKey(), e.getValue()));
    }
}
