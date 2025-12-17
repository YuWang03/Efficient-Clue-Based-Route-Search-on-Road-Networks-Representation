# PB-Tree: Pivot reverse Binary Tree for Clue-Based Route Search

This project implements the **PB-Tree (Pivot reverse Binary Tree)** algorithm for efficient `findNext()` queries in clue-based route search on road networks.

## Overview

PB-Tree is a space-efficient alternative to AB-Tree, based on the 2-hop label technique. It significantly reduces index space while maintaining high query efficiency.

### Key Features

- **2-Hop Label Index**: Uses pivot vertices for distance computation
- **Space Efficient**: O(|L| × h) vs AB-Tree's O(|V|²)
- **Main Memory Based**: No disk I/O overhead
- **Fast Predecessor/Successor Queries**: O(log |L|/|V|) expected time

### Algorithm Concept

The distance between any two vertices u and v is computed through a common pivot o:

```
d_G(u, v) = d_G(u, o) + d_G(o, v)
```

For each pivot o, we build a PB(o) tree containing all vertices that can be reached through o, organized by distance.

## Project Structure

```
pbtree-project/
├── src/main/java/pbtree/
│   ├── Main.java                    # Entry point
│   ├── model/
│   │   ├── Node.java                # Graph vertex
│   │   ├── Edge.java                # Graph edge
│   │   ├── RoadNetwork.java         # Road network graph
│   │   ├── Query.java               # Query Q = (vq, C)
│   │   ├── Clue.java                # Clue m(w, d, ε)
│   │   ├── LabelEntry.java          # 2-hop label entry (pivot, distance)
│   │   └── PREntry.java             # Pivot reverse entry
│   ├── algorithm/
│   │   ├── TwoHopLabel.java         # 2-hop label index construction
│   │   ├── PBTree.java              # PB-Tree implementation
│   │   ├── BABWithPBTree.java       # BAB search with PB-Tree
│   │   └── SearchResult.java        # Search result container
│   ├── parser/
│   │   └── OSMParser.java           # OpenStreetMap parser
│   └── visualization/
│       └── HtmlVisualizer.java      # HTML visualization generator
├── lib/
│   └── gson-2.10.1.jar              # JSON library
├── run.sh                           # Unix build/run script
├── run.bat                          # Windows build/run script
└── README.md
```

## Usage

### Build and Run

**Windows (PowerShell):**
```powershell
.\run.bat ..\map.osm
```

**Unix/Mac:**
```bash
chmod +x run.sh
./run.sh ../map.osm
```

**Manual compilation:**
```bash
# Compile
javac -cp "lib/*" -d bin src/main/java/pbtree/**/*.java src/main/java/pbtree/*.java

# Run
java -cp "bin;lib/*" pbtree.Main <osm_file>
```

### Command Line Options

```
java pbtree.Main <osm_file> [options]

Options:
  --interactive    Interactive mode for testing queries
  --benchmark      Run performance benchmark
```

### Interactive Mode Commands

```
findnext <source> <keyword> <distance> <epsilon> <theta> <ub>
    Find next candidate vertex using PB-Tree

bab <source> <kw1,d1,e1> <kw2,d2,e2> ...
    Run BAB search with multiple clues

pbtree <pivot>
    Show PB-Tree structure for a pivot

label <vertex>
    Show 2-hop label for a vertex

distance <u> <v>
    Query distance between two vertices

quit
    Exit interactive mode
```

## Algorithm Details

### 2-Hop Label Construction

1. Order vertices by degree (higher degree = higher priority)
2. Run pruned BFS from each pivot
3. Add label entries (pivot, distance) to each reachable vertex
4. Sort labels by distance

### PB-Tree Construction

For each pivot o, PB(o) contains:
- All vertices v that have o in their label L(v)
- Each entry stores (vertex, distance to pivot, keywords)
- Organized as a B-Tree for efficient range queries

### findNext() with PB-Tree

Given current vertex v_{i-1} and clue m(w, d, ε):

1. For each pivot o in L(v_{i-1}):
   - Compute adjusted distance bounds for PB(o)
   - Run predecessor/successor queries
   - Verify shortest path through o
   - Update best candidate

2. Pruning:
   - Skip pivots where d_G(v_{i-1}, o) > rB
   - Update bounds when candidates are found

## Example

From the paper (Example 6):
```
Query: Q = (v7, {(w1, 6, 0.5), (w2, 4, 0.5), (w4, 5, 0.5)})
Stack: (v7, v4, v3)
UB = 0.4

Step 1: Check PB(v3) with d_G(v3, v3) = 0
        Successor query → no result
        Predecessor query → v1 found
        d_G(v3, v1) = 4 ≤ lB_v3 = 4 ✓

Step 2: Check PB(v4), PB(v6) → no candidates

Result: Report v1 with d_m^3(v1) = 0.4
```

## Performance Comparison

| Metric | AB-Tree | PB-Tree |
|--------|---------|---------|
| Space | O(\|V\|²) | O(\|L\| × h) |
| Storage | Disk-based | Main memory |
| Build Time | High | Moderate |
| Query Time | O(log n) | O(log \|L\|/\|V\|) |

## References

Based on: "Efficient Clue-Based Route Search on Road Networks"
- Section 5.3: Keyword-Based Label Approach
- Algorithm 4: findNext() procedure
- PB-Tree: Pivot reverse Binary Tree

## License

This is an academic implementation for educational purposes.
