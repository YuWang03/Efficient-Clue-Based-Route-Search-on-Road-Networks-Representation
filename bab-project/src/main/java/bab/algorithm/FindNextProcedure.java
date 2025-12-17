package bab.algorithm;

import bab.model.Clue;
import bab.model.RoadNetwork;

import java.util.Set;

/**
 * FindNext Procedure Implementation
 * 
 * As described in the paper, findNext() locates the next candidate vertex vi
 * given the current vertex v_{i-1}, clue mi, and search threshold θ.
 * 
 * The procedure finds vi such that:
 * - vi contains keyword wi
 * - Network distance dG(v_{i-1}, vi) is within confidence interval [d(1-ε), d(1+ε)]
 * - Matching distance dm >= θ and is minimized
 */
public class FindNextProcedure {
    
    private final RoadNetwork network;

    public FindNextProcedure(RoadNetwork network) {
        this.network = network;
    }

    /**
     * Result of findNext() procedure
     */
    public static class FindNextResult {
        private final boolean found;
        private final long vertex;
        private final double matchingDistance;
        private final double networkDistance;

        public FindNextResult(boolean found, long vertex, double matchingDistance, double networkDistance) {
            this.found = found;
            this.vertex = vertex;
            this.matchingDistance = matchingDistance;
            this.networkDistance = networkDistance;
        }

        public static FindNextResult notFound() {
            return new FindNextResult(false, -1, Double.MAX_VALUE, Double.MAX_VALUE);
        }

        public boolean isFound() { return found; }
        public long getVertex() { return vertex; }
        public double getMatchingDistance() { return matchingDistance; }
        public double getNetworkDistance() { return networkDistance; }

        @Override
        public String toString() {
            if (found) {
                return String.format("FindNextResult(v=%d, dm=%.3f, dG=%.1fm)", 
                                     vertex, matchingDistance, networkDistance);
            } else {
                return "FindNextResult(NOT_FOUND)";
            }
        }
    }

    /**
     * Calculate matching distance as defined in Equation 1:
     * dm(m, s) = |dG(u,v) - d| / (ε × d)
     * 
     * @param networkDistance dG(u,v) - actual network distance
     * @param clue Contains d (expected distance) and ε (confidence factor)
     * @return The matching distance
     */
    public double calculateMatchingDistance(double networkDistance, Clue clue) {
        return Math.abs(networkDistance - clue.getDistance()) / 
               (clue.getEpsilon() * clue.getDistance());
    }

    /**
     * Execute findNext procedure
     * 
     * Given vertex v_{i-1}, finds vi such that:
     * 1. vi contains keyword wi
     * 2. dG(v_{i-1}, vi) ∈ [d(1-ε), d(1+ε)]
     * 3. dm(mi, (v_{i-1}, vi)) >= θ and is minimized
     * 
     * @param fromVertex Current vertex v_{i-1}
     * @param clue Query clue mi(wi, di, εi)
     * @param theta Search threshold θ
     * @param excluded Set of vertices to exclude (already tried at this level)
     * @return FindNextResult with best candidate, or not found
     */
    public FindNextResult findNext(long fromVertex, Clue clue, double theta, Set<Long> excluded) {
        // Get all vertices with the required keyword
        Set<Long> candidates = network.getNodesWithKeyword(clue.getKeyword());
        
        if (candidates.isEmpty()) {
            return FindNextResult.notFound();
        }

        // Get confidence interval bounds
        double minDist = clue.getMinDistance();
        double maxDist = clue.getMaxDistance();

        FindNextResult best = FindNextResult.notFound();

        for (long candidateId : candidates) {
            // Skip excluded vertices
            if (excluded.contains(candidateId)) {
                continue;
            }

            // Calculate network distance
            double networkDist = network.getNetworkDistance(fromVertex, candidateId);

            // Check if within confidence interval
            if (networkDist < minDist || networkDist > maxDist || networkDist == Double.MAX_VALUE) {
                continue;
            }

            // Calculate matching distance
            double matchDist = calculateMatchingDistance(networkDist, clue);

            // Find candidate with dm >= theta and closest to theta (minimized)
            if (matchDist >= theta && matchDist < best.getMatchingDistance()) {
                best = new FindNextResult(true, candidateId, matchDist, networkDist);
            }
        }

        return best;
    }

    /**
     * Find all valid candidates for visualization/debugging
     * 
     * @param fromVertex Current vertex
     * @param clue Query clue
     * @param theta Search threshold
     * @param excluded Excluded vertices
     * @return List of all valid candidates (not just the best one)
     */
    public java.util.List<FindNextResult> findAllCandidates(long fromVertex, Clue clue, 
                                                             double theta, Set<Long> excluded) {
        java.util.List<FindNextResult> results = new java.util.ArrayList<>();
        Set<Long> candidates = network.getNodesWithKeyword(clue.getKeyword());
        
        double minDist = clue.getMinDistance();
        double maxDist = clue.getMaxDistance();

        for (long candidateId : candidates) {
            if (excluded.contains(candidateId)) continue;
            
            double networkDist = network.getNetworkDistance(fromVertex, candidateId);
            if (networkDist < minDist || networkDist > maxDist || networkDist == Double.MAX_VALUE) {
                continue;
            }

            double matchDist = calculateMatchingDistance(networkDist, clue);
            if (matchDist >= theta) {
                results.add(new FindNextResult(true, candidateId, matchDist, networkDist));
            }
        }

        // Sort by matching distance (ascending)
        results.sort((a, b) -> Double.compare(a.getMatchingDistance(), b.getMatchingDistance()));
        
        return results;
    }
}
