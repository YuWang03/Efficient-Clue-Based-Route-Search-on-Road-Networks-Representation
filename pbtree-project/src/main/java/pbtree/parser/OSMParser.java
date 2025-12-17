package pbtree.parser;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import pbtree.model.*;

/**
 * Parser for OpenStreetMap XML data.
 * Extracts nodes, ways, and creates the road network graph.
 */
public class OSMParser {
    
    public static RoadNetwork parse(String filename) throws Exception {
        RoadNetwork network = new RoadNetwork();
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        
        // First pass: collect nodes
        Map<Long, double[]> nodeCoords = new HashMap<>();
        Map<Long, Set<String>> nodeKeywords = new HashMap<>();
        
        System.out.println("Pass 1: Collecting nodes...");
        parser.parse(new File(filename), new DefaultHandler() {
            private long currentNodeId = -1;
            private double currentLat, currentLon;
            private Set<String> currentKeywords = new HashSet<>();
            
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attrs) {
                if ("node".equals(qName)) {
                    currentNodeId = Long.parseLong(attrs.getValue("id"));
                    currentLat = Double.parseDouble(attrs.getValue("lat"));
                    currentLon = Double.parseDouble(attrs.getValue("lon"));
                    currentKeywords = new HashSet<>();
                } else if ("tag".equals(qName) && currentNodeId != -1) {
                    String key = attrs.getValue("k");
                    String value = attrs.getValue("v");
                    
                    // Extract keywords from various tags
                    if (isKeywordTag(key)) {
                        currentKeywords.add(value.toLowerCase());
                    }
                    if ("name".equals(key) && value != null) {
                        // Add name words as keywords
                        for (String word : value.toLowerCase().split("\\s+")) {
                            if (word.length() > 2) {
                                currentKeywords.add(word);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("node".equals(qName) && currentNodeId != -1) {
                    nodeCoords.put(currentNodeId, new double[]{currentLat, currentLon});
                    if (!currentKeywords.isEmpty()) {
                        nodeKeywords.put(currentNodeId, new HashSet<>(currentKeywords));
                    }
                    currentNodeId = -1;
                }
            }
        });
        
        System.out.println("  Found " + nodeCoords.size() + " nodes, " + nodeKeywords.size() + " with keywords");
        
        // Second pass: collect ways and build edges
        System.out.println("Pass 2: Building edges from ways...");
        List<long[]> wayNodes = new ArrayList<>();
        Map<Long, Set<String>> wayKeywords = new HashMap<>();
        
        parser.parse(new File(filename), new DefaultHandler() {
            private long currentWayId = -1;
            private List<Long> currentNodes = new ArrayList<>();
            private Set<String> currentKeywords = new HashSet<>();
            private boolean isHighway = false;
            
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attrs) {
                if ("way".equals(qName)) {
                    currentWayId = Long.parseLong(attrs.getValue("id"));
                    currentNodes = new ArrayList<>();
                    currentKeywords = new HashSet<>();
                    isHighway = false;
                } else if ("nd".equals(qName) && currentWayId != -1) {
                    currentNodes.add(Long.parseLong(attrs.getValue("ref")));
                } else if ("tag".equals(qName) && currentWayId != -1) {
                    String key = attrs.getValue("k");
                    String value = attrs.getValue("v");
                    
                    if ("highway".equals(key)) {
                        isHighway = true;
                        currentKeywords.add(value.toLowerCase());
                    }
                    if (isKeywordTag(key) && value != null) {
                        currentKeywords.add(value.toLowerCase());
                    }
                }
            }
            
            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("way".equals(qName) && currentWayId != -1 && isHighway && currentNodes.size() >= 2) {
                    long[] nodes = currentNodes.stream().mapToLong(Long::longValue).toArray();
                    wayNodes.add(nodes);
                    wayKeywords.put(currentWayId, new HashSet<>(currentKeywords));
                    
                    // Add way keywords to all nodes in the way
                    for (long nodeId : nodes) {
                        nodeKeywords.computeIfAbsent(nodeId, k -> new HashSet<>()).addAll(currentKeywords);
                    }
                    currentWayId = -1;
                }
            }
        });
        
        System.out.println("  Found " + wayNodes.size() + " ways");
        
        // Build network
        System.out.println("Building network graph...");
        Set<Long> usedNodes = new HashSet<>();
        for (long[] way : wayNodes) {
            for (long nodeId : way) {
                usedNodes.add(nodeId);
            }
        }
        
        // Add nodes
        for (long nodeId : usedNodes) {
            double[] coords = nodeCoords.get(nodeId);
            if (coords != null) {
                Node node = new Node(nodeId, coords[0], coords[1]);
                Set<String> keywords = nodeKeywords.get(nodeId);
                if (keywords != null) {
                    for (String kw : keywords) {
                        node.addKeyword(kw);
                    }
                }
                network.addNode(node);
            }
        }
        
        // Add edges (bidirectional)
        int edgeCount = 0;
        for (long[] way : wayNodes) {
            for (int i = 0; i < way.length - 1; i++) {
                long from = way[i];
                long to = way[i + 1];
                
                double[] c1 = nodeCoords.get(from);
                double[] c2 = nodeCoords.get(to);
                
                if (c1 != null && c2 != null) {
                    double weight = RoadNetwork.haversineDistance(c1[0], c1[1], c2[0], c2[1]);
                    network.addEdge(new Edge(from, to, weight));
                    network.addEdge(new Edge(to, from, weight));
                    edgeCount += 2;
                }
            }
        }
        
        System.out.println("  Added " + usedNodes.size() + " nodes and " + edgeCount + " edges");
        
        // Remove isolated nodes
        network.removeIsolatedNodes();
        
        return network;
    }
    
    private static boolean isKeywordTag(String key) {
        return key != null && (
            key.equals("highway") ||
            key.equals("amenity") ||
            key.equals("shop") ||
            key.equals("tourism") ||
            key.equals("leisure") ||
            key.equals("building") ||
            key.equals("landuse") ||
            key.equals("natural") ||
            key.equals("crossing") ||
            key.equals("entrance") ||
            key.equals("barrier") ||
            key.equals("footway") ||
            key.equals("bicycle") ||
            key.equals("surface") ||
            key.equals("service")
        );
    }
}
