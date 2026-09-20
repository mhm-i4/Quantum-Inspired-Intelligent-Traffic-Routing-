package com.qpso.traffic.optimization.benchmarks;

import com.qpso.traffic.optimization.core.FitnessFunction;

/**
 * Standard mathematical benchmark functions for validating
 * continuous optimization algorithms.
 *
 * Global optimum for all functions:
 *   f(x*) = 0  at  x* = (0, 0, ..., 0)
 */
public final class BenchmarkFunctions {

    private BenchmarkFunctions() {}

    /**
     * Sphere function (simple, convex, unimodal).
     *   f(x) = sum_i  x_i^2
     *
     * Typical search domain: [-5.12, 5.12]^D
     */
    public static FitnessFunction<double[]> sphere() {
        return x -> {
            double sum = 0.0;
            for (double xi : x) sum += xi * xi;
            return sum;
        };
    }

    /**
     * Rastrigin function (highly multimodal, many local optima).
     *   f(x) = 10*D + sum_i [x_i^2 - 10*cos(2*pi*x_i)]
     *
     * Typical search domain: [-5.12, 5.12]^D
     */
    public static FitnessFunction<double[]> rastrigin() {
        return x -> {
            final int dim = x.length;
            double sum = 10.0 * dim;
            for (double xi : x) {
                sum += xi * xi - 10.0 * Math.cos(2.0 * Math.PI * xi);
            }
            return sum;
        };
    }

    /**
     * Rosenbrock function (banana-shaped, narrow, ill-conditioned valley).
     *   f(x) = sum_{i=1..D-1} [100*(x_{i+1} - x_i^2)^2 + (1 - x_i)^2]
     *
     * Typical search domain: [-2.048, 2.048]^D
     */
    public static FitnessFunction<double[]> rosenbrock() {
        return x -> {
            double sum = 0.0;
            for (int i = 0; i < x.length - 1; i++) {
                double a = x[i];
                double b = x[i + 1];
                double t1 = b - a * a;
                double t2 = 1.0 - a;
                sum += 100.0 * t1 * t1 + t2 * t2;
            }
            return sum;
        };
    }
}
