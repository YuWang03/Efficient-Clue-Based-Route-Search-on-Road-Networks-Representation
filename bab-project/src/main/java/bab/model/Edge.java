package bab.model;

/**
 * Edge in the road network
 * Represents a directed edge from one node to another with weight (distance)
 */
public class Edge {
    private final long from;
    private final long to;
    private final double weight;  // distance in meters

    public Edge(long from, long to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return String.format("Edge(%d -> %d, %.2fm)", from, to, weight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return from == edge.from && to == edge.to;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(from) * 31 + Long.hashCode(to);
    }
}
