package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * 搜尋結果 - 包含匹配節點和匹配距離
 */
public class SearchResult {
    private final Node matchVertex;
    private final double matchingDistance;
    private final double networkDistance;
    private final List<Node> pathNodes;
    private final Clue clue;
    
    public SearchResult(Node matchVertex, double matchingDistance, double networkDistance, 
                       List<Node> pathNodes, Clue clue) {
        this.matchVertex = matchVertex;
        this.matchingDistance = matchingDistance;
        this.networkDistance = networkDistance;
        this.pathNodes = pathNodes;
        this.clue = clue;
    }
    
    public Node getMatchVertex() { return matchVertex; }
    public double getMatchingDistance() { return matchingDistance; }
    public double getNetworkDistance() { return networkDistance; }
    public List<Node> getPathNodes() { return pathNodes; }
    public Clue getClue() { return clue; }
    
    public boolean isValid() { return matchVertex != null; }
    
    @Override
    public String toString() {
        if (!isValid()) return "SearchResult[無匹配]";
        return String.format("SearchResult[節點=%s, dm=%.4f, dG=%.2fm, clue=%s]",
            matchVertex.getName(), matchingDistance, networkDistance, clue);
    }
}
