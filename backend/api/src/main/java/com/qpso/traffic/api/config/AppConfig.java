package com.qpso.traffic.api.config;

import com.qpso.traffic.ga.RealCodedGA;
import com.qpso.traffic.graph.io.OsmXmlImporter;
import com.qpso.traffic.graph.model.SampleNetwork;
import com.qpso.traffic.graph.model.TransportGraph;
import com.qpso.traffic.optimization.core.OptimizationAlgorithm;
import com.qpso.traffic.pso.StandardPSO;
import com.qpso.traffic.qpso.QPSOOptimizer;
import com.qpso.traffic.traffic.model.NetworkTrafficState;
import com.qpso.traffic.traffic.sim.TrafficSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Configuration
public class AppConfig {

    @Bean
    public TransportGraph transportGraph(@Value("${app.graph.osm-resource:}") String osmResource) {
        if (osmResource != null && !osmResource.isBlank()) {
            try {
                return OsmXmlImporter.importGraph(new ClassPathResource(osmResource).getInputStream());
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load OSM graph resource: " + osmResource, e);
            }
        }
        return SampleNetwork.create();
    }

    @Bean
    public NetworkTrafficState networkTrafficState(TransportGraph g) {
        NetworkTrafficState s = new NetworkTrafficState();
        new TrafficSimulator(g).apply(s, com.qpso.traffic.traffic.model.TrafficScenario.NORMAL);
        return s;
    }

    @Bean
    public TrafficSimulator trafficSimulator(TransportGraph g) {
        return new TrafficSimulator(g);
    }

    @Bean
    public Map<String, OptimizationAlgorithm<double[]>> algorithms() {
        Map<String, OptimizationAlgorithm<double[]>> map = new ConcurrentHashMap<>();
        map.put("QPSO", new QPSOOptimizer(50, QPSOOptimizer.QPSOParams.DEFAULT));
        map.put("PSO", new StandardPSO(50, StandardPSO.PSOParams.DEFAULT));
        map.put("GA", new RealCodedGA(80, RealCodedGA.GAParams.DEFAULT));
        return map;
    }
}
