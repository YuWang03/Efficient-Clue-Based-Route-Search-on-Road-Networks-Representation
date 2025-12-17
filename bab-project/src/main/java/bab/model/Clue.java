package bab.model;

/**
 * Clue m(w, d, ε) as defined in Definition 1
 * 
 * A clue represents a landmark description with:
 * - w: query keyword (e.g., "restaurant", "bank")
 * - d: user-defined distance in meters
 * - ε: confidence factor ∈ [0, 1]
 * 
 * The confidence interval is [d(1-ε), d(1+ε)]
 */
public class Clue {
    private final String keyword;    // w: query keyword
    private final double distance;   // d: user-defined distance (meters)
    private final double epsilon;    // ε: confidence factor ∈ [0, 1]

    public Clue(String keyword, double distance, double epsilon) {
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("Keyword cannot be null or empty");
        }
        if (distance <= 0) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        if (epsilon < 0 || epsilon > 1) {
            throw new IllegalArgumentException("Epsilon must be in range [0, 1]");
        }
        
        this.keyword = keyword.toLowerCase();
        this.distance = distance;
        this.epsilon = epsilon;
    }

    public String getKeyword() {
        return keyword;
    }

    public double getDistance() {
        return distance;
    }

    public double getEpsilon() {
        return epsilon;
    }

    /**
     * Get minimum distance in confidence interval: d(1-ε)
     */
    public double getMinDistance() {
        return distance * (1 - epsilon);
    }

    /**
     * Get maximum distance in confidence interval: d(1+ε)
     */
    public double getMaxDistance() {
        return distance * (1 + epsilon);
    }

    /**
     * Check if a network distance falls within the confidence interval
     */
    public boolean isWithinConfidenceInterval(double networkDistance) {
        return networkDistance >= getMinDistance() && networkDistance <= getMaxDistance();
    }

    @Override
    public String toString() {
        return String.format("m(%s, %.1f, %.2f)", keyword, distance, epsilon);
    }
}
