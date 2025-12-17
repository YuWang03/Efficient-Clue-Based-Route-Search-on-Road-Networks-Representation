package abtree.algorithm;

import java.util.*;

/**
 * Stores the result of BAB search with AB-Tree optimization.
 */
public class SearchResult {
    private final List<Long> bestPath;
    private final double matchingDistance;
    private final List<SearchStep> searchSteps;
    private final long executionTimeMs;
    private final int abTreeBuildTimeMs;
    private final int totalIterations;
    
    public SearchResult(List<Long> bestPath, double matchingDistance, 
                       List<SearchStep> searchSteps, long executionTimeMs,
                       int abTreeBuildTimeMs, int totalIterations) {
        this.bestPath = bestPath != null ? new ArrayList<>(bestPath) : new ArrayList<>();
        this.matchingDistance = matchingDistance;
        this.searchSteps = new ArrayList<>(searchSteps);
        this.executionTimeMs = executionTimeMs;
        this.abTreeBuildTimeMs = abTreeBuildTimeMs;
        this.totalIterations = totalIterations;
    }
    
    public List<Long> getBestPath() { return bestPath; }
    public double getMatchingDistance() { return matchingDistance; }
    public List<SearchStep> getSearchSteps() { return searchSteps; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public int getAbTreeBuildTimeMs() { return abTreeBuildTimeMs; }
    public int getTotalIterations() { return totalIterations; }
    public boolean hasPath() { return !bestPath.isEmpty(); }
    
    /**
     * Represents a single step in the BAB search process.
     */
    public static class SearchStep {
        public int stepNumber;
        public String action;
        public List<Long> stackV;
        public List<Double> stackD;
        public double upperBound;
        public Long candidate;
        public Double candidateDm;
        public boolean accepted;
        public String reason;
        public List<ABTree.SearchStep> abTreeSteps; // AB-Tree search details
        
        public SearchStep(int stepNumber, String action) {
            this.stepNumber = stepNumber;
            this.action = action;
            this.stackV = new ArrayList<>();
            this.stackD = new ArrayList<>();
            this.upperBound = Double.MAX_VALUE;
            this.accepted = false;
            this.reason = "";
            this.abTreeSteps = new ArrayList<>();
        }
        
        @Override
        public String toString() {
            return String.format("Step %d [%s]: stackV=%s, UB=%.4f, %s", 
                stepNumber, action, stackV, upperBound, accepted ? "✓" : "✗");
        }
    }
}
