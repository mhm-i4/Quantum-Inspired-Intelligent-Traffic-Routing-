package com.qpso.traffic.api.web;

import com.qpso.traffic.api.dto.DTOs;
import com.qpso.traffic.benchmarking.BenchmarkRunner;
import com.qpso.traffic.benchmarking.BenchmarkStatistics;
import com.qpso.traffic.optimization.benchmarks.BenchmarkFunctions;
import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/benchmark")
@CrossOrigin(origins = "*")
public class BenchmarkController {

    private final Map<String, OptimizationAlgorithm<double[]>> algorithms;
    private final BenchmarkRunner runner = new BenchmarkRunner();

    public BenchmarkController(Map<String, OptimizationAlgorithm<double[]>> algorithms) {
        this.algorithms = algorithms;
    }

    public record BenchmarkRequest(
            String problem,
            Integer dimensions,
            Integer trials,
            Integer iterations,
            List<String> algorithms
    ) {}

    @PostMapping
    public DTOs.BenchmarkResponse runBenchmark(@RequestBody BenchmarkRequest req) {
        String problem = req.problem() == null ? "sphere" : req.problem().toLowerCase();
        int dims = req.dimensions() == null ? 10 : req.dimensions();
        int trials = req.trials() == null ? 10 : req.trials();
        int iters = req.iterations() == null ? 250 : req.iterations();

        FitnessFunction<double[]> fit = switch (problem) {
            case "rastrigin" -> BenchmarkFunctions.rastrigin();
            case "rosenbrock" -> BenchmarkFunctions.rosenbrock();
            default -> BenchmarkFunctions.sphere();
        };
        double lo = problem.equals("rosenbrock") ? -2.048 : -5.12;
        double hi = problem.equals("rosenbrock") ?  2.048 :  5.12;

        List<String> requested = (req.algorithms() == null || req.algorithms().isEmpty())
                ? List.of("QPSO", "PSO", "GA")
                : req.algorithms().stream().map(String::toUpperCase).toList();

        List<DTOs.BenchmarkMetric> metrics = new ArrayList<>();
        for (String name : requested) {
            OptimizationAlgorithm<double[]> alg = algorithms.get(name);
            if (alg == null) continue;
            var cfg = new BenchmarkRunner.BenchmarkConfig(trials, iters, 1L);
            BenchmarkStatistics stats = BenchmarkStatistics.compute(
                    name, runTrials(alg, fit, dims, lo, hi, iters, trials));
            metrics.add(new DTOs.BenchmarkMetric(
                    stats.algorithmName(),
                    stats.best(), stats.worst(), stats.mean(), stats.stdDev(),
                    stats.meanRuntimeMs(), stats.meanEvaluations(),
                    stats.meanConvergence(), trials
            ));
        }
        return new DTOs.BenchmarkResponse(problem, dims, metrics);
    }

    private List<com.qpso.traffic.optimization.core.OptimizationResult<double[]>> runTrials(
            OptimizationAlgorithm<double[]> alg,
            FitnessFunction<double[]> fit, int dims, double lo, double hi, int iters, int trials) {
        List<com.qpso.traffic.optimization.core.OptimizationResult<double[]>> list = new ArrayList<>();
        for (int t = 0; t < trials; t++) {
            list.add(alg.optimize(fit, dims, lo, hi, iters, 1L + t));
        }
        return list;
    }
}
