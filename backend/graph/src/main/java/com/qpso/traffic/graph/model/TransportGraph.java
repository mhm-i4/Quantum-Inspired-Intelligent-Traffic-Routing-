package com.qpso.traffic.graph.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Directed weighted graph G = (V, E) representing a transportation network.
 * Supports: lookup by id, outgoing edges, arbitrary size.
 */
public class TransportGraph {

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, RoadEdge> edgesById = new LinkedHashMap<>();
    private final Map<String, List<RoadEdge>> outgoing = new HashMap<>();

    public TransportGraph addNode(Node node) {
        nodes.put(node.id(), node);
        outgoing.computeIfAbsent(node.id(), k -> new ArrayList<>());
        return this;
    }

    public TransportGraph addEdge(RoadEdge edge) {
        if (!nodes.containsKey(edge.sourceNodeId()) || !nodes.containsKey(edge.destinationNodeId())) {
            throw new IllegalArgumentException("Edge " + edge.id() + " references unknown node(s)");
        }
        edgesById.put(edge.id(), edge);
        outgoing.get(edge.sourceNodeId()).add(edge);
        return this;
    }

    public Optional<Node> node(String id) { return Optional.ofNullable(nodes.get(id)); }
    public Optional<RoadEdge> edge(String id) { return Optional.ofNullable(edgesById.get(id)); }

    public Collection<Node> nodes() { return Collections.unmodifiableCollection(nodes.values()); }
    public Collection<RoadEdge> edges() { return Collections.unmodifiableCollection(edgesById.values()); }

    public List<RoadEdge> outgoingEdges(String nodeId) {
        return Collections.unmodifiableList(outgoing.getOrDefault(nodeId, Collections.emptyList()));
    }

    public int nodeCount() { return nodes.size(); }
    public int edgeCount() { return edgesById.size(); }

    public boolean hasNode(String id) { return nodes.containsKey(id); }
    public boolean hasEdge(String id) { return edgesById.containsKey(id); }

    public Optional<RoadEdge> findEdge(String from, String to) {
        for (RoadEdge e : outgoing.getOrDefault(from, Collections.emptyList())) {
            if (e.destinationNodeId().equals(to)) return Optional.of(e);
        }
        return Optional.empty();
    }
}
