package com.qpso.traffic.optimization.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of an optimization run.
 *
 * @param <S> the type of the best solution found
 */
public record OptimizationResult<S>(
        S bestSolution,
        double bestFitness,
        FitnessBreakdown bestFitnessBreakdown,
        List<Double> convergenceHistory,
        long runtimeMs,
        int iterations,
        long evaluations,
        boolean feasible,
        List<String> violatedConstraints,
        String algorithmName
) {
    public static <S> Builder<S> builder() {
        return new Builder<>();
    }

    public static class Builder<S> {
        private S bestSolution;
        private double bestFitness = Double.POSITIVE_INFINITY;
        private FitnessBreakdown bestFitnessBreakdown;
        private final List<Double> convergenceHistory = new ArrayList<>();
        private long runtimeMs;
        private int iterations;
        private long evaluations;
        private boolean feasible = true;
        private final List<String> violatedConstraints = new ArrayList<>();
        private String algorithmName;

        public Builder<S> bestSolution(S s) { this.bestSolution = s; return this; }
        public Builder<S> bestFitness(double f) { this.bestFitness = f; return this; }
        public Builder<S> bestFitnessBreakdown(FitnessBreakdown b) { this.bestFitnessBreakdown = b; return this; }
        public Builder<S> recordConvergence(double bestIterFitness) {
            this.convergenceHistory.add(bestIterFitness);
            return this;
        }
        public Builder<S> runtimeMs(long ms) { this.runtimeMs = ms; return this; }
        public Builder<S> iterations(int it) { this.iterations = it; return this; }
        public Builder<S> evaluations(long ev) { this.evaluations = ev; return this; }
        public Builder<S> feasible(boolean f) { this.feasible = f; return this; }
        public Builder<S> violatedConstraints(List<String> v) {
            this.violatedConstraints.clear();
            if (v != null) this.violatedConstraints.addAll(v);
            return this;
        }
        public Builder<S> algorithmName(String name) { this.algorithmName = name; return this; }

        public OptimizationResult<S> build() {
            return new OptimizationResult<>(
                    bestSolution, bestFitness,
                    bestFitnessBreakdown == null ? FitnessBreakdown.of(bestFitness) : bestFitnessBreakdown,
                    Collections.unmodifiableList(new ArrayList<>(convergenceHistory)),
                    runtimeMs, iterations, evaluations, feasible,
                    Collections.unmodifiableList(new ArrayList<>(violatedConstraints)),
                    algorithmName
            );
        }
    }
}
