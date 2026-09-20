package com.qpso.traffic.optimization.core;

import java.util.Collections;
import java.util.List;

public record FitnessBreakdown(
        double travelTimeCost,
        double distanceCost,
        double congestionCost,
        double constraintPenalty,
        double totalFitness,
        List<String> violatedConstraints
) {
    public static FitnessBreakdown of(double totalFitness) {
        return new FitnessBreakdown(0.0, 0.0, 0.0, 0.0, totalFitness, Collections.emptyList());
    }

    public static FitnessBreakdown of(double totalFitness, double travelTimeCost, double distanceCost,
                                      double congestionCost, double constraintPenalty,
                                      List<String> violatedConstraints) {
        return new FitnessBreakdown(travelTimeCost, distanceCost, congestionCost,
                constraintPenalty, totalFitness,
                violatedConstraints == null ? Collections.emptyList() : violatedConstraints);
    }
}
