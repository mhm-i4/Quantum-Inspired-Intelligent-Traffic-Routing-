package com.qpso.traffic.constraints.model;

import com.qpso.traffic.graph.model.Node;

public record Vehicle(
        String id,
        double capacityKg,
        String depotNodeId,
        double maxRouteDurationMin
) {
    public static Vehicle standard(String id, Node depot, double capacityKg, double maxRouteMin) {
        return new Vehicle(id, capacityKg, depot.id(), maxRouteMin);
    }
}
