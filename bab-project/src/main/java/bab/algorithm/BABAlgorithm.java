package bab.algorithm;

import bab.model.Clue;
import bab.model.Query;
import bab.model.RoadNetwork;
import bab.algorithm.FindNextProcedure.FindNextResult;
import bab.algorithm.SearchResult.SearchStep;

import java.util.*;

/**
 * Algorithm 3: Branch and Bound BAB
 * 
 * Implementation of the BAB algorithm from:
 * "Efficient Clue-Based Route Search on Road Networks" (IEEE TKDE 2017)
 * 
 * Input: Q = (vq, C) where vq is source vertex and C is sequence of clues
 * Output: FP_bab with dm(C, FP_bab) - optimal feasible path with matching distance
 * 
 * The algorithm uses depth-first search with:
 * - stackV: stores partial path vertices
 * - stackD: stores matching distances
 * - θ (theta): search threshold for relaxation
 * - UB: upper bound for pruning
 */
public class BABAlgorithm {
    
    private final RoadNetwork network;
    private final FindNextProcedure findNext;
    private final int maxIterations;

    public BABAlgorithm(RoadNetwork network) {
        this(network, 10000);
    }

    public BABAlgorithm(RoadNetwork network, int maxIterations) {
        this.network = network;
        this.findNext = new FindNextProcedure(network);
        this.maxIterations = maxIterations;
    }

    /**
     * Execute BAB algorithm
     * 
     * @param query Query Q = (vq, C)
     * @return SearchResult containing best path and execution trace
     */
    public SearchResult execute(Query query) {
        long startTime = System.currentTimeMillis();
        List<SearchStep> searchSteps = new ArrayList<>();
        int stepCounter = 0;

        // Line 1: Initialize stackV, stackD, and search threshold θ
        List<Long> stackV = new ArrayList<>();
        List<Double> stackD = new ArrayList<>();
        double theta = 0.0;
        double upperBound = Double.MAX_VALUE;
        List<Long> bestPath = new ArrayList<>();
        double bestMatchingDistance = Double.MAX_VALUE;
        
        int k = query.getClueCount();

        // Excluded vertices at each level (for backtracking)
        Map<Integer, Set<Long>> excludedAtLevel = new HashMap<>();
        for (int i = 0; i <= k; i++) {
            excludedAtLevel.put(i, new HashSet<>());
        }

        // Line 2: Push vq into stackV
        stackV.add(query.getSourceVertex());

        searchSteps.add(new SearchStep(
            ++stepCounter, "INITIALIZE",
            stackV, stackD, upperBound,
            null, null, true,
            "Line 1-2: Initialize stackV, stackD, θ=0; Push vq into stackV"
        ));

        int iterations = 0;

        // Line 3: while stackV is not empty do
        while (!stackV.isEmpty() && iterations < maxIterations) {
            iterations++;

            // Line 4: i ← stackV.size()
            int level = stackV.size();

            // Safety check: prevent going beyond k levels
            if (level > k + 1) {
                stackV.remove(stackV.size() - 1);
                if (!stackD.isEmpty()) stackD.remove(stackD.size() - 1);
                continue;
            }

            if (level > k) {
                stackV.remove(stackV.size() - 1);
                if (!stackD.isEmpty()) stackD.remove(stackD.size() - 1);
                continue;
            }

            long currentVertex = stackV.get(stackV.size() - 1);
            Clue currentClue = query.getClue(level - 1);  // 0-indexed
            Set<Long> excluded = excludedAtLevel.get(level);

            // Line 5: if findNext(v_{i-1}, d_i, w_i, θ) = true then
            FindNextResult result = findNext.findNext(currentVertex, currentClue, theta, excluded);

            if (result.isFound()) {
                // Check if matching distance exceeds upper bound (pruning)
                if (result.getMatchingDistance() > upperBound) {
                    // Line 15-17: Candidate exceeds UB, backtrack
                    searchSteps.add(new SearchStep(
                        ++stepCounter, "FIND_NEXT",
                        stackV, stackD, upperBound,
                        result.getVertex(), result.getMatchingDistance(), false,
                        String.format("Line 15-17: d^%d_m(v_%d)=%.3f > UB=%.3f. Update θ, backtrack.",
                                      level, level, result.getMatchingDistance(), upperBound)
                    ));

                    excluded.add(result.getVertex());
                    stackV.remove(stackV.size() - 1);
                    if (!stackD.isEmpty()) {
                        theta = stackD.remove(stackD.size() - 1);
                    } else {
                        theta = 0;
                    }

                    // Clear excluded sets for higher levels
                    for (int l = level + 1; l <= k; l++) {
                        excludedAtLevel.get(l).clear();
                    }
                    continue;
                }

                // Line 6: Obtain vi and d^i_m(vi)
                // Line 7: θ ← 0.0
                // Line 8: Push vi into stackV and d^i_m(vi) into stackD

                searchSteps.add(new SearchStep(
                    ++stepCounter, "FIND_NEXT",
                    stackV, stackD, upperBound,
                    result.getVertex(), result.getMatchingDistance(), true,
                    String.format("Line 6-8: Found v_%d=%d, d^%d_m=%.3f. θ←0, push to stacks.",
                                  level, result.getVertex(), level, result.getMatchingDistance())
                ));

                theta = 0.0;
                stackV.add(result.getVertex());
                stackD.add(result.getMatchingDistance());

                // Line 9: if i equals to k then
                if (stackV.size() == k + 1) {
                    double maxDm = stackD.stream().mapToDouble(d -> d).max().orElse(0);

                    // Line 10: if max{stackD} <= UB then
                    if (maxDm <= upperBound) {
                        // Line 11: Update UB by max{stackD}
                        upperBound = maxDm;
                        // Line 12: Update FP_bab by stackV
                        bestPath = new ArrayList<>(stackV);
                        bestMatchingDistance = maxDm;

                        searchSteps.add(new SearchStep(
                            ++stepCounter, "FEASIBLE_PATH",
                            stackV, stackD, upperBound,
                            null, null, true,
                            String.format("Line 10-12: Feasible path found! max{stackD}=%.3f ≤ UB. " +
                                          "Update UB←%.3f, FP_bab←path", maxDm, upperBound)
                        ));
                    } else {
                        searchSteps.add(new SearchStep(
                            ++stepCounter, "FEASIBLE_PATH",
                            stackV, stackD, upperBound,
                            null, null, false,
                            String.format("Line 10: max{stackD}=%.3f > UB=%.3f. Path not better.",
                                          maxDm, upperBound)
                        ));
                    }

                    // Line 13: Update θ by top of stackD
                    // Line 14: Update stackV and stackD (backtrack)
                    excluded.add(stackV.remove(stackV.size() - 1));
                    stackD.remove(stackD.size() - 1);

                    // Also backtrack one more level
                    if (stackV.size() > 1) {
                        long vk1 = stackV.remove(stackV.size() - 1);
                        excludedAtLevel.get(level - 1).add(vk1);
                        if (!stackD.isEmpty()) {
                            theta = stackD.remove(stackD.size() - 1);
                        }
                    }

                    excludedAtLevel.get(level).clear();
                }
            } else {
                // Line 15-17: else branch - no candidate found
                searchSteps.add(new SearchStep(
                    ++stepCounter, "FIND_NEXT",
                    stackV, stackD, upperBound,
                    null, null, false,
                    String.format("Line 15-17: No valid v_%d found for w=\"%s\", d=%.1fm, θ=%.3f. Backtrack.",
                                  level, currentClue.getKeyword(), currentClue.getDistance(), theta)
                ));

                // Line 16: Update θ by top of stackD
                // Line 17: Update stackV and stackD
                stackV.remove(stackV.size() - 1);
                if (!stackD.isEmpty()) {
                    theta = stackD.remove(stackD.size() - 1);
                }

                excludedAtLevel.get(level).clear();
                for (int l = level + 1; l <= k; l++) {
                    excludedAtLevel.get(l).clear();
                }
            }
        }

        // Line 18: return FP_bab and dm(C, FP_bab) ← UB
        searchSteps.add(new SearchStep(
            ++stepCounter, "TERMINATE",
            bestPath, Collections.emptyList(), bestMatchingDistance,
            null, null, !bestPath.isEmpty(),
            !bestPath.isEmpty()
                ? String.format("Line 18: Return FP_bab with dm(C, FP_bab)=%.3f", bestMatchingDistance)
                : "Line 18: No feasible path found"
        ));

        long executionTime = System.currentTimeMillis() - startTime;
        
        return new SearchResult(bestPath, bestMatchingDistance, searchSteps, executionTime);
    }

    /**
     * Get the underlying road network
     */
    public RoadNetwork getNetwork() {
        return network;
    }

    /**
     * Get the FindNext procedure instance
     */
    public FindNextProcedure getFindNextProcedure() {
        return findNext;
    }
}
