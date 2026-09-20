package com.qpso.traffic.api.web;

import com.qpso.traffic.api.dto.DTOs;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.traffic.model.NetworkTrafficState;
import com.qpso.traffic.traffic.model.TrafficScenario;
import com.qpso.traffic.traffic.sim.TrafficSimulator;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "*")
public class TrafficController {

    private final NetworkTrafficState traffic;
    private final TrafficSimulator simulator;
    private final TransportGraph graph;

    public TrafficController(NetworkTrafficState traffic, TrafficSimulator simulator, TransportGraph graph) {
        this.traffic = traffic;
        this.simulator = simulator;
        this.graph = graph;
    }

    @GetMapping
    public List<DTOs.TrafficStateDTO> current() {
        return traffic.allStates().values().stream().map(DTOs.TrafficStateDTO::from).toList();
    }

    public record SimulateRequest(String scenario) {}

    @PostMapping("/simulate")
    public List<DTOs.TrafficStateDTO> simulate(@RequestBody SimulateRequest req) {
        TrafficScenario scenario;
        try {
            scenario = TrafficScenario.valueOf(req.scenario().toUpperCase());
        } catch (Exception e) {
            scenario = TrafficScenario.NORMAL;
        }
        simulator.apply(traffic, scenario);
        // Make sure all edges have some state
        for (RoadEdge e : graph.edges()) {
            if (!traffic.allStates().containsKey(e.id())) {
                traffic.setState(e.id(), 0.05, e.freeFlowTravelTimeMin());
            }
        }
        return current();
    }

    public record SetCongestionRequest(String edgeId, double congestionLevel) {}

    @PostMapping("/set")
    public DTOs.TrafficStateDTO setCongestion(@RequestBody SetCongestionRequest r) {
        RoadEdge e = graph.edge(r.edgeId()).orElseThrow(
                () -> new IllegalArgumentException("Unknown edge: " + r.edgeId()));
        traffic.setState(r.edgeId(), Math.max(0, Math.min(1, r.congestionLevel())),
                e.freeFlowTravelTimeMin());
        return DTOs.TrafficStateDTO.from(traffic.stateOf(r.edgeId()));
    }
}
