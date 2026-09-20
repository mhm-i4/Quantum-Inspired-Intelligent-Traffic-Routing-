package com.qpso.traffic.routing.model;

import com.qpso.traffic.optimization.core.FitnessBreakdown;

/**
 * Configurable objective-function weights for routing.
 *
 * F(R) = α · travelTime + β · distance + γ · congestionCost + penalty
 */
public record ObjectiveWeights(
        double alphaTravelTime,
        double betaDistance,
        double gammaCongestion
) {
    public static final ObjectiveWeights DEFAULT = new ObjectiveWeights(1.0, 0.5, 2.0);

    public FitnessBreakdown breakdown(double time, double dist, double congestion, double penalty,
                                      java.util.List<String> violations) {
        double tCost = alphaTravelTime * time;
        double dCost = betaDistance * dist;
        double cCost = gammaCongestion * congestion;
        double total = tCost + dCost + cCost + penalty;
        return FitnessBreakdown.of(total, tCost, dCost, cCost, penalty, violations);
    }
}
