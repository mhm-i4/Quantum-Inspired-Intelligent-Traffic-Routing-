import axios from 'axios';
import type {
  Node, Edge, TrafficState, Route, FitnessBreakdown,
  OptimizationResponse, BenchmarkResponse, RerouteResponse
} from './types';

const API = axios.create({ baseURL: '/api' });

export const api = {
  getNetwork: () => API.get<{ nodes: Node[]; edges: Edge[] }>('/network').then(r => r.data),
  getNodes: () => API.get<Node[]>('/network/nodes').then(r => r.data),
  getEdges: () => API.get<Edge[]>('/network/edges').then(r => r.data),
  getTraffic: () => API.get<TrafficState[]>('/traffic').then(r => r.data),
  simulateTraffic: (scenario: string) =>
    API.post<TrafficState[]>('/traffic/simulate', { scenario }).then(r => r.data),
  setCongestion: (edgeId: string, level: number) =>
    API.post<TrafficState>('/traffic/set', { edgeId, congestionLevel: level }).then(r => r.data),
  optimizeRoute: (body: {
    source: string; destination: string; algorithm: string; iterations?: number;
    seed?: number; maxSteps?: number; alpha?: number; beta?: number; gamma?: number;
  }) => API.post<OptimizationResponse>('/route/optimize', body).then(r => r.data),
  dijkstra: (body: { source: string; destination: string; alpha?: number; beta?: number; gamma?: number }) =>
    API.post<Route>('/route/dijkstra', body).then(r => r.data),
  reroute: (body: {
    source: string; destination: string; algorithm: string;
    alpha?: number; beta?: number; gamma?: number; iterations?: number;
  }) => API.post<RerouteResponse>('/route/reroute', body).then(r => r.data),
  getOptimization: (id: string) => API.get<OptimizationResponse>(`/optimization/${id}`).then(r => r.data),
  getConvergence: (id: string) => API.get<number[]>(`/optimization/${id}/convergence`).then(r => r.data),
  benchmark: (body: {
    problem: string; dimensions?: number; trials?: number; iterations?: number; algorithms?: string[];
  }) => API.post<BenchmarkResponse>('/benchmark', body).then(r => r.data),
};
