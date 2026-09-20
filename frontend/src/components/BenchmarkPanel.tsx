import { useMemo, useState } from 'react';
import { LineChart, Line, CartesianGrid, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import type { BenchmarkResponse } from '../types';

interface Props {
  loading: boolean;
  data: BenchmarkResponse | null;
  onRun: (body: { problem: string; dimensions?: number; trials?: number; iterations?: number; algorithms?: string[] }) => Promise<void>;
}

const COLORS: Record<string, string> = { QPSO: '#4fd1c5', PSO: '#667eea', GA: '#f59e0b' };

export default function BenchmarkPanel({ loading, data, onRun }: Props) {
  const [problem, setProblem] = useState('sphere');
  const [dims, setDims] = useState(10);
  const [trials, setTrials] = useState(10);
  const [iters, setIters] = useState(200);
  const [algs, setAlgs] = useState<string[]>(['QPSO', 'PSO', 'GA']);

  const toggleAlg = (a: string) => {
    setAlgs(prev => prev.includes(a) ? prev.filter(x => x !== a) : [...prev, a]);
  };

  const chartData = useMemo(() => {
    if (!data) return [];
    const len = Math.max(...data.results.map(r => r.meanConvergence.length), 0);
    const rows: any[] = [];
    for (let i = 0; i < len; i++) {
      const row: any = { iter: i };
      data.results.forEach(r => {
        const value = r.meanConvergence[i];
        const safeValue = typeof value === 'number' && Number.isFinite(value) && value >= 0
          ? Number(value.toFixed(6))
          : undefined;
        row[r.algorithm] = safeValue;
      });
      rows.push(row);
    }
    return rows;
  }, [data]);

  return (
    <div className="benchmarkLayout">
      <div className="benchmarkControls">
        <div className="card" style={{ padding: 12 }}>
          <h2>Configuration</h2>
          <div className="field">
            <label>Challenge type</label>
            <select value={problem} onChange={e => setProblem(e.target.value)}>
              <option value="sphere">Sphere · smooth and simple</option>
              <option value="rastrigin">Rastrigin · many local traps</option>
              <option value="rosenbrock">Rosenbrock · narrow winding valley</option>
            </select>
          </div>
          <div className="row">
            <div className="field">
              <label>Dimensions</label>
              <input type="number" value={dims} onChange={e => setDims(+e.target.value)} />
            </div>
            <div className="field">
              <label>Trials</label>
              <input type="number" value={trials} onChange={e => setTrials(+e.target.value)} />
            </div>
            <div className="field">
              <label>Iterations</label>
              <input type="number" value={iters} onChange={e => setIters(+e.target.value)} />
            </div>
          </div>
          <div className="field">
            <label>Algorithms</label>
            <div className="scenarioChips">
              {['QPSO', 'PSO', 'GA'].map(a => (
                <button key={a} className={`chip ${algs.includes(a) ? 'active' : ''}`} onClick={() => toggleAlg(a)}>{a}</button>
              ))}
            </div>
          </div>
          <div className="field">
            <button className="primary" disabled={loading || algs.length === 0}
              onClick={() => onRun({ problem, dimensions: dims, trials, iterations: iters, algorithms: algs })}>
              {loading ? 'Running…' : 'Run Benchmark'}
            </button>
          </div>
        </div>

        <div className="card" style={{ padding: 12 }}>
          <h2>How to read the results</h2>
          <p className="benchmarkIntro">Each algorithm solves the same challenge {data?.results[0]?.trials ?? trials} times. Lower fitness means a better answer.</p>
          {!data && <div style={{ fontSize: 12, color: 'var(--muted)' }}>No data. Run a benchmark.</div>}
          {data && data.results.map(r => (
            <div key={r.algorithm} style={{ marginBottom: 12, paddingBottom: 10, borderBottom: '1px dashed var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{ width: 10, height: 10, borderRadius: 999, background: COLORS[r.algorithm] ?? '#fff' }} />
                <b style={{ fontSize: 13 }}>{r.algorithm}</b>
              </div>
              <div className="breakdown" style={{ marginTop: 8 }}>
                <div className="k" title="The lowest score achieved in any trial">Best score</div><div className="v">{r.best.toExponential(3)}</div>
                <div className="k" title="The highest score achieved in any trial">Highest score</div><div className="v">{r.worst.toExponential(3)}</div>
                <div className="k" title="The typical score across all trials">Typical score</div><div className="v">{r.mean.toExponential(3)}</div>
                <div className="k" title="How much the results vary from trial to trial">Consistency range</div><div className="v">± {r.stdDev.toExponential(3)}</div>
                <div className="k" title="Average time needed to complete one trial">Average time</div><div className="v">{r.meanRuntimeMs} ms</div>
                <div className="k" title="How many candidate solutions were checked">Solutions checked</div><div className="v">{r.meanEvaluations.toLocaleString()}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="benchmarkChart">
        <h2 style={{ margin: 0, marginBottom: 4 }}>
          Improvement over time ({data?.problem ?? 'benchmark'}, {data?.dimensions ?? dims} variables)
        </h2>
        <div style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 6 }}>
          Curves show how quickly each method finds better answers. Lower is better; each line averages all trials.
        </div>
        {chartData.length === 0 ? (
          <div style={{ display: 'grid', placeItems: 'center', height: '80%', color: 'var(--muted)', fontSize: 13 }}>
            Configure and run a benchmark to see convergence comparison between QPSO / PSO / GA.
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="85%">
            <LineChart data={chartData} margin={{ top: 8, right: 16, bottom: 8, left: 4 }}>
              <CartesianGrid stroke="#2a3563" strokeDasharray="3 3" />
              <XAxis dataKey="iter" stroke="#9aa7c7" fontSize={11} />
              <YAxis stroke="#9aa7c7" fontSize={11} domain={[0, 'auto']} allowDataOverflow />
              <Tooltip contentStyle={{ background: '#121a33', border: '1px solid #2a3563', borderRadius: 8, color: '#e6ecff' }} />
              <Legend wrapperStyle={{ color: '#e6ecff' }} />
              {(data?.results ?? []).map(r => (
                <Line key={r.algorithm} type="monotone" isAnimationActive={false}
                  dataKey={r.algorithm} stroke={COLORS[r.algorithm] ?? '#fff'}
                  strokeWidth={2} dot={false} />
              ))}
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}
