package com.qpso.traffic.optimization.core;

/**
 * Generic fitness function interface for optimization.
 * Lower fitness values represent better solutions.
 *
 * @param <S> the type of the solution/particle representation
 */
@FunctionalInterface
public interface FitnessFunction<S> {

    /**
     * Evaluate the given solution and return its fitness (lower is better).
     */
    double evaluate(S solution);

    /**
     * Optional: evaluate the solution and return a detailed breakdown
     * of each cost component. Default implementation wraps the scalar value.
     */
    default FitnessBreakdown evaluateDetailed(S solution) {
        return FitnessBreakdown.of(evaluate(solution));
    }
}
