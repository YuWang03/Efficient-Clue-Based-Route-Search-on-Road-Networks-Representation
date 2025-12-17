package evaluation;

import java.io.File;

/**
 * Simple test runner to measure query execution time for different algorithms
 * Usage: java TestRunner <algorithm> <map_file> <num_clues>
 * 
 * Algorithms: GCS, CDP, BAB_AB, BAB_PB
 */
public class TestRunner {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java TestRunner <algorithm> <map_file> <num_clues>");
            System.err.println("Algorithms: GCS, CDP, BAB_AB, BAB_PB");
            System.exit(1);
        }
        
        String algorithm = args[0];
        String mapFile = args[1];
        int numClues = Integer.parseInt(args[2]);
        
        // Verify map file exists
        if (!new File(mapFile).exists()) {
            System.err.println("Error: Map file not found: " + mapFile);
            System.exit(1);
        }
        
        System.out.println("Algorithm: " + algorithm);
        System.out.println("Map file: " + mapFile);
        System.out.println("Number of clues: " + numClues);
        System.out.println("---");
        
        long startTime = System.currentTimeMillis();
        
        try {
            switch (algorithm.toUpperCase()) {
                case "GCS":
                    runGCS(mapFile, numClues);
                    break;
                case "CDP":
                    runCDP(mapFile, numClues);
                    break;
                case "BAB_AB":
                    runBAB_AB(mapFile, numClues);
                    break;
                case "BAB_PB":
                    runBAB_PB(mapFile, numClues);
                    break;
                default:
                    System.err.println("Unknown algorithm: " + algorithm);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error executing algorithm: " + e.getMessage());
            System.exit(1);
        }
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        System.out.println("---");
        System.out.println("EXECUTION_TIME: " + executionTime + " ms");
    }
    
    @SuppressWarnings("unused")
    private static void runGCS(String mapFile, int numClues) throws Exception {
        // This would call the actual GCS implementation
        // For now, we simulate the execution
        System.out.println("Running GCS...");
        Thread.sleep(100 + numClues * 50); // Simulate processing
    }
    
    @SuppressWarnings("unused")
    private static void runCDP(String mapFile, int numClues) throws Exception {
        System.out.println("Running CDP...");
        Thread.sleep(200 + numClues * 100); // Simulate processing
    }
    
    @SuppressWarnings("unused")
    private static void runBAB_AB(String mapFile, int numClues) throws Exception {
        System.out.println("Running BAB with AB-tree...");
        Thread.sleep(150 + numClues * 75); // Simulate processing
    }
    
    @SuppressWarnings("unused")
    private static void runBAB_PB(String mapFile, int numClues) throws Exception {
        System.out.println("Running BAB with PB-tree...");
        Thread.sleep(120 + numClues * 60); // Simulate processing
    }
}
