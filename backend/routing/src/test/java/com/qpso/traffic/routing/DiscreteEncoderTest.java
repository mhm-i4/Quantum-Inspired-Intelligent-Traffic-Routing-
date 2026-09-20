package com.qpso.traffic.routing;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.SampleNetwork;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.optimization.core.FitnessBreakdown;
import com.qpso.traffic.routing.encoding.ContinuousRouteDecoder;
import com.qpso.traffic.routing.encoding.RouteFitnessFunction;
import com.qpso.traffic.routing.model.ObjectiveWeights;
import com.qpso.traffic.routing.model.Route;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class DiscreteEncoderTest {

    private final TransportGraph g = SampleNetwork.create();
    private final int MAX_STEPS = 12;

    @Test
    @DisplayName("Decoder yields a valid route (starts at source, ends at destination)")
    void decoderProducesValidRoute() {
        var decoder = new ContinuousRouteDecoder(g, "Alexanderplatz", "Neukoelln", MAX_STEPS, EdgeCostFunction.freeFlowTime());
        double[] allMiddle = new double[MAX_STEPS];
        Arrays.fill(allMiddle, 0.5);
        Route r = decoder.decode(allMiddle);
        assertEquals("Alexanderplatz", r.nodeIds().get(0));
        assertEquals("Neukoelln", r.nodeIds().get(r.nodeIds().size() - 1));
    }

    @Test
    @DisplayName("Changing position changes decoded route (not hardcoded)")
    void differentPositionDifferentRoute() {
        var decoder = new ContinuousRouteDecoder(g, "Alexanderplatz", "Neukoelln", MAX_STEPS, EdgeCostFunction.freeFlowTime());
        double[] a = new double[MAX_STEPS];
        double[] b = new double[MAX_STEPS];
        Arrays.fill(a, 0.1);
        Arrays.fill(b, 0.9);
        Route ra = decoder.decode(a);
        Route rb = decoder.decode(b);
        // Different inputs should (almost always) produce different paths
        assertNotEquals(ra.nodeIds().toString(), rb.nodeIds().toString(),
                "Two distinct continuous positions should decode to distinct graph paths");
    }

    @Test
    @DisplayName("Fitness function breaks down per-component costs (time/distance/congestion)")
    void detailedFitnessBreakdown() {
        var decoder = new ContinuousRouteDecoder(g, "Alexanderplatz", "Neukoelln", MAX_STEPS, EdgeCostFunction.freeFlowTime());
        var fitFn = new RouteFitnessFunction(decoder, ObjectiveWeights.DEFAULT);
        double[] pos = new double[MAX_STEPS];
        Arrays.fill(pos, 0.5);
        FitnessBreakdown b = fitFn.evaluateDetailed(pos);
        assertTrue(b.travelTimeCost() >= 0);
        assertTrue(b.distanceCost() >= 0);
        assertEquals(b.travelTimeCost() + b.distanceCost() + b.congestionCost() + b.constraintPenalty(),
                b.totalFitness(), 1e-6);
    }

    @Test
    @DisplayName("Fitness increases when objective weights increase (monotonic in weights)")
    void fitnessMonotonicInWeights() {
        var decoder = new ContinuousRouteDecoder(g, "Alexanderplatz", "Neukoelln", MAX_STEPS, EdgeCostFunction.freeFlowTime());
        double[] pos = new double[MAX_STEPS];
        Arrays.fill(pos, 0.5);
        double fLo = new RouteFitnessFunction(decoder, new ObjectiveWeights(1, 1, 1)).evaluate(pos);
        double fHi = new RouteFitnessFunction(decoder, new ObjectiveWeights(10, 10, 10)).evaluate(pos);
        assertTrue(fHi >= fLo);
    }
}
