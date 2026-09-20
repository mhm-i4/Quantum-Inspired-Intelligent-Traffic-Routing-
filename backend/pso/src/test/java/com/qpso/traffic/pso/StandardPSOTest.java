package com.qpso.traffic.pso;

import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 validation: Standard PSO baseline must also converge meaningfully
 * on mathematical benchmark functions so comparison with QPSO is meaningful.
 */
class StandardPSOTest {

    private static final int SEED = 42;
    private static final int SWARM_SIZE = 40;
    private static final int MAX_ITER = 300;
    private static final int DIM = 10;

    private StandardPSO pso;

    @BeforeEach
    void setUp() {
        pso = new StandardPSO(SWARM_SIZE, StandardPSO.PSOParams.DEFAULT);
    }

    @Test
    @DisplayName("PSO result structure is valid")
    void resultStructureIsValid() {
        OptimizationResult<double[]> res = pso.optimize(
                BenchmarkFunctions.sphere(), 5, -5.12, 5.12, 20, SEED);
        assertNotNull(res);
        assertEquals(5, res.bestSolution().length);
        assertEquals("StandardPSO", res.algorithmName());
        assertTrue(res.feasible());
        assertEquals(21, res.convergenceHistory().size());
    }

    @Test
    @DisplayName("Convergence history is non-increasing")
    void historyNonIncreasing() {
        OptimizationResult<double[]> res = pso.optimize(
                BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);
        List<Double> hist = res.convergenceHistory();
        for (int i = 1; i < hist.size(); i++) {
            assertTrue(hist.get(i) <= hist.get(i - 1) + 1e-9,
                    "iter " + i + ": " + hist.get(i) + " > " + hist.get(i-1));
        }
    }

    @Test
    @DisplayName("Deterministic with fixed seed")
    void deterministic() {
        OptimizationResult<double[]> a = pso.optimize(
                BenchmarkFunctions.rosenbrock(), 5, -2.048, 2.048, 80, 99);
        OptimizationResult<double[]> b = pso.optimize(
                BenchmarkFunctions.rosenbrock(), 5, -2.048, 2.048, 80, 99);
        assertEquals(a.bestFitness(), b.bestFitness(), 1e-12);
        assertArrayEquals(a.bestSolution(), b.bestSolution(), 1e-12);
    }

    @Test
    @DisplayName("Sphere: high-quality solution (fitness < 1e-3 for D=10)")
    void sphereHighQuality() {
        OptimizationResult<double[]> res = pso.optimize(
                BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);
        assertTrue(res.bestFitness() < 1e-3,
                "Expected fitness < 1e-3, got " + res.bestFitness());
    }

    @Test
    @DisplayName("Rastrigin: reasonable solution (fitness < 80 for D=10)")
    void rastriginGood() {
        OptimizationResult<double[]> res = pso.optimize(
                BenchmarkFunctions.rastrigin(), DIM, -5.12, 5.12, MAX_ITER, SEED);
        assertTrue(res.bestFitness() < 80.0,
                "Expected fitness < 80, got " + res.bestFitness());
    }
}
