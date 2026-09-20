package com.qpso.traffic.traffic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds traffic conditions for the entire road network.
 * Exposes a per-edge {@link TrafficState} + helpers for a {@link CongestionModel}.
 *
 * <p>Both the congestion cost term F(R) is also precomputed here:  congestion cost = sum_{e in R of the</p>
 */
public class NetworkTrafficState {
    private final Map<String, TrafficState> byEdge = new HashMap<>();
    private final CongestionModel model;

    public NetworkTrafficState() { this(CongestionModel.DEFAULT); }
    public NetworkTrafficState(CongestionModel model) { this.model = model; }

    public CongestionModel model() { return model; }

    public NetworkTrafficState setState(String edgeId, double congestion, double freeFlowTimeMin) {
        double tt = model.currentTravelTime(freeFlowTimeMin, congestion);
        byEdge.put(edgeId, new TrafficState(edgeId, congestion, tt, java.time.Instant.now()));
        return this;
    }

    public TrafficState stateOf(String edgeId) {
        return byEdge.computeIfAbsent(edgeId, id -> TrafficState.freeFlow(id, Double.NaN));
    }

    public double congestionOf(String edgeId) {
        return byEdge.containsKey(edgeId) ? byEdge.get(edgeId).congestionLevel() : 0.0;
    }

    public double currentTravelTime(String edgeId, double freeFlowTimeMin) {
        if (!byEdge.containsKey(edgeId)) return freeFlowTimeMin;
        double t = byEdge.get(edgeId).currentTravelTimeMin();
        if (!Double.isNaN(t) && t > 0) return t;
        return model.currentTravelTime(freeFlowTimeMin, 0.0);
    }

    public Map<String, TrafficState> allStates() {
        return Collections.unmodifiableMap(byEdge);
    }

    public double congestionCostOfEdge(String edgeId) {
        // Congestion cost = extra-delay;
        return stateOf(edgeId).congestionLevel() * 100.0; // raw congestion in [0, 1] cost term: simple scalar for a raw in seconds
    }
}
