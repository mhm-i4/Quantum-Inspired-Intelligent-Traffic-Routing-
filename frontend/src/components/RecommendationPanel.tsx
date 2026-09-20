import type { OptimizationResponse, TrafficState } from '../types';

interface Props {
  opt: OptimizationResponse | null;
  trafficByEdge: Map<string, TrafficState>;
}

function averageCongestion(route: OptimizationResponse['bestRoute'], trafficByEdge: Map<string, TrafficState>) {
  if (!route.edgeIds.length) return 0;
  return route.edgeIds.reduce((sum, edgeId) => sum + (trafficByEdge.get(edgeId)?.congestionLevel ?? 0), 0) / route.edgeIds.length;
}

export default function RecommendationPanel({ opt, trafficByEdge }: Props) {
  if (!opt) {
    return <section className="card recommendationCard recommendationEmpty"><h2>Route recommendation</h2><p>Run an optimizer to receive a route briefing with expected time, distance, congestion, and the reason for the recommendation.</p></section>;
  }

  const route = opt.bestRoute;
  const congestion = averageCongestion(route, trafficByEdge);
  const dijkstra = opt.dijkstraReference;
  const timeDifference = dijkstra ? route.totalTravelTimeMin - dijkstra.totalTravelTimeMin : null;
  const reasons = [
    `lowest combined score found by ${opt.algorithm}`,
    'balances travel time, distance, and congestion',
    `${route.edgeIds.length} road movements connect the selected locations`,
  ];
  if (route.penalty > 0) reasons.push('has a constraint penalty; increase search depth and review it');

  return (
    <div className="recommendationPage">
      <section className="card recommendationCard">
        <div className="recommendationHeader">
          <div><h2>Route recommendation</h2><p className="recommendationLead">A plain-language briefing from the latest {opt.algorithm} run</p></div>
          <span className={`routeBadge ${route.feasible ? 'feasible' : 'infeasible'}`}>{route.feasible ? 'Ready to use' : 'Needs review'}</span>
        </div>
        <div className="recommendationPath">{route.nodeIds.map((node, index) => <span key={`${node}-${index}`}>{node}{index < route.nodeIds.length - 1 && <b> → </b>}</span>)}</div>
        <div className="recommendationStats">
          <div><span>Expected travel time</span><strong>{route.totalTravelTimeMin.toFixed(2)} min</strong></div>
          <div><span>Distance to travel</span><strong>{route.totalDistanceKm.toFixed(2)} km</strong></div>
          <div><span>Expected congestion</span><strong>{(congestion * 100).toFixed(0)}%</strong></div>
        </div>
      </section>

      <div className="recommendationDetailGrid">
        <section className="card recommendationDetailCard">
          <h2>Why was this path chosen?</h2>
          <p className="recommendationCopy">The optimizer compares many possible paths and selects the one with the lowest overall fitness score. That score rewards shorter travel time and distance while accounting for current congestion and route penalties.</p>
          <ul className="recommendationReasons">{reasons.map(reason => <li key={reason}>{reason}</li>)}</ul>
        </section>
        <section className="card recommendationDetailCard">
          <h2>What should the traveller expect?</h2>
          <div className="recommendationBreakdown">
            <div><span>Travel-time contribution</span><strong>{opt.breakdown.travelTimeCost.toFixed(2)}</strong></div>
            <div><span>Distance contribution</span><strong>{opt.breakdown.distanceCost.toFixed(2)}</strong></div>
            <div><span>Congestion contribution</span><strong>{opt.breakdown.congestionCost.toFixed(2)}</strong></div>
            {!opt.algorithm.includes('DIJKSTRA') && (
              <>
                <div><span>Route Score</span><strong>{(100 / (1 + Math.exp((opt.breakdown.totalFitness - 12) / 2.5))).toFixed(1)}/100</strong></div>
                <div><span>Optimization cost</span><strong>{opt.breakdown.totalFitness.toFixed(3)}</strong></div>
              </>
            )}
          </div>
          {timeDifference !== null && <p className="recommendationCopy">Compared with the Dijkstra reference, this route is {Math.abs(timeDifference).toFixed(2)} minutes {timeDifference <= 0 ? 'faster' : 'slower'} under the current traffic conditions.</p>}
        </section>
      </div>
    </div>
  );
}