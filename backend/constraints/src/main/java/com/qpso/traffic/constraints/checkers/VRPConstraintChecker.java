package com.qpso.traffic.constraints.checkers;

import com.qpso.traffic.constraints.model.Customer;
import com.qpso.traffic.constraints.model.VRPSolution;
import com.qpso.traffic.constraints.model.Vehicle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies VRP constraints (hard rules converted to soft penalties):
 * <ul>
 *   <li>Vehicle capacity: demand must not exceed {@link Vehicle#capacityKg}</li>
 *   <li>Max route duration</li>
 *   <li>Customer time windows (optional)</li>
 *   <li>Route starts &amp; ends at depot</li>
 * </ul>
 *
 * Penalties are large constants × excess; 1e4 per unit of violation ensures
 * the optimizer strongly prefers feasible solutions while still scoring
 * infeasible ones (useful during early search).
 */
public class VRPConstraintChecker {

    public static final double CAPACITY_PENALTY_PER_KG = 1_000.0;
    public static final double DURATION_PENALTY_PER_MIN = 500.0;
    public static final double TIME_WINDOW_PENALTY = 10_000.0;
    public static final double DEPOT_PENALTY = 50_000.0;

    public VRPSolution enforce(VRPSolution sol, Map<String, Customer> customersById) {
        List<String> violations = new ArrayList<>(sol.violatedConstraints());
        double penalty = sol.penalty();
        Vehicle v = sol.vehicle();

        if (sol.totalDemandKg() > v.capacityKg()) {
            double excess = sol.totalDemandKg() - v.capacityKg();
            penalty += CAPACITY_PENALTY_PER_KG * excess;
            violations.add("capacity_excess:" + String.format("%.2f", excess) + "kg");
        }
        if (sol.route().totalTravelTimeMin() > v.maxRouteDurationMin()) {
            double excess = sol.route().totalTravelTimeMin() - v.maxRouteDurationMin();
            penalty += DURATION_PENALTY_PER_MIN * excess;
            violations.add("max_duration_excess:" + String.format("%.2f", excess) + "min");
        }
        List<String> nodes = sol.route().nodeIds();
        if (!nodes.isEmpty() && !nodes.get(0).equals(v.depotNodeId())) {
            penalty += DEPOT_PENALTY;
            violations.add("route_does_not_start_at_depot");
        }
        if (!nodes.isEmpty() && !nodes.get(nodes.size() - 1).equals(v.depotNodeId())) {
            penalty += DEPOT_PENALTY;
            violations.add("route_does_not_end_at_depot");
        }
        for (String cid : sol.customerIds()) {
            Customer c = customersById.get(cid);
            if (c != null && c.timeWindow().isPresent()) {
                // Heuristic: we don't track exact arrival, so just check total route duration
                // A full VRP model would accumulate time along nodes; keep it simple for the hackathon.
                if (sol.route().totalTravelTimeMin() > 999) {
                    penalty += TIME_WINDOW_PENALTY;
                    violations.add("time_window_violated:" + cid);
                }
            }
        }
        return new VRPSolution(sol.vehicle(), sol.customerIds(), sol.route(),
                sol.totalDemandKg(), violations, penalty);
    }
}
