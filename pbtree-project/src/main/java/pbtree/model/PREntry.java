package pbtree.model;

import java.util.*;

/**
 * Represents an entry in the Pivot Reverse (PR) index and PB-Tree.
 * Each entry is (vertex, distance, keywords) tuple stored in PR(o) for pivot o.
 * 
 * In PR(o), we store all (v, h_{v,o}) from vertices v that have o as a pivot.
 * This is the inverse of the 2-hop label structure.
 */
public class PREntry implements Comparable<PREntry> {
    private final long vertexId;        // v: the vertex that reaches through this pivot
    private final double distance;      // h_{v,o}: distance from vertex v to pivot o
    private final Set<String> keywords; // H(v): keywords at vertex v
    
    public PREntry(long vertexId, double distance, Set<String> keywords) {
        this.vertexId = vertexId;
        this.distance = distance;
        this.keywords = keywords != null ? new HashSet<>(keywords) : new HashSet<>();
    }
    
    public long getVertexId() { return vertexId; }
    public double getDistance() { return distance; }
    public Set<String> getKeywords() { return keywords; }
    
    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }
    
    @Override
    public int compareTo(PREntry other) {
        // Sort by distance in ascending order
        int cmp = Double.compare(this.distance, other.distance);
        if (cmp == 0) {
            cmp = Long.compare(this.vertexId, other.vertexId);
        }
        return cmp;
    }
    
    @Override
    public String toString() {
        return String.format("(v%d, %.2fm, %s)", vertexId, distance, keywords);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PREntry that = (PREntry) o;
        return vertexId == that.vertexId && Double.compare(that.distance, distance) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(vertexId, distance);
    }
}
