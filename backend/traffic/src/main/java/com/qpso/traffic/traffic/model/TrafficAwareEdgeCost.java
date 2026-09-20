package com.qpso.traffic.traffic.model;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.RoadEdge;

/**
 * Edge-cost function that consults {@link NetworkTrafficState} so that
 * congested edges have higher cost in the routing fitness landscape.
 */
public class TrafficAwareEdgeCost implements EdgeCostFunction {
    private final NetworkTrafficState traffic;
    private final double weightTravelTime;

    public TrafficAwareEdgeCost(NetworkTrafficState traffic, double weightTravelTime) {
        this.traffic = traffic;
        this.weightTravelTime = weightTravelTime;
    }

    @Override
    public Double apply(RoadEdge e) {
        return weightTravelTime * traffic.currentTravelTime(e.id(), e.freeFlowTravelTimeMin());
    }
}
