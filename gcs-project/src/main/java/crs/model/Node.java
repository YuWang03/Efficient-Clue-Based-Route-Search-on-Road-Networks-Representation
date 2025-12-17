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
    private final Set<String> keywords;  // F(v) - 節點包含的關鍵字集合
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
        // 只將真正的 POI 標籤加入關鍵字集合（不包括道路名稱）
        // 這樣搜尋 cafe/restaurant 時只會找到真正的 POI，不會找到道路節點
        if (key.equals("amenity") || key.equals("shop") || 
            key.equals("tourism") || key.equals("leisure")) {
            keywords.add(value.toLowerCase());
        }
        // 處理 cuisine 標籤：coffee_shop -> cafe
        if (key.equals("cuisine")) {
            String cuisineValue = value.toLowerCase();
            if (cuisineValue.equals("coffee_shop")) {
                keywords.add("cafe");
            } else {
                keywords.add(cuisineValue);
            }
        }
    }
    
    public void addKeyword(String keyword) {
        keywords.add(keyword.toLowerCase());
    }
    
    public boolean containsKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }
    
    // Getters
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
        Node node = (Node) o;
        return id == node.id;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
    
    @Override
    public String toString() {
        return String.format("Node[%d](%.6f, %.6f) keywords=%s", id, lat, lon, keywords);
    }
}
