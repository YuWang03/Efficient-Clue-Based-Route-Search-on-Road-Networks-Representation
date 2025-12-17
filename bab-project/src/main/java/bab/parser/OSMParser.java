package bab.parser;

import bab.model.Node;
import bab.model.RoadNetwork;
import java.io.File;
import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for OpenStreetMap (OSM) XML files
 * 
 * Extracts road network structure including:
 * - Nodes with coordinates
 * - Ways (road segments)
 * - Keywords from tags (highway type, name, amenity, etc.)
 */
public class OSMParser {

    // Tags to extract as keywords
    private static final Set<String> KEYWORD_TAGS = new HashSet<>(Arrays.asList(
        "highway", "amenity", "crossing", "entrance", "name", 
        "shop", "tourism", "public_transport", "building", 
        "landuse", "natural", "leisure", "railway"
    ));

    /**
     * Parse OSM file and build road network
     * 
     * @param filename Path to OSM XML file
     * @return RoadNetwork with parsed data
     */
    public RoadNetwork parse(String filename) throws Exception {
        System.out.println("Parsing OSM file: " + filename);
        
        RoadNetwork network = new RoadNetwork();
        
        // Temporary storage for ways
        Map<Long, List<Long>> wayNodes = new HashMap<>();
        Map<Long, Map<String, String>> wayTags = new HashMap<>();
        
        // First pass: collect all data
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        
        OSMHandler handler = new OSMHandler(network, wayNodes, wayTags);
        saxParser.parse(new File(filename), handler);
        
        System.out.printf("Parsed %d nodes from OSM file%n", network.getNodeCount());
        
        // Second pass: build edges from ways
        int edgeCount = buildEdgesFromWays(network, wayNodes, wayTags);
        System.out.printf("Created %d bidirectional edges%n", edgeCount);
        
        // Remove isolated nodes
        network.removeIsolatedNodes();
        System.out.printf("Final graph: %d connected nodes%n", network.getNodeCount());
        
        return network;
    }

    /**
     * Build edges from way data
     */
    private int buildEdgesFromWays(RoadNetwork network, 
                                    Map<Long, List<Long>> wayNodes,
                                    Map<Long, Map<String, String>> wayTags) {
        int edgeCount = 0;
        
        for (Map.Entry<Long, List<Long>> entry : wayNodes.entrySet()) {
            long wayId = entry.getKey();
            List<Long> nodeList = entry.getValue();
            Map<String, String> tags = wayTags.get(wayId);
            
            // Get keywords from way tags
            String highway = tags.get("highway");
            String name = tags.get("name");
            
            // Build edges between consecutive nodes in the way
            for (int i = 0; i < nodeList.size() - 1; i++) {
                long fromId = nodeList.get(i);
                long toId = nodeList.get(i + 1);
                
                Node fromNode = network.getNode(fromId);
                Node toNode = network.getNode(toId);
                
                if (fromNode != null && toNode != null) {
                    // Add keywords from way to nodes
                    if (highway != null) {
                        network.indexNodeKeyword(fromId, highway);
                        network.indexNodeKeyword(toId, highway);
                    }
                    if (name != null) {
                        network.indexNodeKeyword(fromId, name);
                        network.indexNodeKeyword(toId, name);
                    }
                    
                    // Calculate edge weight (Haversine distance)
                    double weight = RoadNetwork.haversineDistance(
                        fromNode.getLat(), fromNode.getLon(),
                        toNode.getLat(), toNode.getLon()
                    );
                    
                    // Add bidirectional edge
                    network.addBidirectionalEdge(fromId, toId, weight);
                    edgeCount++;
                }
            }
        }
        
        return edgeCount;
    }

    /**
     * SAX Handler for OSM XML parsing
     */
    private static class OSMHandler extends DefaultHandler {
        private final RoadNetwork network;
        private final Map<Long, List<Long>> wayNodes;
        private final Map<Long, Map<String, String>> wayTags;
        
        // Current parsing state
        private long currentNodeId = -1;
        private double currentLat = 0;
        private double currentLon = 0;
        private Map<String, String> currentTags = null;
        
        private long currentWayId = -1;
        private List<Long> currentWayNodes = null;
        private boolean inWay = false;
        
        private long wayIdCounter = 0;

        OSMHandler(RoadNetwork network, 
                   Map<Long, List<Long>> wayNodes,
                   Map<Long, Map<String, String>> wayTags) {
            this.network = network;
            this.wayNodes = wayNodes;
            this.wayTags = wayTags;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "node":
                    currentNodeId = Long.parseLong(attributes.getValue("id"));
                    currentLat = Double.parseDouble(attributes.getValue("lat"));
                    currentLon = Double.parseDouble(attributes.getValue("lon"));
                    currentTags = new HashMap<>();
                    break;
                    
                case "way":
                    inWay = true;
                    currentWayId = wayIdCounter++;
                    currentWayNodes = new ArrayList<>();
                    currentTags = new HashMap<>();
                    break;
                    
                case "nd":
                    if (inWay && currentWayNodes != null) {
                        long ref = Long.parseLong(attributes.getValue("ref"));
                        currentWayNodes.add(ref);
                    }
                    break;
                    
                case "tag":
                    if (currentTags != null) {
                        String k = attributes.getValue("k");
                        String v = attributes.getValue("v");
                        currentTags.put(k, v);
                    }
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "node":
                    if (currentNodeId != -1) {
                        Node node = new Node(currentNodeId, currentLat, currentLon);
                        
                        // Extract keywords from node tags
                        for (Map.Entry<String, String> tag : currentTags.entrySet()) {
                            if (KEYWORD_TAGS.contains(tag.getKey())) {
                                node.addKeyword(tag.getValue());
                            }
                        }
                        
                        network.addNode(node);
                        currentNodeId = -1;
                    }
                    currentTags = null;
                    break;
                    
                case "way":
                    if (inWay && currentWayNodes != null && currentWayNodes.size() >= 2) {
                        wayNodes.put(currentWayId, new ArrayList<>(currentWayNodes));
                        wayTags.put(currentWayId, new HashMap<>(currentTags));
                    }
                    inWay = false;
                    currentWayNodes = null;
                    currentTags = null;
                    break;
            }
        }
    }
}
