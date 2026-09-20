import { useEffect, useState } from 'react';
import type { Node, RerouteRequest, RerouteResponse } from '../types';

interface Props {
  nodes: Node[];
  loading: string | null;
  onReroute: (body: RerouteRequest) => Promise<void>;
}

export default function ReroutePanel({ nodes, loading, onReroute }: Props) {
  const [src, setSrc] = useState('Alexanderplatz');
  const [dst, setDst] = useState('Neukoelln');
  const [algo, setAlgo] = useState<'QPSO' | 'PSO' | 'GA'>('QPSO');
  const [iters, setIters] = useState('80');

  useEffect(() => {
    if (nodes.length === 0) return;
    setSrc(current => nodes.some(n => n.id === current) ? current : nodes[0].id);
    setDst(current => nodes.some(n => n.id === current) ? current : nodes[nodes.length - 1].id);
  }, [nodes]);

  const labelOf = (n: Node) => n.metadata?.label ?? n.metadata?.name ?? n.id;
  const nodeOpts = nodes.map((n) => (
    <option key={n.id} value={n.id}>{labelOf(n)}</option>
  ));

  return (
    <section className="card reroutePanel">
      <div className="rerouteHeading">
        <span className="rerouteIcon" aria-hidden="true">↻</span>
        <h3>Dynamic Re-route</h3>
      </div>
      <p className="hint">Run an optimization, inject an accident, and compare the original route with the response.</p>

      <div className="rerouteControls">
        <label>
          Source
          <select value={src} onChange={(e) => setSrc(e.target.value)}>{nodeOpts}</select>
        </label>
        <label>
          Destination
          <select value={dst} onChange={(e) => setDst(e.target.value)}>{nodeOpts}</select>
        </label>
        <label>
          Algorithm
          <select value={algo} onChange={(e) => setAlgo(e.target.value as any)}>
            <option value="QPSO">QPSO</option>
            <option value="PSO">PSO</option>
            <option value="GA">GA</option>
          </select>
        </label>
        <label>
          Iterations
          <input type="number" min={10} max={500} step={1} inputMode="numeric" value={iters}
                 onChange={(e) => setIters(e.target.value)} />
        </label>
      </div>
      <button
        className="primary rerouteAction"
        disabled={loading !== null || src === dst}
        onClick={() => onReroute({
          source: src,
          destination: dst,
          algorithm: algo,
          iterations: Math.max(10, Math.min(500, Number(iters) || 80)),
          alpha: 1.0,
          beta: 0.5,
          gamma: 2.0,
          seed: undefined,
        })}>
        {loading === 'reroute' ? 'Running…' : 'Trigger re-route demo'}
      </button>
    </section>
  );
}

export function RerouteVisual({ reroute }: { reroute: RerouteResponse | null }) {
  if (!reroute) {
    return (
      <div style={{ padding: 16, color: 'var(--fg-dim)', fontStyle: 'italic' }}>
        Trigger the re-route demo via the sidebar to see a side-by-side comparison of the route before
        and after a sudden congestion event.
      </div>
    );
  }
  const { oldRoute, newRoute, changedEdges, oldFitness, newFitness, newBreakdown } = reroute;
  const delta = newFitness - oldFitness;
  const deltaColor = delta <= 0 ? 'var(--good)' : 'var(--warn)';
  return (
    <div className="reroute-compare">
      <div className="row summary">
        <div className="chip"><strong>Old fitness:</strong> {oldFitness.toFixed(3)}</div>
        <div className="chip"><strong>New fitness:</strong> {newFitness.toFixed(3)}</div>
        <div className="chip" style={{ color: deltaColor }}>
          <strong>Δ:</strong> {delta >= 0 ? '+' : ''}{delta.toFixed(3)}
          {delta <= 0 ? '  ✓ re-route improved fitness' : '  ⚠ re-route absorbed incident with higher cost'}
        </div>
      </div>
      <div className="grid-2">
        <div>
          <h4>Before incident (old route)</h4>
          <ul>
            <li>Nodes: <code>{oldRoute.nodeIds.join(' → ')}</code></li>
            <li>Distance: {oldRoute.totalDistanceKm.toFixed(2)} km</li>
            <li>Travel time: {oldRoute.totalTravelTimeMin.toFixed(2)} min</li>
            <li>Congestion cost: {oldRoute.totalCongestionCost.toFixed(2)}</li>
          </ul>
        </div>
        <div>
          <h4>After incident (re-routed)</h4>
          <ul>
            <li>Nodes: <code>{newRoute.nodeIds.join(' → ')}</code></li>
            <li>Distance: {newRoute.totalDistanceKm.toFixed(2)} km</li>
            <li>Travel time: {newRoute.totalTravelTimeMin.toFixed(2)} min</li>
            <li>Congestion cost: {newBreakdown.congestionCost.toFixed(2)}</li>
          </ul>
        </div>
      </div>
      <div>
        <h4>Changed edges ({changedEdges.length})</h4>
        {changedEdges.length === 0
          ? <span style={{ color: 'var(--fg-dim)' }}>No changes — route absorbed the shock locally.</span>
          : <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {changedEdges.map((e) => <span key={e} className="chip">{e}</span>)}
            </div>
        }
      </div>
    </div>
  );
}

