package com.qpso.traffic.api.web;

import com.qpso.traffic.api.dto.DTOs;
import com.qpso.traffic.graph.model.TransportGraph;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NetworkController {

    private final TransportGraph graph;

    public NetworkController(TransportGraph graph) { this.graph = graph; }

    @GetMapping("/network")
    public DTOs.NetworkDTO network() {
        return DTOs.NetworkDTO.of(graph.nodes(), graph.edges());
    }

    @GetMapping("/network/nodes")
    public java.util.List<DTOs.NodeDTO> nodes() {
        return graph.nodes().stream().map(DTOs.NodeDTO::from).toList();
    }

    @GetMapping("/network/edges")
    public java.util.List<DTOs.EdgeDTO> edges() {
        return graph.edges().stream().map(DTOs.EdgeDTO::from).toList();
    }
}
