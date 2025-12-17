package crs.model;

import java.util.*;

/**
 * 道路網路中的節點（頂點）
 * 對應論文中的 vertex v ∈ V
 */
public class Node {
    private final long id;
    private final double lat;
    private final double lon;
    private final Set<String> keywords;
    private final Map<String, String> tags;
    
    public Node(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.keywords = new HashSet<>();
        this.tags = new HashMap<>();
    }
    
    public void addTag(String key, String value) {
        tags.put(key, value);
        if (key.equals("name") || key.equals("amenity") || key.equals("shop") || 
            key.equals("highway") || key.equals("cuisine") || key.equals("tourism")) {
            keywords.add(value.toLowerCase());
        }
    }
    
    public void addKeyword(String keyword) {
        keywords.add(keyword.toLowerCase());
    }
    
    public boolean containsKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }
    
    public long getId() { return id; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public Set<String> getKeywords() { return Collections.unmodifiableSet(keywords); }
    public Map<String, String> getTags() { return Collections.unmodifiableMap(tags); }
    public String getName() { return tags.getOrDefault("name", "Node-" + id); }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return id == ((Node) o).id;
    }
    
    @Override
    public int hashCode() { return Long.hashCode(id); }
    
    @Override
    public String toString() {
        return String.format("Node[%d](%s)", id, getName());
    }
}
