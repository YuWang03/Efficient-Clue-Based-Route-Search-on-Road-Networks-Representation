package bab.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Query Q = (vq, C) as defined in Definition 2
 * 
 * A clue-based route query consists of:
 * - vq: source vertex (starting point)
 * - C: sequence of clues C = {m1, m2, ..., mk}
 */
public class Query {
    private final long sourceVertex;  // vq: source vertex
    private final List<Clue> clues;   // C: sequence of clues

    public Query(long sourceVertex, List<Clue> clues) {
        if (clues == null || clues.isEmpty()) {
            throw new IllegalArgumentException("Query must have at least one clue");
        }
        this.sourceVertex = sourceVertex;
        this.clues = new ArrayList<>(clues);
    }

    public long getSourceVertex() {
        return sourceVertex;
    }

    public List<Clue> getClues() {
        return Collections.unmodifiableList(clues);
    }

    public Clue getClue(int index) {
        return clues.get(index);
    }

    public int getClueCount() {
        return clues.size();
    }

    @Override
    public String toString() {
        return String.format("Q(%d, %s)", sourceVertex, clues);
    }
}
