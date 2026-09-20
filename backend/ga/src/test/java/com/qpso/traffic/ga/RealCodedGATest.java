package com.qpso.traffic.ga;

import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RealCodedGATest {

    private RealCodedGA ga;

    @BeforeEach
    void setUp() { ga = new RealCodedGA(60, RealCodedGA.GAParams.DEFAULT); }

    @Test
    @DisplayName("GA result structure is valid")
    void resultStructure() {
        OptimizationResult<double[]> r = ga.optimize(BenchmarkFunctions.sphere(), 5, -5.12, 5.12, 20, 1L);
        assertEquals("RealCodedGA", r.algorithmName());
        assertTrue(r.bestFitness() >= 0.0);
        assertEquals(21, r.convergenceHistory().size());
    }

    @Test
    @DisplayName("Convergence history is non-increasing")
    void historyNonIncreasing() {
        OptimizationResult<double[]> r = ga.optimize(BenchmarkFunctions.sphere(), 10, -5.12, 5.12, 100, 3L);
        List<Double> h = r.convergenceHistory();
        for (int i = 1; i < h.size(); i++) assertTrue(h.get(i) <= h.get(i - 1) + 1e-9);
    }

    @Test
    @DisplayName("GA improves fitness >1 order of magnitude on Sphere D=10 in 200 iters")
    void sphereConvergence() {
        OptimizationResult<double[]> r = ga.optimize(BenchmarkFunctions.sphere(), 10, -5.12, 5.12, 200, 5L);
        double initial = r.convergenceHistory().get(0);
        assertTrue(r.bestFitness() < initial / 10.0,
                "final=" + r.bestFitness() + " should be < initial/10=" + (initial / 10));
    }

    @Test
    @DisplayName("GA determinism: same seed → same result")
    void deterministic() {
        OptimizationResult<double[]> a = ga.optimize(BenchmarkFunctions.rosenbrock(), 5, -2.048, 2.048, 60, 11L);
        OptimizationResult<double[]> b = ga.optimize(BenchmarkFunctions.rosenbrock(), 5, -2.048, 2.048, 60, 11L);
        assertEquals(a.bestFitness(), b.bestFitness(), 1e-9);
    }
}
