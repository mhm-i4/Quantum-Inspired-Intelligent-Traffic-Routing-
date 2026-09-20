package com.qpso.traffic.graph.model;

import java.util.function.Function;

/**
 * Returns the current traversal cost for a RoadEdge.
 *
 * Defaults: {@link #freeFlowTime()}, {@link #distance()}.
 * In Phase 4+ this is replaced with a traffic-aware implementation
 * (typically weighting current travel time + congestion).
 */
@FunctionalInterface
public interface EdgeCostFunction extends Function<RoadEdge, Double> {

    static EdgeCostFunction freeFlowTime() {
        return RoadEdge::freeFlowTravelTimeMin;
    }

    static EdgeCostFunction distance() {
        return RoadEdge::distanceKm;
    }
}
