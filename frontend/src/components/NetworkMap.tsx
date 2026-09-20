import { useMemo } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, CircleMarker } from 'react-leaflet';
import type { Node, Edge, TrafficState, Route } from '../types';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// Use a custom route-label icon instead of Leaflet's default image assets.
delete (L.Icon.Default.prototype as any)._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: '', iconUrl: '', shadowUrl: '',
});

interface Props {
  nodes: Node[];
  edges: Edge[];
  trafficByEdge: Map<string, TrafficState>;
  highlightRoute: Route | null;
  highlightAltRoute: Route | null;
  sourceId: string;
  destinationId: string;
  selectionTarget: 'source' | 'destination';
  selectedEdgeId: string | null;
  onNodeClick: (id: string) => void;
  onEdgeClick: (id: string) => void;
}

function colorForCongestion(c: number): string {
  if (c < 0.2) return '#4ade80';
  if (c < 0.5) return '#fbbf24';
  if (c < 0.8) return '#f97316';
  return '#ef4444';
}

export default function NetworkMap({
  nodes, edges, trafficByEdge, highlightRoute, highlightAltRoute,
  sourceId, destinationId, selectionTarget, selectedEdgeId, onNodeClick, onEdgeClick,
}: Props) {
  const nodeMap = useMemo(() => {
    const m = new Map<string, Node>();
    nodes.forEach(n => m.set(n.id, n));
    return m;
  }, [nodes]);

  const center: [number, number] = useMemo(() => {
    if (nodes.length === 0) return [52.5200, 13.4050];
    const lat = nodes.reduce((s, n) => s + n.latitude, 0) / nodes.length;
    const lon = nodes.reduce((s, n) => s + n.longitude, 0) / nodes.length;
    return [lat, lon];
  }, [nodes]);

  const nodeToLatLng = (id: string): [number, number] | null => {
    const n = nodeMap.get(id);
    return n ? [n.latitude, n.longitude] : null;
  };

  const edgePolyline = (e: Edge): [number, number][] | null => {
    const a = nodeToLatLng(e.source);
    const b = nodeToLatLng(e.destination);
    return a && b ? [a, b] : null;
  };

  const routePolyline = (r: Route | null): [number, number][] => {
    if (!r) return [];
    const out: [number, number][] = [];
    r.nodeIds.forEach(id => { const p = nodeToLatLng(id); if (p) out.push(p); });
    return out;
  };

  const highlightedEdges = useMemo(() => {
    const set = new Set<string>();
    if (highlightRoute) highlightRoute.edgeIds.forEach(id => set.add(id));
    if (highlightAltRoute) highlightAltRoute.edgeIds.forEach(id => set.add(id));
    return set;
  }, [highlightRoute, highlightAltRoute]);

  return (
    <div className="mapContainer">
      <MapContainer
        center={center}
        zoom={14}
        style={{ height: '100%', width: '100%', background: '#0f1632' }}
        zoomControl={true}
        preferCanvas={true}
      >
        <TileLayer
          attribution='&copy; OpenStreetMap contributors'
          url='https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png'
        />
        {edges.map(e => {
          const pts = edgePolyline(e);
          if (!pts) return null;
          const congestion = trafficByEdge.get(e.id)?.congestionLevel ?? 0;
          const isInRoute = highlightedEdges.has(e.id);
          const dashArray = e.id.endsWith('_B') ? '6 6' : undefined;
          return (
            <Polyline
              key={e.id}
              positions={pts}
              pathOptions={{
                color: colorForCongestion(congestion),
                weight: selectedEdgeId === e.id ? 8 : isInRoute ? 7 : congestion > 0.7 ? 5 : 3,
                opacity: selectedEdgeId === e.id || isInRoute ? 1 : 0.65,
                lineCap: 'round',
              }}
              dashArray={dashArray}
              eventHandlers={{ click: () => onEdgeClick(e.id) }}
            >
              <Popup>
                <div style={{ color: '#0a1030', minWidth: 200 }}>
                  <div><b>Edge</b> {e.id}</div>
                  <div>From {e.source} → {e.destination}</div>
                  <div>Distance: {e.distanceKm.toFixed(3)} km</div>
                  <div>Free-flow time: {e.freeFlowTimeMin.toFixed(2)} min</div>
                  <div>Congestion: {(congestion * 100).toFixed(0)}%</div>
                  {trafficByEdge.has(e.id) && (
                    <div>Current travel time: {trafficByEdge.get(e.id)!.currentTravelTimeMin.toFixed(2)} min</div>
                  )}
                </div>
              </Popup>
            </Polyline>
          );
        })}

        {highlightAltRoute && routePolyline(highlightAltRoute).length > 1 && (
          <Polyline positions={routePolyline(highlightAltRoute)}
            pathOptions={{ color: '#667eea', weight: 10, opacity: 0.35, lineCap: 'round', dashArray: '12 8' }} />
        )}
        {highlightRoute && routePolyline(highlightRoute).length > 1 && (
          <Polyline positions={routePolyline(highlightRoute)}
            pathOptions={{ color: '#4fd1c5', weight: 6, opacity: 0.95, lineCap: 'round' }} />
        )}

        {nodes.map(n => (
          <CircleMarker
            key={n.id}
            center={[n.latitude, n.longitude]}
            radius={9}
            pathOptions={{
              color: n.id === sourceId ? '#4fd1c5' : n.id === destinationId ? '#fbbf24' : '#e6ecff',
              weight: 3,
              fillColor: n.id === sourceId ? '#4fd1c5' : n.id === destinationId ? '#fbbf24' : '#1a2447',
              fillOpacity: 0.95,
            }}
            eventHandlers={{ click: () => onNodeClick(n.id) }}
          >
            <Popup>
              <div style={{ color: '#0a1030' }}>
                <div><b>Node</b> {n.id}</div>
                <div>Lat: {n.latitude.toFixed(5)}</div>
                <div>Lon: {n.longitude.toFixed(5)}</div>
                <div style={{ marginTop: 6, fontWeight: 700 }}>
                  {selectionTarget === 'source' ? 'Click to set source' : 'Click to set destination'}
                </div>
              </div>
            </Popup>
          </CircleMarker>
        ))}

        {nodes.map(n => {
          const inRoute = highlightRoute?.nodeIds.includes(n.id);
          const inAlt = highlightAltRoute?.nodeIds.includes(n.id);
          if (!inRoute && !inAlt) return null;
          return (
            <Marker key={'lbl-' + n.id} position={[n.latitude, n.longitude]}
              icon={L.divIcon({
                className: '', html: `<div style="
                  font-size: 11px; padding: 2px 6px; border-radius: 999px;
                  background: ${inRoute ? '#4fd1c5' : '#667eea'}; color: #0a1030;
                  font-weight: 700; transform: translate(-50%, -140%);
                  box-shadow: 0 4px 10px rgba(0,0,0,.25); white-space: nowrap;">${n.id}</div>`,
                iconSize: [0, 0]
              })} />
          );
        })}
      </MapContainer>
    </div>
  );
}
