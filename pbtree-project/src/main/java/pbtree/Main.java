package pbtree;

import java.util.*;
import pbtree.algorithm.*;
import pbtree.model.*;
import pbtree.parser.*;
import pbtree.visualization.*;

/**
 * Main entry point for PB-Tree algorithm demonstration.
 * 
 * PB-Tree (Pivot reverse Binary Tree) is a space-efficient alternative to AB-Tree
 * for the findNext() procedure in clue-based route search.
 * 
 * Key features:
 * - Uses 2-hop label for distance computation
 * - Stores only pivot-based indices instead of per-vertex trees
 * - Space complexity: O(|L| × h) vs AB-Tree's O(|V|²)
 * - Main memory based, avoiding disk I/O
 * 
 * Usage:
 *   java pbtree.Main <osm_file> [--interactive]
 *   java pbtree.Main <osm_file> [--benchmark]
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java pbtree.Main <osm_file> [--interactive|--benchmark]");
            System.out.println("  --interactive: Interactive mode for testing queries");
            System.out.println("  --benchmark: Run performance comparison");
            return;
        }
        
        String osmFile = args[0];
        boolean interactive = args.length > 1 && "--interactive".equals(args[1]);
        boolean benchmark = args.length > 1 && "--benchmark".equals(args[1]);
        
        // Parse OSM data
        System.out.println("Parsing OSM file: " + osmFile);
        RoadNetwork network = OSMParser.parse(osmFile);
        network.printStatistics();
        
        // Build 2-hop label index
        System.out.println("\n=== Building 2-Hop Label Index ===");
        TwoHopLabel labelIndex = new TwoHopLabel(network);
        labelIndex.build();
        
        // Export graph data for visualization
        HtmlVisualizer visualizer = new HtmlVisualizer(network);
        visualizer.exportGraphData("graph_data.json");
        
        if (interactive) {
            runInteractiveMode(network, labelIndex);
        } else if (benchmark) {
            runBenchmark(network, labelIndex);
        } else {
            runDefaultDemo(network, labelIndex, visualizer);
        }
    }
    
    private static void runDefaultDemo(RoadNetwork network, TwoHopLabel labelIndex, 
                                        HtmlVisualizer visualizer) throws Exception {
        System.out.println("\n=== Running Default Demo ===");
        
        // Find a good source vertex (one with edges and keywords)
        long sourceVertex = -1;
        for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
            if (entry.getValue().size() > 3) {
                Node node = network.getNode(entry.getKey());
                if (node != null && !node.getKeywords().isEmpty()) {
                    sourceVertex = entry.getKey();
                    break;
                }
            }
        }
        
        if (sourceVertex == -1) {
            // Fallback: any vertex with edges
            for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    sourceVertex = entry.getKey();
                    break;
                }
            }
        }
        
        if (sourceVertex == -1) {
            System.out.println("No suitable source vertex found");
            return;
        }
        
        System.out.println("Source vertex: " + sourceVertex);
        
        // Find common keywords
        List<String> keywords = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : network.getKeywordIndex().entrySet()) {
            if (entry.getValue().size() >= 5) {
                keywords.add(entry.getKey());
                if (keywords.size() >= 3) break;
            }
        }
        
        if (keywords.isEmpty()) {
            keywords.add("footway");
        }
        
        System.out.println("Using keywords: " + keywords);
        
        // Create query
        List<Clue> clues = new ArrayList<>();
        double distance = 100;
        for (String kw : keywords) {
            clues.add(new Clue(kw, distance, 0.5));
            distance += 100;
        }
        
        Query query = new Query(sourceVertex, clues);
        System.out.println("Query: " + query);
        
        // Run BAB with PB-Tree
        System.out.println("\n--- Running BAB with PB-Tree ---");
        BABWithPBTree babPBTree = new BABWithPBTree(network, labelIndex);
        SearchResult result = babPBTree.search(query);
        
        System.out.println("\n=== Results ===");
        if (result.hasPath()) {
            System.out.println("Best path: " + result.getBestPath());
            System.out.println("Matching distance: " + result.getMatchingDistance());
        } else {
            System.out.println("No feasible path found");
        }
        System.out.println("Execution time: " + result.getExecutionTimeMs() + " ms");
        System.out.println("Label build time: " + result.getLabelBuildTimeMs() + " ms");
        System.out.println("PB-Tree build time: " + result.getPbTreeBuildTimeMs() + " ms");
        System.out.println("Iterations: " + result.getTotalIterations());
        System.out.println("Search steps: " + result.getSearchSteps().size());
        
        // Print detailed search steps
        System.out.println("\n=== Search Steps ===");
        for (SearchResult.SearchStep step : result.getSearchSteps()) {
            System.out.println(step);
            if (step.pivot != null) {
                System.out.println("  Via pivot: v" + step.pivot);
            }
        }
        
        // Generate visualization
        visualizer.generateStandaloneHtml("graph_data.json", "pbtree_visualization.html");
        System.out.println("\nVisualization generated: pbtree_visualization.html");
    }
    
    private static void runInteractiveMode(RoadNetwork network, TwoHopLabel labelIndex) {
        try (Scanner scanner = new Scanner(System.in)) {
            BABWithPBTree bab = new BABWithPBTree(network, labelIndex);
            
            System.out.println("\n=== Interactive Mode ===");
            System.out.println("Commands:");
            System.out.println("  findnext <source> <keyword> <distance> <epsilon> <theta> <ub>");
            System.out.println("  bab <source> <kw1,d1,e1> <kw2,d2,e2> ...");
            System.out.println("  pbtree <pivot> - Show PB-Tree for pivot");
            System.out.println("  label <vertex> - Show 2-hop label for vertex");
            System.out.println("  distance <u> <v> - Query distance using labels");
            System.out.println("  quit");
            
            while (true) {
                System.out.print("\n> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("\\s+");
                String cmd = parts[0].toLowerCase();
                
                try {
                    if ("quit".equals(cmd) || "exit".equals(cmd)) {
                        break;
                } else if ("findnext".equals(cmd) && parts.length >= 7) {
                    long source = Long.parseLong(parts[1]);
                    String keyword = parts[2];
                    double distance = Double.parseDouble(parts[3]);
                    double epsilon = Double.parseDouble(parts[4]);
                    double theta = Double.parseDouble(parts[5]);
                    double ub = Double.parseDouble(parts[6]);
                    
                    Clue clue = new Clue(keyword, distance, epsilon);
                    BABWithPBTree.FindNextWithPBResult result = bab.findNext(
                        source, clue, theta, ub, new HashSet<>()
                    );
                    
                    if (result.found) {
                        System.out.println("Found: vertex " + result.vertexId);
                        System.out.println("  Network distance: " + result.distance + " m");
                        System.out.println("  Matching distance: " + result.matchingDistance);
                        System.out.println("  Via pivot: v" + result.viaPivot);
                    } else {
                        System.out.println("No candidate found");
                    }
                    
                    System.out.println("\nPB-Tree search steps:");
                    for (PBTree.SearchStep step : result.pbTreeSteps) {
                        System.out.println("  " + step.action + " [" + step.nodeType + "] " + 
                            (step.candidateVertex != null ? "v" + step.candidateVertex : "") + 
                            " - " + step.result);
                    }
                    
                } else if ("bab".equals(cmd) && parts.length >= 3) {
                    long source = Long.parseLong(parts[1]);
                    List<Clue> clues = new ArrayList<>();
                    
                    for (int i = 2; i < parts.length; i++) {
                        String[] clueparts = parts[i].split(",");
                        if (clueparts.length >= 3) {
                            clues.add(new Clue(clueparts[0], 
                                Double.parseDouble(clueparts[1]),
                                Double.parseDouble(clueparts[2])));
                        }
                    }
                    
                    Query query = new Query(source, clues);
                    System.out.println("Query: " + query);
                    
                    SearchResult result = bab.search(query);
                    
                    if (result.hasPath()) {
                        System.out.println("Best path: " + result.getBestPath());
                        System.out.println("Matching distance: " + result.getMatchingDistance());
                    } else {
                        System.out.println("No feasible path found");
                    }
                    System.out.println("Time: " + result.getExecutionTimeMs() + " ms");
                    
                } else if ("pbtree".equals(cmd) && parts.length >= 2) {
                    long pivot = Long.parseLong(parts[1]);
                    System.out.println("Building PB-Tree for pivot v" + pivot + "...");
                    long start = System.currentTimeMillis();
                    PBTree tree = PBTree.buildForPivot(pivot, labelIndex, network);
                    System.out.println("Built in " + (System.currentTimeMillis() - start) + " ms");
                    tree.printTree();
                    
                } else if ("label".equals(cmd) && parts.length >= 2) {
                    long vertex = Long.parseLong(parts[1]);
                    List<LabelEntry> label = labelIndex.getLabel(vertex);
                    System.out.println("L(v" + vertex + ") = " + label.size() + " entries:");
                    for (LabelEntry entry : label) {
                        System.out.println("  " + entry);
                    }
                    
                } else if ("distance".equals(cmd) && parts.length >= 3) {
                    long u = Long.parseLong(parts[1]);
                    long v = Long.parseLong(parts[2]);
                    
                    long start = System.currentTimeMillis();
                    double distLabel = labelIndex.queryDistance(u, v);
                    long labelTime = System.currentTimeMillis() - start;
                    
                    start = System.currentTimeMillis();
                    double distDijkstra = network.getNetworkDistance(u, v);
                    long dijkstraTime = System.currentTimeMillis() - start;
                    
                    System.out.println("Distance d_G(v" + u + ", v" + v + "):");
                    System.out.println("  Via 2-hop label: " + String.format("%.2f", distLabel) + " m (" + labelTime + " ms)");
                    System.out.println("  Via Dijkstra:    " + String.format("%.2f", distDijkstra) + " m (" + dijkstraTime + " ms)");
                    
                } else {
                    System.out.println("Unknown command. Type 'quit' to exit.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
    
    private static void runBenchmark(RoadNetwork network, TwoHopLabel labelIndex) {
        System.out.println("\n=== Performance Benchmark ===");
        
        // Collect test vertices
        List<Long> testVertices = new ArrayList<>();
        for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
            if (entry.getValue().size() > 2) {
                testVertices.add(entry.getKey());
                if (testVertices.size() >= 100) break;
            }
        }
        
        if (testVertices.size() < 2) {
            System.out.println("Not enough vertices for benchmark");
            return;
        }
        
        // Benchmark distance queries
        System.out.println("\n--- Distance Query Benchmark ---");
        Random rand = new Random(42);
        int numQueries = Math.min(1000, testVertices.size() * testVertices.size() / 2);
        
        // 2-hop label queries
        long labelStart = System.currentTimeMillis();
        @SuppressWarnings("unused")
        double labelTotal = 0;
        for (int i = 0; i < numQueries; i++) {
            long u = testVertices.get(rand.nextInt(testVertices.size()));
            long v = testVertices.get(rand.nextInt(testVertices.size()));
            labelTotal += labelIndex.queryDistance(u, v);
        }
        long labelTime = System.currentTimeMillis() - labelStart;
        
        System.out.println("2-Hop Label: " + numQueries + " queries in " + labelTime + " ms");
        System.out.println("  Average: " + String.format("%.3f", (double)labelTime / numQueries) + " ms/query");
        
        // Benchmark findNext with PB-Tree
        System.out.println("\n--- findNext() Benchmark ---");
        
        List<String> keywords = new ArrayList<>(network.getKeywordIndex().keySet());
        if (keywords.isEmpty()) {
            System.out.println("No keywords for benchmark");
            return;
        }
        
        BABWithPBTree bab = new BABWithPBTree(network, labelIndex);
        
        int findNextQueries = Math.min(100, testVertices.size());
        long findNextStart = System.currentTimeMillis();
        int found = 0;
        
        for (int i = 0; i < findNextQueries; i++) {
            long source = testVertices.get(i);
            String keyword = keywords.get(rand.nextInt(keywords.size()));
            Clue clue = new Clue(keyword, 200, 0.5);
            
            BABWithPBTree.FindNextWithPBResult result = bab.findNext(source, clue, 0, 1.0, new HashSet<>());
            if (result.found) found++;
        }
        
        long findNextTime = System.currentTimeMillis() - findNextStart;
        System.out.println("findNext with PB-Tree: " + findNextQueries + " queries in " + findNextTime + " ms");
        System.out.println("  Average: " + String.format("%.3f", (double)findNextTime / findNextQueries) + " ms/query");
        System.out.println("  Found: " + found + "/" + findNextQueries);
        
        // Memory usage
        System.out.println("\n--- Memory Usage ---");
        System.out.println("2-Hop Label size: " + labelIndex.getTotalLabelSize() + " entries");
        System.out.println("Estimated PB-Tree memory: " + bab.getMemoryUsage() / 1024 + " KB");
    }
}
