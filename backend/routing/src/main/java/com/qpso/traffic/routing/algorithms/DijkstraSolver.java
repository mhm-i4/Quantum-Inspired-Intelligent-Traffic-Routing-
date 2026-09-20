package com.qpso.traffic.routing.algorithms;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.routing.model.Route;

import java.util.*;

/**
 * Dijkstra shortest-path algorithm for directed weighted graph.
 * Returns the minimum-cost sequence of nodes/edges from source to destination
 * according to the given EdgeCostFunction.
 */
public final class DijkstraSolver {

    public static Optional<Route> shortestPath(TransportGraph graph,
                                               String sourceId,
                                               String destinationId,
                                               EdgeCostFunction costFn) {
        if (!graph.hasNode(sourceId) || !graph.hasNode(destinationId)) {
            return Optional.empty();
        }

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, RoadEdge> prevEdge = new HashMap<>();
        Set<String> settled = new HashSet<>();
        PriorityQueue<String> pq = new PriorityQueue<>(
                Comparator.comparingDouble(n -> dist.getOrDefault(n, Double.POSITIVE_INFINITY)));

        dist.put(sourceId, 0.0);
        pq.add(sourceId);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (!settled.add(u)) continue;
            if (u.equals(destinationId)) break;

            double du = dist.getOrDefault(u, Double.POSITIVE_INFINITY);
            for (RoadEdge e : graph.outgoingEdges(u)) {
                String v = e.destinationNodeId();
                if (settled.contains(v)) continue;
                double cost = costFn.apply(e);
                if (cost < 0) {
                    throw new IllegalStateException("Negative edge cost breaks Dijkstra: " + e.id());
                }
                double alt = du + cost;
                if (alt < dist.getOrDefault(v, Double.POSITIVE_INFINITY)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    prevEdge.put(v, e);
                    pq.add(v);
                }
            }
        }

        if (!dist.containsKey(destinationId)
                || dist.get(destinationId).equals(Double.POSITIVE_INFINITY)) {
            return Optional.empty();
        }

        // reconstruct
        List<String> nodes = new ArrayList<>();
        List<String> edges = new ArrayList<>();
        double totalDist = 0.0;
        double totalTime = 0.0;
        String cur = destinationId;
        nodes.add(cur);
        while (!cur.equals(sourceId)) {
            RoadEdge e = prevEdge.get(cur);
            if (e == null) return Optional.empty();
            edges.add(e.id());
            totalDist += e.distanceKm();
            totalTime += e.freeFlowTravelTimeMin();
            cur = prev.get(cur);
            nodes.add(cur);
        }
        Collections.reverse(nodes);
        Collections.reverse(edges);

        return Optional.of(Route.of(nodes, edges, totalDist, totalTime, 0.0));
    }
}
