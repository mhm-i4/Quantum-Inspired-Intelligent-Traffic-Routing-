package com.qpso.traffic.ga;

import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.optimization.core.PopulationConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Simple real-coded Genetic Algorithm (GA) baseline with:
 * <ul>
 *   <li>Tournament selection</li>
 *   <li>Whole-arithmetic crossover BLX-α-style blend (per-dimension)</li>
 *   <li>Gaussian mutation</li>
 *   <li>Elitism: best 1 individual survives unchanged.</li>
 * </ul>
 *
 * <p>Used alongside QPSO/PSO as a classical baseline so we can fairly compare the
 * quantum-inspired optimizer against a well-understood evolutionary method.</p>
 */
public class RealCodedGA implements OptimizationAlgorithm<double[]> {

    public record GAParams(
            double crossoverProb,
            double mutationProb,
            double mutationSigma,
            double alphaBlend
    ) {
        public static final GAParams DEFAULT = new GAParams(0.85, 0.10, 0.10, 0.30);
    }

    private final int populationSize;
    private final GAParams params;

    public RealCodedGA() { this(60, GAParams.DEFAULT); }

    public RealCodedGA(int populationSize, GAParams params) {
        this.populationSize = populationSize;
        this.params = params;
    }

    @Override
    public OptimizationResult<double[]> optimize(
            FitnessFunction<double[]> fitness,
            int dimensions,
            double lowerBound,
            double upperBound,
            int maxIterations,
            long seed
    ) {
        PopulationConfig cfg = new PopulationConfig(populationSize, maxIterations, seed);
        Random rng = cfg.createRandom();
        long start = System.currentTimeMillis();
        long evals = 0L;
        double range = upperBound - lowerBound;

        // 1. Initialize population
        List<double[]> pop = new ArrayList<>(populationSize);
        double[] popFitness = new double[populationSize];
        double[] bestPos = null;
        double bestFit = Double.POSITIVE_INFINITY;

        for (int i = 0; i < populationSize; i++) {
            double[] x = new double[dimensions];
            for (int d = 0; d < dimensions; d++) x[d] = lowerBound + rng.nextDouble() * range;
            pop.add(x);
            popFitness[i] = fitness.evaluate(x);
            evals++;
            if (popFitness[i] < bestFit) { bestFit = popFitness[i]; bestPos = x.clone(); }
        }

        OptimizationResult.Builder<double[]> builder = OptimizationResult.<double[]>builder()
                .algorithmName(algorithmName());
        builder.recordConvergence(bestFit);

        // 2. Evolution loop
        for (int t = 0; t < maxIterations; t++) {
            List<double[]> next = new ArrayList<>(populationSize);
            double[] nextFitness = new double[populationSize];

            // Elitism: preserve single best
            int bestIdx = 0;
            for (int i = 1; i < populationSize; i++) if (popFitness[i] < popFitness[bestIdx]) bestIdx = i;
            next.add(pop.get(bestIdx).clone());
            nextFitness[0] = popFitness[bestIdx];

            while (next.size() < populationSize) {
                double[] p1 = pop.get(tournamentSelect(rng, popFitness, 3)).clone();
                double[] p2 = pop.get(tournamentSelect(rng, popFitness, 3)).clone();

                if (rng.nextDouble() < params.crossoverProb()) {
                    double[] c1 = new double[dimensions];
                    double[] c2 = new double[dimensions];
                    for (int d = 0; d < dimensions; d++) {
                        double a = p1[d], b = p2[d];
                        double lo = Math.min(a, b) - params.alphaBlend() * Math.abs(b - a);
                        double hi = Math.max(a, b) + params.alphaBlend() * Math.abs(b - a);
                        c1[d] = clamp(lo + rng.nextDouble() * (hi - lo), lowerBound, upperBound);
                        c2[d] = clamp(lo + rng.nextDouble() * (hi - lo), lowerBound, upperBound);
                    }
                    p1 = c1; p2 = c2;
                }
                mutate(p1, rng, lowerBound, upperBound);
                mutate(p2, rng, lowerBound, upperBound);

                double f1 = fitness.evaluate(p1); evals++;
                int idx = next.size(); next.add(p1); nextFitness[idx] = f1;
                if (f1 < bestFit) { bestFit = f1; bestPos = p1.clone(); }

                if (next.size() < populationSize) {
                    double f2 = fitness.evaluate(p2); evals++;
                    int idx2 = next.size(); next.add(p2); nextFitness[idx2] = f2;
                    if (f2 < bestFit) { bestFit = f2; bestPos = p2.clone(); }
                }
            }
            pop = next;
            popFitness = nextFitness;
            builder.recordConvergence(bestFit);
        }

        long elapsed = System.currentTimeMillis() - start;
        return builder
                .bestSolution(bestPos)
                .bestFitness(bestFit)
                .iterations(maxIterations)
                .evaluations(evals)
                .runtimeMs(elapsed)
                .feasible(true)
                .build();
    }

    private int tournamentSelect(Random rng, double[] fits, int k) {
        int best = rng.nextInt(fits.length);
        for (int i = 1; i < k; i++) {
            int c = rng.nextInt(fits.length);
            if (fits[c] < fits[best]) best = c;
        }
        return best;
    }

    private void mutate(double[] x, Random rng, double lo, double hi) {
        for (int d = 0; d < x.length; d++) {
            if (rng.nextDouble() < params.mutationProb()) {
                double range = hi - lo;
                x[d] = clamp(x[d] + rng.nextGaussian() * params.mutationSigma() * range, lo, hi);
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public String algorithmName() { return "RealCodedGA"; }
}
