import type { OptimizationResponse, TrafficState, BenchmarkResponse } from '../types';
import BenchmarkPanel from './BenchmarkPanel';
import ConvergencePanel from './ConvergencePanel';

interface Props {
  opt: OptimizationResponse | null;
  trafficByEdge: Map<string, TrafficState>;
  benchmark: BenchmarkResponse | null;
  loading: boolean;
  onRunBenchmark: (body: { problem: string; dimensions?: number; trials?: number; iterations?: number; algorithms?: string[] }) => Promise<void>;
}

export default function MetricsPanel({ opt, trafficByEdge, benchmark, loading, onRunBenchmark }: Props) {
  return (
    <div className="metricsPage">
      <section className="card metricsSection">
        <div className="metricsSectionHeading">
          <h2>Route improvement</h2>
          <span>See how the selected optimizer improves its route over time.</span>
        </div>
        <ConvergencePanel opt={opt} />
      </section>
      <section className="card metricsSection">
        <div className="metricsSectionHeading">
          <h2>Algorithm comparison</h2>
          <span>Run the same challenge several times to compare QPSO, PSO, and GA fairly.</span>
        </div>
        <BenchmarkPanel loading={loading} data={benchmark} onRun={onRunBenchmark} />
      </section>
    </div>
  );
}