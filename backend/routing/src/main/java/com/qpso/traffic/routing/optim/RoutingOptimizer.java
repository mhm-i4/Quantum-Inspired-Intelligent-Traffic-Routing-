package com.qpso.traffic.routing.optim;

import com.qpso.traffic.graph.model.EdgeCostFunction;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.optimization.core.FitnessBreakdown;
import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.optimization.core.OptimizationResult;
import com.qpso.traffic.routing.algorithms.DijkstraSolver;
import com.qpso.traffic.routing.encoding.ContinuousRouteDecoder;
import com.qpso.traffic.routing.encoding.RouteFitnessFunction;
import com.qpso.traffic.routing.model.ObjectiveWeights;
import com.qpso.traffic.routing.model.Route;
import com.qpso.traffic.traffic.model.NetworkTrafficState;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level facade that runs a chosen continuous optimizer against a
 * routing problem: (graph, traffic, source, destination, weights).
 *
 * <p>The optimizer (QPSO, PSO, GA, …) searches over [0,1]^D and the
 * {@link ContinuousRouteDecoder} maps each position into a route over the
 * graph, then the {@link RouteFitnessFunction} scores it using the objective
 * weights plus any penalties.</p>
 *
 * <p>The returned OptimizationResult carries {@code double[]} as the raw
 * decision vector AND the detailed {@link FitnessBreakdown} includes all
 * cost components, so the frontend can display them separately.</p>
 */
public class RoutingOptimizer {

    public record RoutingResult(
            Route bestRoute,
            FitnessBreakdown breakdown,
            double bestFitness,
            List<Double> convergenceHistory,
            long runtimeMs,
            int iterations,
            long evaluations,
            String algorithmName,
            Route dijkstraReference
    ) {}

    private final TransportGraph graph;
    private final NetworkTrafficState traffic;
    private final String source;
    private final String destination;
    private final ObjectiveWeights weights;
    private final int maxSteps;

    public RoutingOptimizer(TransportGraph graph,
                            NetworkTrafficState traffic,
                            String source,
                            String destination,
                            ObjectiveWeights weights,
                            int maxSteps) {
        this.graph = graph;
        this.traffic = traffic;
        this.source = source;
        this.destination = destination;
        this.weights = weights;
        this.maxSteps = maxSteps;
    }

    /** Edge-cost function that combines the current travel time + congestion terms */
    public EdgeCostFunction effectiveCost() {
        return (RoadEdge e) -> {
            double tt = traffic.currentTravelTime(e.id(), e.freeFlowTravelTimeMin());
            double cong = traffic.congestionOf(e.id());
            return weights.alphaTravelTime() * tt
                    + weights.betaDistance() * e.distanceKm()
                    + weights.gammaCongestion() * cong;
        };
    }

    /** Returns the weighted objective used by the routing optimizer for a decoded route. */
    public double weightedFitness(Route route) {
        double congestion = route.edgeIds().stream()
                .mapToDouble(traffic::congestionOf)
                .sum();
        return weights.alphaTravelTime() * route.totalTravelTimeMin()
                + weights.betaDistance() * route.totalDistanceKm()
                + weights.gammaCongestion() * congestion
                + route.penalty();
    }

    /** Distance-only fitness for the Dijkstra reference route. */
    public double dijkstraFitness(Route route) {
        return route.totalDistanceKm();
    }

    public RoutingResult optimize(OptimizationAlgorithm<double[]> algorithm, long seed, int iterations) {
        var decoder = new ContinuousRouteDecoder(graph, source, destination, maxSteps, effectiveCost());
        java.util.function.ToDoubleFunction<Route> congestionCost = route -> {
            double sum = 0.0;
            for (String eId : route.edgeIds()) sum += traffic.congestionOf(eId);
            return sum;
        };
        var fitFn = new RouteFitnessFunction(decoder, weights, congestionCost);

        OptimizationResult<double[]> raw = algorithm.optimize(
                adaptScalarFn(fitFn), decoder.dimensions(), 0.0, 1.0, iterations, seed);

        Route bestRoute = decoder.decode(raw.bestSolution());
        FitnessBreakdown breakdown = fitFn.evaluateDetailed(raw.bestSolution());

        Route dijkstra = DijkstraSolver.shortestPath(graph, source, destination, EdgeCostFunction.distance())
                .orElseGet(() -> Route.invalid(source, destination, "dijkstra_unreachable"));

        return new RoutingResult(
                bestRoute, breakdown, breakdown.totalFitness(),
                raw.convergenceHistory(), raw.runtimeMs(), raw.iterations(), raw.evaluations(),
                algorithm.algorithmName(), dijkstra
        );
    }

    /** Adapter: the generic optimize() method only needs scalar fitness; we
     *  pass the scalar form here but keep {@link #evaluateDetailed} accessible
     *  through the original RouteFitnessFunction object for post-processing. */
    private static FitnessFunction<double[]> adaptScalarFn(RouteFitnessFunction fn) {
        return new FitnessFunction<double[]>() {
            @Override
            public double evaluate(double[] solution) { return fn.evaluate(solution); }
            @Override
            public FitnessBreakdown evaluateDetailed(double[] solution) { return fn.evaluateDetailed(solution); }
        };
    }

    /** Convenience: decode an explicit continuous position (used for re-rerun UI). */
    public Route decodePosition(double[] position) {
        var decoder = new ContinuousRouteDecoder(graph, source, destination, maxSteps, effectiveCost());
        return decoder.decode(position);
    }
}
