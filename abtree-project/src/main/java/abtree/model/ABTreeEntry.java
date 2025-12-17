package abtree.model;

import java.util.*;

/**
 * Represents an entry in the AB-Tree.
 * Each entry contains (distance, vertex, keywords) tuple.
 * Entries are sorted by distance (d_p).
 */
public class ABTreeEntry implements Comparable<ABTreeEntry> {
    private final double distance;      // d_p: network distance from source
    private final long vertexId;        // v_p: vertex ID
    private final Set<String> keywords; // H(v_p): keywords at vertex
    
    public ABTreeEntry(double distance, long vertexId, Set<String> keywords) {
        this.distance = distance;
        this.vertexId = vertexId;
        this.keywords = new HashSet<>(keywords);
    }
    
    public double getDistance() { return distance; }
    public long getVertexId() { return vertexId; }
    public Set<String> getKeywords() { return keywords; }
    
    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }
    
    @Override
    public int compareTo(ABTreeEntry other) {
        int cmp = Double.compare(this.distance, other.distance);
        if (cmp == 0) {
            cmp = Long.compare(this.vertexId, other.vertexId);
        }
        return cmp;
    }
    
    @Override
    public String toString() {
        return String.format("(%.2fm, v%d, %s)", distance, vertexId, keywords);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ABTreeEntry that = (ABTreeEntry) o;
        return Double.compare(that.distance, distance) == 0 && vertexId == that.vertexId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(distance, vertexId);
    }
}
