package com.qpso.traffic.traffic.sim;

import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.traffic.model.NetworkTrafficState;
import com.qpso.traffic.traffic.model.TrafficScenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Applies a {@link TrafficScenario} to a {@link NetworkTrafficState} based on
 * the graph topology.
 *
 * <p>Importantly: applied traffic conditions are reflected directly into actual the
 * simulation layer via {@link NetworkTrafficState#currentTravelTime(String, double)},
 * which is consumed by the routing fitness.  Therefore congestion really does change
 * the actual fitness landscape of the optimization — we don't just repaint just repaint UI labels.</p>
 */
public class TrafficSimulator {

    private final TransportGraph graph;
    private final long seed;

    public TrafficSimulator(TransportGraph graph) { this(graph, 42L); }
    public TrafficSimulator(TransportGraph graph, long seed) {
        this.graph = graph;
        this.seed = seed;
    }

    public NetworkTrafficState apply(NetworkTrafficState state, TrafficScenario scenario) {
        Random rng = new Random(seed ^ scenario.ordinal() * 31L + System.identityHashCode(graph));
        List<RoadEdge> edges = new ArrayList<>(graph.edges());
        Collections.shuffle(edges, rng);

        switch (scenario) {
            case NORMAL -> applyNormal(state, edges, rng);
            case MORNING_PEAK -> applyPeak(state, edges, rng, 0.35, 0.70);
            case EVENING_PEAK -> applyPeak(state, edges, rng, 0.30, 0.65);
            case ACCIDENT -> applyAccident(state, edges, rng);
            case RANDOM_DYNAMIC -> applyRandom(state, edges, rng);
        }
        return state;
    }

    private void applyNormal(NetworkTrafficState s, List<RoadEdge> edges, Random r) {
        for (RoadEdge e : edges) {
            double c = 0.05 + 0.15 * r.nextDouble();
            s.setState(e.id(), c, e.freeFlowTravelTimeMin());
        }
    }

    private void applyPeak(NetworkTrafficState s, List<RoadEdge> edges, Random r, double min, double max) {
        for (RoadEdge e : edges) {
            double base = min + (max - min) * r.nextDouble();
            double c = Math.max(0.0, Math.min(1.0, base));
            s.setState(e.id(), c, e.freeFlowTravelTimeMin());
        }
    }

    private void applyAccident(NetworkTrafficState s, List<RoadEdge> edges, Random r) {
        // Start from a "normal light-congestion except for one random edge which becomes
        // fully congested (near 0.95+) plus neighbourhood edges get 0.5+.7+.
        applyNormal(s, edges, r);
        if (edges.isEmpty()) return;
        RoadEdge crash = edges.get(r.nextInt(edges.size()));
        s.setState(crash.id(), 0.98, crash.freeFlowTravelTimeMin());
        // Neighbour edges (same source or destination) get heavier traffic from the incident
        for (RoadEdge e : edges) {
            if (e.sourceNodeId().equals(crash.sourceNodeId())
                    || e.destinationNodeId().equals(crash.destinationNodeId())) {
                double c = Math.max(s.congestionOf(e.id()), 0.5 + 0.3 * r.nextDouble());
                s.setState(e.id(), c, e.freeFlowTravelTimeMin());
            }
        }
    }

    private void applyRandom(NetworkTrafficState s, List<RoadEdge> edges, Random r) {
        for (RoadEdge e : edges) {
            double c = Math.pow(r.nextDouble(), 2.5); // bias low, occasional spikes
            c = Math.max(0.0, Math.min(1.0, c));
            s.setState(e.id(), c, e.freeFlowTravelTimeMin());
        }
    }
}
