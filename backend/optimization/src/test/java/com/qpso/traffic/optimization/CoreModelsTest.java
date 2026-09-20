package com.qpso.traffic.optimization;

import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.FitnessBreakdown;
import com.qpso.traffic.optimization.core.Particle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoreModelsTest {

    @Test
    @DisplayName("Particle personal best updates correctly when fitness improves")
    void particleUpdatesPersonalBest() {
        Particle<Object> p = Particle.unevaluated(new double[]{1.0, 2.0});
        assertFalse(p.evaluated());

        Particle<Object> evaluated = p.withEvaluation(5.0, null);
        assertTrue(evaluated.evaluated());
        assertEquals(5.0, evaluated.currentFitness());
        assertEquals(5.0, evaluated.personalBestFitness());
        assertArrayEquals(new double[]{1.0, 2.0}, evaluated.personalBestPosition());

        Particle<Object> worse = evaluated.withPosition(new double[]{3.0, 4.0}).withEvaluation(10.0, null);
        assertEquals(10.0, worse.currentFitness());
        assertEquals(5.0, worse.personalBestFitness(),
                "worse fitness must not overwrite personal best");
        assertArrayEquals(new double[]{1.0, 2.0}, worse.personalBestPosition());

        Particle<Object> better = worse.withPosition(new double[]{0.1, 0.2}).withEvaluation(2.0, null);
        assertEquals(2.0, better.currentFitness());
        assertEquals(2.0, better.personalBestFitness(),
                "better fitness must overwrite personal best");
        assertArrayEquals(new double[]{0.1, 0.2}, better.personalBestPosition());
    }

    @Test
    @DisplayName("FitnessBreakdown scalar wrapper is consistent")
    void fitnessBreakdownScalar() {
        FitnessBreakdown b = FitnessBreakdown.of(42.0);
        assertEquals(42.0, b.totalFitness());
        assertEquals(0.0, b.travelTimeCost());
        assertEquals(0.0, b.distanceCost());
        assertEquals(0.0, b.congestionCost());
        assertEquals(0.0, b.constraintPenalty());
        assertTrue(b.violatedConstraints().isEmpty());
    }

    @Test
    @DisplayName("Benchmark Sphere known points evaluate correctly")
    void sphereKnown() {
        assertEquals(0.0, BenchmarkFunctions.sphere().evaluate(new double[]{0, 0, 0}), 1e-9);
        assertEquals(14.0, BenchmarkFunctions.sphere().evaluate(new double[]{1.0, 2.0, 3.0}), 1e-9);
    }

    @Test
    @DisplayName("Benchmark Rastrigin optimum is 0 at origin")
    void rastriginOptimum() {
        assertEquals(0.0, BenchmarkFunctions.rastrigin().evaluate(new double[]{0, 0, 0, 0}), 1e-9);
    }

    @Test
    @DisplayName("Benchmark Rosenbrock optimum is 0 at (1,1,...,1)")
    void rosenbrockOptimum() {
        assertEquals(0.0, BenchmarkFunctions.rosenbrock().evaluate(new double[]{1.0, 1.0, 1.0}), 1e-9);
    }
}
