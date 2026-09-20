package com.qpso.traffic.pso;

import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.optimization.core.PopulationConfig;

import java.util.Random;

/**
 * Standard (canonical) Particle Swarm Optimization.
 *
 * <h2>Equations (per dimension d, particle i):</h2>
 * <pre>
 *   v_{i,d}(t+1) = w * v_{i,d}(t)
 *                + c1 * r1 * (pbest_{i,d} - x_{i,d}(t))
 *                + c2 * r2 * (gbest_d     - x_{i,d}(t))
 *
 *   x_{i,d}(t+1) = x_{i,d}(t) + v_{i,d}(t+1)
 * </pre>
 *
 * where:
 * <ul>
 *   <li>w  = inertia weight (linearly decreased)</li>
 *   <li>c1 = cognitive coefficient (self-confidence)</li>
 *   <li>c2 = social coefficient (swarm-confidence)</li>
 *   <li>r1, r2 ~ U(0,1)</li>
 * </ul>
 */
public class StandardPSO implements OptimizationAlgorithm<double[]> {

    public record PSOParams(
            double inertiaStart,
            double inertiaEnd,
            double cognitiveCoeff,
            double socialCoeff,
            double velocityMaxRatio
    ) {
        public static final PSOParams DEFAULT = new PSOParams(0.9, 0.4, 2.0, 2.0, 0.2);
    }

    private final int swarmSize;
    private final PSOParams params;

    public StandardPSO() {
        this(40, PSOParams.DEFAULT);
    }

    public StandardPSO(int swarmSize, PSOParams params) {
        this.swarmSize = swarmSize;
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
        PopulationConfig config = new PopulationConfig(swarmSize, maxIterations, seed);
        Random rng = config.createRandom();
        long start = System.currentTimeMillis();
        long evals = 0L;

        double range = upperBound - lowerBound;
        double vMax = params.velocityMaxRatio() * range;

        double[][] positions = new double[swarmSize][dimensions];
        double[][] velocities = new double[swarmSize][dimensions];
        double[][] pBestPos = new double[swarmSize][dimensions];
        double[]   pBestFit = new double[swarmSize];
        double[]   fitnesses = new double[swarmSize];

        double[] gBestPos = new double[dimensions];
        double   gBestFit = Double.POSITIVE_INFINITY;

        for (int i = 0; i < swarmSize; i++) {
            for (int d = 0; d < dimensions; d++) {
                positions[i][d] = lowerBound + rng.nextDouble() * range;
                velocities[i][d] = (-vMax) + rng.nextDouble() * 2.0 * vMax;
                pBestPos[i][d] = positions[i][d];
            }
            fitnesses[i] = fitness.evaluate(positions[i]);
            pBestFit[i] = fitnesses[i];
            evals++;
            if (fitnesses[i] < gBestFit) {
                gBestFit = fitnesses[i];
                System.arraycopy(positions[i], 0, gBestPos, 0, dimensions);
            }
        }

        OptimizationResult.Builder<double[]> builder = OptimizationResult.<double[]>builder()
                .algorithmName(algorithmName());
        builder.recordConvergence(gBestFit);

        double wStep = (params.inertiaStart() - params.inertiaEnd()) / Math.max(1, maxIterations);

        for (int t = 0; t < maxIterations; t++) {
            double w = params.inertiaStart() - wStep * t;

            double iterBest = gBestFit;

            for (int i = 0; i < swarmSize; i++) {
                for (int d = 0; d < dimensions; d++) {
                    double r1 = rng.nextDouble();
                    double r2 = rng.nextDouble();
                    double cog = params.cognitiveCoeff() * r1 * (pBestPos[i][d] - positions[i][d]);
                    double soc = params.socialCoeff() * r2 * (gBestPos[d] - positions[i][d]);
                    velocities[i][d] = w * velocities[i][d] + cog + soc;
                    if (velocities[i][d] > vMax) velocities[i][d] = vMax;
                    if (velocities[i][d] < -vMax) velocities[i][d] = -vMax;

                    positions[i][d] += velocities[i][d];
                    if (positions[i][d] > upperBound) positions[i][d] = upperBound;
                    if (positions[i][d] < lowerBound) positions[i][d] = lowerBound;
                }

                fitnesses[i] = fitness.evaluate(positions[i]);
                evals++;

                if (fitnesses[i] < pBestFit[i]) {
                    pBestFit[i] = fitnesses[i];
                    System.arraycopy(positions[i], 0, pBestPos[i], 0, dimensions);
                    if (pBestFit[i] < gBestFit) {
                        gBestFit = pBestFit[i];
                        System.arraycopy(pBestPos[i], 0, gBestPos, 0, dimensions);
                    }
                }
            }

            if (gBestFit < iterBest) iterBest = gBestFit;
            builder.recordConvergence(iterBest);
        }

        long elapsed = System.currentTimeMillis() - start;
        return builder
                .bestSolution(gBestPos)
                .bestFitness(gBestFit)
                .iterations(maxIterations)
                .evaluations(evals)
                .runtimeMs(elapsed)
                .feasible(true)
                .build();
    }

    @Override
    public String algorithmName() {
        return "StandardPSO";
    }
}
