package com.qpso.traffic.constraints.model;

import com.qpso.traffic.routing.model.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * VRP solution: every vehicle has a route (a tour starting and ending at the
 * depot, with customers visited in order).  Solution cost is the sum of
 * per-vehicle route fitness plus any capacity/duration/violation penalties.
 */
public record VRPSolution(
        Vehicle vehicle,
        List<String> customerIds,
        Route route,
        double totalDemandKg,
        List<String> violatedConstraints,
        double penalty
) {
    public boolean feasible() {
        return route.feasible() && violatedConstraints.isEmpty();
    }

    public VRPSolution withPenalty(String constraint, double additionalPenalty) {
        List<String> vi = new ArrayList<>(violatedConstraints);
        vi.add(constraint);
        return new VRPSolution(vehicle, customerIds, route,
                totalDemandKg, Collections.unmodifiableList(vi),
                penalty + additionalPenalty);
    }
}
