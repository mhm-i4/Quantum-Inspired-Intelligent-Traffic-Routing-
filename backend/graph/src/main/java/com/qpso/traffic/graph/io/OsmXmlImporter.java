package com.qpso.traffic.graph.io;

import com.qpso.traffic.graph.model.Node;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Imports a bounded OpenStreetMap XML extract into the internal road graph. */
public final class OsmXmlImporter {
    private static final Set<String> ROAD_TYPES = Set.of(
            "motorway", "trunk", "primary", "secondary", "tertiary",
            "unclassified", "residential", "living_street", "service"
    );

    private OsmXmlImporter() {}

    public static TransportGraph importGraph(InputStream input) throws IOException {
        Map<String, OsmNode> nodes = new HashMap<>();
        List<OsmWay> ways = new ArrayList<>();

        try {
            XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(input);
            while (reader.hasNext()) {
                if (reader.next() != XMLStreamConstants.START_ELEMENT) continue;

                if (reader.getLocalName().equals("node")) {
                    nodes.put(reader.getAttributeValue(null, "id"), new OsmNode(
                            reader.getAttributeValue(null, "id"),
                            Double.parseDouble(reader.getAttributeValue(null, "lat")),
                            Double.parseDouble(reader.getAttributeValue(null, "lon"))));
                } else if (reader.getLocalName().equals("way")) {
                    ways.add(readWay(reader));
                }
            }
            reader.close();
        } catch (XMLStreamException | NumberFormatException e) {
            throw new IOException("Invalid OpenStreetMap XML", e);
        }

        TransportGraph graph = new TransportGraph();
        Set<String> usedNodes = new HashSet<>();
        for (OsmWay way : ways) {
            if (!isSupportedRoad(way.highway())) continue;
            for (String ref : way.nodeRefs()) {
                if (nodes.containsKey(ref)) usedNodes.add(ref);
            }
        }
        for (String id : usedNodes) {
            OsmNode node = nodes.get(id);
            graph.addNode(new Node("osm-" + id, node.latitude(), node.longitude())
                    .withMeta("source", "OpenStreetMap"));
        }

        for (OsmWay way : ways) {
            if (!isSupportedRoad(way.highway())) continue;
            addWayEdges(graph, nodes, way);
        }
        if (graph.nodeCount() == 0 || graph.edgeCount() == 0) {
            throw new IOException("OpenStreetMap extract contains no supported road ways");
        }
        return graph;
    }

    private static boolean isSupportedRoad(String highway) {
        return highway != null && ROAD_TYPES.contains(highway);
    }

    private static OsmWay readWay(XMLStreamReader reader) throws XMLStreamException {
        String id = reader.getAttributeValue(null, "id");
        List<String> refs = new ArrayList<>();
        Map<String, String> tags = new HashMap<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT && reader.getLocalName().equals("way")) break;
            if (event != XMLStreamConstants.START_ELEMENT) continue;
            if (reader.getLocalName().equals("nd")) {
                refs.add(reader.getAttributeValue(null, "ref"));
            } else if (reader.getLocalName().equals("tag")) {
                tags.put(reader.getAttributeValue(null, "k"), reader.getAttributeValue(null, "v"));
            }
        }
        return new OsmWay(id, refs, tags.get("highway"), tags.get("oneway"), tags.get("maxspeed"));
    }

    private static void addWayEdges(TransportGraph graph, Map<String, OsmNode> nodes, OsmWay way) {
        double speed = speedKmh(way.highway(), way.maxspeed());
        for (int i = 0; i + 1 < way.nodeRefs().size(); i++) {
            String from = way.nodeRefs().get(i);
            String to = way.nodeRefs().get(i + 1);
            OsmNode a = nodes.get(from);
            OsmNode b = nodes.get(to);
            if (a == null || b == null || !graph.hasNode("osm-" + from) || !graph.hasNode("osm-" + to)) continue;

            double distance = haversineKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
            String base = "osm-" + way.id() + "-" + i;
            if ("-1".equals(way.oneway())) {
                graph.addEdge(RoadEdge.create(base + "-B", "osm-" + to, "osm-" + from, distance, speed));
            } else {
                graph.addEdge(RoadEdge.create(base + "-F", "osm-" + from, "osm-" + to, distance, speed));
                if (!isOneWay(way.oneway())) {
                    graph.addEdge(RoadEdge.create(base + "-B", "osm-" + to, "osm-" + from, distance, speed));
                }
            }
        }
    }

    private static boolean isOneWay(String value) {
        return "yes".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static double speedKmh(String highway, String maxspeed) {
        if (maxspeed != null) {
            try {
                String numeric = maxspeed.replace(',', '.').replaceAll("[^0-9.].*", "");
                double parsed = Double.parseDouble(numeric);
                if (maxspeed.toLowerCase().contains("mph")) return parsed * 1.60934;
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {
                // Use the road-class default below when maxspeed is non-numeric.
            }
        }
        return switch (highway) {
            case "motorway" -> 100.0;
            case "trunk", "primary" -> 70.0;
            case "secondary" -> 50.0;
            case "tertiary" -> 40.0;
            case "service", "living_street" -> 20.0;
            default -> 30.0;
        };
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double latDelta = Math.toRadians(lat2 - lat1);
        double lonDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record OsmNode(String id, double latitude, double longitude) {}

    private record OsmWay(String id, List<String> nodeRefs, String highway,
                          String oneway, String maxspeed) {}
}