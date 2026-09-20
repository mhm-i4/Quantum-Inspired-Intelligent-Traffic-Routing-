import { useState } from 'react';
import type { Node, OptimizationResponse } from '../types';

function normalizeRouteScore(fitness: number) {
  const midpoint = 12;
  const slope = 2.5;
  const clipped = Math.max(0, Number.isFinite(fitness) ? fitness : 0);
  const score = 100 / (1 + Math.exp((clipped - midpoint) / slope));
  return Math.max(0, Math.min(100, score));
}

interface Props {
  nodes: Node[];
  source: string;
  destination: string;
  selectionTarget: 'source' | 'destination';
  onSourceChange: (id: string) => void;
  onDestinationChange: (id: string) => void;
  onSelectionTargetChange: (target: 'source' | 'destination') => void;
  loading: string | null;
  onOptimize: (body: {
    source: string; destination: string; algorithm: string;
    iterations?: number; seed?: number; maxSteps?: number;
    alpha?: number; beta?: number; gamma?: number;
  }) => Promise<void>;
  onDijkstra: (body: { source: string; destination: string; alpha?: number; beta?: number; gamma?: number }) => Promise<void>;
}

export default function RoutePanel({
  nodes, source, destination: dest, selectionTarget,
  onSourceChange, onDestinationChange, onSelectionTargetChange,
  loading, onOptimize, onDijkstra,
}: Props) {
  const [algo, setAlgo] = useState('QPSO');
  const [iters, setIters] = useState(250);
  const [seed, setSeed] = useState(7);
  const [searchDepth, setSearchDepth] = useState('32');
  const [alpha, setAlpha] = useState(1.0);
  const [beta, setBeta] = useState(0.5);
  const [gamma, setGamma] = useState(2.0);

  const body = {
    source, destination: dest, algorithm: algo, iterations: iters, seed,
    maxSteps: Math.max(8, Math.min(100, Number(searchDepth) || 32)), alpha, beta, gamma,
  };
  const displayedDepth = Math.max(8, Math.min(100, Number(searchDepth) || 32));

  return (
    <div className="card">
      <h2>Route Optimization</h2>

      <div className="row">
        <div className="field">
          <label>Source node</label>
          <select value={source} onChange={e => onSourceChange(e.target.value)}>
            {nodes.map(n => <option key={n.id} value={n.id}>{n.id}</option>)}
          </select>
          <button className={`mapPick ${selectionTarget === 'source' ? 'active' : ''}`} onClick={() => onSelectionTargetChange('source')}>
            {selectionTarget === 'source' ? 'Click a node on map' : 'Pick on map'}
          </button>
        </div>
        <div className="field">
          <label>Destination</label>
          <select value={dest} onChange={e => onDestinationChange(e.target.value)}>
            {nodes.map(n => <option key={n.id} value={n.id}>{n.id}</option>)}
          </select>
          <button className={`mapPick ${selectionTarget === 'destination' ? 'active' : ''}`} onClick={() => onSelectionTargetChange('destination')}>
            {selectionTarget === 'destination' ? 'Click a node on map' : 'Pick on map'}
          </button>
        </div>
      </div>

      <div className="row">
        <div className="field">
          <label>Algorithm</label>
          <select value={algo} onChange={e => setAlgo(e.target.value)}>
            <option>QPSO</option>
            <option>PSO</option>
            <option>GA</option>
          </select>
        </div>
        <div className="field">
          <label>Iterations</label>
          <input type="number" value={iters} onChange={e => setIters(+e.target.value)} />
        </div>
        <div className="field">
          <label>
            <span className="fieldLabelWithHint">Seed <span className="infoDot" title="A repeatable starting point for QPSO randomness.">?</span></span>
          </label>
          <input type="number" value={seed} onChange={e => setSeed(+e.target.value)} />
        </div>
        <div className="field">
          <label>Search depth</label>
          <input type="number" min={8} max={100} step={1} value={searchDepth}
            onChange={e => setSearchDepth(e.target.value)} />
        </div>
      </div>

      <div className="field">
        <label>Objective weights: α·time + β·distance + γ·congestion</label>
        <div className="row">
          <input type="number" step="0.1" value={alpha} onChange={e => setAlpha(+e.target.value)} />
          <input type="number" step="0.1" value={beta} onChange={e => setBeta(+e.target.value)} />
          <input type="number" step="0.1" value={gamma} onChange={e => setGamma(+e.target.value)} />
        </div>
      </div>

      <div className="searchExplainer">
        <div className="searchExplainerHeader">
          <div>
            <strong>How the route search works</strong>
            <span>QPSO tests many possible journeys and keeps improving the best one.</span>
          </div>
          <span className="searchDepthBadge">{displayedDepth} choices</span>
        </div>
        <div className="searchFlow" aria-label="Route search flow">
          <div className="searchStep"><span>1</span><b>Start</b><small>{source || 'origin'}</small></div>
          <i aria-hidden="true">→</i>
          <div className="searchStep"><span>2</span><b>Choose roads</b><small>up to {displayedDepth}</small></div>
          <i aria-hidden="true">→</i>
          <div className="searchStep"><span>3</span><b>Score route</b><small>time + distance</small></div>
          <i aria-hidden="true">→</i>
          <div className="searchStep"><span>4</span><b>Improve</b><small>{iters} rounds</small></div>
        </div>
        <div className="searchExplainerNotes">
          <span><b>Search depth</b> maximum road choices for one candidate journey</span>
          <span><b>Fitness</b> lower is better: faster, shorter, less congested</span>
          <span><b>Seed</b> repeat the same starting conditions for a fair comparison</span>
          <span><b>Iterations</b> repeated improvement rounds for the journey population</span>
        </div>
      </div>

      <div className="row">
        <div className="field">
          <button
            className="primary"
            disabled={loading !== null || source === dest}
            onClick={() => onOptimize(body)}
          >
            {loading === 'optimize' ? 'Running…' : 'Optimize Route'}
          </button>
        </div>
        <div className="field">
          <button className="ghost"
            disabled={loading !== null || source === dest}
            onClick={() => onDijkstra({ source, destination: dest, alpha, beta, gamma })}>
            {loading === 'dijkstra' ? 'Running…' : 'Dijkstra (baseline)'}
          </button>
        </div>
      </div>
    </div>
  );
}

/** Small helper used in both Routing + Convergence panels */
export function RouteSummary({ title, route, algorithm, fitness }: {
  title: string;
  route: OptimizationResponse['bestRoute'] | null;
  algorithm?: string;
  fitness?: number;
}) {
  if (!route) {
    return (
      <div style={{ fontSize: 12, color: 'var(--muted)' }}>
        {title}: no data yet. Run an optimization.
      </div>
    );
  }

  const isDijkstraOnly = algorithm === 'Dijkstra' || algorithm === 'DIJKSTRA';
  const rawFitness = fitness ?? route.totalFitness;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <b style={{ fontSize: 13 }}>{title}</b>
        <span className={`routeBadge ${route.feasible ? 'feasible' : 'infeasible'}`}>
          {route.feasible ? 'Feasible' : 'Infeasible'} · {algorithm}
        </span>
      </div>
      <div className="panelGrid">
        <div className="stat"><span className="l">Distance</span><span className="v">{route.totalDistanceKm.toFixed(2)} km</span></div>
        <div className="stat"><span className="l">Time</span><span className="v">{route.totalTravelTimeMin.toFixed(2)} min</span></div>
        {!isDijkstraOnly && (
          <div className="stat"><span className="l">Route Score</span><span className="v">{normalizeRouteScore(rawFitness).toFixed(1)}/100</span></div>
        )}
      </div>
      <div className="listEdge" title="node path">
        {route.nodeIds.map((n, i) => (
          <span key={i}>{n}{i < route.nodeIds.length - 1 ? ' →' : ''}</span>
        ))}
      </div>
    </div>
  );
}
