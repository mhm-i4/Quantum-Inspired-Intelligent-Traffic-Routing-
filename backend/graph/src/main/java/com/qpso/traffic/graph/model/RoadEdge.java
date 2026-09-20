package com.qpso.traffic.graph.model;

/**
 * Directed road edge.  source → destination.
 * For two-way roads, insert two edges (A→B and B→A), possibly with
 * symmetric or asymmetric properties.
 */
public record RoadEdge(
        String id,
        String sourceNodeId,
        String destinationNodeId,
        double distanceKm,
        double freeFlowTravelTimeMin,
        double capacityVehiclesPerHour,
        double freeFlowSpeedKmh
) {
    public static RoadEdge create(String id, String from, String to, double distanceKm, double freeFlowSpeedKmh) {
        double timeMin = (distanceKm / Math.max(1e-6, freeFlowSpeedKmh)) * 60.0;
        double capacity = 1200.0; // default 2-lane capacity
        return new RoadEdge(id, from, to, distanceKm, timeMin, capacity, freeFlowSpeedKmh);
    }
}
