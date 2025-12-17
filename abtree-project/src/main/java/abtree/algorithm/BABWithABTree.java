package abtree.algorithm;

import abtree.model.*;
import java.util.*;

/**
 * Algorithm 3: BAB (Branch and Bound) with AB-Tree optimization.
 * 
 * Uses AB-Tree (Algorithm 4) for efficient findNext operations instead of
 * scanning all candidates.
 * 
 * Key improvement: O(log n) findNext instead of O(n) linear scan.
 */
public class BABWithABTree {
    private final RoadNetwork network;
    private final Map<Long, ABTree> abTrees; // Cache of AB-Trees per source vertex
    private final List<SearchResult.SearchStep> searchSteps;
    private int stepCounter;
    
    // Configuration
    private int maxIterations = 10000;
    private boolean buildTreesLazily = true;
    
    public BABWithABTree(RoadNetwork network) {
        this.network = network;
        this.abTrees = new HashMap<>();
        this.searchSteps = new ArrayList<>();
        this.stepCounter = 0;
    }
    
    public void setMaxIterations(int max) { this.maxIterations = max; }
    public void setBuildTreesLazily(boolean lazy) { this.buildTreesLazily = lazy; }
    
    /**
     * Get or build AB-Tree for a vertex.
     */
    private ABTree getABTree(long vertexId) {
        return abTrees.computeIfAbsent(vertexId, 
            id -> ABTree.buildFromNetwork(network, id));
    }
    
    /**
     * Execute BAB search with AB-Tree optimization.
     */
    public SearchResult search(Query query) {
        long startTime = System.currentTimeMillis();
        searchSteps.clear();
        stepCounter = 0;
        
        long sourceVertex = query.getSourceVertex();
        List<Clue> clues = query.getClues();
        int k = clues.size();
        
        // Build AB-Tree for source (will be used for first clue)
        long treeBuildStart = System.currentTimeMillis();
        if (!buildTreesLazily) {
            getABTree(sourceVertex);
        }
        int treeBuildTime = (int)(System.currentTimeMillis() - treeBuildStart);
        
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
            
            // Get AB-Tree for current vertex
            ABTree abTree = getABTree(currentVertex);
            abTree.clearSearchSteps();
            
            // Line 5: findNext using AB-Tree (Algorithm 4)
            ABTree.FindNextResult result = abTree.findNext(currentClue, theta, excluded);
            
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
                    step.accepted = false;
                    step.reason = String.format("Line 15-17: dm=%.4f > UB=%.4f, prune and backtrack", 
                        result.matchingDistance, upperBound);
                    step.abTreeSteps = new ArrayList<>(abTree.getSearchSteps());
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
                step.accepted = true;
                step.reason = String.format("Line 6-8: Found v_%d=%d via AB-Tree, dm=%.4f, push to stack", 
                    level, result.vertexId, result.matchingDistance);
                step.abTreeSteps = new ArrayList<>(abTree.getSearchSteps());
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
                step.abTreeSteps = new ArrayList<>(abTree.getSearchSteps());
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
            treeBuildTime,
            iterations
        );
    }
    
    /**
     * Pre-build AB-Trees for all connected vertices (for benchmarking).
     */
    public void prebuildAllTrees() {
        System.out.println("Pre-building AB-Trees for all vertices...");
        int count = 0;
        for (long vertexId : network.getAdjacencyList().keySet()) {
            if (!network.getEdges(vertexId).isEmpty()) {
                getABTree(vertexId);
                count++;
                if (count % 100 == 0) {
                    System.out.println("  Built " + count + " trees...");
                }
            }
        }
        System.out.println("Built " + count + " AB-Trees");
    }
    
    /**
     * Clear cached AB-Trees to free memory.
     */
    public void clearTreeCache() {
        abTrees.clear();
    }
    
    /**
     * Get statistics about cached AB-Trees.
     */
    public String getTreeCacheStats() {
        int totalNodes = 0;
        int totalEntries = 0;
        for (ABTree tree : abTrees.values()) {
            totalNodes += tree.getNodeCount();
            totalEntries += tree.getEntryCount();
        }
        return String.format("AB-Tree Cache: %d trees, %d total nodes, %d total entries",
            abTrees.size(), totalNodes, totalEntries);
    }
}
