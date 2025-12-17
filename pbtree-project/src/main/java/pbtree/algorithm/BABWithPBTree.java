package pbtree.algorithm;

import java.util.*;
import pbtree.model.*;

/**
 * Algorithm 3: BAB (Branch and Bound) with PB-Tree optimization.
 * 
 * Uses PB-Tree (Pivot reverse Binary tree) for efficient findNext operations.
 * Based on Section 5.3 of the paper.
 * 
 * Key improvements over AB-Tree:
 * - Smaller space complexity: O(|L| * h) vs O(|V|^2)
 * - Main memory based instead of disk-based
 * - Uses 2-hop label for distance computation
 * 
 * The findNext procedure with PB-Tree:
 * 1. For each pivot o in L(v_{i-1}), query PB(o) for candidates
 * 2. Distance is computed as d_G(v_{i-1}, o) + d_G(o, v_i)
 * 3. Verify that the path through o is the shortest path
 * 4. Use pruning based on current UB to skip unnecessary pivots
 */
public class BABWithPBTree {
    private final RoadNetwork network;
    private final TwoHopLabel labelIndex;
    private final Map<Long, PBTree> pbTrees;           // Cache of PB-Trees per pivot
    private final List<SearchResult.SearchStep> searchSteps;
    private int stepCounter;
    
    // Configuration
    private int maxIterations = 10000;
    private boolean buildTreesLazily = true;
    
    public BABWithPBTree(RoadNetwork network, TwoHopLabel labelIndex) {
        this.network = network;
        this.labelIndex = labelIndex;
        this.pbTrees = new HashMap<>();
        this.searchSteps = new ArrayList<>();
        this.stepCounter = 0;
    }
    
    public void setMaxIterations(int max) { this.maxIterations = max; }
    public void setBuildTreesLazily(boolean lazy) { this.buildTreesLazily = lazy; }
    
    /**
     * Get or build PB-Tree for a pivot.
     */
    private PBTree getPBTree(long pivot) {
        return pbTrees.computeIfAbsent(pivot, 
            id -> PBTree.buildForPivot(id, labelIndex, network));
    }
    
    /**
     * findNext() with PB-Tree (Section 5.3.2)
     * 
     * Given current vertex v_{i-1}, find next candidate v_i that:
     * - Has keyword w_i
     * - d_G(v_{i-1}, v_i) is within confidence interval
     * - Minimizes matching distance d_m^i(v_i)
     * 
     * Uses pivots from L(v_{i-1}) and searches PB(o) for each pivot o.
     */
    public FindNextWithPBResult findNext(long prevVertex, Clue clue, double theta, 
                                          double upperBound, Set<Long> excluded) {
        String keyword = clue.getKeyword();
        double di = clue.getDistance();
        double epsilon = clue.getEpsilon();
        
        // Compute distance bounds
        // lD, rD: confidence interval bounds
        double lD = di * (1 - epsilon) + theta;  // With offset theta
        double rD = di * (1 + epsilon) - theta;
        
        // lB, rB: pruning bounds based on UB
        // d_m^i(v_i) must not exceed UB
        // |d_G(v_{i-1}, v_i) - di| / (di * epsilon) <= UB
        // => d_G(v_{i-1}, v_i) in [di - di*epsilon*UB, di + di*epsilon*UB]
        double lB = di - di * epsilon * upperBound;
        double rB = di + di * epsilon * upperBound;
        
        // Ensure lB <= lD and rD <= rB
        lB = Math.max(0, lB);
        
        FindNextWithPBResult bestResult = new FindNextWithPBResult(false);
        List<PBTree.SearchStep> allSteps = new ArrayList<>();
        
        // Get label entries for prevVertex, sorted by distance
        List<LabelEntry> labelEntries = labelIndex.getLabel(prevVertex);
        
        for (LabelEntry labelEntry : labelEntries) {
            long pivot = labelEntry.getPivot();
            double distToPivot = labelEntry.getDistance();  // d_G(v_{i-1}, o)
            
            // Pruning: if rB < d_G(v_{i-1}, o), we cannot find candidates
            // because the minimum possible d_G(v_{i-1}, v_i) would exceed rB
            if (rB < distToPivot) {
                break;  // Pivots are sorted by distance, so stop here
            }
            
            // Compute adjusted bounds for this pivot
            // lDo = lD - d_G(v_{i-1}, o)
            // rDo = rD - d_G(v_{i-1}, o)
            // lBo = lB - d_G(v_{i-1}, o)
            // rBo = rB - d_G(v_{i-1}, o)
            double lDo = lD - distToPivot;
            double rDo = rD - distToPivot;
            double lBo = lB - distToPivot;
            double rBo = rB - distToPivot;
            
            // Get PB-Tree for this pivot
            PBTree pbTree = getPBTree(pivot);
            pbTree.clearSearchSteps();
            
            // Try successor query first (if rB >= distToPivot)
            // Looking for d_G(o, v_i) in [rDo, rBo]
            if (rBo >= 0 && rDo <= rBo) {
                PBTree.FindNextResult succResult = pbTree.successor(
                    keyword, Math.max(0, rDo), rBo, excluded, di, epsilon
                );
                allSteps.addAll(pbTree.getSearchSteps());
                
                if (succResult.found) {
                    // Verify: check if pivot o is on shortest path
                    double totalDist = distToPivot + succResult.distanceFromPivot;
                    double actualDist = labelIndex.queryDistance(prevVertex, succResult.vertexId);
                    
                    if (Math.abs(totalDist - actualDist) < 0.001) {
                        // Valid candidate
                        double dm = Math.abs(actualDist - di) / (di * epsilon);
                        if (!bestResult.found || dm < bestResult.matchingDistance) {
                            bestResult = new FindNextWithPBResult(
                                succResult.vertexId, actualDist, dm, pivot
                            );
                        }
                    }
                }
            }
            
            // Try predecessor query (if lD >= distToPivot)
            // Looking for d_G(o, v_i) in [lBo, lDo]
            if (lD >= distToPivot && lDo >= 0 && lBo <= lDo) {
                pbTree.clearSearchSteps();
                PBTree.FindNextResult predResult = pbTree.predecessor(
                    keyword, Math.max(0, lBo), lDo, excluded, di, epsilon
                );
                allSteps.addAll(pbTree.getSearchSteps());
                
                if (predResult.found) {
                    double totalDist = distToPivot + predResult.distanceFromPivot;
                    double actualDist = labelIndex.queryDistance(prevVertex, predResult.vertexId);
                    
                    if (Math.abs(totalDist - actualDist) < 0.001) {
                        double dm = Math.abs(actualDist - di) / (di * epsilon);
                        if (!bestResult.found || dm < bestResult.matchingDistance) {
                            bestResult = new FindNextWithPBResult(
                                predResult.vertexId, actualDist, dm, pivot
                            );
                        }
                    }
                }
            }
            
            // Update pruning bounds if we found a candidate
            if (bestResult.found) {
                rB = Math.min(rB, 2 * di - (di - di * epsilon * bestResult.matchingDistance));
                lB = di - di * epsilon * bestResult.matchingDistance;
            }
        }
        
        bestResult.pbTreeSteps = allSteps;
        return bestResult;
    }
    
    /**
     * Result of findNext with PB-Tree
     */
    public static class FindNextWithPBResult {
        public boolean found;
        public long vertexId;
        public double distance;           // Actual network distance
        public double matchingDistance;   // d_m
        public long viaPivot;             // The pivot used to find this result
        public List<PBTree.SearchStep> pbTreeSteps;
        
        public FindNextWithPBResult(boolean found) {
            this.found = found;
            this.pbTreeSteps = new ArrayList<>();
        }
        
        public FindNextWithPBResult(long vertexId, double distance, double matchingDistance, long viaPivot) {
            this.found = true;
            this.vertexId = vertexId;
            this.distance = distance;
            this.matchingDistance = matchingDistance;
            this.viaPivot = viaPivot;
            this.pbTreeSteps = new ArrayList<>();
        }
    }
    
    /**
     * Execute BAB search with PB-Tree optimization.
     */
    public SearchResult search(Query query) {
        long startTime = System.currentTimeMillis();
        searchSteps.clear();
        stepCounter = 0;
        
        long sourceVertex = query.getSourceVertex();
        List<Clue> clues = query.getClues();
        int k = clues.size();
        
        // Build PB-Trees if not lazy
        long pbTreeBuildStart = System.currentTimeMillis();
        if (!buildTreesLazily) {
            for (long pivot : labelIndex.getPivotOrder()) {
                getPBTree(pivot);
            }
        }
        int pbTreeBuildTime = (int)(System.currentTimeMillis() - pbTreeBuildStart);
        
        // Initialize stacks and state
        List<Long> stackV = new ArrayList<>();
        List<Double> stackD = new ArrayList<>();
        stackV.add(sourceVertex);
        
        double theta = 0.0;
        double upperBound = Double.MAX_VALUE;
        List<Long> bestPath = new ArrayList<>();
        double bestMatchingDistance = Double.MAX_VALUE;
        
        // Excluded vertices at each level
        Map<Integer, Set<Long>> excludedAtLevel = new HashMap<>();
        for (int i = 0; i <= k; i++) {
            excludedAtLevel.put(i, new HashSet<>());
        }
        
        // Record initialization step
        SearchResult.SearchStep initStep = new SearchResult.SearchStep(++stepCounter, "INIT");
        initStep.stackV = new ArrayList<>(stackV);
        initStep.stackD = new ArrayList<>(stackD);
        initStep.upperBound = upperBound;
        initStep.accepted = true;
        initStep.reason = "Line 1-2: Initialize stackV=[vq], stackD=[], θ=0";
        searchSteps.add(initStep);
        
        int iterations = 0;
        
        // Main BAB loop
        while (!stackV.isEmpty() && iterations < maxIterations) {
            iterations++;
            
            int level = stackV.size();
            
            // If we've matched all clues, need to backtrack
            if (level > k) {
                stackV.remove(stackV.size() - 1);
                if (!stackD.isEmpty()) stackD.remove(stackD.size() - 1);
                continue;
            }
            
            long currentVertex = stackV.get(stackV.size() - 1);
            Clue currentClue = clues.get(level - 1);
            Set<Long> excluded = excludedAtLevel.get(level);
            
            // Line 5: findNext using PB-Tree
            FindNextWithPBResult result = findNext(currentVertex, currentClue, theta, upperBound, excluded);
            
            if (result.found) {
                // Check pruning condition
                if (result.matchingDistance > upperBound) {
                    // Line 15-17: Prune and backtrack
                    SearchResult.SearchStep step = new SearchResult.SearchStep(++stepCounter, "PRUNE");
                    step.stackV = new ArrayList<>(stackV);
                    step.stackD = new ArrayList<>(stackD);
                    step.upperBound = upperBound;
                    step.candidate = result.vertexId;
                    step.candidateDm = result.matchingDistance;
                    step.pivot = result.viaPivot;
                    step.accepted = false;
                    step.reason = String.format("Line 15-17: dm=%.4f > UB=%.4f, prune and backtrack", 
                        result.matchingDistance, upperBound);
                    step.pbTreeSteps = new ArrayList<>(result.pbTreeSteps);
                    searchSteps.add(step);
                    
                    excluded.add(result.vertexId);
                    stackV.remove(stackV.size() - 1);
                    if (!stackD.isEmpty()) {
                        theta = stackD.remove(stackD.size() - 1);
                    } else {
                        theta = 0;
                    }
                    
                    // Clear excluded sets for deeper levels
                    for (int l = level + 1; l <= k; l++) {
                        excludedAtLevel.get(l).clear();
                    }
                    continue;
                }
                
                // Line 6-8: Accept candidate
                SearchResult.SearchStep step = new SearchResult.SearchStep(++stepCounter, "PUSH");
                step.stackV = new ArrayList<>(stackV);
                step.stackD = new ArrayList<>(stackD);
                step.upperBound = upperBound;
                step.candidate = result.vertexId;
                step.candidateDm = result.matchingDistance;
                step.pivot = result.viaPivot;
                step.accepted = true;
                step.reason = String.format("Line 6-8: Found v_%d=%d via pivot %d, dm=%.4f, push to stack", 
                    level, result.vertexId, result.viaPivot, result.matchingDistance);
                step.pbTreeSteps = new ArrayList<>(result.pbTreeSteps);
                searchSteps.add(step);
                
                theta = 0.0;
                stackV.add(result.vertexId);
                stackD.add(result.matchingDistance);
                
                // Line 9-12: Check if complete path found
                if (stackV.size() == k + 1) {
                    double maxDm = Collections.max(stackD);
                    
                    if (maxDm <= upperBound) {
                        // New best path!
                        upperBound = maxDm;
                        bestPath = new ArrayList<>(stackV);
                        bestMatchingDistance = maxDm;
                        
                        SearchResult.SearchStep ubStep = new SearchResult.SearchStep(++stepCounter, "UPDATE_UB");
                        ubStep.stackV = new ArrayList<>(stackV);
                        ubStep.stackD = new ArrayList<>(stackD);
                        ubStep.upperBound = upperBound;
                        ubStep.accepted = true;
                        ubStep.reason = String.format("Line 10-12: Complete path! UB ← max(stackD) = %.4f", upperBound);
                        searchSteps.add(ubStep);
                    } else {
                        SearchResult.SearchStep feasStep = new SearchResult.SearchStep(++stepCounter, "FEASIBLE_NO_UPDATE");
                        feasStep.stackV = new ArrayList<>(stackV);
                        feasStep.stackD = new ArrayList<>(stackD);
                        feasStep.upperBound = upperBound;
                        feasStep.accepted = false;
                        feasStep.reason = String.format("Line 10: max(stackD)=%.4f > UB=%.4f, no update", maxDm, upperBound);
                        searchSteps.add(feasStep);
                    }
                    
                    // Backtrack to find more paths
                    excluded.add(stackV.remove(stackV.size() - 1));
                    stackD.remove(stackD.size() - 1);
                    
                    // Also backtrack one more level to explore alternatives
                    if (stackV.size() > 1) {
                        excludedAtLevel.get(level - 1).add(stackV.remove(stackV.size() - 1));
                        if (!stackD.isEmpty()) {
                            theta = stackD.remove(stackD.size() - 1);
                        }
                    }
                    excludedAtLevel.get(level).clear();
                }
            } else {
                // Line 15-17: No candidate found, backtrack
                SearchResult.SearchStep step = new SearchResult.SearchStep(++stepCounter, "BACKTRACK");
                step.stackV = new ArrayList<>(stackV);
                step.stackD = new ArrayList<>(stackD);
                step.upperBound = upperBound;
                step.accepted = false;
                step.reason = String.format("Line 15-17: No candidate for clue %d (w='%s'), backtrack", 
                    level, currentClue.getKeyword());
                step.pbTreeSteps = new ArrayList<>(result.pbTreeSteps);
                searchSteps.add(step);
                
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
        
        // Line 18: Return result
        SearchResult.SearchStep doneStep = new SearchResult.SearchStep(++stepCounter, "DONE");
        doneStep.stackV = new ArrayList<>(bestPath);
        doneStep.upperBound = bestMatchingDistance;
        doneStep.accepted = !bestPath.isEmpty();
        doneStep.reason = bestPath.isEmpty() ? 
            "Line 18: No feasible path found" : 
            String.format("Line 18: Return FP_bab, dm=%.4f", bestMatchingDistance);
        searchSteps.add(doneStep);
        
        long totalTime = System.currentTimeMillis() - startTime;
        
        return new SearchResult(
            bestPath, 
            bestMatchingDistance, 
            searchSteps, 
            totalTime,
            (int) labelIndex.getConstructionTimeMs(),
            pbTreeBuildTime,
            iterations
        );
    }
    
    /**
     * Pre-build PB-Trees for all pivots (for benchmarking).
     */
    public void prebuildAllTrees() {
        System.out.println("Pre-building PB-Trees for all pivots...");
        int count = 0;
        for (long pivot : labelIndex.getPivotOrder()) {
            getPBTree(pivot);
            count++;
            if (count % 100 == 0) {
                System.out.println("  Built " + count + "/" + labelIndex.getPivotOrder().size() + " PB-Trees");
            }
        }
        System.out.println("Built " + pbTrees.size() + " PB-Trees");
    }
    
    /**
     * Get memory usage estimate.
     */
    public long getMemoryUsage() {
        long size = 0;
        for (PBTree tree : pbTrees.values()) {
            size += tree.getEntryCount() * 64; // Rough estimate per entry
        }
        return size;
    }
}
