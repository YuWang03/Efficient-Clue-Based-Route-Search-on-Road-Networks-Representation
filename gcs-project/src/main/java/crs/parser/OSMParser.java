package crs.parser;

import crs.model.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.io.*;
import java.util.*;

/**
 * OpenStreetMap 檔案解析器
 * 解析 .osm XML 格式，建立道路網路圖
 */
public class OSMParser {
    
    public RoadNetwork parse(String filePath) throws Exception {
        RoadNetwork network = new RoadNetwork();
        Map<Long, Node> tempNodes = new HashMap<>();
        List<WayData> ways = new ArrayList<>();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        
        OSMHandler handler = new OSMHandler(tempNodes, ways);
        saxParser.parse(new File(filePath), handler);
        
        System.out.println("解析完成: " + tempNodes.size() + " 個節點, " + ways.size() + " 條道路");
        
        // 保留兩類節點：1) 被道路引用的節點  2) 有 POI 標籤的節點
        Set<Long> usedNodeIds = new HashSet<>();
        Set<Long> poiNodeIds = new HashSet<>();
        
        // 1. 收集道路節點
        for (WayData way : ways) {
            usedNodeIds.addAll(way.nodeRefs);
        }
        
        // 2. 收集 POI 節點（有 amenity/shop/tourism/cuisine 等標籤）
        for (Map.Entry<Long, Node> entry : tempNodes.entrySet()) {
            Node node = entry.getValue();
            if (hasPOITags(node)) {
                poiNodeIds.add(entry.getKey());
            }
        }
        
        System.out.println("  - 道路節點: " + usedNodeIds.size() + " 個");
        System.out.println("  - POI 節點: " + poiNodeIds.size() + " 個");
        
        // 添加所有需要的節點到網路
        Set<Long> allNodeIds = new HashSet<>();
        allNodeIds.addAll(usedNodeIds);
        allNodeIds.addAll(poiNodeIds);
        
        for (Long nodeId : allNodeIds) {
            Node node = tempNodes.get(nodeId);
            if (node != null) {
                network.addNode(node);
            }
        }
        
        // 添加邊到網路
        for (WayData way : ways) {
            if (!isRoadWay(way.tags)) continue;
            
            boolean isOneway = "yes".equals(way.tags.get("oneway"));
            String wayName = way.tags.getOrDefault("name", "未命名道路");
            String wayType = way.tags.getOrDefault("highway", "road");
            
            for (int i = 0; i < way.nodeRefs.size() - 1; i++) {
                Node from = network.getNode(way.nodeRefs.get(i));
                Node to = network.getNode(way.nodeRefs.get(i + 1));
                
                if (from != null && to != null) {
                    double distance = RoadNetwork.calculateDistance(from, to);
                    
                    if (isOneway) {
                        network.addEdge(from, to, distance, wayName, wayType);
                    } else {
                        network.addBidirectionalEdge(from, to, distance, wayName, wayType);
                    }
                    
                    // 不再將道路名稱加為關鍵字，避免污染 POI 搜尋
                    // 只有 amenity/shop/tourism 等標籤才會成為可搜尋的關鍵字
                }
            }
        }
        
        // 連接 POI 到道路網路
        connectPOIToRoadNetwork(network, poiNodeIds, usedNodeIds);
        
        System.out.println("建構完成: " + network);
        return network;
    }
    
    private boolean isRoadWay(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null) return false;
        
        // 排除人行道、台階等非車行道路
        Set<String> excluded = Set.of("steps", "pedestrian", "path", "cycleway", "construction");
        return !excluded.contains(highway);
    }
    
    /**
     * 判斷節點是否有 POI 標籤
     */
    private boolean hasPOITags(Node node) {
        Map<String, String> tags = node.getTags();
        // 檢查常見的 POI 標籤
        return tags.containsKey("amenity") || 
               tags.containsKey("shop") || 
               tags.containsKey("tourism") || 
               tags.containsKey("leisure") ||
               (tags.containsKey("cuisine") && !tags.containsKey("highway"));
    }
    
    /**
     * 為 POI 節點連接到最近的道路節點
     * 這樣 POI 才能在路網搜尋中被找到
     */
    private void connectPOIToRoadNetwork(RoadNetwork network, Set<Long> poiNodeIds, Set<Long> roadNodeIds) {
        int connectedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;
        
        System.out.println("  - 開始連接 POI 到道路網路...");
        
        for (Long poiId : poiNodeIds) {
            if (roadNodeIds.contains(poiId)) {
                skippedCount++; // POI 本身就在道路上，跳過
                continue;
            }
            
            Node poiNode = network.getNode(poiId);
            if (poiNode == null) continue;
            
            // 找到最近的道路節點（增加連接距離以跨越小區域）
            Node nearestRoadNode = null;
            double minDistance = Double.MAX_VALUE;
            
            for (Long roadId : roadNodeIds) {
                Node roadNode = network.getNode(roadId);
                if (roadNode == null) continue;
                
                double dist = RoadNetwork.calculateDistance(poiNode, roadNode);
                if (dist < minDistance && dist < 500) { // 500m 以連接鄰近 POI 區域
                    minDistance = dist;
                    nearestRoadNode = roadNode;
                }
            }
            
            // 創建雙向連接
            if (nearestRoadNode != null) {
                network.addBidirectionalEdge(poiNode, nearestRoadNode, minDistance, 
                    "POI連接", "access");
                connectedCount++;
                
                // 調試：顯示前幾個連接和所有 cafe/restaurant/convenience
                if (connectedCount <= 5 || poiNode.containsKeyword("cafe") || 
                    poiNode.containsKeyword("restaurant") || poiNode.containsKeyword("convenience")) {
                    System.out.println("    • POI " + poiNode.getName() + " (關鍵字: " + 
                                     poiNode.getKeywords() + ") -> 道路節點 " + 
                                     nearestRoadNode.getId() + " (距離: " + 
                                     String.format("%.1f", minDistance) + "m)");
                }
            } else {
                failedCount++;
                // 顯示連接失敗的 cafe
                if (poiNode.containsKeyword("cafe")) {
                    System.out.println("    ✗ POI " + poiNode.getName() + " (關鍵字: " + 
                                     poiNode.getKeywords() + ") 連接失敗（無附近道路節點）");
                }
            }
        }
        
        System.out.println("  - 連接了 " + connectedCount + " 個 POI 到道路網路");
        System.out.println("  - 跳過 " + skippedCount + " 個（已在道路上）");
        if (failedCount > 0) {
            System.out.println("  - 失敗 " + failedCount + " 個（無附近道路節點）");
        }
        
        // 橋接 POI 區域：連接鄰近的 POI 節點（restaurant, cafe, convenience）
        System.out.println("  - 橋接 POI 區域...");
        List<Node> targetPOIs = new ArrayList<>();
        for (Node node : network.getAllNodes()) {
            if (node.containsKeyword("restaurant") || node.containsKeyword("cafe") || node.containsKeyword("convenience")) {
                targetPOIs.add(node);
            }
        }
        
        int bridgeCount = 0;
        for (int i = 0; i < targetPOIs.size(); i++) {
            Node poi1 = targetPOIs.get(i);
            for (int j = i + 1; j < targetPOIs.size(); j++) {
                Node poi2 = targetPOIs.get(j);
                double dist = RoadNetwork.calculateDistance(poi1, poi2);
                if (dist < 800) { // 800m 內的 POI 互相連接
                    network.addBidirectionalEdge(poi1, poi2, dist, "POI橋接", "access");
                    bridgeCount++;
                    if (bridgeCount <= 3) {
                        System.out.println("    ✓ 橋接: " + poi1.getName() + " <-> " + poi2.getName() + " (距離: " + String.format("%.1f", dist) + "m)");
                    }
                }
            }
        }
        System.out.println("  - 建立了 " + bridgeCount + " 個 POI 橋接");
    }
    
    // 內部類別：儲存道路資料
    private static class WayData {
        List<Long> nodeRefs = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
    }
    
    // SAX 解析處理器
    private static class OSMHandler extends DefaultHandler {
        private final Map<Long, Node> nodes;
        private final List<WayData> ways;
        
        private Node currentNode;
        private WayData currentWay;
        private boolean inNode = false;
        private boolean inWay = false;
        
        public OSMHandler(Map<Long, Node> nodes, List<WayData> ways) {
            this.nodes = nodes;
            this.ways = ways;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            switch (qName) {
                case "node":
                    long nodeId = Long.parseLong(attrs.getValue("id"));
                    double lat = Double.parseDouble(attrs.getValue("lat"));
                    double lon = Double.parseDouble(attrs.getValue("lon"));
                    currentNode = new Node(nodeId, lat, lon);
                    inNode = true;
                    break;
                    
                case "way":
                    currentWay = new WayData();
                    inWay = true;
                    break;
                    
                case "nd":
                    if (inWay && currentWay != null) {
                        long ref = Long.parseLong(attrs.getValue("ref"));
                        currentWay.nodeRefs.add(ref);
                    }
                    break;
                    
                case "tag":
                    String k = attrs.getValue("k");
                    String v = attrs.getValue("v");
                    if (inNode && currentNode != null) {
                        currentNode.addTag(k, v);
                    } else if (inWay && currentWay != null) {
                        currentWay.tags.put(k, v);
                    }
                    break;
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "node":
                    if (currentNode != null) {
                        nodes.put(currentNode.getId(), currentNode);
                    }
                    currentNode = null;
                    inNode = false;
                    break;
                    
                case "way":
                    if (currentWay != null && !currentWay.nodeRefs.isEmpty()) {
                        ways.add(currentWay);
                    }
                    currentWay = null;
                    inWay = false;
                    break;
            }
        }
    }
}
