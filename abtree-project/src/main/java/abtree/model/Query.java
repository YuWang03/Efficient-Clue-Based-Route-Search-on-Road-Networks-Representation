package abtree.model;

import java.util.*;

/**
 * Represents a query Q = (vq, C) where:
 * - vq: source vertex
 * - C: ordered sequence of clues [m1, m2, ..., mk]
 */
public class Query {
    private final long sourceVertex;  // vq
    private final List<Clue> clues;   // C = [m1, m2, ..., mk]
    
    public Query(long sourceVertex, List<Clue> clues) {
        this.sourceVertex = sourceVertex;
        this.clues = Collections.unmodifiableList(new ArrayList<>(clues));
    }
    
    public long getSourceVertex() { return sourceVertex; }
    public List<Clue> getClues() { return clues; }
    public int getClueCount() { return clues.size(); }
    public Clue getClue(int index) { return clues.get(index); }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Q = (v").append(sourceVertex).append(", [");
        for (int i = 0; i < clues.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(clues.get(i));
        }
        sb.append("])");
        return sb.toString();
    }
}
