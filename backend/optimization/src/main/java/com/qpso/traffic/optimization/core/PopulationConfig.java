package com.qpso.traffic.optimization.core;

import java.util.Random;

/**
 * Common parameters shared by population-based optimizers (PSO/QPSO/GA).
 */
public record PopulationConfig(
        int populationSize,
        int maxIterations,
        long seed
) {
    public static final PopulationConfig DEFAULT = new PopulationConfig(40, 500, 42L);

    public Random createRandom() {
        return new Random(seed);
    }
}
