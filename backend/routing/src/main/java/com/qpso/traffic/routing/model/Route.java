package com.qpso.traffic.routing.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Route(
        List<String> nodeIds,
        List<String> edgeIds,
        double totalDistanceKm,
        double totalTravelTimeMin,
        double totalCongestionCost,
        double penalty,
        double totalFitness,
        boolean feasible,
        List<String> violatedConstraints
) {
    public static Route invalid(String source, String destination, String reason) {
        return new Route(
                List.of(source, destination),
                Collections.emptyList(),
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                false,
                List.of(reason)
        );
    }

    public static Route of(List<String> nodeIds, List<String> edgeIds,
                           double distance, double time, double congestion) {
        return new Route(new ArrayList<>(nodeIds), new ArrayList<>(edgeIds),
                distance, time, congestion, 0.0,
                distance + time + congestion, true, Collections.emptyList());
    }

    public Route withPenalty(double penalty, List<String> violations) {
        List<String> v = new ArrayList<>(this.violatedConstraints);
        if (violations != null) v.addAll(violations);
        double total = this.totalTravelTimeMin + this.totalDistanceKm + this.totalCongestionCost + penalty;
        return new Route(this.nodeIds, this.edgeIds,
                this.totalDistanceKm, this.totalTravelTimeMin, this.totalCongestionCost,
                this.penalty + penalty, total,
                v.isEmpty(), v);
    }

    public List<String> nodeIds() { return Collections.unmodifiableList(nodeIds); }
    public List<String> edgeIds() { return Collections.unmodifiableList(edgeIds); }
    public List<String> violatedConstraints() { return Collections.unmodifiableList(violatedConstraints); }
}
