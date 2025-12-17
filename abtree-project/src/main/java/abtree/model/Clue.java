package abtree.model;

/**
 * Represents a clue m(w, d, ε) in the query.
 * - w: keyword to match
 * - d: expected distance in meters
 * - ε: confidence interval (0 < ε ≤ 1)
 */
public class Clue {
    private final String keyword;      // w
    private final double distance;     // d (meters)
    private final double epsilon;      // ε
    
    public Clue(String keyword, double distance, double epsilon) {
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be null or empty");
        }
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (epsilon <= 0 || epsilon > 1) {
            throw new IllegalArgumentException("Epsilon must be in (0, 1]");
        }
        this.keyword = keyword.toLowerCase();
        this.distance = distance;
        this.epsilon = epsilon;
    }
    
    public String getKeyword() { return keyword; }
    public double getDistance() { return distance; }
    public double getEpsilon() { return epsilon; }
    
    // lD = d - d·ε (minimum distance in confidence interval)
    public double getMinDistance() {
        return distance * (1 - epsilon);
    }
    
    // rD = d + d·ε (maximum distance in confidence interval)
    public double getMaxDistance() {
        return distance * (1 + epsilon);
    }
    
    // Check if a network distance is within confidence interval
    public boolean isWithinConfidenceInterval(double networkDistance) {
        return networkDistance >= getMinDistance() && networkDistance <= getMaxDistance();
    }
    
    @Override
    public String toString() {
        return String.format("m(%s, %.0fm, ε=%.2f) [%.0f-%.0f]", 
            keyword, distance, epsilon, getMinDistance(), getMaxDistance());
    }
}
