package com.qpso.traffic.traffic.model;

import java.time.Instant;

/**
 * Live condition of a single directed road edge.
 *
 * congestionLevel is in [0, 1]:
 *   0.0 = free flow
 *   1.0 = fully congested (extreme slowdown)
 */
public record TrafficState(
        String edgeId,
        double congestionLevel,
        double currentTravelTimeMin,
        Instant timestamp
) {
    public static TrafficState freeFlow(String edgeId, double freeFlowTimeMin) {
        return new TrafficState(edgeId, 0.0, freeFlowTimeMin, Instant.now());
    }
}
