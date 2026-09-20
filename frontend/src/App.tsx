import { useEffect, useMemo, useState } from 'react';
import { api } from './api';
import type {
  Node, Edge, TrafficState, OptimizationResponse, BenchmarkResponse,
  RerouteResponse, RouteRequest, RerouteRequest, BenchmarkRequest
} from './types';
import NetworkMap from './components/NetworkMap';
import RoutePanel from './components/RoutePanel';
import TrafficPanel from './components/TrafficPanel';
import MetricsPanel from './components/MetricsPanel';
import RecommendationPanel from './components/RecommendationPanel';
import ReroutePanel, { RerouteVisual } from './components/ReroutePanel';

export default function App() {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [traffic, setTraffic] = useState<TrafficState[]>([]);
  const [source, setSource] = useState('Alexanderplatz');
  const [destination, setDestination] = useState('Neukoelln');
  const [selectionTarget, setSelectionTarget] = useState<'source' | 'destination'>('source');
  const [selectedEdgeId, setSelectedEdgeId] = useState<string | null>(null);
  const [opt, setOpt] = useState<OptimizationResponse | null>(null);
  const [bench, setBench] = useState<BenchmarkResponse | null>(null);
  const [reroute, setReroute] = useState<RerouteResponse | null>(null);
  const [tab, setTab] = useState<'recommendation' | 'metrics' | 'reroute'>('recommendation');
  const [loading, setLoading] = useState<string | null>(null);

  useEffect(() => {
    api.getNetwork().then(n => {
      setNodes(n.nodes);
      setEdges(n.edges);
      setSource(current => n.nodes.some(node => node.id === current) ? current : n.nodes[0]?.id ?? '');
      setDestination(current => n.nodes.some(node => node.id === current)
        ? current : n.nodes[n.nodes.length - 1]?.id ?? '');
    });
    api.getTraffic().then(setTraffic);
  }, []);

  const refreshTraffic = () => api.getTraffic().then(setTraffic);
  const trafficByEdge = useMemo(() => {
    const m = new Map<string, TrafficState>();
    traffic.forEach(t => m.set(t.edgeId, t));
    return m;
  }, [traffic]);
  const selectedEdge = edges.find(edge => edge.id === selectedEdgeId) ?? null;
  const selectedTraffic = selectedEdge ? trafficByEdge.get(selectedEdge.id) : undefined;

  const handleNodeClick = (id: string) => {
    if (selectionTarget === 'source') {
      setSource(id);
      setSelectionTarget('destination');
    } else if (id !== source) {
      setDestination(id);
      setSelectionTarget('source');
    }
  };

  return (
    <div className="layout">
      <div className="header">
        <h1>QPSO Traffic Route Optimization Platform</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          <span className="tag">Quantum-Inspired Metaheuristic · Hackathon Demo</span>
          <span className="tag">{nodes.length} nodes · {edges.length} edges</span>
        </div>
      </div>

      <aside className="sidebar">
        <TrafficPanel
          traffic={traffic}
          edges={edges}
          loading={loading}
          onSimulate={async (s) => {
            setLoading('traffic');
            await api.simulateTraffic(s);
            await refreshTraffic();
            setLoading(null);
          }}
        />

        <RoutePanel
          nodes={nodes}
          source={source}
          destination={destination}
          selectionTarget={selectionTarget}
          onSourceChange={setSource}
          onDestinationChange={setDestination}
          onSelectionTargetChange={setSelectionTarget}
          loading={loading}
          onOptimize={async (body) => {
            setLoading('optimize');
            try {
              const r = await api.optimizeRoute(body);
              setOpt(r);
              setReroute(null);
            } finally { setLoading(null); }
          }}
          onDijkstra={async (body) => {
            setLoading('dijkstra');
            try {
              const r = await api.dijkstra(body);
              setOpt(prev => prev ? { ...prev, dijkstraReference: r } : {
                optimizationId: 'DIJKSTRA-ONLY',
                algorithm: 'DIJKSTRA',
                bestRoute: r,
                breakdown: {
                  travelTimeCost: 0, distanceCost: 0, congestionCost: 0,
                  constraintPenalty: r.penalty, totalFitness: r.totalFitness,
                  violatedConstraints: r.violatedConstraints,
                },
                bestFitness: r.totalFitness,
                convergenceHistory: [],
                runtimeMs: 0, iterations: 0, evaluations: 0,
                dijkstraReference: r,
              });
            } finally { setLoading(null); }
          }}
        />

        <ReroutePanel
          nodes={nodes}
          loading={loading}
          onReroute={async (body) => {
            setLoading('reroute');
            try {
              const r = await api.reroute(body);
              setReroute(r);
              refreshTraffic();
            } finally { setLoading(null); }
          }}
        />
      </aside>

      <main className="main">
        <div className="card" style={{ padding: 8 }}>
          <NetworkMap
            nodes={nodes}
            edges={edges}
            trafficByEdge={trafficByEdge}
            sourceId={source}
            destinationId={destination}
            selectionTarget={selectionTarget}
            selectedEdgeId={selectedEdgeId}
            onNodeClick={handleNodeClick}
            onEdgeClick={setSelectedEdgeId}
            highlightRoute={reroute ? reroute.newRoute : opt?.bestRoute ?? null}
            highlightAltRoute={reroute ? reroute.oldRoute : opt?.dijkstraReference ?? null}
          />
        </div>

        <div className="card connectionInspector">
          <h2>Selected Road Connection</h2>
          {!selectedEdge ? (
            <div className="emptyState">Click a road on the map to inspect its live connection data.</div>
          ) : (
            <>
              <div className="connectionTitle">
                <b>{selectedEdge.source} → {selectedEdge.destination}</b>
                <span>{selectedEdge.id}</span>
              </div>
              <div className="panelGrid">
                <div className="stat"><span className="l">Distance</span><span className="v">{selectedEdge.distanceKm.toFixed(3)} km</span></div>
                <div className="stat"><span className="l">Current time</span><span className="v">{(selectedTraffic?.currentTravelTimeMin ?? selectedEdge.freeFlowTimeMin).toFixed(2)} min</span></div>
                <div className="stat"><span className="l">Congestion cost</span><span className="v">{((selectedTraffic?.congestionLevel ?? 0) * 100).toFixed(1)}</span></div>
              </div>
              <div className="connectionMeta">
                Free flow: {selectedEdge.freeFlowTimeMin.toFixed(2)} min · Speed: {selectedEdge.speedKmh.toFixed(0)} km/h · Congestion: {((selectedTraffic?.congestionLevel ?? 0) * 100).toFixed(0)}%
                {selectedTraffic && <> · Updated {new Date(selectedTraffic.timestamp).toLocaleTimeString()}</>}
              </div>
            </>
          )}
        </div>

        <div className="card">
          <div className="tabs">
            {(['recommendation', 'metrics', 'reroute'] as const).map(t => (
              <button key={t} className={`tab ${tab === t ? 'active' : ''}`} onClick={() => setTab(t)}>
                {t === 'recommendation' ? 'Recommendation' : t === 'metrics' ? 'Metrics' : 'Dynamic Re-Routing'}
              </button>
            ))}
          </div>
          {tab === 'recommendation' && <RecommendationPanel opt={opt} trafficByEdge={trafficByEdge} />}
          {tab === 'metrics' && (
            <MetricsPanel
              opt={opt}
              trafficByEdge={trafficByEdge}
              benchmark={bench}
              loading={loading === 'benchmark'}
              onRunBenchmark={async (req) => {
                setLoading('benchmark');
                try {
                  const r = await api.benchmark(req);
                  setBench(r);
                } finally { setLoading(null); }
              }}
            />
          )}
          {tab === 'reroute' && <RerouteVisual reroute={reroute} />}
        </div>
      </main>
    </div>
  );
}
