package com.qpso.traffic.optimization.core;

/**
 * Common super-interface for any continuous-space optimization algorithm
 * that can minimize a FitnessFunction over a bounded continuous domain.
 *
 * @param <D> decoded solution type
 */
public interface OptimizationAlgorithm<D> {

    /**
     * Run the optimizer against the given fitness function and bounds.
     *
     * @param fitness         fitness function (lower is better)
     * @param dimensions      number of continuous dimensions
     * @param lowerBound      per-dimension lower bound (inclusive)
     * @param upperBound      per-dimension upper bound (inclusive)
     * @param maxIterations   maximum number of iterations
     * @param seed            random seed for reproducibility
     */
    OptimizationResult<double[]> optimize(
            FitnessFunction<double[]> fitness,
            int dimensions,
            double lowerBound,
            double upperBound,
            int maxIterations,
            long seed
    );

    /**
     * Display name for reporting / benchmarking.
     */
    String algorithmName();
}
