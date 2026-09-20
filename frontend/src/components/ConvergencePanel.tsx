import { useMemo } from 'react';
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { OptimizationResponse } from '../types';
import { RouteSummary } from './RoutePanel';

export default function ConvergencePanel({ opt }: { opt: OptimizationResponse | null }) {
  const data = useMemo(() => {
    if (!opt) return [];

    let bestSoFar = Number.POSITIVE_INFINITY;
    return opt.convergenceHistory.map((y, i) => {
      const value = typeof y === 'number' && Number.isFinite(y) ? y : Number.POSITIVE_INFINITY;
      if (value < bestSoFar) bestSoFar = value;
      return {
        iter: i,
        bestFitness: Number.isFinite(bestSoFar) ? Number(bestSoFar.toFixed(6)) : undefined,
      };
    });
  }, [opt]);

  const summary = useMemo(() => {
    if (!opt || data.length === 0) return null;
    const first = data[0]?.bestFitness ?? 0;
    const last = data[data.length - 1]?.bestFitness ?? first;
    const improvement = first > 0 ? ((first - last) / first) * 100 : 0;
    return { first, last, improvement };
  }, [opt, data]);

  const yValues = useMemo(() => data.map(d => d.bestFitness).filter((v): v is number => typeof v === 'number' && Number.isFinite(v)), [data]);
  const yDomain = useMemo(() => {
    if (yValues.length === 0) return [0, 1];
    const min = Math.min(...yValues);
    const max = Math.max(...yValues);
    const range = max - min || Math.abs(max) * 0.1 || 0.0001;
    const padding = range * 0.08;
    const lower = Math.max(0, min - padding);
    const upper = max + padding;
    return [Number(lower.toFixed(6)), Number(upper.toFixed(6))];
  }, [yValues]);

  const xTicks = useMemo(() => {
    const last = data.length > 0 ? data[data.length - 1].iter : 0;
    if (last <= 0) return [0];
    const step = last <= 50 ? 5 : last <= 100 ? 10 : last <= 200 ? 25 : 50;
    const ticks: number[] = [];
    for (let value = 0; value <= last; value += step) {
      ticks.push(value);
    }
    if (ticks[ticks.length - 1] !== last) ticks.push(last);
    return ticks;
  }, [data]);
  const isDijkstraOnly = opt?.algorithm === 'DIJKSTRA';

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 16, height: '100%' }}>
      <div style={{ height: '100%', minHeight: 420 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
          <div>
            <h2 style={{ margin: 0, fontSize: 18 }}>{opt ? `${opt.algorithm} Convergence` : 'Convergence'}</h2>
            {opt && (
              <div style={{ fontSize: 12, color: 'var(--muted)', marginTop: 4 }}>
                {opt.iterations} iterations • {opt.evaluations.toLocaleString()} evaluations • {opt.runtimeMs} ms
              </div>
            )}
          </div>
        </div>
        {data.length === 0 ? (
          <div style={{ display: 'grid', placeItems: 'center', height: '80%', color: 'var(--muted)', fontSize: 13 }}>
            No optimization run yet. Choose nodes on the left and press &quot;Optimize Route&quot;.
          </div>
        ) : (
          <div style={{ height: 360, width: '100%' }}>
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data} margin={{ top: 8, right: 16, bottom: 8, left: 8 }}>
                <CartesianGrid stroke="#2a3563" strokeDasharray="3 3" />
                <XAxis
                  dataKey="iter"
                  ticks={xTicks}
                  tickFormatter={(value) => Number(value).toString()}
                  stroke="#9aa7c7"
                  fontSize={11}
                  interval={0}
                />
                <YAxis
                  stroke="#9aa7c7"
                  fontSize={11}
                  domain={yDomain}
                  tickCount={5}
                  tickFormatter={(value) => Number(value).toFixed(5)}
                />
                <Tooltip
                  cursor={{ stroke: '#4fd1c5', strokeDasharray: '4 4' }}
                  contentStyle={{ background: '#121a33', border: '1px solid #2a3563', borderRadius: 8, color: '#e6ecff', boxShadow: '0 8px 24px rgba(0,0,0,0.25)' }}
                  labelStyle={{ color: '#e6ecff', fontWeight: 600 }}
                  formatter={(value: number | string) => [`${Number(value).toFixed(5)}`, 'Best Fitness']}
                  labelFormatter={(label) => `Iteration: ${label}`}
                  itemStyle={{ color: '#e6ecff' }}
                  position={{ y: 12 }}
                />
                <Legend wrapperStyle={{ color: '#e6ecff' }} />
                <Line
                  type="monotone"
                  isAnimationActive={false}
                  dataKey="bestFitness"
                  name="Best Fitness"
                  stroke="#4fd1c5"
                  strokeWidth={3}
                  dot={false}
                  activeDot={{ r: 5, strokeWidth: 0, fill: '#4fd1c5' }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}

        {summary && (
          <div style={{ display: 'flex', gap: 12, marginTop: 12, fontSize: 12, color: 'var(--muted)', flexWrap: 'wrap' }}>
            <span><b style={{ color: '#e6ecff' }}>Initial Fitness:</b> {summary.first.toFixed(5)}</span>
            <span><b style={{ color: '#e6ecff' }}>Best Fitness:</b> {summary.last.toFixed(5)}</span>
            <span><b style={{ color: '#e6ecff' }}>Improvement:</b> {summary.improvement.toFixed(1)}%</span>
          </div>
        )}
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 12, minHeight: 0, overflow: 'auto' }}>
        <div className="card" style={{ padding: 12 }}>
          <RouteSummary
            title={isDijkstraOnly ? 'Dijkstra route' : 'Best route'}
            route={opt?.bestRoute ?? null}
            algorithm={opt?.algorithm}
            fitness={opt?.breakdown.totalFitness}
          />
          {opt?.breakdown && (
            <div className="breakdown" style={{ marginTop: 12 }}>
              <div className="k">Travel-time cost</div><div className="v">{opt.breakdown.travelTimeCost.toFixed(3)}</div>
              <div className="k">Distance cost</div><div className="v">{opt.breakdown.distanceCost.toFixed(3)}</div>
              <div className="k">Congestion cost</div><div className="v">{opt.breakdown.congestionCost.toFixed(3)}</div>
              <div className="k">Penalties</div><div className="v">{opt.breakdown.constraintPenalty.toFixed(3)}</div>
              {!isDijkstraOnly && (
                <>
                  <div className="k">Route Score</div><div className="v">{(100 / (1 + Math.exp((opt.breakdown.totalFitness - 12) / 2.5))).toFixed(1)}/100</div>
                  <div className="k total">Optimization cost</div><div className="v total">{opt.breakdown.totalFitness.toFixed(3)}</div>
                </>
              )}
            </div>
          )}
        </div>
        {!isDijkstraOnly && (
          <div className="card" style={{ padding: 12 }}>
            <RouteSummary title="Dijkstra reference" route={opt?.dijkstraReference ?? null} algorithm="Dijkstra" />
          </div>
        )}
      </div>
    </div>
  );
}
