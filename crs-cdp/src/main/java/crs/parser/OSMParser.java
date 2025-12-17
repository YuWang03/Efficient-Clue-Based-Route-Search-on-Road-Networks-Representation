package crs.parser;

import crs.model.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import java.io.*;
import java.util.*;

/**
 * OpenStreetMap 檔案解析器
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
        
        System.out.println("  解析完成: " + tempNodes.size() + " 個節點, " + ways.size() + " 條道路");
        
        Set<Long> usedNodeIds = new HashSet<>();
        for (WayData way : ways) {
            usedNodeIds.addAll(way.nodeRefs);
        }
        
        for (Long nodeId : usedNodeIds) {
            Node node = tempNodes.get(nodeId);
            if (node != null) network.addNode(node);
        }
        
        int roadWayCount = 0;
        int edgeCount = 0;
        int skippedEdges = 0;
        
        for (WayData way : ways) {
            if (!isRoadWay(way.tags)) continue;
            
            roadWayCount++;
            boolean isOneway = "yes".equals(way.tags.get("oneway"));
            String wayName = way.tags.getOrDefault("name", "未命名道路");
            
            for (int i = 0; i < way.nodeRefs.size() - 1; i++) {
                Node from = network.getNode(way.nodeRefs.get(i));
                Node to = network.getNode(way.nodeRefs.get(i + 1));
                
                if (from != null && to != null) {
                    double dist = RoadNetwork.haversineDistance(from, to);
                    if (isOneway) {
                        network.addEdge(from, to, dist, wayName);
                        edgeCount++;
                    } else {
                        network.addBidirectionalEdge(from, to, dist, wayName);
                        edgeCount += 2;
                    }
                    if (!wayName.equals("未命名道路")) {
                        from.addKeyword(wayName);
                        to.addKeyword(wayName);
                    }
                } else {
                    skippedEdges++;
                }
            }
        }
        
        // 重新建立關鍵字索引（因為道路名稱是在節點添加到network後才加入的）
        network.rebuildKeywordIndex();
        
        System.out.println("  道路數: " + roadWayCount);
        System.out.println("  邊數: " + edgeCount);
        if (skippedEdges > 0) {
            System.out.println("  警告: " + skippedEdges + " 條邊因節點缺失被跳過");
        }
        
        // 檢查網絡連通性
        int isolatedNodes = 0;
        for (Node node : network.getAllNodes()) {
            if (network.getEdges(node.getId()).isEmpty()) {
                isolatedNodes++;
            }
        }
        if (isolatedNodes > 0) {
            System.out.println("  警告: " + isolatedNodes + " 個孤立節點（無連接）");
        }
        
        return network;
    }
    
    private boolean isRoadWay(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null) return false;
        
        // 只排除明確不能通行的道路類型
        Set<String> excluded = Set.of("proposed", "construction", "raceway");
        
        // 包含所有可通行的道路類型（包括人行道、小徑等）
        return !excluded.contains(highway);
    }
    
    private static class WayData {
        List<Long> nodeRefs = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
    }
    
    private static class OSMHandler extends DefaultHandler {
        private final Map<Long, Node> nodes;
        private final List<WayData> ways;
        private Node currentNode;
        private WayData currentWay;
        private boolean inNode = false, inWay = false;
        
        public OSMHandler(Map<Long, Node> nodes, List<WayData> ways) {
            this.nodes = nodes;
            this.ways = ways;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) {
            switch (qName) {
                case "node":
                    currentNode = new Node(
                        Long.parseLong(attrs.getValue("id")),
                        Double.parseDouble(attrs.getValue("lat")),
                        Double.parseDouble(attrs.getValue("lon"))
                    );
                    inNode = true;
                    break;
                case "way":
                    currentWay = new WayData();
                    inWay = true;
                    break;
                case "nd":
                    if (inWay && currentWay != null)
                        currentWay.nodeRefs.add(Long.parseLong(attrs.getValue("ref")));
                    break;
                case "tag":
                    String k = attrs.getValue("k"), v = attrs.getValue("v");
                    if (inNode && currentNode != null) currentNode.addTag(k, v);
                    else if (inWay && currentWay != null) currentWay.tags.put(k, v);
                    break;
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("node".equals(qName)) {
                if (currentNode != null) nodes.put(currentNode.getId(), currentNode);
                currentNode = null; inNode = false;
            } else if ("way".equals(qName)) {
                if (currentWay != null && !currentWay.nodeRefs.isEmpty()) ways.add(currentWay);
                currentWay = null; inWay = false;
            }
        }
    }
}
