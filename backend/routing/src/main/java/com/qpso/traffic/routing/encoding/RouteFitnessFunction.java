package com.qpso.traffic.routing.encoding;

import com.qpso.traffic.optimization.core.FitnessBreakdown;
import com.qpso.traffic.optimization.core.FitnessFunction;
import com.qpso.traffic.routing.model.ObjectiveWeights;
import com.qpso.traffic.routing.model.Route;

/**
 * Wraps a continuous → Route decoder and an objective weight vector
 * into a {@link FitnessFunction double[]} that PSO/QPSO can minimize.
 *
 * <p>Exposes per-component breakdown so the frontend can show
 * travel-time cost, distance cost, congestion cost, and penalties
 * separately.</p>
 */
public class RouteFitnessFunction implements FitnessFunction<double[]> {

    private final ContinuousRouteDecoder decoder;
    private final ObjectiveWeights weights;
    private final java.util.function.ToDoubleFunction<Route> congestionExtractor;

    public RouteFitnessFunction(ContinuousRouteDecoder decoder, ObjectiveWeights weights) {
        this(decoder, weights, Route::totalCongestionCost);
    }

    public RouteFitnessFunction(ContinuousRouteDecoder decoder,
                                ObjectiveWeights weights,
                                java.util.function.ToDoubleFunction<Route> congestionExtractor) {
        this.decoder = decoder;
        this.weights = weights;
        this.congestionExtractor = congestionExtractor;
    }

    public ContinuousRouteDecoder decoder() { return decoder; }

    @Override
    public double evaluate(double[] position) {
        Route r = decoder.decode(position);
        return weighted(r);
    }

    @Override
    public FitnessBreakdown evaluateDetailed(double[] position) {
        Route r = decoder.decode(position);
        double t = r.totalTravelTimeMin();
        double d = r.totalDistanceKm();
        double c = congestionExtractor.applyAsDouble(r);
        double p = r.penalty();
        return weights.breakdown(t, d, c, p, r.violatedConstraints());
    }

    private double weighted(Route r) {
        double t = weights.alphaTravelTime() * r.totalTravelTimeMin();
        double d = weights.betaDistance() * r.totalDistanceKm();
        double c = weights.gammaCongestion() * congestionExtractor.applyAsDouble(r);
        return t + d + c + r.penalty();
    }
}
