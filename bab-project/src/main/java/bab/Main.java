package bab;

import bab.algorithm.BABAlgorithm;
import bab.algorithm.SearchResult;
import bab.model.Clue;
import bab.model.Query;
import bab.model.RoadNetwork;
import bab.parser.OSMParser;
import bab.visualization.HtmlVisualizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main Entry Point for BAB Route Search
 * 
 * Usage: java bab.Main [osm_file] [--interactive]
 * 
 * Example:
 *   java bab.Main map.osm
 *   java bab.Main map.osm --interactive
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  BAB Algorithm - Clue-Based Route Search");
        System.out.println("  IEEE TKDE 2017 Implementation");
        System.out.println("========================================\n");

        try {
            // Parse command line arguments
            String osmFile = args.length > 0 ? args[0] : "map.osm";
            boolean interactive = args.length > 1 && args[1].equals("--interactive");

            // Step 1: Parse OSM file
            System.out.println("[1/4] Parsing OSM file...");
            OSMParser parser = new OSMParser();
            RoadNetwork network = parser.parse(osmFile);
            
            // Print statistics
            network.printStatistics();
            network.printTopKeywords(15);

            // Step 2: Export graph data for visualization
            System.out.println("\n[2/4] Exporting graph data...");
            HtmlVisualizer visualizer = new HtmlVisualizer(network);
            visualizer.exportGraphToJson("graph_data.json");

            // Step 3: Run BAB algorithm
            System.out.println("\n[3/4] Running BAB algorithm...");
            BABAlgorithm bab = new BABAlgorithm(network);
            
            Query query;
            if (interactive) {
                query = createInteractiveQuery(network);
            } else {
                query = createDefaultQuery(network);
            }

            if (query != null) {
                System.out.println("\nExecuting query: " + query);
                SearchResult result = bab.execute(query);
                
                // Print results
                result.printDetails();
                
                // Export search trace
                visualizer.exportSearchTraceToJson(result, "search_trace.json");
            }

            // Step 4: Generate HTML visualization
            System.out.println("\n[4/4] Generating HTML visualization...");
            visualizer.generateHtmlVisualization("bab_visualization.html", "graph_data.json");

            System.out.println("\n========================================");
            System.out.println("  Output files:");
            System.out.println("    - graph_data.json");
            System.out.println("    - search_trace.json");
            System.out.println("    - bab_visualization.html");
            System.out.println("========================================");
            System.out.println("\nOpen bab_visualization.html in a browser to use the interactive visualization.");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Create a default query based on available keywords
     */
    private static Query createDefaultQuery(RoadNetwork network) {
        // Get a source vertex
        List<Long> connectedNodes = network.getConnectedNodeIds();
        if (connectedNodes.isEmpty()) {
            System.out.println("No connected nodes found!");
            return null;
        }
        long sourceVertex = connectedNodes.get(0);

        // Build clues from common keywords
        List<Clue> clues = new ArrayList<>();
        Map<String, Integer> keywordStats = network.getKeywordStatistics();

        // Find keywords with enough nodes
        List<String> usableKeywords = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : keywordStats.entrySet()) {
            if (entry.getValue() >= 5) {
                usableKeywords.add(entry.getKey());
            }
        }

        // Sort by frequency and pick top keywords
        usableKeywords.sort((a, b) -> keywordStats.get(b) - keywordStats.get(a));

        if (usableKeywords.size() >= 2) {
            // Create 2 clues with common keywords
            clues.add(new Clue(usableKeywords.get(0), 100, 0.5));
            clues.add(new Clue(usableKeywords.get(1), 150, 0.5));
        } else if (usableKeywords.size() == 1) {
            clues.add(new Clue(usableKeywords.get(0), 100, 0.5));
        } else {
            System.out.println("Not enough keywords with sufficient nodes for query.");
            return null;
        }

        return new Query(sourceVertex, clues);
    }

    /**
     * Create query interactively from user input
     */
    private static Query createInteractiveQuery(RoadNetwork network) {
        try (Scanner scanner = new Scanner(System.in)) {
            // Show available source vertices
            List<Long> connectedNodes = network.getConnectedNodeIds();
            System.out.println("\nAvailable source vertices (showing first 10):");
            for (int i = 0; i < Math.min(10, connectedNodes.size()); i++) {
                long nodeId = connectedNodes.get(i);
                System.out.printf("  %d: %s%n", nodeId, network.getNode(nodeId).getKeywords());
            }

        System.out.print("\nEnter source vertex ID: ");
        long sourceVertex = Long.parseLong(scanner.nextLine().trim());

        // Show available keywords
        System.out.println("\nTop keywords:");
        network.printTopKeywords(20);

        // Get clues
        List<Clue> clues = new ArrayList<>();
        System.out.println("\nEnter clues (empty keyword to finish):");
        
        int clueNum = 1;
        while (true) {
            System.out.printf("\nClue %d:%n", clueNum);
            System.out.print("  Keyword: ");
            String keyword = scanner.nextLine().trim();
            
            if (keyword.isEmpty()) break;
            
            System.out.print("  Distance (m): ");
            double distance = Double.parseDouble(scanner.nextLine().trim());
            
            System.out.print("  Epsilon (0-1): ");
            double epsilon = Double.parseDouble(scanner.nextLine().trim());
            
            clues.add(new Clue(keyword, distance, epsilon));
            clueNum++;
        }

        if (clues.isEmpty()) {
            System.out.println("No clues entered!");
            return null;
        }

        return new Query(sourceVertex, clues);
        }
    }
}
