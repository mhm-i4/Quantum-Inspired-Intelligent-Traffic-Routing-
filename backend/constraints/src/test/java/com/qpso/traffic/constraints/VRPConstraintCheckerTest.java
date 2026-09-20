package com.qpso.traffic.constraints;

import com.qpso.traffic.constraints.checkers.VRPConstraintChecker;
import com.qpso.traffic.constraints.model.Customer;
import com.qpso.traffic.constraints.model.VRPSolution;
import com.qpso.traffic.constraints.model.Vehicle;
import com.qpso.traffic.graph.model.Node;
import com.qpso.traffic.routing.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VRPConstraintCheckerTest {

    private final Node depot = new Node("DEPOT", 0, 0);
    private final Vehicle smallVan = Vehicle.standard("V1", depot, 100.0, 60.0);
    private final Route emptyRoute = Route.of(List.of(depot.id(), depot.id()), List.of(), 0.0, 5.0, 0.0);
    private final VRPConstraintChecker checker = new VRPConstraintChecker();

    @Test
    @DisplayName("Feasible route passes (demand OK, duration OK, starts/ends at depot)")
    void feasibleRoute() {
        Customer c1 = new Customer("C1", depot.id(), 20.0, 5.0, java.util.Optional.empty());
        var sol = new VRPSolution(smallVan, List.of(c1.id()), emptyRoute, 20.0, List.of(), 0.0);
        var enforced = checker.enforce(sol, Map.of(c1.id(), c1));
        assertTrue(enforced.violatedConstraints().isEmpty());
        assertEquals(0.0, enforced.penalty());
    }

    @Test
    @DisplayName("Exceeding capacity yields a capacity_excess penalty")
    void capacityExcessPenalized() {
        Customer c = new Customer("C1", depot.id(), 150.0, 5.0, java.util.Optional.empty());
        var sol = new VRPSolution(smallVan, List.of(c.id()), emptyRoute, c.demandKg(), List.of(), 0.0);
        var enforced = checker.enforce(sol, Map.of(c.id(), c));
        assertTrue(enforced.violatedConstraints().stream().anyMatch(x -> x.startsWith("capacity_excess")));
        assertTrue(enforced.penalty() > 0);
    }

    @Test
    @DisplayName("Duration in excess of max route duration penalized")
    void durationPenalized() {
        var longRoute = Route.of(List.of(depot.id(), depot.id()), List.of(), 0.0, 500.0, 0.0);
        Customer c = new Customer("C1", depot.id(), 10.0, 5.0, java.util.Optional.empty());
        var sol = new VRPSolution(smallVan, List.of(c.id()), longRoute, 10.0, List.of(), 0.0);
        var enforced = checker.enforce(sol, Map.of(c.id(), c));
        assertTrue(enforced.violatedConstraints().stream().anyMatch(x -> x.startsWith("max_duration_excess")));
        assertTrue(enforced.penalty() > 0);
    }

    @Test
    @DisplayName("Route that doesn't start/end at depot penalized")
    void depotPenalized() {
        Node other = new Node("X", 0, 0);
        var r = Route.of(List.of(other.id(), other.id()), List.of(), 0.0, 10.0, 0.0);
        var sol = new VRPSolution(smallVan, List.of(), r, 0.0, List.of(), 0.0);
        var enforced = checker.enforce(sol, Map.of());
        assertTrue(enforced.violatedConstraints().contains("route_does_not_start_at_depot"));
        assertTrue(enforced.violatedConstraints().contains("route_does_not_end_at_depot"));
    }
}
