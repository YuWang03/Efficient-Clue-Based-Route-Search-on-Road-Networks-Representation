package bab.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Node in the road network
 * Represents a vertex v with coordinates and associated keywords
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

    public long getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            this.keywords.add(keyword.toLowerCase());
        }
    }

    public boolean hasKeyword(String keyword) {
        return keywords.contains(keyword.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("Node(%d, %.6f, %.6f, keywords=%s)", id, lat, lon, keywords);
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
