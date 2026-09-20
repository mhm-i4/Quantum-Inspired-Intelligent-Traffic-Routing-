package com.qpso.traffic.routing.encoding;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.optimization.core.SolutionDecoder;
import com.qpso.traffic.routing.model.Route;

import java.util.*;

/**
 * Decodes a continuous particle position into a valid walk over the graph
 * from source to destination.
 *
 * <h2>Encoding scheme</h2>
 * Each particle has D dimensions where D = maximum allowed path length
 * (a configurable hyperparameter).  At each step the walker is at node `u`
 * and must pick one outgoing edge. We treat the real values `pos[i]` as
 * <i>preferences</i> over the outgoing edges of the current node:
 *
 * <pre>
 *   candidates = outgoing(u)  sorted alphabetically by edge id
 *   idx = (int) (pos[step] * len(candidates))  clipped to [0, len-1]
 *   pick candidate[idx]
 * </pre>
 *
 * If the chosen edge's destination equals the target, we stop early (the
 * remaining dimensions are simply ignored).  If we reach step D without
 * arriving at the target, we return an INFEASIBLE route with a large
 * penalty (and attempt a Dijkstra shortcut as a last resort from the
 * final node).
 *
 * <h3>Rationale</h3>
 * This is a well-known "random-key" style decoder: small tweaks in the
 * continuous space produce small changes in the decoded route (or no
 * change), which makes the fitness function piecewise-smooth enough for
 * PSO/QPSO to explore. It differs from pure permutation encodings because
 * the number of available edges changes per step.
 */
public class ContinuousRouteDecoder implements SolutionDecoder<Route> {

    private final TransportGraph graph;
    private final String source;
    private final String destination;
    private final int maxSteps;
    private final EdgeCostFunction costFn;

    public ContinuousRouteDecoder(TransportGraph graph, String source, String destination,
                                  int maxSteps, EdgeCostFunction costFn) {
        if (!graph.hasNode(source) || !graph.hasNode(destination)) {
            throw new IllegalArgumentException("Unknown source/destination node");
        }
        this.graph = graph;
        this.source = source;
        this.destination = destination;
        this.maxSteps = maxSteps;
        this.costFn = costFn;
    }

    public int dimensions() { return maxSteps; }

    @Override
    public Route decode(double[] position) {
        List<String> nodePath = new ArrayList<>();
        List<String> edgePath = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        nodePath.add(source);
        visited.add(source);

        String current = source;
        double totalDist = 0.0;
        double totalTime = 0.0;
        double congestion = 0.0;

        for (int step = 0; step < maxSteps; step++) {
            if (current.equals(destination)) break;

            List<RoadEdge> outgoing = new ArrayList<>(graph.outgoingEdges(current));
            // Prefer edges leading to unvisited nodes; keep them ordered by id for determinism
            outgoing.sort(Comparator.comparing(RoadEdge::id));

            if (outgoing.isEmpty()) break;

            double p = position[step % position.length];
            double clamped = Math.max(0.0, Math.min(1.0, p));

            int bestIdx = 0;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < outgoing.size(); i++) {
                RoadEdge candidate = outgoing.get(i);

                // Use the continuous value as a soft preference over the ordered list of
                // outgoing edges. This preserves the random-key behavior while still
                // allowing a meaningful choice when there are multiple edges at a node.
                double center = (i + 0.5) / outgoing.size();
                double preference = 1.0 - Math.abs(center - clamped);

                if (candidate.destinationNodeId().equals(destination)) {
                    preference += 1.0;
                }
                if (visited.contains(candidate.destinationNodeId())) {
                    preference -= 1.5;
                }

                if (preference > bestScore) {
                    bestScore = preference;
                    bestIdx = i;
                }
            }

            RoadEdge picked = outgoing.get(bestIdx);

            // Prevent simple cycles: if the chosen destination has been visited,
            // try to pick the best unvisited neighbour (or accept with penalty).
            if (visited.contains(picked.destinationNodeId()) && outgoing.size() > 1) {
                RoadEdge alt = outgoing.stream()
                        .filter(e -> !visited.contains(e.destinationNodeId()))
                        .min(Comparator.comparingDouble((RoadEdge e) -> costFn.apply(e)))
                        .orElse(picked);
                picked = alt;
            }

            edgePath.add(picked.id());
            current = picked.destinationNodeId();
            nodePath.add(current);
            visited.add(current);
            totalDist += picked.distanceKm();
            totalTime += picked.freeFlowTravelTimeMin();
        }

        boolean reached = current.equals(destination);

        // If walker didn't arrive, append Dijkstra shortcut from the final node to
        // destination so the route is at least structurally valid (but incurs a penalty
        // proportional to how late it arrived).
        List<String> violations = new ArrayList<>();
        double penalty = 0.0;

        if (!reached) {
            var fallback = com.qpso.traffic.routing.algorithms.DijkstraSolver
                    .shortestPath(graph, current, destination, costFn);
            if (fallback.isPresent()) {
                Route f = fallback.get();
                for (int i = 1; i < f.nodeIds().size(); i++) nodePath.add(f.nodeIds().get(i));
                for (String e : f.edgeIds()) edgePath.add(e);
                totalDist += f.totalDistanceKm();
                totalTime += f.totalTravelTimeMin();
                penalty += 1000.0 * (nodePath.size() - maxSteps);
                violations.add("path_exceeded_max_steps_used_fallback");
                reached = true;
            } else {
                violations.add("no_path_found_" + source + "_" + destination);
                penalty += 1_000_000.0;
                return new Route(nodePath, edgePath, totalDist, totalTime, congestion,
                        penalty, totalTime + totalDist + congestion + penalty,
                        false, violations);
            }
        }

        return new Route(nodePath, edgePath, totalDist, totalTime, congestion,
                penalty, totalTime + totalDist + congestion + penalty,
                violations.isEmpty(), violations);
    }
}
