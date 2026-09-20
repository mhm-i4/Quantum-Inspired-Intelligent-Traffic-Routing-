package com.qpso.traffic.optimization.core;

/**
 * Optional solution decoder: maps a continuous particle position
 * into a discrete, problem-specific solution D.
 *
 * @param <D> the decoded solution type
 */
@FunctionalInterface
public interface SolutionDecoder<D> {
    D decode(double[] position);
}
