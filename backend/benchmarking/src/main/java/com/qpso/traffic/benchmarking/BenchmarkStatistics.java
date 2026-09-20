package com.qpso.traffic.benchmarking;

import com.qpso.traffic.optimization.core.OptimizationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Statistics recorded across repeated trials for the same problem.
 */
public record BenchmarkStatistics(
        String algorithmName,
        int trials,
        double best,
        double worst,
        double mean,
        double stdDev,
        long meanRuntimeMs,
        long meanEvaluations,
        List<Double> perTrialBestFitness,
        List<Double> meanConvergence
) {
    public static BenchmarkStatistics compute(String algorithmName, List<OptimizationResult<double[]>> results) {
        if (results == null || results.isEmpty()) {
            return new BenchmarkStatistics(algorithmName, 0,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    0L, 0L, Collections.emptyList(), Collections.emptyList());
        }

        int trials = results.size();
        List<Double> perTrial = new ArrayList<>(trials);
        double best = Double.POSITIVE_INFINITY;
        double worst = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        long rtSum = 0L;
        long evSum = 0L;

        for (OptimizationResult<double[]> r : results) {
            double f = r.bestFitness();
            perTrial.add(f);
            if (f < best) best = f;
            if (f > worst) worst = f;
            sum += f;
            rtSum += r.runtimeMs();
            evSum += r.evaluations();
        }

        double mean = sum / trials;
        double var = 0.0;
        for (double f : perTrial) {
            double d = f - mean;
            var += d * d;
        }
        var /= trials;
        double std = Math.sqrt(var);

        // mean convergence history (across same-length histories; truncate to minimum)
        int minLen = results.stream().mapToInt(r -> r.convergenceHistory().size()).min().orElse(0);
        List<Double> meanConv = new ArrayList<>(minLen);
        for (int i = 0; i < minLen; i++) {
            double s = 0.0;
            for (OptimizationResult<double[]> r : results) {
                s += r.convergenceHistory().get(i);
            }
            meanConv.add(s / trials);
        }

        return new BenchmarkStatistics(algorithmName, trials, best, worst, mean, std,
                rtSum / trials, evSum / trials,
                Collections.unmodifiableList(perTrial),
                Collections.unmodifiableList(meanConv));
    }
}
