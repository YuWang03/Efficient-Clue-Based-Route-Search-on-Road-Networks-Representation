package bab.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of the BAB algorithm execution
 * 
 * Contains:
 * - bestPath: The optimal feasible path FP_bab
 * - matchingDistance: dm(C, FP_bab)
 * - searchSteps: Detailed execution trace for visualization
 */
public class SearchResult {
    private final List<Long> bestPath;
    private final double matchingDistance;
    private final List<SearchStep> searchSteps;
    private final long executionTimeMs;

    public SearchResult(List<Long> bestPath, double matchingDistance, 
                        List<SearchStep> searchSteps, long executionTimeMs) {
        this.bestPath = bestPath != null ? new ArrayList<>(bestPath) : new ArrayList<>();
        this.matchingDistance = matchingDistance;
        this.searchSteps = searchSteps != null ? new ArrayList<>(searchSteps) : new ArrayList<>();
        this.executionTimeMs = executionTimeMs;
    }

    public List<Long> getBestPath() {
        return Collections.unmodifiableList(bestPath);
    }

    public double getMatchingDistance() {
        return matchingDistance;
    }

    public List<SearchStep> getSearchSteps() {
        return Collections.unmodifiableList(searchSteps);
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public boolean hasValidPath() {
        return !bestPath.isEmpty() && matchingDistance < Double.MAX_VALUE;
    }

    public int getStepCount() {
        return searchSteps.size();
    }

    @Override
    public String toString() {
        if (hasValidPath()) {
            return String.format("SearchResult(path=%s, dm=%.4f, steps=%d, time=%dms)",
                                 bestPath, matchingDistance, searchSteps.size(), executionTimeMs);
        } else {
            return String.format("SearchResult(NO_PATH_FOUND, steps=%d, time=%dms)",
                                 searchSteps.size(), executionTimeMs);
        }
    }

    /**
     * Print detailed results
     */
    public void printDetails() {
        System.out.println("\n=== BAB Algorithm Result ===");
        if (hasValidPath()) {
            System.out.println("Status: SUCCESS");
            System.out.println("Best Path (FP_bab): " + bestPath);
            System.out.printf("Matching Distance dm(C, FP_bab): %.4f%n", matchingDistance);
        } else {
            System.out.println("Status: NO FEASIBLE PATH FOUND");
        }
        System.out.println("Total search steps: " + searchSteps.size());
        System.out.println("Execution time: " + executionTimeMs + " ms");
    }

    /**
     * Print all search steps (for debugging/visualization)
     */
    public void printAllSteps() {
        System.out.println("\n=== Search Steps ===");
        for (SearchStep step : searchSteps) {
            System.out.println(step.toDetailedString());
            System.out.println();
        }
    }

    /**
     * Represents a single step in the BAB search process
     */
    public static class SearchStep {
        private final int stepNumber;
        private final String action;
        private final List<Long> stackV;
        private final List<Double> stackD;
        private final double upperBound;
        private final Long candidate;
        private final Double candidateMatchingDist;
        private final boolean accepted;
        private final String reason;

        public SearchStep(int stepNumber, String action, List<Long> stackV, List<Double> stackD,
                          double upperBound, Long candidate, Double candidateMatchingDist,
                          boolean accepted, String reason) {
            this.stepNumber = stepNumber;
            this.action = action;
            this.stackV = new ArrayList<>(stackV);
            this.stackD = new ArrayList<>(stackD);
            this.upperBound = upperBound;
            this.candidate = candidate;
            this.candidateMatchingDist = candidateMatchingDist;
            this.accepted = accepted;
            this.reason = reason;
        }

        public int getStepNumber() { return stepNumber; }
        public String getAction() { return action; }
        public List<Long> getStackV() { return Collections.unmodifiableList(stackV); }
        public List<Double> getStackD() { return Collections.unmodifiableList(stackD); }
        public double getUpperBound() { return upperBound; }
        public Long getCandidate() { return candidate; }
        public Double getCandidateMatchingDist() { return candidateMatchingDist; }
        public boolean isAccepted() { return accepted; }
        public String getReason() { return reason; }

        public String toDetailedString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Step %d [%s] %s%n", stepNumber, action, accepted ? "✓" : "✗"));
            sb.append(String.format("  stackV: %s%n", stackV));
            sb.append(String.format("  stackD: %s%n", stackD));
            sb.append(String.format("  UB: %s%n", upperBound == Double.MAX_VALUE ? "∞" : String.format("%.3f", upperBound)));
            if (candidate != null) {
                sb.append(String.format("  Candidate: %d (dm: %.3f)%n", candidate, candidateMatchingDist));
            }
            sb.append(String.format("  %s", reason));
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format("Step %d [%s] %s", stepNumber, action, accepted ? "✓" : "✗");
        }
    }
}
