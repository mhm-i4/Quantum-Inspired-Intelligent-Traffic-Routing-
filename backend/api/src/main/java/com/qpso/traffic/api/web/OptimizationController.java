package com.qpso.traffic.api.web;

import com.qpso.traffic.api.dto.DTOs;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.qpso.QPSOOptimizer;
import com.qpso.traffic.routing.algorithms.DijkstraSolver;
import com.qpso.traffic.routing.encoding.ContinuousRouteDecoder;
import com.qpso.traffic.routing.encoding.RouteFitnessFunction;
import com.qpso.traffic.routing.model.ObjectiveWeights;
import com.qpso.traffic.routing.model.Route;
import com.qpso.traffic.routing.optim.RoutingOptimizer;
import com.qpso.traffic.traffic.model.NetworkTrafficState;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OptimizationController {

    private final TransportGraph graph;
    private final NetworkTrafficState traffic;
    private final Map<String, OptimizationAlgorithm<double[]>> algorithms;

    private final Map<String, StoredResult> storage = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1L);

    public OptimizationController(TransportGraph graph,
                                  NetworkTrafficState traffic,
                                  Map<String, OptimizationAlgorithm<double[]>> algorithms) {
        this.graph = graph;
        this.traffic = traffic;
        this.algorithms = algorithms;
    }

    record StoredResult(RoutingOptimizer.RoutingResult r, double[] position, ObjectiveWeights weights) {}

    public record OptimizeRequest(
            String source,
            String destination,
            String algorithm,
            Integer iterations,
            Long seed,
            Integer maxSteps,
            Double alpha, Double beta, Double gamma
    ) {}

    @PostMapping("/route/optimize")
    public DTOs.OptimizationResponse optimize(@RequestBody OptimizeRequest req) {
        String algorithm = req.algorithm() == null ? "QPSO" : req.algorithm().toUpperCase();
        OptimizationAlgorithm<double[]> alg = algorithms.getOrDefault(algorithm, new QPSOOptimizer());
        ObjectiveWeights w = new ObjectiveWeights(
                req.alpha() != null ? req.alpha() : 1.0,
                req.beta()  != null ? req.beta()  : 0.5,
                req.gamma() != null ? req.gamma() : 2.0
        );
        int maxSteps = req.maxSteps() != null ? req.maxSteps() : Math.max(8, graph.nodeCount() * 2);
        int iterations = req.iterations() != null ? req.iterations() : 250;
        long seed = req.seed() != null ? req.seed() : 7L;

        var router = new RoutingOptimizer(graph, traffic, req.source(), req.destination(), w, maxSteps);
        var res = router.optimize(alg, seed, iterations);

        String id = "OPT-" + idSeq.getAndIncrement();
        storage.put(id, new StoredResult(res, null, w));

        return new DTOs.OptimizationResponse(id, res.algorithmName(),
                DTOs.RouteDTO.from(res.bestRoute()),
                DTOs.FitnessBreakdownDTO.from(res.breakdown()),
                res.bestFitness(),
                res.convergenceHistory(),
                res.runtimeMs(), res.iterations(), res.evaluations(),
                DTOs.RouteDTO.from(res.dijkstraReference(), router.dijkstraFitness(res.dijkstraReference())));
    }

    @PostMapping("/route/dijkstra")
    public DTOs.RouteDTO dijkstra(@RequestBody OptimizeRequest req) {
        ObjectiveWeights w = new ObjectiveWeights(
                req.alpha() != null ? req.alpha() : 1.0,
                req.beta()  != null ? req.beta()  : 0.5,
                req.gamma() != null ? req.gamma() : 2.0
        );
        var router = new RoutingOptimizer(graph, traffic, req.source(), req.destination(), w,
                Math.max(8, graph.nodeCount() * 2));
        Route r = DijkstraSolver.shortestPath(graph, req.source(), req.destination(), EdgeCostFunction.distance())
                .orElseGet(() -> Route.invalid(req.source(), req.destination(), "dijkstra_unreachable"));
        return DTOs.RouteDTO.from(r, router.dijkstraFitness(r));
    }

    @GetMapping("/optimization/{id}")
    public DTOs.OptimizationResponse get(@PathVariable String id) {
        StoredResult s = storage.get(id);
        if (s == null) throw new NoSuchElementException("No optimization with id: " + id);
        return new DTOs.OptimizationResponse(id, s.r().algorithmName(),
                DTOs.RouteDTO.from(s.r().bestRoute()),
                DTOs.FitnessBreakdownDTO.from(s.r().breakdown()),
                s.r().bestFitness(), s.r().convergenceHistory(),
                s.r().runtimeMs(), s.r().iterations(), s.r().evaluations(),
                DTOs.RouteDTO.from(s.r().dijkstraReference(),
                        s.r().dijkstraReference().totalDistanceKm()));
    }

    @GetMapping("/optimization/{id}/convergence")
    public List<Double> convergence(@PathVariable String id) {
        StoredResult s = storage.get(id);
        if (s == null) throw new NoSuchElementException("No optimization with id: " + id);
        return s.r().convergenceHistory();
    }

    public record RerouteRequest(String source, String destination, String algorithm,
                                 Double alpha, Double beta, Double gamma, Integer maxSteps,
                                 Integer iterations, Long seed) {}

    @PostMapping("/route/reroute")
    public DTOs.RerouteResponse reroute(@RequestBody RerouteRequest req) {
        // Build the previous route with NORMAL traffic.
        ObjectiveWeights w = new ObjectiveWeights(
                req.alpha() != null ? req.alpha() : 1.0,
                req.beta()  != null ? req.beta()  : 0.5,
                req.gamma() != null ? req.gamma() : 2.0
        );
        int maxSteps = req.maxSteps() != null ? req.maxSteps() : Math.max(8, graph.nodeCount() * 2);
        int iterations = req.iterations() != null ? req.iterations() : 200;
        long seed = req.seed() != null ? req.seed() : 11L;

        var decoder = new ContinuousRouteDecoder(graph, req.source(), req.destination(), maxSteps,
                new com.qpso.traffic.traffic.model.TrafficAwareEdgeCost(traffic, w.alphaTravelTime()));
        var fitFn = new RouteFitnessFunction(decoder, w, route -> {
            double s = 0.0; for (String eId : route.edgeIds()) s += traffic.congestionOf(eId); return s;
        });

        OptimizationAlgorithm<double[]> alg = algorithms.getOrDefault(
                (req.algorithm() == null ? "QPSO" : req.algorithm()).toUpperCase(), new QPSOOptimizer());

        // Route after (already using latest traffic state)
        OptimizationResult<double[]> raw2 = alg.optimize(fitFn, decoder.dimensions(), 0.0, 1.0, iterations, seed);
        Route newR = decoder.decode(raw2.bestSolution());

        // Route before: temporarily set all edges to 0 congestion; find best route on old state
        Map<String, Double> savedCongestion = new HashMap<>();
        for (var en : traffic.allStates().entrySet()) {
            savedCongestion.put(en.getKey(), en.getValue().congestionLevel());
        }
        for (var en : savedCongestion.entrySet()) {
            var edge = graph.edge(en.getKey()).orElse(null);
            if (edge != null) traffic.setState(en.getKey(), 0.0, edge.freeFlowTravelTimeMin());
        }
        OptimizationResult<double[]> raw1 = alg.optimize(fitFn, decoder.dimensions(), 0.0, 1.0, iterations, seed);
        Route oldR = decoder.decode(raw1.bestSolution());
        // Restore
        for (var en : savedCongestion.entrySet()) {
            var edge = graph.edge(en.getKey()).orElse(null);
            if (edge != null) traffic.setState(en.getKey(), en.getValue(), edge.freeFlowTravelTimeMin());
        }

        Set<String> oldEdges = new HashSet<>(oldR.edgeIds());
        Set<String> newEdges = new HashSet<>(newR.edgeIds());
        List<String> changed = new ArrayList<>();
        for (String e : oldEdges) if (!newEdges.contains(e)) changed.add("-" + e);
        for (String e : newEdges) if (!oldEdges.contains(e)) changed.add("+" + e);

        return new DTOs.RerouteResponse(
                DTOs.RouteDTO.from(oldR),
                DTOs.RouteDTO.from(newR),
                DTOs.FitnessBreakdownDTO.from(fitFn.evaluateDetailed(raw2.bestSolution())),
                changed,
                fitFn.evaluate(raw1.bestSolution()),
                fitFn.evaluate(raw2.bestSolution())
        );
    }
}
