export interface Node {
  id: string;
  latitude: number;
  longitude: number;
  metadata: Record<string, string>;
}

export interface Edge {
  id: string;
  source: string;
  destination: string;
  distanceKm: number;
  freeFlowTimeMin: number;
  speedKmh: number;
}

export interface TrafficState {
  edgeId: string;
  congestionLevel: number;
  currentTravelTimeMin: number;
  timestamp: string;
}

export interface Route {
  nodeIds: string[];
  edgeIds: string[];
  totalDistanceKm: number;
  totalTravelTimeMin: number;
  totalCongestionCost: number;
  penalty: number;
  totalFitness: number;
  feasible: boolean;
  violatedConstraints: string[];
}

export interface FitnessBreakdown {
  travelTimeCost: number;
  distanceCost: number;
  congestionCost: number;
  constraintPenalty: number;
  totalFitness: number;
  violatedConstraints: string[];
}

export interface OptimizationResponse {
  optimizationId: string;
  algorithm: string;
  bestRoute: Route;
  breakdown: FitnessBreakdown;
  bestFitness: number;
  convergenceHistory: number[];
  runtimeMs: number;
  iterations: number;
  evaluations: number;
  dijkstraReference: Route;
}

export interface BenchmarkMetric {
  algorithm: string;
  best: number; worst: number; mean: number; stdDev: number;
  meanRuntimeMs: number; meanEvaluations: number;
  meanConvergence: number[]; trials: number;
}

export interface BenchmarkResponse {
  problem: string;
  dimensions: number;
  results: BenchmarkMetric[];
}

export interface RerouteResponse {
  oldRoute: Route;
  newRoute: Route;
  newBreakdown: FitnessBreakdown;
  changedEdges: string[];
  oldFitness: number;
  newFitness: number;
}

export interface RouteRequest {
  source: string;
  destination: string;
  algorithm: 'QPSO' | 'PSO' | 'GA' | 'DIJKSTRA';
  iterations?: number;
  seed?: number;
  maxSteps?: number;
  alpha?: number;
  beta?: number;
  gamma?: number;
}

export interface RerouteRequest {
  source: string;
  destination: string;
  algorithm: 'QPSO' | 'PSO' | 'GA';
  iterations?: number;
  seed?: number;
  alpha?: number;
  beta?: number;
  gamma?: number;
}

export interface BenchmarkRequest {
  problem: 'sphere' | 'rastrigin' | 'rosenbrock';
  dimensions?: number;
  trials?: number;
  iterations?: number;
  algorithms?: string[];
}
