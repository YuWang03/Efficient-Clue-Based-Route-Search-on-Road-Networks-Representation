package pbtree.algorithm;

import java.util.*;
import pbtree.model.*;

/**
 * 2-Hop Label Index for efficient distance queries.
 * 
 * Based on the paper's description:
 * - L(v): label of vertex v, containing (pivot, distance) pairs
 * - Distance between u and v: min{h_{u,o} + h_{o,v}} for common pivot o
 * 
 * This implementation uses a simplified approach where high-degree vertices
 * are chosen as pivots for the hierarchical label.
 */
public class TwoHopLabel {
    private final RoadNetwork network;
    private final Map<Long, List<LabelEntry>> labels;  // L(v) for each vertex v
    private final List<Long> pivotOrder;               // Ordered list of pivots
    private long constructionTimeMs;
    
    public TwoHopLabel(RoadNetwork network) {
        this.network = network;
        this.labels = new HashMap<>();
        this.pivotOrder = new ArrayList<>();
    }
    
    /**
     * Build the 2-hop label index for the network.
     * Uses a degree-based ordering for pivot selection.
     */
    public void build() {
        long startTime = System.currentTimeMillis();
        System.out.println("Building 2-hop label index...");
        
        // Step 1: Order vertices by degree (higher degree = higher priority)
        List<Long> vertices = new ArrayList<>(network.getAllNodeIds());
        vertices.sort((a, b) -> {
            int degA = network.getEdges(a).size();
            int degB = network.getEdges(b).size();
            if (degA != degB) return Integer.compare(degB, degA);
            return Long.compare(a, b);
        });
        pivotOrder.addAll(vertices);
        
        // Step 2: Initialize empty labels
        for (long v : vertices) {
            labels.put(v, new ArrayList<>());
        }
        
        // Step 3: Build labels using pruned BFS from each vertex in order
        int processed = 0;
        for (long pivot : pivotOrder) {
            buildLabelFromPivot(pivot);
            processed++;
            if (processed % 100 == 0) {
                System.out.println("  Processed " + processed + "/" + vertices.size() + " pivots");
            }
        }
        
        // Step 4: Sort labels by distance
        for (List<LabelEntry> label : labels.values()) {
            label.sort(Comparator.comparingDouble(LabelEntry::getDistance));
        }
        
        constructionTimeMs = System.currentTimeMillis() - startTime;
        System.out.println("2-hop label built in " + constructionTimeMs + " ms");
        printStatistics();
    }
    
    /**
     * Build labels by running pruned BFS from a pivot vertex.
     */
    private void buildLabelFromPivot(long pivot) {
        int pivotRank = pivotOrder.indexOf(pivot);
        // pivotRank could be used for advanced query optimization in the future
        
        // Dijkstra from pivot
        Map<Long, Double> distances = new HashMap<>();
        PriorityQueue<long[]> pq = new PriorityQueue<>(
            Comparator.comparingDouble(a -> Double.longBitsToDouble(a[1]))
        );
        Set<Long> visited = new HashSet<>();
        
        distances.put(pivot, 0.0);
        pq.offer(new long[]{pivot, Double.doubleToLongBits(0.0)});
        
        while (!pq.isEmpty()) {
            long[] current = pq.poll();
            long node = current[0];
            double dist = Double.longBitsToDouble(current[1]);
            
            if (visited.contains(node)) continue;
            visited.add(node);
            
            // Pruning: check if existing labels already provide a shorter path
            if (node != pivot && queryDistance(pivot, node) <= dist) {
                continue; // Prune this path
            }
            
            // Add label entry: in L(node), add (pivot, dist)
            labels.get(node).add(new LabelEntry(pivot, dist));
            
            // Explore neighbors
            for (Edge edge : network.getEdges(node)) {
                long neighbor = edge.getTo();
                if (!visited.contains(neighbor)) {
                    double newDist = dist + edge.getWeight();
                    if (newDist < distances.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        distances.put(neighbor, newDist);
                        pq.offer(new long[]{neighbor, Double.doubleToLongBits(newDist)});
                    }
                }
            }
        }
    }
    
    /**
     * Query the distance between two vertices using labels.
     * d_G(u, v) = min{h_{u,o} + h_{o,v}} for common pivot o
     */
    public double queryDistance(long u, long v) {
        if (u == v) return 0;
        
        List<LabelEntry> labelU = labels.getOrDefault(u, Collections.emptyList());
        List<LabelEntry> labelV = labels.getOrDefault(v, Collections.emptyList());
        
        if (labelU.isEmpty() || labelV.isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        // Find minimum distance through common pivot
        double minDist = Double.MAX_VALUE;
        
        // Use two-pointer merge since labels are sorted by pivot order
        Map<Long, Double> pivotDistU = new HashMap<>();
        for (LabelEntry e : labelU) {
            pivotDistU.put(e.getPivot(), e.getDistance());
        }
        
        for (LabelEntry e : labelV) {
            Double distU = pivotDistU.get(e.getPivot());
            if (distU != null) {
                double totalDist = distU + e.getDistance();
                if (totalDist < minDist) {
                    minDist = totalDist;
                }
            }
        }
        
        return minDist;
    }
    
    /**
     * Check if pivot o is on the shortest path between u and v.
     * Returns true if d_G(u,v) == d_G(u,o) + d_G(o,v)
     */
    public boolean isOnShortestPath(long u, long v, long pivot) {
        double duv = queryDistance(u, v);
        double duo = queryDistance(u, pivot);
        double dov = queryDistance(pivot, v);
        
        // Allow small floating point tolerance
        return Math.abs(duv - (duo + dov)) < 0.001;
    }
    
    /**
     * Get the label for a vertex.
     */
    public List<LabelEntry> getLabel(long vertex) {
        return labels.getOrDefault(vertex, Collections.emptyList());
    }
    
    /**
     * Get all pivots in order.
     */
    public List<Long> getPivotOrder() {
        return pivotOrder;
    }
    
    /**
     * Get total label size (sum of all label entries).
     */
    public int getTotalLabelSize() {
        int size = 0;
        for (List<LabelEntry> label : labels.values()) {
            size += label.size();
        }
        return size;
    }
    
    /**
     * Get construction time.
     */
    public long getConstructionTimeMs() {
        return constructionTimeMs;
    }
    
    /**
     * Print statistics about the label index.
     */
    public void printStatistics() {
        int totalSize = getTotalLabelSize();
        int numVertices = labels.size();
        double avgLabelSize = numVertices > 0 ? (double) totalSize / numVertices : 0;
        
        int maxLabelSize = 0;
        int minLabelSize = Integer.MAX_VALUE;
        for (List<LabelEntry> label : labels.values()) {
            maxLabelSize = Math.max(maxLabelSize, label.size());
            minLabelSize = Math.min(minLabelSize, label.size());
        }
        
        System.out.println("\n=== 2-Hop Label Statistics ===");
        System.out.println("Vertices: " + numVertices);
        System.out.println("Total label entries: " + totalSize);
        System.out.println("Average label size: " + String.format("%.2f", avgLabelSize));
        System.out.println("Max label size: " + maxLabelSize);
        System.out.println("Min label size: " + minLabelSize);
    }
}
