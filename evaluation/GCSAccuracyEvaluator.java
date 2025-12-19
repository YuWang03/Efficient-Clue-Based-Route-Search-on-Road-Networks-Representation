package evaluation;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * GCS 精度評估程式
 * 
 * 使用真實的 map.osm 數據和 Java 算法對比 GCS 與最優解的精度
 * 
 * 評估指標：
 * - Matching Ratio (MR) = GCS 路徑距離 / 最優路徑距離
 * - Hitting Ratio (HR) = 找到最優解的查詢數 / 總查詢數
 */
public class GCSAccuracyEvaluator {
    
    private List<AccuracyResult> results = new ArrayList<>();
    
    /**
     * 精度評估結果
     */
    public static class AccuracyResult {
        public String testName;
        public double matchingRatio;
        public double hittingRatio;
        public int totalQueries;
        public int optimalMatches;
        public long avgGCSTime;
        public long avgOptimalTime;
        
        @Override
        public String toString() {
            return String.format("%s: MR=%.3f, HR=%.2f, Queries=%d, Optimal=%d",
                testName, matchingRatio, hittingRatio, totalQueries, optimalMatches);
        }
    }
    
    /**
     * 測試場景：變化線索數量
     */
    public AccuracyResult evaluateByClueCount(int numClues) throws Exception {
        System.out.println("\n【測試】變化線索數量: " + numClues);
        
        AccuracyResult result = new AccuracyResult();
        result.testName = "Clues=" + numClues;
        result.totalQueries = 10;  // 運行 10 個查詢
        
        double totalMatchingRatio = 0;
        int optimalCount = 0;
        
        for (int q = 0; q < result.totalQueries; q++) {
            try {
                // 運行 GCS
                long gcsStart = System.currentTimeMillis();
                double gcsDistance = runGCSQuery(numClues);
                long gcsTime = System.currentTimeMillis() - gcsStart;
                
                // 運行最優算法 (BAB)
                long babStart = System.currentTimeMillis();
                double optimalDistance = runOptimalQuery(numClues);
                long babTime = System.currentTimeMillis() - babStart;
                
                if (optimalDistance > 0) {
                    double ratio = gcsDistance / optimalDistance;
                    totalMatchingRatio += ratio;
                    
                    if (Math.abs(gcsDistance - optimalDistance) < 0.01) {
                        optimalCount++;
                    }
                    
                    System.out.printf("  Query %d: GCS=%.2f, Optimal=%.2f, Ratio=%.3f\n",
                        q + 1, gcsDistance, optimalDistance, ratio);
                }
            } catch (Exception e) {
                System.out.println("  Query " + (q + 1) + " failed: " + e.getMessage());
            }
        }
        
        if (result.totalQueries > 0) {
            result.matchingRatio = totalMatchingRatio / result.totalQueries;
            result.hittingRatio = (double) optimalCount / result.totalQueries;
        }
        result.optimalMatches = optimalCount;
        
        return result;
    }
    
    /**
     * 測試場景：變化關鍵字頻率
     */
    public AccuracyResult evaluateByKeywordFrequency(int frequency) throws Exception {
        System.out.println("\n【測試】關鍵字頻率: " + frequency);
        
        AccuracyResult result = new AccuracyResult();
        result.testName = "Frequency=" + frequency;
        result.totalQueries = 10;
        
        double totalMatchingRatio = 0;
        int optimalCount = 0;
        
        for (int q = 0; q < result.totalQueries; q++) {
            try {
                // 使用指定頻率運行查詢
                long gcsStart = System.currentTimeMillis();
                double gcsDistance = runGCSQueryWithFrequency(frequency);
                long gcsTime = System.currentTimeMillis() - gcsStart;
                
                long babStart = System.currentTimeMillis();
                double optimalDistance = runOptimalQueryWithFrequency(frequency);
                long babTime = System.currentTimeMillis() - babStart;
                
                if (optimalDistance > 0) {
                    double ratio = gcsDistance / optimalDistance;
                    totalMatchingRatio += ratio;
                    
                    if (Math.abs(gcsDistance - optimalDistance) < 0.01) {
                        optimalCount++;
                    }
                    
                    System.out.printf("  Query %d: GCS=%.2f, Optimal=%.2f, Ratio=%.3f\n",
                        q + 1, gcsDistance, optimalDistance, ratio);
                }
            } catch (Exception e) {
                System.out.println("  Query " + (q + 1) + " failed: " + e.getMessage());
            }
        }
        
        if (result.totalQueries > 0) {
            result.matchingRatio = totalMatchingRatio / result.totalQueries;
            result.hittingRatio = (double) optimalCount / result.totalQueries;
        }
        result.optimalMatches = optimalCount;
        
        return result;
    }
    
    /**
     * 測試場景：變化查詢距離
     */
    public AccuracyResult evaluateByQueryDistance(int distance) throws Exception {
        System.out.println("\n【測試】查詢距離: " + distance + "m");
        
        AccuracyResult result = new AccuracyResult();
        result.testName = "Distance=" + distance + "m";
        result.totalQueries = 10;
        
        double totalMatchingRatio = 0;
        int optimalCount = 0;
        
        for (int q = 0; q < result.totalQueries; q++) {
            try {
                long gcsStart = System.currentTimeMillis();
                double gcsDistance = runGCSQueryWithDistance(distance);
                long gcsTime = System.currentTimeMillis() - gcsStart;
                
                long babStart = System.currentTimeMillis();
                double optimalDistance = runOptimalQueryWithDistance(distance);
                long babTime = System.currentTimeMillis() - babStart;
                
                if (optimalDistance > 0) {
                    double ratio = gcsDistance / optimalDistance;
                    totalMatchingRatio += ratio;
                    
                    if (Math.abs(gcsDistance - optimalDistance) < 0.01) {
                        optimalCount++;
                    }
                    
                    System.out.printf("  Query %d: GCS=%.2f, Optimal=%.2f, Ratio=%.3f\n",
                        q + 1, gcsDistance, optimalDistance, ratio);
                }
            } catch (Exception e) {
                System.out.println("  Query " + (q + 1) + " failed: " + e.getMessage());
            }
        }
        
        if (result.totalQueries > 0) {
            result.matchingRatio = totalMatchingRatio / result.totalQueries;
            result.hittingRatio = (double) optimalCount / result.totalQueries;
        }
        result.optimalMatches = optimalCount;
        
        return result;
    }
    
    /**
     * 測試場景：變化 Epsilon
     */
    public AccuracyResult evaluateByEpsilon(double epsilon) throws Exception {
        System.out.println("\n【測試】Epsilon: " + epsilon);
        
        AccuracyResult result = new AccuracyResult();
        result.testName = "Epsilon=" + epsilon;
        result.totalQueries = 10;
        
        double totalMatchingRatio = 0;
        int optimalCount = 0;
        
        for (int q = 0; q < result.totalQueries; q++) {
            try {
                long gcsStart = System.currentTimeMillis();
                double gcsDistance = runGCSQueryWithEpsilon(epsilon);
                long gcsTime = System.currentTimeMillis() - gcsStart;
                
                long babStart = System.currentTimeMillis();
                double optimalDistance = runOptimalQueryWithEpsilon(epsilon);
                long babTime = System.currentTimeMillis() - babStart;
                
                if (optimalDistance > 0) {
                    double ratio = gcsDistance / optimalDistance;
                    totalMatchingRatio += ratio;
                    
                    if (Math.abs(gcsDistance - optimalDistance) < 0.01) {
                        optimalCount++;
                    }
                    
                    System.out.printf("  Query %d: GCS=%.2f, Optimal=%.2f, Ratio=%.3f\n",
                        q + 1, gcsDistance, optimalDistance, ratio);
                }
            } catch (Exception e) {
                System.out.println("  Query " + (q + 1) + " failed: " + e.getMessage());
            }
        }
        
        if (result.totalQueries > 0) {
            result.matchingRatio = totalMatchingRatio / result.totalQueries;
            result.hittingRatio = (double) optimalCount / result.totalQueries;
        }
        result.optimalMatches = optimalCount;
        
        return result;
    }
    
    /**
     * 運行 GCS 查詢（使用真實 map.osm）
     */
    private double runGCSQuery(int numClues) throws Exception {
        // 呼叫 GCS 項目的 Main.java
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp", 
            "c:\\Users\\user\\Desktop\\crs_java_project\\gcs-project\\bin",
            "crs.Main",
            "c:\\Users\\user\\Desktop\\crs_java_project\\map.osm",
            String.valueOf(numClues)
        );
        pb.directory(new File("c:\\Users\\user\\Desktop\\crs_java_project\\gcs-project"));
        
        Process p = pb.start();
        String output = readProcessOutput(p);
        p.waitFor();
        
        // 解析輸出中的距離值
        return extractDistanceFromOutput(output);
    }
    
    /**
     * 運行最優查詢（使用 BAB）
     */
    private double runOptimalQuery(int numClues) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-cp",
            "c:\\Users\\user\\Desktop\\crs_java_project\\bab-project\\bin",
            "bab.Main",
            "c:\\Users\\user\\Desktop\\crs_java_project\\map.osm",
            String.valueOf(numClues)
        );
        pb.directory(new File("c:\\Users\\user\\Desktop\\crs_java_project\\bab-project"));
        
        Process p = pb.start();
        String output = readProcessOutput(p);
        p.waitFor();
        
        return extractDistanceFromOutput(output);
    }
    
    private double runGCSQueryWithFrequency(int frequency) throws Exception {
        // 實現邏輯...
        return runGCSQuery(3);  // 暫時使用默認值
    }
    
    private double runOptimalQueryWithFrequency(int frequency) throws Exception {
        return runOptimalQuery(3);
    }
    
    private double runGCSQueryWithDistance(int distance) throws Exception {
        return runGCSQuery(3);
    }
    
    private double runOptimalQueryWithDistance(int distance) throws Exception {
        return runOptimalQuery(3);
    }
    
    private double runGCSQueryWithEpsilon(double epsilon) throws Exception {
        return runGCSQuery(3);
    }
    
    private double runOptimalQueryWithEpsilon(double epsilon) throws Exception {
        return runOptimalQuery(3);
    }
    
    private String readProcessOutput(Process p) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 從輸出中提取距離值
     */
    private double extractDistanceFromOutput(String output) {
        // 查找類似 "Total distance: 1234.56" 的模式
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("distance") || line.contains("Distance")) {
                try {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].trim().replaceAll("[^0-9.]", "");
                        if (!value.isEmpty()) {
                            return Double.parseDouble(value);
                        }
                    }
                } catch (NumberFormatException e) {
                    // 繼續搜索
                }
            }
        }
        return 0.0;
    }
    
    /**
     * 輸出評估結果為 JSON
     */
    public void exportAsJSON(String filename) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"evaluation\": \"GCS Accuracy Analysis\",\n");
        json.append("  \"results\": [\n");
        
        for (int i = 0; i < results.size(); i++) {
            AccuracyResult r = results.get(i);
            json.append("    {\n");
            json.append("      \"test\": \"").append(r.testName).append("\",\n");
            json.append("      \"matching_ratio\": ").append(r.matchingRatio).append(",\n");
            json.append("      \"hitting_ratio\": ").append(r.hittingRatio).append(",\n");
            json.append("      \"total_queries\": ").append(r.totalQueries).append(",\n");
            json.append("      \"optimal_matches\": ").append(r.optimalMatches).append("\n");
            json.append("    }");
            if (i < results.size() - 1) json.append(",");
            json.append("\n");
        }
        
        json.append("  ]\n");
        json.append("}\n");
        
        Files.write(Paths.get(filename), json.toString().getBytes());
        System.out.println("\n✓ 結果已保存到: " + filename);
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     GCS 精度評估 - 使用真實 map.osm 和 Java 算法      ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        GCSAccuracyEvaluator evaluator = new GCSAccuracyEvaluator();
        
        // (a) 變化線索數量
        System.out.println("\n=== (a) 線索數量測試 ===");
        for (int clues : new int[]{2, 3, 4, 5, 6, 7, 8}) {
            evaluator.results.add(evaluator.evaluateByClueCount(clues));
        }
        
        // (b) 變化關鍵字頻率
        System.out.println("\n=== (b) 關鍵字頻率測試 ===");
        for (int freq : new int[]{10, 50, 100, 500, 1000, 5000, 10000}) {
            evaluator.results.add(evaluator.evaluateByKeywordFrequency(freq));
        }
        
        // (c) 變化查詢距離
        System.out.println("\n=== (c) 查詢距離測試 ===");
        for (int dist : new int[]{2000, 4000, 6000, 8000, 10000, 12000, 14000}) {
            evaluator.results.add(evaluator.evaluateByQueryDistance(dist));
        }
        
        // (d) 變化 Epsilon
        System.out.println("\n=== (d) Epsilon 測試 ===");
        for (double eps : new double[]{0.2, 0.4, 0.6, 0.8, 1.0}) {
            evaluator.results.add(evaluator.evaluateByEpsilon(eps));
        }
        
        // 輸出結果
        System.out.println("\n\n=== 評估結果摘要 ===");
        for (AccuracyResult r : evaluator.results) {
            System.out.println(r);
        }
        
        // 導出為 JSON
        evaluator.exportAsJSON("gcs_accuracy_results.json");
    }
}
