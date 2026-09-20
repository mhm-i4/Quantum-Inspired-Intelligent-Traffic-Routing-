package com.qpso.traffic.traffic.model;

/**
 * Configurable mapping from congestion [0,1] → travel-time multiplier.
 *
 * currentTravelTime = freeFlowTime × congestionFactor(congestion)
 *
 * Default BPR-like (Bureau of Public Roads) style curve:
 *
 *   factor(c) = 1 + K * c^N
 *
 * K and N are configurable (defaults K=5, N=2).
 */
public record CongestionModel(double k, double n) {
    public static final CongestionModel DEFAULT = new CongestionModel(5.0, 2.0);

    public double factor(double congestion) {
        double c = Math.max(0.0, Math.min(1.0, congestion));
        return 1.0 + k * Math.pow(c, n);
    }

    public double currentTravelTime(double freeFlowTimeMin, double congestion) {
        return freeFlowTimeMin * factor(congestion);
    }
}
