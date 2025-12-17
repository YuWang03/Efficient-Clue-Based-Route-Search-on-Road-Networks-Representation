package abtree.algorithm;

import abtree.model.*;
import java.util.*;

/**
 * AB-Tree: A B-Tree indexed by network distance for efficient range queries.
 * 
 * Structure:
 * - Internal nodes contain routing keys (distances) and child pointers
 * - Leaf nodes contain ABTreeEntry (distance, vertex, keywords)
 * - Entries are sorted by distance
 * 
 * Key operations for Algorithm 4:
 * - predecessor(lD, w): find largest d ≤ lD with keyword w
 * - successor(rD, w): find smallest d ≥ rD with keyword w  (but we need d ≤ rD)
 * 
 * Based on the paper's Algorithm 4: findNext() with AB-Tree
 */
public class ABTree {
    private static final int ORDER = 32; // B-Tree order (max children per node)
    @SuppressWarnings("unused")
    private static final int MIN_KEYS = ORDER / 2;
    
    private ABTreeNode root;
    private final long sourceVertex;
    private int nodeCount;
    private int entryCount;
    
    // For visualization: track search steps
    private final List<SearchStep> searchSteps;
    
    public static class SearchStep {
        public String action;
        public String nodeType;
        public double queryDistance;
        public String keyword;
        public Long candidateVertex;
        public Double candidateDistance;
        public String result;
        
        public SearchStep(String action, String nodeType, double queryDistance, String keyword) {
            this.action = action;
            this.nodeType = nodeType;
            this.queryDistance = queryDistance;
            this.keyword = keyword;
        }
    }
    
    /**
     * AB-Tree Node (internal or leaf)
     */
    public static class ABTreeNode {
        boolean isLeaf;
        List<Double> keys;           // Routing keys (distances)
        List<ABTreeNode> children;   // Child pointers (for internal nodes)
        List<ABTreeEntry> entries;   // Entries (for leaf nodes)
        Set<String> subtreeKeywords; // H(Node): all keywords in subtree
        ABTreeNode next;             // Pointer to next leaf (for range queries)
        @SuppressWarnings("unused")
        ABTreeNode prev;             // Pointer to previous leaf
        
        public ABTreeNode(boolean isLeaf) {
            this.isLeaf = isLeaf;
            this.keys = new ArrayList<>();
            this.children = isLeaf ? null : new ArrayList<>();
            this.entries = isLeaf ? new ArrayList<>() : null;
            this.subtreeKeywords = new HashSet<>();
        }
        
        public int size() {
            return isLeaf ? entries.size() : keys.size();
        }
    }
    
    public ABTree(long sourceVertex) {
        this.sourceVertex = sourceVertex;
        this.root = new ABTreeNode(true);
        this.nodeCount = 1;
        this.entryCount = 0;
        this.searchSteps = new ArrayList<>();
    }
    
    public long getSourceVertex() { return sourceVertex; }
    public int getNodeCount() { return nodeCount; }
    public int getEntryCount() { return entryCount; }
    public List<SearchStep> getSearchSteps() { return searchSteps; }
    public void clearSearchSteps() { searchSteps.clear(); }
    
    /**
     * Build AB-Tree from road network for a given source vertex.
     */
    public static ABTree buildFromNetwork(RoadNetwork network, long sourceVertex) {
        ABTree tree = new ABTree(sourceVertex);
        
        // Compute all distances from source
        Map<Long, Double> distances = network.computeAllDistancesFrom(sourceVertex);
        
        // Create entries for all reachable vertices with keywords
        List<ABTreeEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : distances.entrySet()) {
            long vertexId = entry.getKey();
            double distance = entry.getValue();
            
            if (distance < Double.MAX_VALUE && vertexId != sourceVertex) {
                Node node = network.getNode(vertexId);
                if (node != null && !node.getKeywords().isEmpty()) {
                    entries.add(new ABTreeEntry(distance, vertexId, node.getKeywords()));
                }
            }
        }
        
        // Sort by distance and bulk insert
        entries.sort(Comparator.comparingDouble(ABTreeEntry::getDistance));
        for (ABTreeEntry e : entries) {
            tree.insert(e);
        }
        
        return tree;
    }
    
    /**
     * Insert an entry into the AB-Tree.
     */
    public void insert(ABTreeEntry entry) {
        // Update root's subtree keywords
        root.subtreeKeywords.addAll(entry.getKeywords());
        
        if (root.isLeaf) {
            insertIntoLeaf(root, entry);
            if (root.entries.size() > ORDER - 1) {
                ABTreeNode newRoot = new ABTreeNode(false);
                newRoot.children.add(root);
                newRoot.subtreeKeywords.addAll(root.subtreeKeywords);
                splitChild(newRoot, 0);
                root = newRoot;
            }
        } else {
            insertNonFull(root, entry);
            if (root.keys.size() > ORDER - 1) {
                ABTreeNode newRoot = new ABTreeNode(false);
                newRoot.children.add(root);
                newRoot.subtreeKeywords.addAll(root.subtreeKeywords);
                splitChild(newRoot, 0);
                root = newRoot;
            }
        }
        entryCount++;
    }
    
    private void insertIntoLeaf(ABTreeNode leaf, ABTreeEntry entry) {
        int pos = Collections.binarySearch(leaf.entries, entry);
        if (pos < 0) pos = -(pos + 1);
        leaf.entries.add(pos, entry);
        leaf.subtreeKeywords.addAll(entry.getKeywords());
    }
    
    private void insertNonFull(ABTreeNode node, ABTreeEntry entry) {
        node.subtreeKeywords.addAll(entry.getKeywords());
        
        if (node.isLeaf) {
            insertIntoLeaf(node, entry);
        } else {
            int i = node.keys.size() - 1;
            while (i >= 0 && entry.getDistance() < node.keys.get(i)) {
                i--;
            }
            i++;
            
            ABTreeNode child = node.children.get(i);
            child.subtreeKeywords.addAll(entry.getKeywords());
            
            if (child.isLeaf && child.entries.size() >= ORDER - 1) {
                splitChild(node, i);
                if (entry.getDistance() > node.keys.get(i)) {
                    i++;
                }
            } else if (!child.isLeaf && child.keys.size() >= ORDER - 1) {
                splitChild(node, i);
                if (entry.getDistance() > node.keys.get(i)) {
                    i++;
                }
            }
            
            insertNonFull(node.children.get(i), entry);
        }
    }
    
    private void splitChild(ABTreeNode parent, int index) {
        ABTreeNode child = parent.children.get(index);
        ABTreeNode newNode = new ABTreeNode(child.isLeaf);
        nodeCount++;
        
        int mid;
        double midKey;
        
        if (child.isLeaf) {
            mid = child.entries.size() / 2;
            midKey = child.entries.get(mid).getDistance();
            
            // Move half entries to new node
            newNode.entries.addAll(child.entries.subList(mid, child.entries.size()));
            child.entries.subList(mid, child.entries.size()).clear();
            
            // Update subtree keywords
            newNode.subtreeKeywords.clear();
            for (ABTreeEntry e : newNode.entries) {
                newNode.subtreeKeywords.addAll(e.getKeywords());
            }
            child.subtreeKeywords.clear();
            for (ABTreeEntry e : child.entries) {
                child.subtreeKeywords.addAll(e.getKeywords());
            }
            
            // Link leaves
            newNode.next = child.next;
            newNode.prev = child;
            if (child.next != null) child.next.prev = newNode;
            child.next = newNode;
        } else {
            mid = child.keys.size() / 2;
            midKey = child.keys.get(mid);
            
            // Move keys and children
            newNode.keys.addAll(child.keys.subList(mid + 1, child.keys.size()));
            child.keys.subList(mid, child.keys.size()).clear();
            
            newNode.children.addAll(child.children.subList(mid + 1, child.children.size()));
            child.children.subList(mid + 1, child.children.size()).clear();
            
            // Update subtree keywords
            newNode.subtreeKeywords.clear();
            for (ABTreeNode c : newNode.children) {
                newNode.subtreeKeywords.addAll(c.subtreeKeywords);
            }
            child.subtreeKeywords.clear();
            for (ABTreeNode c : child.children) {
                child.subtreeKeywords.addAll(c.subtreeKeywords);
            }
        }
        
        parent.keys.add(index, midKey);
        parent.children.add(index + 1, newNode);
    }
    
    // ==================== Algorithm 4 Implementation ====================
    
    /**
     * Result of findNext operation
     */
    public static class FindNextResult {
        public final boolean found;
        public final long vertexId;
        public final double distance;
        public final double matchingDistance;
        
        public FindNextResult(boolean found, long vertexId, double distance, double matchingDistance) {
            this.found = found;
            this.vertexId = vertexId;
            this.distance = distance;
            this.matchingDistance = matchingDistance;
        }
        
        public static FindNextResult notFound() {
            return new FindNextResult(false, -1, -1, Double.MAX_VALUE);
        }
    }
    
    /**
     * Algorithm 4: findNext() with AB-Tree
     * 
     * Input: clue (w_i, d_i, ε), threshold θ
     * Output: Next candidate v_i with d_m(v_i)
     */
    public FindNextResult findNext(Clue clue, double theta, Set<Long> excluded) {
        searchSteps.clear();
        
        String w = clue.getKeyword();
        double d = clue.getDistance();
        double epsilon = clue.getEpsilon();
        
        // Line 2: lD ← d - d·ε; rD ← d + d·ε
        double lD = d - d * epsilon;
        double rD = d + d * epsilon;
        
        searchSteps.add(new SearchStep("INIT", "range", d, w));
        searchSteps.get(searchSteps.size()-1).result = String.format("lD=%.2f, rD=%.2f, θ=%.4f", lD, rD, theta);
        
        // Line 3: v_p and d_p ← BT(v_{i-1}).predecessor(lD, w)
        ABTreeEntry predecessor = findPredecessor(lD, w, excluded);
        
        // Line 4: v_s and d_s ← BT(v_{i-1}).successor(rD, w)
        ABTreeEntry successor = findSuccessor(rD, w, excluded);
        
        // No candidates found
        if (predecessor == null && successor == null) {
            SearchStep step = new SearchStep("NO_CANDIDATE", "result", d, w);
            step.result = "No valid candidate found";
            searchSteps.add(step);
            return FindNextResult.notFound();
        }
        
        // Line 5-8: Compare and return best candidate
        if (predecessor != null && successor != null) {
            // Both found: compare d - d_p vs d_s - d
            double diffP = d - predecessor.getDistance();
            double diffS = successor.getDistance() - d;
            
            if (diffP <= diffS) {
                // Line 6: return v_p with d_m(v_p)
                double dm = calculateMatchingDistance(predecessor.getDistance(), clue);
                if (dm >= theta) {
                    SearchStep step = new SearchStep("SELECT_PREDECESSOR", "result", d, w);
                    step.candidateVertex = predecessor.getVertexId();
                    step.candidateDistance = predecessor.getDistance();
                    step.result = String.format("d-d_p=%.2f ≤ d_s-d=%.2f, dm=%.4f", diffP, diffS, dm);
                    searchSteps.add(step);
                    return new FindNextResult(true, predecessor.getVertexId(), predecessor.getDistance(), dm);
                }
            } else {
                // Line 8: return v_s with d_m(v_s)
                double dm = calculateMatchingDistance(successor.getDistance(), clue);
                if (dm >= theta) {
                    SearchStep step = new SearchStep("SELECT_SUCCESSOR", "result", d, w);
                    step.candidateVertex = successor.getVertexId();
                    step.candidateDistance = successor.getDistance();
                    step.result = String.format("d_s-d=%.2f < d-d_p=%.2f, dm=%.4f", diffS, diffP, dm);
                    searchSteps.add(step);
                    return new FindNextResult(true, successor.getVertexId(), successor.getDistance(), dm);
                }
            }
        } else if (predecessor != null) {
            double dm = calculateMatchingDistance(predecessor.getDistance(), clue);
            if (dm >= theta) {
                SearchStep step = new SearchStep("SELECT_PREDECESSOR", "result", d, w);
                step.candidateVertex = predecessor.getVertexId();
                step.candidateDistance = predecessor.getDistance();
                step.result = String.format("Only predecessor found, dm=%.4f", dm);
                searchSteps.add(step);
                return new FindNextResult(true, predecessor.getVertexId(), predecessor.getDistance(), dm);
            }
        } else {
            if (successor != null) {
                double dm = calculateMatchingDistance(successor.getDistance(), clue);
                if (dm >= theta) {
                    SearchStep step = new SearchStep("SELECT_SUCCESSOR", "result", d, w);
                    step.candidateVertex = successor.getVertexId();
                    step.candidateDistance = successor.getDistance();
                    step.result = String.format("Only successor found, dm=%.4f", dm);
                    searchSteps.add(step);
                    return new FindNextResult(true, successor.getVertexId(), successor.getDistance(), dm);
                }
            }
        }
        
        SearchStep step = new SearchStep("THRESHOLD_FAIL", "result", d, w);
        step.result = "Candidates found but dm < θ";
        searchSteps.add(step);
        return FindNextResult.notFound();
    }
    
    /**
     * Procedure Predecessor(lD, w, Node)
     * Find the largest distance ≤ lD with keyword w
     */
    private ABTreeEntry findPredecessor(double lD, String w, Set<Long> excluded) {
        return predecessorHelper(root, lD, w, excluded);
    }
    
    private ABTreeEntry predecessorHelper(ABTreeNode node, double lD, String w, Set<Long> excluded) {
        SearchStep step = new SearchStep("PREDECESSOR", node.isLeaf ? "leaf" : "internal", lD, w);
        
        // Line 9: if H(w) ∧ H(Node) = ∅ then return false
        if (!node.subtreeKeywords.contains(w)) {
            step.result = "H(w) ∧ H(Node) = ∅, skip subtree";
            searchSteps.add(step);
            return null;
        }
        
        if (node.isLeaf) {
            // Line 1-6: Leaf node - find entry with keyword w and d_p ≤ lD
            ABTreeEntry best = null;
            for (int i = node.entries.size() - 1; i >= 0; i--) {
                ABTreeEntry entry = node.entries.get(i);
                if (entry.getDistance() > lD) continue;
                if (entry.hasKeyword(w) && !excluded.contains(entry.getVertexId())) {
                    best = entry;
                    break;
                }
            }
            step.result = best != null ? 
                String.format("Found: v%d at %.2fm", best.getVertexId(), best.getDistance()) : 
                "No matching entry";
            if (best != null) {
                step.candidateVertex = best.getVertexId();
                step.candidateDistance = best.getDistance();
            }
            searchSteps.add(step);
            return best;
        } else {
            // Internal node - navigate to appropriate child
            // Line 11-13: if lD < Node.routing then search left subtree
            int i = node.keys.size() - 1;
            while (i >= 0 && lD < node.keys.get(i)) {
                i--;
            }
            
            // Try the found position and potentially siblings
            ABTreeEntry best = null;
            for (int j = i + 1; j >= 0 && j < node.children.size(); j--) {
                ABTreeEntry candidate = predecessorHelper(node.children.get(j), lD, w, excluded);
                if (candidate != null) {
                    if (best == null || candidate.getDistance() > best.getDistance()) {
                        best = candidate;
                    }
                    break; // Found in rightmost applicable subtree
                }
            }
            
            step.result = best != null ? 
                String.format("Best from children: v%d at %.2fm", best.getVertexId(), best.getDistance()) :
                "No result from children";
            searchSteps.add(step);
            return best;
        }
    }
    
    /**
     * Find smallest distance ≥ some lower bound with keyword w, but ≤ rD
     */
    private ABTreeEntry findSuccessor(double rD, String w, Set<Long> excluded) {
        return successorHelper(root, rD, w, excluded);
    }
    
    private ABTreeEntry successorHelper(ABTreeNode node, double rD, String w, Set<Long> excluded) {
        SearchStep step = new SearchStep("SUCCESSOR", node.isLeaf ? "leaf" : "internal", rD, w);
        
        if (!node.subtreeKeywords.contains(w)) {
            step.result = "H(w) ∧ H(Node) = ∅, skip subtree";
            searchSteps.add(step);
            return null;
        }
        
        if (node.isLeaf) {
            // Find entry with keyword w and d_s ≤ rD, closest to rD from below
            ABTreeEntry best = null;
            for (ABTreeEntry entry : node.entries) {
                if (entry.getDistance() > rD) break;
                if (entry.hasKeyword(w) && !excluded.contains(entry.getVertexId())) {
                    if (best == null || entry.getDistance() > best.getDistance()) {
                        best = entry;
                    }
                }
            }
            step.result = best != null ?
                String.format("Found: v%d at %.2fm", best.getVertexId(), best.getDistance()) :
                "No matching entry";
            if (best != null) {
                step.candidateVertex = best.getVertexId();
                step.candidateDistance = best.getDistance();
            }
            searchSteps.add(step);
            return best;
        } else {
            // Internal node
            ABTreeEntry best = null;
            for (int j = 0; j < node.children.size(); j++) {
                // Skip children that are entirely > rD
                if (j < node.keys.size() && node.keys.get(j) > rD) {
                    // This child might still have entries ≤ rD
                }
                ABTreeEntry candidate = successorHelper(node.children.get(j), rD, w, excluded);
                if (candidate != null) {
                    if (best == null || candidate.getDistance() > best.getDistance()) {
                        best = candidate;
                    }
                }
            }
            
            step.result = best != null ?
                String.format("Best from children: v%d at %.2fm", best.getVertexId(), best.getDistance()) :
                "No result from children";
            searchSteps.add(step);
            return best;
        }
    }
    
    /**
     * Calculate matching distance according to Equation 1
     */
    private double calculateMatchingDistance(double networkDistance, Clue clue) {
        return Math.abs(networkDistance - clue.getDistance()) / (clue.getEpsilon() * clue.getDistance());
    }
    
    /**
     * Get all entries in distance range [minD, maxD] with keyword w
     */
    public List<ABTreeEntry> rangeQuery(double minD, double maxD, String w) {
        List<ABTreeEntry> results = new ArrayList<>();
        rangeQueryHelper(root, minD, maxD, w, results);
        return results;
    }
    
    private void rangeQueryHelper(ABTreeNode node, double minD, double maxD, String w, List<ABTreeEntry> results) {
        if (!node.subtreeKeywords.contains(w)) return;
        
        if (node.isLeaf) {
            for (ABTreeEntry entry : node.entries) {
                if (entry.getDistance() > maxD) break;
                if (entry.getDistance() >= minD && entry.hasKeyword(w)) {
                    results.add(entry);
                }
            }
        } else {
            for (int i = 0; i < node.children.size(); i++) {
                // Check if this subtree could contain entries in range
                boolean couldContain = true;
                if (i < node.keys.size() && node.keys.get(i) < minD) {
                    // Check next subtree
                }
                if (i > 0 && node.keys.get(i-1) > maxD) {
                    couldContain = false;
                }
                if (couldContain) {
                    rangeQueryHelper(node.children.get(i), minD, maxD, w, results);
                }
            }
        }
    }
    
    /**
     * Print tree structure for debugging
     */
    public void printTree() {
        System.out.println("=== AB-Tree for source vertex " + sourceVertex + " ===");
        System.out.println("Nodes: " + nodeCount + ", Entries: " + entryCount);
        printNode(root, 0);
    }
    
    private void printNode(ABTreeNode node, int depth) {
        String indent = "  ".repeat(depth);
        if (node.isLeaf) {
            System.out.println(indent + "LEAF [" + node.entries.size() + " entries, keywords: " + node.subtreeKeywords + "]");
            for (ABTreeEntry e : node.entries) {
                System.out.println(indent + "  " + e);
            }
        } else {
            System.out.println(indent + "INTERNAL [keys: " + node.keys + ", keywords: " + node.subtreeKeywords + "]");
            for (int i = 0; i < node.children.size(); i++) {
                if (i < node.keys.size()) {
                    System.out.println(indent + "  --- key: " + node.keys.get(i) + " ---");
                }
                printNode(node.children.get(i), depth + 1);
            }
        }
    }
}
