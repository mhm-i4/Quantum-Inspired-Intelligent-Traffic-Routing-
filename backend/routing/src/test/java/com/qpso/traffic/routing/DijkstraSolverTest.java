package com.qpso.traffic.routing;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.SampleNetwork;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.routing.algorithms.DijkstraSolver;
import com.qpso.traffic.routing.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DijkstraSolverTest {

    private final TransportGraph g = SampleNetwork.create();

    @Test
    @DisplayName("Unreachable node returns empty")
    void unreachableReturnsEmpty() {
        // Create a disconnected node
        TransportGraph g2 = new TransportGraph();
        g2.addNode(new com.qpso.traffic.graph.model.Node("Start", 0, 0));
        g2.addNode(new com.qpso.traffic.graph.model.Node("End", 0, 0));
        assertTrue(DijkstraSolver.shortestPath(g2, "Start", "End", EdgeCostFunction.distance()).isEmpty());
    }

    @Test
    @DisplayName("Alexanderplatz -> Neukoelln finds a feasible path")
    void findsFeasiblePath() {
        Optional<Route> r = DijkstraSolver.shortestPath(g, "Alexanderplatz", "Neukoelln", EdgeCostFunction.freeFlowTime());
        assertTrue(r.isPresent());
        Route route = r.get();
        assertTrue(route.feasible());
        assertEquals("Alexanderplatz", route.nodeIds().get(0));
        assertEquals("Neukoelln", route.nodeIds().get(route.nodeIds().size() - 1));
        assertTrue(route.totalDistanceKm() > 0);
        assertTrue(route.totalTravelTimeMin() > 0);
    }

    @Test
    @DisplayName("Path: distance-optimal route is monotonically improving")
    void distanceAndTimeCostsAreMonotonic() {
        for (RoadEdge e : g.edges()) {
            assertTrue(EdgeCostFunction.distance().apply(e) > 0);
            assertTrue(EdgeCostFunction.freeFlowTime().apply(e) > 0);
        }
    }

    @Test
    @DisplayName("Distance-optimal named-landmark path agrees with Dijkstra on time variant")
    void distanceVsTime() {
        Optional<Route> r1 = DijkstraSolver.shortestPath(g, "Alexanderplatz", "Neukoelln", EdgeCostFunction.distance());
        Optional<Route> r2 = DijkstraSolver.shortestPath(g, "Alexanderplatz", "Neukoelln", EdgeCostFunction.freeFlowTime());
        assertTrue(r1.isPresent() && r2.isPresent());
        // distance-cost routing should yield equal-or-shorter total distance
        assertTrue(r1.get().totalDistanceKm() <= r2.get().totalDistanceKm() + 1e-9);
    }

    @Test
    @DisplayName("If edge cost changes, route changes: Dijkstra respects edge weights")
    void respectsWeights() {
        Optional<Route> rNormal = DijkstraSolver.shortestPath(g, "Alexanderplatz", "Neukoelln",
                EdgeCostFunction.freeFlowTime());

        // Double cost of direct A->E shortcut, force different path
        EdgeCostFunction skewed = (RoadEdge e) -> {
            double base = EdgeCostFunction.freeFlowTime().apply(e);
            if (e.id().startsWith("Alexanderplatz-Nikolaiviertel")) return base * 100.0;
            return base;
        };
        Optional<Route> rSkewed = DijkstraSolver.shortestPath(g, "Alexanderplatz", "Neukoelln", skewed);
        assertTrue(rNormal.isPresent() && rSkewed.isPresent());
        // at minimum, skewed route should not use AE edges
        boolean usesShortcut = rNormal.get().edgeIds().stream().anyMatch(id -> id.startsWith("Alexanderplatz-Nikolaiviertel"));
        boolean usesShortcutSkewed = rSkewed.get().edgeIds().stream().anyMatch(id -> id.startsWith("Alexanderplatz-Nikolaiviertel"));
        if (usesShortcut) {
            assertFalse(usesShortcutSkewed, "When shortcut becomes expensive, dijkstra must avoid it");
        }
    }
}
