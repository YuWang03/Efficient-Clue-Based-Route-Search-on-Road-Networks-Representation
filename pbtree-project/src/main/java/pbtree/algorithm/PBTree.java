package pbtree.algorithm;

import java.util.*;
import pbtree.model.*;

/**
 * PB-Tree: Pivot reverse Binary Tree for efficient findNext() queries.
 * 
 * Based on Section 5.3 of the paper:
 * - PB(o) stores all label entries regarding vertex o as pivot
 * - Each PB(o) is organized as a binary tree sorted by distance
 * - Uses 2-hop label for distance computation: d_G(u,v) = d_G(u,o) + d_G(o,v)
 * 
 * Key advantage over AB-tree:
 * - Much smaller space: O(|L| * h) instead of O(|V|^2)
 * - Main memory based instead of disk-based
 * - Still provides fast predecessor/successor queries
 */
public class PBTree {
    private final long pivotVertex;              // o: the pivot this PB-tree is for
    private PBTreeNode root;
    private int nodeCount;
    private int entryCount;
    
    // For visualization: track search steps
    private List<SearchStep> searchSteps;
    
    private static final int ORDER = 32; // B-Tree order
    
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
     * PB-Tree Node (internal or leaf)
     */
    public static class PBTreeNode {
        boolean isLeaf;
        List<Double> keys;           // Routing keys (distances)
        List<PBTreeNode> children;   // Child pointers (for internal nodes)
        List<PREntry> entries;       // Entries (for leaf nodes)
        Set<String> subtreeKeywords; // H(Node): all keywords in subtree
        PBTreeNode next;             // Pointer to next leaf
        PBTreeNode prev;             // Pointer to previous leaf
        
        public PBTreeNode(boolean isLeaf) {
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
    
    /**
     * Result of findNext operation on PB-tree
     */
    public static class FindNextResult {
        public boolean found;
        public long vertexId;
        public double distanceFromPivot;  // d_G(o, v)
        public double matchingDistance;
        
        public FindNextResult(boolean found) {
            this.found = found;
        }
        
        public FindNextResult(long vertexId, double distanceFromPivot, double matchingDistance) {
            this.found = true;
            this.vertexId = vertexId;
            this.distanceFromPivot = distanceFromPivot;
            this.matchingDistance = matchingDistance;
        }
    }
    
    public PBTree(long pivotVertex) {
        this.pivotVertex = pivotVertex;
        this.root = new PBTreeNode(true);
        this.nodeCount = 1;
        this.entryCount = 0;
        this.searchSteps = new ArrayList<>();
    }
    
    public long getPivotVertex() { return pivotVertex; }
    public int getNodeCount() { return nodeCount; }
    public int getEntryCount() { return entryCount; }
    public List<SearchStep> getSearchSteps() { return searchSteps; }
    public void clearSearchSteps() { searchSteps.clear(); }
    
    /**
     * Build PB-Tree for a pivot from the 2-hop labels.
     * Collects all (v, h_{v,o}) entries from vertices that have o as pivot.
     */
    public static PBTree buildForPivot(long pivot, TwoHopLabel labelIndex, RoadNetwork network) {
        PBTree tree = new PBTree(pivot);
        
        // Collect all vertices that have this pivot in their label
        List<PREntry> entries = new ArrayList<>();
        
        for (long v : network.getAllNodeIds()) {
            List<LabelEntry> label = labelIndex.getLabel(v);
            for (LabelEntry entry : label) {
                if (entry.getPivot() == pivot) {
                    Node node = network.getNode(v);
                    Set<String> keywords = (node != null) ? node.getKeywords() : new HashSet<>();
                    if (!keywords.isEmpty()) {
                        entries.add(new PREntry(v, entry.getDistance(), keywords));
                    }
                    break; // Each vertex appears at most once per pivot
                }
            }
        }
        
        // Sort by distance and insert
        entries.sort(Comparator.comparingDouble(PREntry::getDistance));
        for (PREntry entry : entries) {
            tree.insert(entry);
        }
        
        return tree;
    }
    
    /**
     * Insert an entry into the PB-Tree.
     */
    public void insert(PREntry entry) {
        root.subtreeKeywords.addAll(entry.getKeywords());
        
        if (root.isLeaf) {
            insertIntoLeaf(root, entry);
            if (root.entries.size() > ORDER - 1) {
                PBTreeNode newRoot = new PBTreeNode(false);
                newRoot.children.add(root);
                newRoot.subtreeKeywords.addAll(root.subtreeKeywords);
                splitChild(newRoot, 0);
                root = newRoot;
            }
        } else {
            insertNonFull(root, entry);
            if (root.keys.size() > ORDER - 1) {
                PBTreeNode newRoot = new PBTreeNode(false);
                newRoot.children.add(root);
                newRoot.subtreeKeywords.addAll(root.subtreeKeywords);
                splitChild(newRoot, 0);
                root = newRoot;
            }
        }
        entryCount++;
    }
    
    private void insertIntoLeaf(PBTreeNode leaf, PREntry entry) {
        int pos = Collections.binarySearch(leaf.entries, entry);
        if (pos < 0) pos = -(pos + 1);
        leaf.entries.add(pos, entry);
        leaf.subtreeKeywords.addAll(entry.getKeywords());
    }
    
    private void insertNonFull(PBTreeNode node, PREntry entry) {
        node.subtreeKeywords.addAll(entry.getKeywords());
        
        if (node.isLeaf) {
            insertIntoLeaf(node, entry);
        } else {
            int i = node.keys.size() - 1;
            while (i >= 0 && entry.getDistance() < node.keys.get(i)) {
                i--;
            }
            i++;
            
            PBTreeNode child = node.children.get(i);
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
    
    private void splitChild(PBTreeNode parent, int index) {
        PBTreeNode child = parent.children.get(index);
        PBTreeNode newNode = new PBTreeNode(child.isLeaf);
        nodeCount++;
        
        int mid;
        double midKey;
        
        if (child.isLeaf) {
            mid = child.entries.size() / 2;
            midKey = child.entries.get(mid).getDistance();
            
            newNode.entries.addAll(child.entries.subList(mid, child.entries.size()));
            child.entries.subList(mid, child.entries.size()).clear();
            
            // Update leaf chain
            newNode.next = child.next;
            if (child.next != null) {
                child.next.prev = newNode;
            }
            child.next = newNode;
            newNode.prev = child;
            
            // Update subtree keywords
            newNode.subtreeKeywords.clear();
            for (PREntry e : newNode.entries) {
                newNode.subtreeKeywords.addAll(e.getKeywords());
            }
            child.subtreeKeywords.clear();
            for (PREntry e : child.entries) {
                child.subtreeKeywords.addAll(e.getKeywords());
            }
        } else {
            mid = child.keys.size() / 2;
            midKey = child.keys.get(mid);
            
            newNode.keys.addAll(child.keys.subList(mid + 1, child.keys.size()));
            newNode.children.addAll(child.children.subList(mid + 1, child.children.size()));
            
            child.keys.subList(mid, child.keys.size()).clear();
            child.children.subList(mid + 1, child.children.size()).clear();
            
            // Update subtree keywords
            updateSubtreeKeywords(child);
            updateSubtreeKeywords(newNode);
        }
        
        parent.keys.add(index, midKey);
        parent.children.add(index + 1, newNode);
    }
    
    private void updateSubtreeKeywords(PBTreeNode node) {
        node.subtreeKeywords.clear();
        if (node.isLeaf) {
            for (PREntry e : node.entries) {
                node.subtreeKeywords.addAll(e.getKeywords());
            }
        } else {
            for (PBTreeNode child : node.children) {
                node.subtreeKeywords.addAll(child.subtreeKeywords);
            }
        }
    }
    
    /**
     * Predecessor query: Find largest distance ≤ lDo with keyword w
     * Returns vertex in distance range [lBo, lDo]
     */
    public FindNextResult predecessor(String keyword, double lBo, double lDo, 
                                       Set<Long> excluded, double expectedDist, double epsilon) {
        searchSteps.clear();
        
        if (!root.subtreeKeywords.contains(keyword.toLowerCase())) {
            SearchStep step = new SearchStep("PREDECESSOR", "ROOT", lDo, keyword);
            step.result = "Keyword not in tree";
            searchSteps.add(step);
            return new FindNextResult(false);
        }
        
        // Find predecessor: largest distance ≤ lDo with keyword, distance ≥ lBo
        PREntry result = findPredecessor(root, keyword.toLowerCase(), lBo, lDo, excluded);
        
        if (result != null) {
            double dm = Math.abs(result.getDistance() - expectedDist) / (expectedDist * epsilon);
            SearchStep step = new SearchStep("PREDECESSOR", "FOUND", result.getDistance(), keyword);
            step.candidateVertex = result.getVertexId();
            step.candidateDistance = result.getDistance();
            step.result = "Found v" + result.getVertexId() + " at distance " + 
                         String.format("%.2f", result.getDistance());
            searchSteps.add(step);
            return new FindNextResult(result.getVertexId(), result.getDistance(), dm);
        }
        
        SearchStep step = new SearchStep("PREDECESSOR", "LEAF", lDo, keyword);
        step.result = "No match found in range [" + String.format("%.2f", lBo) + ", " + 
                     String.format("%.2f", lDo) + "]";
        searchSteps.add(step);
        return new FindNextResult(false);
    }
    
    private PREntry findPredecessor(PBTreeNode node, String keyword, double lBo, double lDo, 
                                     Set<Long> excluded) {
        if (!node.subtreeKeywords.contains(keyword)) {
            return null;
        }
        
        if (node.isLeaf) {
            // Search from right to left (largest to smallest)
            for (int i = node.entries.size() - 1; i >= 0; i--) {
                PREntry entry = node.entries.get(i);
                double dist = entry.getDistance();
                
                if (dist > lDo) continue;  // Too far
                if (dist < lBo) break;     // Below range
                
                if (entry.hasKeyword(keyword) && !excluded.contains(entry.getVertexId())) {
                    return entry;
                }
            }
            
            // Check previous leaves
            PBTreeNode prev = node.prev;
            while (prev != null) {
                if (!prev.subtreeKeywords.contains(keyword)) {
                    prev = prev.prev;
                    continue;
                }
                for (int i = prev.entries.size() - 1; i >= 0; i--) {
                    PREntry entry = prev.entries.get(i);
                    double dist = entry.getDistance();
                    
                    if (dist < lBo) return null;  // Below range
                    if (dist > lDo) continue;
                    
                    if (entry.hasKeyword(keyword) && !excluded.contains(entry.getVertexId())) {
                        return entry;
                    }
                }
                prev = prev.prev;
            }
            return null;
        } else {
            // Internal node: find the rightmost child with distance ≤ lDo
            int i = node.keys.size() - 1;
            while (i >= 0 && node.keys.get(i) > lDo) {
                i--;
            }
            
            // Search children from right to left
            for (int j = i + 1; j >= 0 && j < node.children.size(); j--) {
                PBTreeNode child = node.children.get(j);
                if (child.subtreeKeywords.contains(keyword)) {
                    PREntry result = findPredecessor(child, keyword, lBo, lDo, excluded);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }
    }
    
    /**
     * Successor query: Find smallest distance ≥ rDo with keyword w
     * Returns vertex in distance range [rDo, rBo]
     */
    public FindNextResult successor(String keyword, double rDo, double rBo, 
                                     Set<Long> excluded, double expectedDist, double epsilon) {
        if (!root.subtreeKeywords.contains(keyword.toLowerCase())) {
            SearchStep step = new SearchStep("SUCCESSOR", "ROOT", rDo, keyword);
            step.result = "Keyword not in tree";
            searchSteps.add(step);
            return new FindNextResult(false);
        }
        
        PREntry result = findSuccessor(root, keyword.toLowerCase(), rDo, rBo, excluded);
        
        if (result != null) {
            double dm = Math.abs(result.getDistance() - expectedDist) / (expectedDist * epsilon);
            SearchStep step = new SearchStep("SUCCESSOR", "FOUND", result.getDistance(), keyword);
            step.candidateVertex = result.getVertexId();
            step.candidateDistance = result.getDistance();
            step.result = "Found v" + result.getVertexId() + " at distance " + 
                         String.format("%.2f", result.getDistance());
            searchSteps.add(step);
            return new FindNextResult(result.getVertexId(), result.getDistance(), dm);
        }
        
        SearchStep step = new SearchStep("SUCCESSOR", "LEAF", rDo, keyword);
        step.result = "No match found in range [" + String.format("%.2f", rDo) + ", " + 
                     String.format("%.2f", rBo) + "]";
        searchSteps.add(step);
        return new FindNextResult(false);
    }
    
    private PREntry findSuccessor(PBTreeNode node, String keyword, double rDo, double rBo, 
                                   Set<Long> excluded) {
        if (!node.subtreeKeywords.contains(keyword)) {
            return null;
        }
        
        if (node.isLeaf) {
            // Search from left to right (smallest to largest)
            for (PREntry entry : node.entries) {
                double dist = entry.getDistance();
                
                if (dist < rDo) continue;  // Too close
                if (dist > rBo) break;     // Beyond range
                
                if (entry.hasKeyword(keyword) && !excluded.contains(entry.getVertexId())) {
                    return entry;
                }
            }
            
            // Check next leaves
            PBTreeNode next = node.next;
            while (next != null) {
                if (!next.subtreeKeywords.contains(keyword)) {
                    next = next.next;
                    continue;
                }
                for (PREntry entry : next.entries) {
                    double dist = entry.getDistance();
                    
                    if (dist > rBo) return null;  // Beyond range
                    if (dist < rDo) continue;
                    
                    if (entry.hasKeyword(keyword) && !excluded.contains(entry.getVertexId())) {
                        return entry;
                    }
                }
                next = next.next;
            }
            return null;
        } else {
            // Internal node: find the leftmost child with distance ≥ rDo
            int i = 0;
            while (i < node.keys.size() && node.keys.get(i) < rDo) {
                i++;
            }
            
            // Search children from left to right
            for (int j = i; j < node.children.size(); j++) {
                PBTreeNode child = node.children.get(j);
                if (child.subtreeKeywords.contains(keyword)) {
                    PREntry result = findSuccessor(child, keyword, rDo, rBo, excluded);
                    if (result != null) {
                        return result;
                    }
                }
            }
            return null;
        }
    }
    
    /**
     * Print tree structure for debugging.
     */
    public void printTree() {
        System.out.println("\n=== PB-Tree for pivot v" + pivotVertex + " ===");
        System.out.println("Nodes: " + nodeCount + ", Entries: " + entryCount);
        printNode(root, 0);
    }
    
    private void printNode(PBTreeNode node, int depth) {
        String indent = "  ".repeat(depth);
        if (node.isLeaf) {
            System.out.println(indent + "[LEAF] " + node.entries.size() + " entries, keywords: " + node.subtreeKeywords);
            for (PREntry entry : node.entries) {
                System.out.println(indent + "  " + entry);
            }
        } else {
            System.out.println(indent + "[INTERNAL] keys: " + node.keys + ", keywords: " + node.subtreeKeywords);
            for (int i = 0; i < node.children.size(); i++) {
                if (i < node.keys.size()) {
                    System.out.println(indent + "  < " + String.format("%.2f", node.keys.get(i)));
                }
                printNode(node.children.get(i), depth + 1);
            }
        }
    }
}
