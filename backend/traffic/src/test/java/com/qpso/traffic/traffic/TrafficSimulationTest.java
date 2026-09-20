package com.qpso.traffic.traffic;

import com.qpso.traffic.graph.model.SampleNetwork;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.traffic.model.CongestionModel;
import com.qpso.traffic.traffic.model.NetworkTrafficState;
import com.qpso.traffic.traffic.model.TrafficScenario;
import com.qpso.traffic.traffic.sim.TrafficSimulator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrafficSimulationTest {

    private final TransportGraph g = SampleNetwork.create();
    private final TrafficSimulator sim = new TrafficSimulator(g);

    @Test
    @DisplayName("Congestion model: free flow multiplier equals 1 at c=0")
    void congestionModelFreeFlow() {
        assertEquals(1.0, CongestionModel.DEFAULT.factor(0.0), 1e-9);
        assertTrue(CongestionModel.DEFAULT.factor(1.0) > 5.0,
                "max congestion should multiply travel time by > 5");
    }

    @Test
    @DisplayName("Travel time increases with congestion")
    void travelTimeIncreasesWithCongestion() {
        double tt0 = CongestionModel.DEFAULT.currentTravelTime(10.0, 0.0);
        double tt1 = CongestionModel.DEFAULT.currentTravelTime(10.0, 0.5);
        double tt2 = CongestionModel.DEFAULT.currentTravelTime(10.0, 1.0);
        assertEquals(10.0, tt0, 1e-9);
        assertTrue(tt1 > tt0);
        assertTrue(tt2 > tt1);
    }

    @Test
    @DisplayName("All NORMAL scenario edges have congestion in [0, 0.4)")
    void normalScenarioRanges() {
        NetworkTrafficState s = new NetworkTrafficState();
        sim.apply(s, TrafficScenario.NORMAL);
        for (var e : g.edges()) {
            double c = s.congestionOf(e.id());
            assertTrue(c >= 0 && c < 0.4, "NORMAL edge " + e.id() + " had c=" + c);
        }
    }

    @Test
    @DisplayName("ACCIDENT scenario creates at least one edge above 95% congestion")
    void accidentScenarioHasCrash() {
        NetworkTrafficState s = new NetworkTrafficState();
        sim.apply(s, TrafficScenario.ACCIDENT);
        long countHigh = s.allStates().values().stream().filter(st -> st.congestionLevel() >= 0.95).count();
        assertTrue(countHigh >= 1, "Expected at least 1 fully-congested edge in ACCIDENT scenario");
    }

    @Test
    @DisplayName("Traffic scenario really does change the travel-time used by the fitness landscape")
    void scenarioChangesEffectiveCost() {
        var sNormal = new NetworkTrafficState();
        sim.apply(sNormal, TrafficScenario.NORMAL);
        var sPeak = new NetworkTrafficState();
        sim.apply(sPeak, TrafficScenario.EVENING_PEAK);
        // Ensure at least some edges are > normal peak
        boolean anyWorse = false;
        for (var e : g.edges()) {
            double tN = sNormal.currentTravelTime(e.id(), e.freeFlowTravelTimeMin());
            double tP = sPeak.currentTravelTime(e.id(), e.freeFlowTravelTimeMin());
            if (tP > tN + 1e-6) anyWorse = true;
        }
        assertTrue(anyWorse, "Peak scenario should increase travel time on at least one edge");
    }
}
