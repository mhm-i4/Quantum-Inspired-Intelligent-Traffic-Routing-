package com.qpso.traffic.benchmarking;

import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-trial benchmark harness.
 *
 * Runs the same optimizer on the same problem {@code trials} times with
 * incremented seeds, so we can report the statistical distribution of
 * results (best / worst / mean / stdDev) rather than one lucky run.
 */
public class BenchmarkRunner {

    public record BenchmarkConfig(
            int trials,
            int maxIterations,
            long baseSeed
    ) {
        public static final BenchmarkConfig DEFAULT = new BenchmarkConfig(10, 300, 1L);
    }

    public record SingleAlgorithmBenchmark(
            String algorithmName,
            int dimensions,
            BenchmarkStatistics statistics
    ) {}

    public <F extends FitnessFunction<double[]>> SingleAlgorithmBenchmark run(
            OptimizationAlgorithm<double[]> alg,
            F fitness,
            int dims,
            double lowerBound,
            double upperBound,
            BenchmarkConfig cfg
    ) {
        List<OptimizationResult<double[]>> results = new ArrayList<>(cfg.trials());
        for (int t = 0; t < cfg.trials(); t++) {
            long seed = cfg.baseSeed() + t;
            results.add(alg.optimize(fitness, dims, lowerBound, upperBound, cfg.maxIterations(), seed));
        }
        return new SingleAlgorithmBenchmark(alg.algorithmName(), dims,
                BenchmarkStatistics.compute(alg.algorithmName(), results));
    }

    public record MultiAlgorithmBenchmark(
            String problem,
            int dimensions,
            List<SingleAlgorithmBenchmark> perAlgorithm
    ) {}

    public <F extends FitnessFunction<double[]>> MultiAlgorithmBenchmark compare(
            String problemName,
            List<OptimizationAlgorithm<double[]>> algorithms,
            F fitness,
            int dims,
            double lo, double hi,
            BenchmarkConfig cfg
    ) {
        List<SingleAlgorithmBenchmark> list = new ArrayList<>();
        for (OptimizationAlgorithm<double[]> alg : algorithms) {
            list.add(run(alg, fitness, dims, lo, hi, cfg));
        }
        return new MultiAlgorithmBenchmark(problemName, dims, list);
    }

    /** For routing benchmarks we typically vary the number of customers visited
     *  to check how the algorithm scales.  This convenience method runs one
     *  algorithm at multiple size configurations and returns per-size stats. */
    public record ScalabilitySeries(
            String algorithmName,
            List<Integer> sizes,
            List<BenchmarkStatistics> perSize
    ) {}
}
