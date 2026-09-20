package com.qpso.traffic.api;

import com.qpso.traffic.api.web.TrafficController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired MockMvc mvc;

    @Test
    void networkEndpointReturnsNodesAndEdges() throws Exception {
        mvc.perform(get("/api/network"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.edges", hasSize(greaterThanOrEqualTo(12))));
    }

    @Test
    void trafficScenarioSimulationChangesState() throws Exception {
        String body = """
                {"scenario":"MORNING_PEAK"}""";
        mvc.perform(post("/api/traffic/simulate").content(body).contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].congestionLevel").exists());
    }

    @Test
    void optimizeRouteReturnsQPSOResult() throws Exception {
        String body = """
                {"source":"Alexanderplatz","destination":"Neukoelln","algorithm":"QPSO",
                 "iterations":80,"seed":7,"alpha":1.0,"beta":0.5,"gamma":2.0}""";
        mvc.perform(post("/api/route/optimize").content(body).contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.algorithm").value("QPSO"))
                .andExpect(jsonPath("$.bestRoute.nodeIds[0]").value("Alexanderplatz"))
                .andExpect(jsonPath("$.bestRoute.nodeIds[-1]").value("Neukoelln"))
                .andExpect(jsonPath("$.bestFitness").isNumber())
                .andExpect(jsonPath("$.convergenceHistory", hasSize(greaterThan(10))))
                .andExpect(jsonPath("$.dijkstraReference").exists());
    }

    @Test
    void benchmarkEndpointRunsMultipleAlgos() throws Exception {
        String body = """
                {"problem":"sphere","dimensions":4,"trials":3,"iterations":40,
                 "algorithms":["QPSO","PSO","GA"]}""";
        mvc.perform(post("/api/benchmark").content(body).contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.problem").value("sphere"))
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.results[*].algorithm", containsInAnyOrder("QPSO", "PSO", "GA")))
                .andExpect(jsonPath("$.results[*].best").exists());
    }

    @Test
    void rerouteEndpointComparesOldAndNew() throws Exception {
        String body = """
                {"source":"Alexanderplatz","destination":"Neukoelln","algorithm":"PSO",
                 "iterations":60,"alpha":1.0,"beta":0.5,"gamma":2.0}""";
        mvc.perform(post("/api/route/reroute").content(body).contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oldRoute").exists())
                .andExpect(jsonPath("$.newRoute").exists())
                .andExpect(jsonPath("$.oldFitness").isNumber())
                .andExpect(jsonPath("$.newFitness").isNumber())
                .andExpect(jsonPath("$.changedEdges").isArray());
    }
}
