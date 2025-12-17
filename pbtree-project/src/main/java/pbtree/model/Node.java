package pbtree.model;

import java.util.*;

/**
 * Represents a vertex in the road network graph.
 */
public class Node {
    private final long id;
    private final double lat;
    private final double lon;
    private final Set<String> keywords;
    
    public Node(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.keywords = new HashSet<>();
    }
    
    public long getId() { return id; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public Set<String> getKeywords() { return keywords; }
    
    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            keywords.add(keyword.toLowerCase());
        }
    }
    
    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }
    
    @Override
    public String toString() {
        return "Node-" + id;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id == node.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
