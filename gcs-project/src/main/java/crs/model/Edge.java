package crs.model;

/**
 * 道路網路中的邊
 * 對應論文中的 edge (u,v) ∈ E
 */
public class Edge {
    private final Node from;
    private final Node to;
    private final double weight;  // e(u,v) - 邊的權重（距離，單位：公尺）
    private final String wayName;
    private final String wayType;
    
    public Edge(Node from, Node to, double weight, String wayName, String wayType) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.wayName = wayName;
        this.wayType = wayType;
    }
    
    public Node getFrom() { return from; }
    public Node getTo() { return to; }
    public double getWeight() { return weight; }
    public String getWayName() { return wayName; }
    public String getWayType() { return wayType; }
    
    @Override
    public String toString() {
        return String.format("Edge[%d -> %d, %.2fm, %s]", 
            from.getId(), to.getId(), weight, wayName);
    }
}
