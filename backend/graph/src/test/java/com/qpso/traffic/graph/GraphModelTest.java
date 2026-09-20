package com.qpso.traffic.graph;

import com.qpso.traffic.graph.model.Node;
import com.qpso.traffic.graph.model.RoadEdge;
import com.qpso.traffic.graph.model.SampleNetwork;
import com.qpso.traffic.graph.model.TransportGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphModelTest {

    @Test
    @DisplayName("Node creation with and without metadata")
    void nodeCreation() {
        Node n = new Node("Start", 52.5, 13.4);
        assertEquals("Start", n.id());
        assertEquals(0, n.metadata().size());
        Node n2 = n.withMeta("type", "intersection");
        assertEquals("intersection", n2.metadata().get("type"));
        assertEquals(0, n.metadata().size(), "original node metadata must be immutable");
    }

    @Test
    @DisplayName("RoadEdge free-flow time equals distance/speed")
    void roadEdgeTimes() {
        RoadEdge e = RoadEdge.create("e1", "Start", "End", 60.0, 60.0);
        assertEquals(60.0, e.freeFlowTravelTimeMin(), 1e-6, "60km @60km/h takes 60 min");
        assertEquals("Start", e.sourceNodeId());
        assertEquals("End", e.destinationNodeId());
    }

    @Test
    @DisplayName("Graph rejects edges with unknown endpoints")
    void graphRejectsUnknownNodes() {
        TransportGraph g = new TransportGraph();
        g.addNode(new Node("Start", 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> g.addEdge(RoadEdge.create("Start-End", "Start", "End", 1.0, 50.0)));
    }

    @Test
    @DisplayName("Sample 4x4 network has expected size and connectivity")
    void sampleNetwork() {
        TransportGraph g = SampleNetwork.create();
        assertEquals(16, g.nodeCount(), "Sixteen named landmarks");
        // 24 grid roads plus 4 diagonals, each represented in both directions.
        assertEquals(56, g.edgeCount(), "two-way roads");
        assertTrue(g.hasNode("Alexanderplatz") && g.hasNode("Neukoelln"));
        // Alexanderplatz has two grid links and one diagonal shortcut.
        assertEquals(3, g.outgoingEdges("Alexanderplatz").size());
    }

    @Test
    @DisplayName("Directed edges have separate forward and reverse ids")
    void directedEdgesSeparateIds() {
        TransportGraph g = SampleNetwork.create();
        assertTrue(g.findEdge("Alexanderplatz", "MuseumIsland").isPresent());
        assertTrue(g.findEdge("MuseumIsland", "Alexanderplatz").isPresent());
        assertNotEquals(g.findEdge("Alexanderplatz", "MuseumIsland").get().id(),
                g.findEdge("MuseumIsland", "Alexanderplatz").get().id());
    }

    @Test
    @DisplayName("SampleNetwork.createGrid builds a rectangular grid")
    void gridNetwork() {
        TransportGraph g = SampleNetwork.createGrid(3, 3, 0, 0, 1.0);
        assertEquals(9, g.nodeCount());
        // 3x3 grid: each interior node has up to 4 outgoing edges;
        // 2 horizontal per row of 3 = 2*3 = 6, each 2-way = 12
        // 2 vertical per col of 3 = 2*3 = 6, each 2-way = 12
        // Total = 24 directed edges
        assertEquals(24, g.edgeCount());
    }
}
