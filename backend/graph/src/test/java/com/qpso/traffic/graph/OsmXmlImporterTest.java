package com.qpso.traffic.graph;

import com.qpso.traffic.graph.io.OsmXmlImporter;
import com.qpso.traffic.graph.model.TransportGraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OsmXmlImporterTest {
    @Test
    void importsRoadNodesAndDirections() throws Exception {
        String osm = """
                <osm version=\"0.6\">
                  <node id=\"1\" lat=\"52.5200\" lon=\"13.4050\"/>
                  <node id=\"2\" lat=\"52.5210\" lon=\"13.4060\"/>
                  <node id=\"3\" lat=\"52.5220\" lon=\"13.4070\"/>
                  <way id=\"10\">
                    <nd ref=\"1\"/><nd ref=\"2\"/><nd ref=\"3\"/>
                    <tag k=\"highway\" v=\"residential\"/>
                    <tag k=\"maxspeed\" v=\"30\"/>
                  </way>
                </osm>
                """;

        TransportGraph graph = OsmXmlImporter.importGraph(
                new ByteArrayInputStream(osm.getBytes(StandardCharsets.UTF_8)));

        assertEquals(3, graph.nodeCount());
        assertEquals(4, graph.edgeCount(), "two road segments imported in both directions");
        assertTrue(graph.hasNode("osm-1"));
        assertEquals(30.0, graph.findEdge("osm-1", "osm-2").orElseThrow().freeFlowSpeedKmh());
    }

    @Test
    void respectsOneWayRoads() throws Exception {
        String osm = """
                <osm version=\"0.6\">
                  <node id=\"1\" lat=\"52.5200\" lon=\"13.4050\"/>
                  <node id=\"2\" lat=\"52.5210\" lon=\"13.4060\"/>
                  <way id=\"11\">
                    <nd ref=\"1\"/><nd ref=\"2\"/>
                    <tag k=\"highway\" v=\"primary\"/>
                    <tag k=\"oneway\" v=\"yes\"/>
                  </way>
                </osm>
                """;

        TransportGraph graph = OsmXmlImporter.importGraph(
                new ByteArrayInputStream(osm.getBytes(StandardCharsets.UTF_8)));

        assertEquals(1, graph.edgeCount());
        assertTrue(graph.findEdge("osm-1", "osm-2").isPresent());
        assertTrue(graph.findEdge("osm-2", "osm-1").isEmpty());
    }
}