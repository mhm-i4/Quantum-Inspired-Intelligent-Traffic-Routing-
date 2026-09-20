package com.qpso.traffic.qpso;

import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 validation: QPSO implementation must converge meaningfully on
 * standard mathematical benchmark functions before being used for routing.
 *
 * Each test uses a seeded random number generator so results are reproducible.
 */
class QPSOOptimizerTest {

    private static final int SEED = 42;
    private static final int SWARM_SIZE = 40;
    private static final int MAX_ITER = 300;
    private static final int DIM = 10;

    private QPSOOptimizer qpso;

    @BeforeEach
    void setUp() {
        qpso = new QPSOOptimizer(SWARM_SIZE, QPSOOptimizer.QPSOParams.DEFAULT);
    }

    // ============================================================
    // Sanity checks on QPSO internals
    // ============================================================
    @Test
    @DisplayName("QPSO returns valid result structure (non-null fields, expected name)")
    void resultStructureIsValid() {
        OptimizationResult<double[]> res = qpso.optimize(
                BenchmarkFunctions.sphere(), 4, -5.12, 5.12, 20, SEED);

        assertNotNull(res, "OptimizationResult must not be null");
        assertNotNull(res.bestSolution(), "best solution must not be null");
        assertEquals(4, res.bestSolution().length, "best solution dimensions");
        assertEquals("QPSO", res.algorithmName());
        assertTrue(res.feasible(), "benchmark problems are unconstrained -> always feasible");
        assertEquals(21, res.convergenceHistory().size(),
                "convergence history length = iterations + 1 (initial + 20 iter)");
        assertTrue(res.bestFitness() >= 0.0, "Sphere is always >= 0");
    }

    @Test
    @DisplayName("Convergence history is monotonically non-increasing (best-so-far)")
    void convergenceHistoryIsNonIncreasing() {
        OptimizationResult<double[]> res = qpso.optimize(
                BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);

        List<Double> hist = res.convergenceHistory();
        for (int i = 1; i < hist.size(); i++) {
            assertTrue(hist.get(i) <= hist.get(i - 1) + 1e-9,
                    "hist[" + i + "]=" + hist.get(i) + " must be <= hist[" + (i-1) + "]=" + hist.get(i-1));
        }
    }

    @Test
    @DisplayName("Global best fitness equals last entry in convergence history")
    void historyMatchesBestFitness() {
        OptimizationResult<double[]> res = qpso.optimize(
                BenchmarkFunctions.rastrigin(), DIM, -5.12, 5.12, MAX_ITER, SEED);
        List<Double> hist = res.convergenceHistory();
        assertEquals(res.bestFitness(), hist.get(hist.size() - 1), 1e-9,
                "final convergence entry must equal best fitness");
    }

    @Test
    @DisplayName("Result is deterministic given the same seed")
    void deterministicWhenSeeded() {
        FitnessFunction<double[]> fit = BenchmarkFunctions.rosenbrock();
        OptimizationResult<double[]> a = qpso.optimize(fit, 5, -2.048, 2.048, 100, 7);
        OptimizationResult<double[]> b = qpso.optimize(fit, 5, -2.048, 2.048, 100, 7);

        assertEquals(a.bestFitness(), b.bestFitness(), 1e-12,
                "Same seed must yield same best fitness");
        assertArrayEquals(a.bestSolution(), b.bestSolution(), 1e-12,
                "Same seed must yield same best solution");
    }

    // ============================================================
    // Convergence tests on standard benchmark functions
    // ============================================================
    @Nested
    @DisplayName("Sphere function convergence (unimodal, convex)")
    class SphereConvergence {
        @Test
        @DisplayName("Final fitness is strictly better than random initialization")
        void betterThanRandom() {
            Random r = new Random(SEED);
            double randomFitness = 0.0;
            for (int t = 0; t < SWARM_SIZE; t++) {
                double[] x = new double[DIM];
                for (int d = 0; d < DIM; d++) x[d] = -5.12 + r.nextDouble() * 10.24;
                randomFitness += BenchmarkFunctions.sphere().evaluate(x);
            }
            randomFitness /= SWARM_SIZE;

            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);

            assertTrue(res.bestFitness() < randomFitness,
                    "Final fitness " + res.bestFitness() + " should be < avg random " + randomFitness);
        }

        @Test
        @DisplayName("Fitness improves by at least 2 orders of magnitude from initial swarm")
        void significantImprovement() {
            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);
            double initial = res.convergenceHistory().get(0);
            double finalFit = res.bestFitness();
            assertTrue(finalFit < initial / 100.0,
                    "Expected " + finalFit + " < initial/100 (" + (initial/100) + ")");
        }

        @Test
        @DisplayName("Finds a high-quality solution for Sphere (fitness < 1e-2 for D=10, 300 iter)")
        void highQualitySolution() {
            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.sphere(), DIM, -5.12, 5.12, MAX_ITER, SEED);
            // Sphere optimum is 0; we require at least 1e-2 for D=10 in 300 iterations
            assertTrue(res.bestFitness() < 1e-2,
                    "Expected fitness < 1e-2, got " + res.bestFitness());
        }
    }

    @Nested
    @DisplayName("Rastrigin function convergence (multimodal, 10D)")
    class RastriginConvergence {
        @Test
        @DisplayName("Final fitness strictly better than average random")
        void betterThanRandom() {
            Random r = new Random(SEED);
            double randomFitness = 0.0;
            for (int t = 0; t < SWARM_SIZE; t++) {
                double[] x = new double[DIM];
                for (int d = 0; d < DIM; d++) x[d] = -5.12 + r.nextDouble() * 10.24;
                randomFitness += BenchmarkFunctions.rastrigin().evaluate(x);
            }
            randomFitness /= SWARM_SIZE;

            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.rastrigin(), DIM, -5.12, 5.12, MAX_ITER, SEED);
            assertTrue(res.bestFitness() < randomFitness,
                    "Final " + res.bestFitness() + " should be < avg random " + randomFitness);
        }

        @Test
        @DisplayName("Improves at least one order of magnitude from initial swarm")
        void magnitudeImprovement() {
            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.rastrigin(), DIM, -5.12, 5.12, MAX_ITER, SEED);
            double initial = res.convergenceHistory().get(0);
            double finalFit = res.bestFitness();
            assertTrue(finalFit < initial / 10.0,
                    "Expected " + finalFit + " < initial/10 (" + (initial/10) + ")");
        }

        @Test
        @DisplayName("Finds a good solution for Rastrigin (fitness < 50 for D=10)")
        void goodSolution() {
            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.rastrigin(), DIM, -5.12, 5.12, MAX_ITER, SEED);
            // Rastrigin D=10 optimum is 0; expect a reasonable solution in 300 iterations
            assertTrue(res.bestFitness() < 50.0,
                    "Expected fitness < 50, got " + res.bestFitness());
        }
    }

    @Nested
    @DisplayName("Rosenbrock function convergence (narrow valley, D=10)")
    class RosenbrockConvergence {
        @Test
        @DisplayName("Final fitness strictly better than average random")
        void betterThanRandom() {
            Random r = new Random(SEED);
            double randomFitness = 0.0;
            for (int t = 0; t < SWARM_SIZE; t++) {
                double[] x = new double[DIM];
                for (int d = 0; d < DIM; d++) x[d] = -2.048 + r.nextDouble() * 4.096;
                randomFitness += BenchmarkFunctions.rosenbrock().evaluate(x);
            }
            randomFitness /= SWARM_SIZE;

            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.rosenbrock(), DIM, -2.048, 2.048, MAX_ITER, SEED);
            assertTrue(res.bestFitness() < randomFitness,
                    "Final " + res.bestFitness() + " should be < avg random " + randomFitness);
        }

        @Test
        @DisplayName("Improves at least one order of magnitude from initial swarm")
        void magnitudeImprovement() {
            OptimizationResult<double[]> res = qpso.optimize(
                    BenchmarkFunctions.rosenbrock(), DIM, -2.048, 2.048, MAX_ITER, SEED);
            double initial = res.convergenceHistory().get(0);
            double finalFit = res.bestFitness();
            assertTrue(finalFit < initial / 10.0,
                    "Expected " + finalFit + " < initial/10 (" + (initial/10) + ")");
        }
    }
}
