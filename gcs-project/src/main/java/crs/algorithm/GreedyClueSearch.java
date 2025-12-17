package crs.algorithm;

import crs.model.*;
import java.util.*;

/**
 * Greedy Clue Search (GCS) 算法
 * 論文 Section 3 - 貪婪線索搜尋算法
 * 
 * 給定 Q = (vq, C)，GCS 反覆調用 findNextMin 來建構可行路徑
 */
public class GreedyClueSearch {
    
    @SuppressWarnings("unused")
    private final RoadNetwork network;
    private final FindNextMinAlgorithm findNextMin;
    
    public GreedyClueSearch(RoadNetwork network) {
        this.network = network;
        this.findNextMin = new FindNextMinAlgorithm(network);
    }
    
    /**
     * 可行路徑結果
     */
    public static class FeasiblePath {
        private final Node source;
        private final List<SearchResult> matches;
        private final List<Node> fullPath;
        private final double maxMatchingDistance;
        
        public FeasiblePath(Node source, List<SearchResult> matches) {
            this.source = source;
            this.matches = matches;
            this.fullPath = buildFullPath(source, matches);
            this.maxMatchingDistance = matches.stream()
                .mapToDouble(SearchResult::getMatchingDistance)
                .max().orElse(Double.MAX_VALUE);
        }
        
        private List<Node> buildFullPath(Node source, List<SearchResult> matches) {
            List<Node> path = new ArrayList<>();
            path.add(source);
            for (SearchResult result : matches) {
                // 添加路徑節點（跳過第一個因為已經是前一段的終點）
                List<Node> segment = result.getPathNodes();
                for (int i = 1; i < segment.size(); i++) {
                    path.add(segment.get(i));
                }
            }
            return path;
        }
        
        public Node getSource() { return source; }
        public List<SearchResult> getMatches() { return matches; }
        public List<Node> getFullPath() { return fullPath; }
        public double getMaxMatchingDistance() { return maxMatchingDistance; }
        
        public boolean isValid() {
            return matches.stream().allMatch(SearchResult::isValid);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== 可行路徑 ===\n");
            sb.append("起點: ").append(source.getName()).append("\n");
            
            // 修正：當沒有匹配時，顯示 N/A 而不是 Double.MAX_VALUE
            if (maxMatchingDistance == Double.MAX_VALUE) {
                sb.append("最大匹配距離: N/A (無有效匹配)\n");
            } else {
                sb.append("最大匹配距離: ").append(String.format("%.4f", maxMatchingDistance)).append("\n");
            }
            
            sb.append("路徑節點數: ").append(fullPath.size()).append("\n");
            sb.append("匹配詳情:\n");
            for (int i = 0; i < matches.size(); i++) {
                SearchResult r = matches.get(i);
                if (r.isValid()) {
                    sb.append(String.format("  [%d] %s -> %s (dm=%.4f, dG=%.2fm)\n",
                        i + 1, r.getClue().getKeyword(), 
                        r.getMatchVertex().getName(),
                        r.getMatchingDistance(), r.getNetworkDistance()));
                } else {
                    sb.append(String.format("  [%d] %s -> 無匹配\n",
                        i + 1, r.getClue().getKeyword()));
                }
            }
            return sb.toString();
        }
    }
    
    /**
     * 執行 GCS 算法
     * 
     * @param source 起始頂點 vq
     * @param clues 線索序列 C = {m1, m2, ..., mk}
     * @return FeasiblePath 可行路徑
     */
    public FeasiblePath search(Node source, List<Clue> clues) {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║      GCS (Greedy Clue Search) 開始     ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("起點: " + source.getName());
        System.out.println("線索數量: " + clues.size());
        
        List<SearchResult> matches = new ArrayList<>();
        Node currentNode = source;
        
        for (int i = 0; i < clues.size(); i++) {
            Clue clue = clues.get(i);
            System.out.println("\n【處理線索 " + (i + 1) + "/" + clues.size() + "】");
            
            SearchResult result = findNextMin.findNextMin(currentNode, clue);
            matches.add(result);
            
            if (!result.isValid()) {
                System.out.println("⚠ 無法找到匹配，GCS 終止");
                break;
            }
            
            currentNode = result.getMatchVertex();
        }
        
        FeasiblePath feasiblePath = new FeasiblePath(source, matches);
        
        System.out.println("\n" + feasiblePath);
        
        return feasiblePath;
    }
    
    /**
     * 取得最後一次搜尋的遍歷歷史
     */
    public List<FindNextMinAlgorithm.TraversalStep> getLastTraversalHistory() {
        return findNextMin.getTraversalHistory();
    }
}
