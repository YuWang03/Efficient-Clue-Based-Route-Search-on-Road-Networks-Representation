package pbtree.model;

import java.util.*;

/**
 * Represents a label entry in the 2-hop label index.
 * Each entry is (pivot, distance) pair where:
 * - pivot (o): the pivot vertex ID
 * - distance (h_{v,o}): network distance from vertex v to pivot o
 */
public class LabelEntry implements Comparable<LabelEntry> {
    private final long pivot;      // o: pivot vertex ID
    private final double distance; // h_{v,o}: distance from vertex to pivot
    
    public LabelEntry(long pivot, double distance) {
        this.pivot = pivot;
        this.distance = distance;
    }
    
    public long getPivot() { return pivot; }
    public double getDistance() { return distance; }
    
    @Override
    public int compareTo(LabelEntry other) {
        // Sort by distance in ascending order
        int cmp = Double.compare(this.distance, other.distance);
        if (cmp == 0) {
            cmp = Long.compare(this.pivot, other.pivot);
        }
        return cmp;
    }
    
    @Override
    public String toString() {
        return String.format("(%d, %.2f)", pivot, distance);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelEntry that = (LabelEntry) o;
        return pivot == that.pivot && Double.compare(that.distance, distance) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pivot, distance);
    }
}
