package crs.model;

/**
 * 線索 (Clue) - 論文 Definition 1
 * m(w, d, ε) 其中:
 *   w = 查詢關鍵字
 *   d = 使用者預期的距離
 *   ε = 信心因子 ∈ [0, 1]
 */
public class Clue {
    private final String keyword;      // w
    private final double distance;     // d (單位：公尺)
    private final double epsilon;      // ε
    
    public Clue(String keyword, double distance, double epsilon) {
        if (epsilon < 0 || epsilon > 1) {
            throw new IllegalArgumentException("Epsilon must be in [0, 1]");
        }
        this.keyword = keyword.toLowerCase();
        this.distance = distance;
        this.epsilon = epsilon;
    }
    
    /**
     * 計算距離的有效區間 [d(1-ε), d(1+ε)]
     */
    public double getMinDistance() {
        return distance * (1 - epsilon);
    }
    
    public double getMaxDistance() {
        return distance * (1 + epsilon);
    }
    
    /**
     * 檢查實際距離是否在有效區間內
     */
    public boolean isDistanceInRange(double actualDistance) {
        return actualDistance >= getMinDistance() && actualDistance <= getMaxDistance();
    }
    
    /**
     * 計算匹配距離 (Matching Distance) - 論文 Equation (1)
     * dm(m, s) = |dG(u,v) - d| / (ε * d)
     */
    public double computeMatchingDistance(double actualDistance) {
        if (epsilon == 0) {
            return actualDistance == distance ? 0 : Double.MAX_VALUE;
        }
        return Math.abs(actualDistance - distance) / (epsilon * distance);
    }
    
    // Getters
    public String getKeyword() { return keyword; }
    public double getDistance() { return distance; }
    public double getEpsilon() { return epsilon; }
    
    @Override
    public String toString() {
        return String.format("Clue(w=%s, d=%.1fm, ε=%.2f) range=[%.1f, %.1f]", 
            keyword, distance, epsilon, getMinDistance(), getMaxDistance());
    }
}
