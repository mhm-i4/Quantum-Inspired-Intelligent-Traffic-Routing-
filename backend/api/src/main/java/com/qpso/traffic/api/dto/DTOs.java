package com.qpso.traffic.api.dto;

import com.qpso.traffic.graph.model.Node;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.optimization.core.FitnessBreakdown;
import com.qpso.traffic.routing.model.Route;
import com.qpso.traffic.traffic.model.TrafficState;

import java.util.Collection;
import java.util.List;

public final class DTOs {
    private DTOs() {}

    public record NodeDTO(String id, double latitude, double longitude, java.util.Map<String,String> metadata) {
        public static NodeDTO from(Node n) { return new NodeDTO(n.id(), n.latitude(), n.longitude(), n.metadata()); }
    }

    public record EdgeDTO(String id, String source, String destination,
                          double distanceKm, double freeFlowTimeMin, double speedKmh) {
        public static EdgeDTO from(RoadEdge e) {
            return new EdgeDTO(e.id(), e.sourceNodeId(), e.destinationNodeId(),
                    e.distanceKm(), e.freeFlowTravelTimeMin(), e.freeFlowSpeedKmh());
        }
    }

    public record NetworkDTO(List<NodeDTO> nodes, List<EdgeDTO> edges) {
        public static NetworkDTO of(Collection<Node> ns, Collection<RoadEdge> es) {
            return new NetworkDTO(ns.stream().map(NodeDTO::from).toList(),
                    es.stream().map(EdgeDTO::from).toList());
        }
    }

    public record TrafficStateDTO(String edgeId, double congestionLevel, double currentTravelTimeMin, String timestamp) {
        public static TrafficStateDTO from(TrafficState s) {
            return new TrafficStateDTO(s.edgeId(), s.congestionLevel(),
                    s.currentTravelTimeMin(), s.timestamp().toString());
        }
    }

    public record RouteDTO(
            List<String> nodeIds,
            List<String> edgeIds,
            double totalDistanceKm,
            double totalTravelTimeMin,
            double totalCongestionCost,
            double penalty,
            double totalFitness,
            boolean feasible,
            List<String> violatedConstraints
    ) {
        public static RouteDTO from(Route r) {
            return new RouteDTO(r.nodeIds(), r.edgeIds(), r.totalDistanceKm(),
                    r.totalTravelTimeMin(), r.totalCongestionCost(),
                    r.penalty(), r.totalFitness(), r.feasible(), r.violatedConstraints());
        }

        public static RouteDTO from(Route r, double fitness) {
            return new RouteDTO(r.nodeIds(), r.edgeIds(), r.totalDistanceKm(),
                r.totalTravelTimeMin(), r.totalCongestionCost(),
                r.penalty(), fitness, r.feasible(), r.violatedConstraints());
        }
    }

    public record FitnessBreakdownDTO(
            double travelTimeCost, double distanceCost, double congestionCost,
            double constraintPenalty, double totalFitness, List<String> violatedConstraints) {
        public static FitnessBreakdownDTO from(FitnessBreakdown b) {
            return new FitnessBreakdownDTO(b.travelTimeCost(), b.distanceCost(),
                    b.congestionCost(), b.constraintPenalty(), b.totalFitness(),
                    b.violatedConstraints());
        }
    }

    public record OptimizationResponse(
            String optimizationId,
            String algorithm,
            RouteDTO bestRoute,
            FitnessBreakdownDTO breakdown,
            double bestFitness,
            List<Double> convergenceHistory,
            long runtimeMs,
            int iterations,
            long evaluations,
            RouteDTO dijkstraReference
    ) {}

    public record BenchmarkMetric(
            String algorithm,
            double best, double worst, double mean, double stdDev,
            long meanRuntimeMs, long meanEvaluations,
            List<Double> meanConvergence,
            int trials
    ) {}

    public record BenchmarkResponse(String problem, int dimensions, List<BenchmarkMetric> results) {}

    public record RerouteResponse(
            RouteDTO oldRoute,
            RouteDTO newRoute,
            FitnessBreakdownDTO newBreakdown,
            List<String> changedEdges,
            double oldFitness,
            double newFitness
    ) {}
}
