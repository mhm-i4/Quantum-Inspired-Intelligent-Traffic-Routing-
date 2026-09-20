package com.qpso.traffic.qpso;

import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.optimization.core.PopulationConfig;

import java.util.Random;

/**
 * <h1>Quantum Particle Swarm Optimization (QPSO)</h1>
 *
 * <h2>Scientific Context</h2>
 * QPSO is a <b>quantum-inspired metaheuristic</b> that runs entirely on classical hardware.
 * It borrows the mathematical notion of a particle existing in a superposition of
 * positions (modeled via the <i>delta potential well</i> probability distribution).
 * In particular, each particle does <b>not</b> have a velocity vector. Instead,
 * its next position is sampled stochastically around a <i>local attractor</i> that
 * blends the personal best and the swarm's global best.
 *
 * <p>This implementation follows the formulation by Sun, Feng &amp; Xu (2004) and
 * the subsequent variants that introduce the mean-best position mbest.</p>
 *
 * <h2>Core Equations (per particle i, dimension d)</h2>
 *
 * <h3>1. Mean best position (mbest)</h3>
 * The centroid of all personal bests in the swarm:
 * <pre>
 *   mbest_d = (1 / N) * sum_{i=1..N} pbest_{i,d}
 * </pre>
 *
 * <h3>2. Local attractor (p)</h3>
 * Stochastically blends pbest_{i,d} and gbest_d using a uniform weight u ~ U(0,1):
 * <pre>
 *   p_{i,d} = u * pbest_{i,d} + (1 - u) * gbest_d
 *
 *   (per-particle, per-dimension, re-sampled every iteration)
 * </pre>
 *
 * <h3>3. Quantum-inspired position update</h3>
 * Let L_{i,d} = alpha * | mbest_d - x_{i,d}(t) |
 * where alpha is the <b>contraction-expansion (CE) coefficient</b>.
 *
 * Sample a random number r ~ U(0,1) and use the inverse-CDF of the
 * double-exponential / delta-well distribution:
 * <pre>
 *   if u2 &lt; 0.5:
 *       x_{i,d}(t+1) = p_{i,d} + alpha * L_{i,d} * ln(1 / r)
 *   else:
 *       x_{i,d}(t+1) = p_{i,d} - alpha * L_{i,d} * ln(1 / r)
 * </pre>
 *
 * (Some authors write this more compactly using a sign factor s = +/-1
 * drawn uniformly, and log(1/u):
 *   x = p + s * alpha * L * log(1/u).)
 *
 * <h3>4. Contraction-Expansion schedule</h3>
 * Linearly decreasing CE coefficient is a common default:
 * <pre>
 *   alpha(t) = alpha_start - (alpha_start - alpha_end) * (t / maxIter)
 * </pre>
 * Default values: alpha_start = 1.0, alpha_end = 0.5.
 *
 * <h2>Difference from standard PSO</h2>
 * <table>
 *   <tr><td>Standard PSO</td><td>QPSO</td></tr>
 *   <tr><td>Has velocity v(t)</td><td><b>No velocity</b> vector</td></tr>
 *   <tr><td>x(t+1) = x(t) + v(t+1)</td><td>x(t+1) sampled around local attractor</td></tr>
 *   <tr><td>Weights w, c1, c2</td><td>Single CE coefficient alpha + mbest</td></tr>
 *   <tr><td>Deterministic update (once r1, r2 sampled)</td><td>Stochastic via ln(1/r)</td></tr>
 * </table>
 */
public class QPSOOptimizer implements OptimizationAlgorithm<double[]> {

    /**
     * Configuration for the QPSO search.
     *
     * @param alphaStart initial CE coefficient
     * @param alphaEnd   final CE coefficient (linearly annealed)
     */
    public record QPSOParams(
            double alphaStart,
            double alphaEnd
    ) {
        public static final QPSOParams DEFAULT = new QPSOParams(1.0, 0.5);
    }

    private final int swarmSize;
    private final QPSOParams params;

    public QPSOOptimizer() {
        this(40, QPSOParams.DEFAULT);
    }

    public QPSOOptimizer(int swarmSize, QPSOParams params) {
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

        // ----- swarm state -----
        double[][] positions = new double[swarmSize][dimensions];
        double[][] pBestPos  = new double[swarmSize][dimensions];
        double[]   pBestFit  = new double[swarmSize];
        double[]   curFit    = new double[swarmSize];

        double[]   gBestPos  = new double[dimensions];
        double     gBestFit  = Double.POSITIVE_INFINITY;

        // ----- initialization -----
        for (int i = 0; i < swarmSize; i++) {
            for (int d = 0; d < dimensions; d++) {
                positions[i][d] = lowerBound + rng.nextDouble() * range;
                pBestPos[i][d]  = positions[i][d];
            }
            curFit[i] = fitness.evaluate(positions[i]);
            pBestFit[i] = curFit[i];
            evals++;
            if (curFit[i] < gBestFit) {
                gBestFit = curFit[i];
                System.arraycopy(positions[i], 0, gBestPos, 0, dimensions);
            }
        }

        OptimizationResult.Builder<double[]> builder = OptimizationResult.<double[]>builder()
                .algorithmName(algorithmName());
        builder.recordConvergence(gBestFit);

        double alphaStep = (params.alphaStart() - params.alphaEnd()) / Math.max(1, maxIterations);

        double[] mbest = new double[dimensions];
        double[] attractor = new double[dimensions];

        for (int t = 0; t < maxIterations; t++) {
            // --- 1. Compute mbest (mean of personal best positions) ---
            for (int d = 0; d < dimensions; d++) {
                double sum = 0.0;
                for (int i = 0; i < swarmSize; i++) sum += pBestPos[i][d];
                mbest[d] = sum / swarmSize;
            }

            double alpha = params.alphaStart() - alphaStep * t;
            double iterBest = gBestFit;

            // --- 2. For each particle, compute attractor & sample new position ---
            for (int i = 0; i < swarmSize; i++) {
                for (int d = 0; d < dimensions; d++) {
                    // local attractor: stochastic blend of pbest and gbest
                    double u = rng.nextDouble();
                    attractor[d] = u * pBestPos[i][d] + (1.0 - u) * gBestPos[d];

                    // Quantum-inspired update:
                    // L = alpha * |mbest - x|
                    // sign s ~ +1 with probability 0.5, else -1
                    // r ~ U(0,1): we re-use a second random draw
                    double L = alpha * Math.abs(mbest[d] - positions[i][d]);
                    double sign = (rng.nextDouble() < 0.5) ? +1.0 : -1.0;
                    double r = rng.nextDouble();
                    // Guard against r == 0 which would blow up log(1/r)
                    if (r < 1e-300) r = 1e-300;

                    positions[i][d] = attractor[d] + sign * L * Math.log(1.0 / r);

                    // Reflect / clamp to bounds (simple saturation)
                    if (positions[i][d] > upperBound) positions[i][d] = upperBound;
                    if (positions[i][d] < lowerBound) positions[i][d] = lowerBound;
                }

                // --- 3. Evaluate, update personal/global bests ---
                curFit[i] = fitness.evaluate(positions[i]);
                evals++;

                if (curFit[i] < pBestFit[i]) {
                    pBestFit[i] = curFit[i];
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
        return "QPSO";
    }
}
