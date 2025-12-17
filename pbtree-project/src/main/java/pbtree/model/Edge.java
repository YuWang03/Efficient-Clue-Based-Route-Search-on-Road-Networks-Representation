package pbtree.model;

/**
 * Represents a directed edge in the road network.
 */
public class Edge {
    private final long from;
    private final long to;
    private final double weight; // distance in meters
    
    public Edge(long from, long to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }
    
    public long getFrom() { return from; }
    public long getTo() { return to; }
    public double getWeight() { return weight; }
    
    @Override
    public String toString() {
        return String.format("Edge(%d→%d, %.2fm)", from, to, weight);
    }
}
