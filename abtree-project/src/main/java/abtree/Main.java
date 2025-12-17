package abtree;

import abtree.algorithm.*;
import abtree.model.*;
import abtree.parser.*;
import abtree.visualization.*;
import java.util.*;

/**
 * Main entry point for AB-Tree algorithm demonstration.
 * 
 * Usage:
 *   java abtree.Main <osm_file> [--interactive]
 *   java abtree.Main <osm_file> [--benchmark]
 */
public class Main {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java abtree.Main <osm_file> [--interactive|--benchmark]");
            System.out.println("  --interactive: Interactive mode for testing queries");
            System.out.println("  --benchmark: Run performance comparison BAB vs BAB+ABTree");
            return;
        }
        
        String osmFile = args[0];
        boolean interactive = args.length > 1 && "--interactive".equals(args[1]);
        boolean benchmark = args.length > 1 && "--benchmark".equals(args[1]);
        
        // Parse OSM data
        System.out.println("Parsing OSM file: " + osmFile);
        RoadNetwork network = OSMParser.parse(osmFile);
        network.printStatistics();
        
        // Export graph data for visualization
        HtmlVisualizer visualizer = new HtmlVisualizer(network);
        visualizer.exportGraphData("graph_data.json");
        
        if (interactive) {
            runInteractiveMode(network);
        } else if (benchmark) {
            runBenchmark(network);
        } else {
            runDefaultDemo(network, visualizer);
        }
    }
    
    private static void runDefaultDemo(RoadNetwork network, HtmlVisualizer visualizer) throws Exception {
        System.out.println("\n=== Running Default Demo ===");
        
        // Find a good source vertex
        long sourceVertex = -1;
        for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
            if (entry.getValue().size() > 3) {
                sourceVertex = entry.getKey();
                break;
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
        
        // Run BAB with AB-Tree
        System.out.println("\n--- Running BAB with AB-Tree ---");
        BABWithABTree babABTree = new BABWithABTree(network);
        SearchResult result = babABTree.search(query);
        
        System.out.println("\n=== Results ===");
        if (result.hasPath()) {
            System.out.println("Best path: " + result.getBestPath());
            System.out.println("Matching distance: " + result.getMatchingDistance());
        } else {
            System.out.println("No feasible path found");
        }
        System.out.println("Execution time: " + result.getExecutionTimeMs() + " ms");
        System.out.println("AB-Tree build time: " + result.getAbTreeBuildTimeMs() + " ms");
        System.out.println("Iterations: " + result.getTotalIterations());
        System.out.println("Search steps: " + result.getSearchSteps().size());
        
        // Generate visualization
        visualizer.generateStandaloneHtml("graph_data.json", "abtree_visualization.html");
        System.out.println("\nVisualization generated: abtree_visualization.html");
    }
    
    private static void runInteractiveMode(RoadNetwork network) {
        try (Scanner scanner = new Scanner(System.in)) {
            BABWithABTree bab = new BABWithABTree(network);
            
            System.out.println("\n=== Interactive Mode ===");
            System.out.println("Commands:");
            System.out.println("  findnext <source> <keyword> <distance> <epsilon> <theta>");
            System.out.println("  bab <source> <kw1,d1,e1> <kw2,d2,e2> ...");
            System.out.println("  tree <source> - Build and inspect AB-Tree");
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
                } else if ("findnext".equals(cmd) && parts.length >= 6) {
                    long source = Long.parseLong(parts[1]);
                    String keyword = parts[2];
                    double distance = Double.parseDouble(parts[3]);
                    double epsilon = Double.parseDouble(parts[4]);
                    double theta = Double.parseDouble(parts[5]);
                    
                    System.out.println("Building AB-Tree for vertex " + source + "...");
                    long start = System.currentTimeMillis();
                    ABTree tree = ABTree.buildFromNetwork(network, source);
                    System.out.println("Built in " + (System.currentTimeMillis() - start) + " ms");
                    System.out.println("Tree: " + tree.getNodeCount() + " nodes, " + tree.getEntryCount() + " entries");
                    
                    Clue clue = new Clue(keyword, distance, epsilon);
                    ABTree.FindNextResult result = tree.findNext(clue, theta, new HashSet<>());
                    
                    if (result.found) {
                        System.out.println("Found: vertex " + result.vertexId);
                        System.out.println("  Network distance: " + result.distance + " m");
                        System.out.println("  Matching distance: " + result.matchingDistance);
                    } else {
                        System.out.println("No candidate found");
                    }
                    
                    System.out.println("\nSearch steps:");
                    for (ABTree.SearchStep step : tree.getSearchSteps()) {
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
                    
                } else if ("tree".equals(cmd) && parts.length >= 2) {
                    long source = Long.parseLong(parts[1]);
                    System.out.println("Building AB-Tree for vertex " + source + "...");
                    long start = System.currentTimeMillis();
                    ABTree tree = ABTree.buildFromNetwork(network, source);
                    System.out.println("Built in " + (System.currentTimeMillis() - start) + " ms");
                    tree.printTree();
                    
                } else {
                    System.out.println("Unknown command or invalid arguments");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            }
        }
    }
    
    private static void runBenchmark(RoadNetwork network) {
        System.out.println("\n=== Performance Benchmark ===");
        System.out.println("Comparing standard BAB vs BAB with AB-Tree\n");
        
        // Find source vertices
        List<Long> sources = new ArrayList<>();
        for (Map.Entry<Long, List<Edge>> entry : network.getAdjacencyList().entrySet()) {
            if (entry.getValue().size() > 3) {
                sources.add(entry.getKey());
                if (sources.size() >= 5) break;
            }
        }
        
        // Find keywords
        List<String> keywords = new ArrayList<>();
        for (Map.Entry<String, Set<Long>> entry : network.getKeywordIndex().entrySet()) {
            if (entry.getValue().size() >= 5 && entry.getValue().size() <= 50) {
                keywords.add(entry.getKey());
                if (keywords.size() >= 5) break;
            }
        }
        
        if (keywords.size() < 2) {
            System.out.println("Not enough keywords for benchmark");
            return;
        }
        
        BABWithABTree babABTree = new BABWithABTree(network);
        
        System.out.printf("%-12s %-8s %-10s %-10s %-8s%n", 
            "Source", "Clues", "Time(ms)", "TreeBuild", "Found");
        System.out.println("-".repeat(55));
        
        for (long source : sources) {
            for (int numClues = 2; numClues <= Math.min(4, keywords.size()); numClues++) {
                List<Clue> clues = new ArrayList<>();
                double dist = 100;
                for (int i = 0; i < numClues; i++) {
                    clues.add(new Clue(keywords.get(i % keywords.size()), dist, 0.5));
                    dist += 100;
                }
                
                Query query = new Query(source, clues);
                SearchResult result = babABTree.search(query);
                
                System.out.printf("%-12d %-8d %-10d %-10d %-8s%n",
                    source, numClues, result.getExecutionTimeMs(),
                    result.getAbTreeBuildTimeMs(),
                    result.hasPath() ? "Yes" : "No");
            }
        }
        
        System.out.println("\n" + babABTree.getTreeCacheStats());
    }
}
