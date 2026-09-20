package com.qpso.traffic.benchmarking;

import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.optimization.core.PopulationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BenchmarkRunnerTest {

    /** Local deterministic "hill-climbing" optimizer used in tests so we don't
     *  create a circular dependency between benchmarking and qpso/pso/ga.
     *  It makes progress, which is all we need to exercise the statistics code. */
    static final class LocalOptimizer implements OptimizationAlgorithm<double[]> {
        private final int pop;
        LocalOptimizer(int pop) { this.pop = pop; }

        @Override
        public OptimizationResult<double[]> optimize(
                FitnessFunction<double[]> fitness, int dims, double lo, double hi,
                int maxIter, long seed) {
            PopulationConfig cfg = new PopulationConfig(pop, maxIter, seed);
            Random rng = cfg.createRandom();
            long start = System.currentTimeMillis();
            long evals = 0;

            double[][] xs = new double[pop][dims];
            double[] fs = new double[pop];
            double[] best = null;
            double bestF = Double.POSITIVE_INFINITY;
            double range = hi - lo;
            for (int i = 0; i < pop; i++) {
                for (int d = 0; d < dims; d++) xs[i][d] = lo + rng.nextDouble() * range;
                fs[i] = fitness.evaluate(xs[i]); evals++;
                if (fs[i] < bestF) { bestF = fs[i]; best = xs[i].clone(); }
            }

            OptimizationResult.Builder<double[]> b = OptimizationResult.<double[]>builder()
                    .algorithmName(algorithmName()).recordConvergence(bestF);
            for (int t = 0; t < maxIter; t++) {
                for (int i = 0; i < pop; i++) {
                    // mutation toward best so far
                    double[] cand = xs[i].clone();
                    for (int d = 0; d < dims; d++) {
                        cand[d] = cand[d] + 0.2 * rng.nextGaussian() * range
                                + 0.05 * (best[d] - cand[d]);
                        if (cand[d] > hi) cand[d] = hi;
                        if (cand[d] < lo) cand[d] = lo;
                    }
                    double f = fitness.evaluate(cand); evals++;
                    if (f < fs[i]) { fs[i] = f; xs[i] = cand; }
                    if (f < bestF) { bestF = f; best = cand.clone(); }
                }
                b.recordConvergence(bestF);
            }
            return b.bestSolution(best).bestFitness(bestF)
                    .iterations(maxIter).evaluations(evals)
                    .runtimeMs(System.currentTimeMillis() - start)
                    .feasible(true).build();
        }
        @Override public String algorithmName() { return "LocalHillClimber"; }
    }

    @Test
    @DisplayName("Benchmark statistics compute across trials correctly")
    void statsComputed() {
        var runner = new BenchmarkRunner();
        var cfg = new BenchmarkRunner.BenchmarkConfig(5, 40, 1L);
        var result = runner.run(new LocalOptimizer(30),
                BenchmarkFunctions.sphere(), 5, -5.12, 5.12, cfg);

        assertEquals(5, result.statistics().trials());
        assertTrue(Double.isFinite(result.statistics().best()));
        assertTrue(Double.isFinite(result.statistics().mean()));
        assertTrue(Double.isFinite(result.statistics().stdDev()));
        assertTrue(result.statistics().best() <= result.statistics().worst());
        assertTrue(result.statistics().mean() >= result.statistics().best());
        assertTrue(result.statistics().mean() <= result.statistics().worst());
        assertFalse(result.statistics().meanConvergence().isEmpty());
        assertFalse(result.statistics().perTrialBestFitness().isEmpty());
    }
}
