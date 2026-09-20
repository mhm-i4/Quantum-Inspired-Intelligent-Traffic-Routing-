import type { Edge, TrafficState } from '../types';

interface Props {
  traffic: TrafficState[];
  edges: Edge[];
  loading: string | null;
  onSimulate: (scenario: string) => Promise<void>;
}

const SCENARIOS = ['NORMAL', 'MORNING_PEAK', 'EVENING_PEAK', 'ACCIDENT', 'RANDOM_DYNAMIC'];

function congestionColor(c: number) {
  if (c < 0.2) return '#4ade80';
  if (c < 0.5) return '#fbbf24';
  if (c < 0.8) return '#f97316';
  return '#ef4444';
}

export default function TrafficPanel({ traffic, edges, loading, onSimulate }: Props) {
  const avgCongestion = traffic.length
    ? traffic.reduce((s, t) => s + t.congestionLevel, 0) / traffic.length
    : 0;
  const roadPairs = new Set(edges.map(e => [e.source, e.destination].sort().join('|')));
  const trafficByEdge = new Map(traffic.map(state => [state.edgeId, state]));

  return (
    <div className="card">
      <h2>Live Traffic</h2>
      <p className="trafficIntro">A quick view of how busy each road is right now. The percentage is a congestion score: 0% is free-flowing and 100% is severely congested. It is not the exact percentage of extra travel time.</p>

      <div className="panelGrid" style={{ marginBottom: 12 }}>
        <div className="stat">
          <span className="l">Average congestion</span>
          <span className="v" style={{ color: congestionColor(avgCongestion) }}>
            {(avgCongestion * 100).toFixed(0)}%
          </span>
        </div>
        <div className="stat">
          <span className="l">Roads monitored</span>
          <span className="v">{roadPairs.size}</span>
        </div>
        <div className="stat">
          <span className="l">Live readings</span>
          <span className="v">{traffic.length}</span>
        </div>
      </div>
      <div className="trafficFootnote">Live readings include both directions of each road.</div>

      <div className="field">
        <label>Traffic conditions</label>
        <div className="scenarioChips">
          {SCENARIOS.map(s => (
            <button
              key={s}
              className={`chip`}
              disabled={!!loading}
              onClick={() => onSimulate(s)}
            >
              {s.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())}
            </button>
          ))}
        </div>
      </div>

      <div className="trafficList">
        {edges.slice(0, 20).map(e => {
          const state = trafficByEdge.get(e.id);
          const c = state?.congestionLevel ?? 0;
          return (
            <div className="trafficItem" key={e.id}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4, flex: 1, marginRight: 10 }}>
                <div className="trafficRoute">
                  <span>{e.source}</span><span className="trafficArrow">→</span><span>{e.destination}</span>
                </div>
                <div className="bar">
                  <div style={{ width: `${c * 100}%`, background: congestionColor(c) }} />
                </div>
              </div>
              <div style={{
                fontSize: 11, fontWeight: 700, color: congestionColor(c), whiteSpace: 'nowrap'
              }}>
                {(c * 100).toFixed(0)}%
              </div>
            </div>
          );
        })}
        {edges.length > 20 && (
          <div style={{ fontSize: 11, color: 'var(--muted)', padding: 4 }}>
            +{edges.length - 20} more directions...
          </div>
        )}
      </div>
    </div>
  );
}
