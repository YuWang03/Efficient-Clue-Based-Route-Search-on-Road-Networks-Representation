package crs.model;

/**
 * 道路網路中的邊
 */
public class Edge {
    private final Node from;
    private final Node to;
    private final double weight;
    private final String wayName;
    
    public Edge(Node from, Node to, double weight, String wayName) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.wayName = wayName;
    }
    
    public Node getFrom() { return from; }
    public Node getTo() { return to; }
    public double getWeight() { return weight; }
    public String getWayName() { return wayName; }
}
