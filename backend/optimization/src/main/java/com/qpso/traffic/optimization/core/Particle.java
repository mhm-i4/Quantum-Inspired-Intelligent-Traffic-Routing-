package com.qpso.traffic.optimization.core;

/**
 * Wraps the decision vector of a particle (continuous position)
 * along with its decoded discrete solution.
 *
 * @param <D> decoded solution type (e.g., a Route or permutation)
 */
public record Particle<D>(
        double[] position,
        double[] personalBestPosition,
        double personalBestFitness,
        double currentFitness,
        D decodedSolution,
        boolean evaluated
) {
    public static <D> Particle<D> unevaluated(double[] position) {
        return new Particle<>(position.clone(), position.clone(), Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY, null, false);
    }

    public Particle<D> withEvaluation(double fitness, D decoded) {
        double[] newPBestPos = this.personalBestPosition;
        double newPBest = this.personalBestFitness;
        if (fitness < this.personalBestFitness) {
            newPBestPos = this.position.clone();
            newPBest = fitness;
        }
        return new Particle<>(this.position, newPBestPos, newPBest, fitness, decoded, true);
    }

    public Particle<D> withPosition(double[] newPosition) {
        return new Particle<>(newPosition.clone(), this.personalBestPosition, this.personalBestFitness,
                Double.POSITIVE_INFINITY, this.decodedSolution, false);
    }

    public int dimensions() {
        return position.length;
    }
}
