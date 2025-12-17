package crs.model;

/**
 * 線索 (Clue) - 論文 Definition 1
 * m(w, d, ε)
 */
public class Clue {
    private final String keyword;
    private final double distance;
    private final double epsilon;
    
    public Clue(String keyword, double distance, double epsilon) {
        this.keyword = keyword.toLowerCase();
        this.distance = distance;
        this.epsilon = epsilon;
    }
    
    public double getMinDistance() { return distance * (1 - epsilon); }
    public double getMaxDistance() { return distance * (1 + epsilon); }
    
    public boolean isDistanceInRange(double actualDistance) {
        return actualDistance >= getMinDistance() && actualDistance <= getMaxDistance();
    }
    
    /**
     * 計算匹配距離 - 論文 Equation (1)
     * dm(m, s) = |dG(u,v) - d| / (ε * d)
     */
    public double computeMatchingDistance(double actualDistance) {
        if (epsilon == 0) return actualDistance == distance ? 0 : Double.MAX_VALUE;
        return Math.abs(actualDistance - distance) / (epsilon * distance);
    }
    
    public String getKeyword() { return keyword; }
    public double getDistance() { return distance; }
    public double getEpsilon() { return epsilon; }
    
    @Override
    public String toString() {
        return String.format("Clue(w=%s, d=%.0fm, ε=%.2f)", keyword, distance, epsilon);
    }
}
