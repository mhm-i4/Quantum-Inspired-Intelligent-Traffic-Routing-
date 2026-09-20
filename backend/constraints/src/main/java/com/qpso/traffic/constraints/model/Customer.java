package com.qpso.traffic.constraints.model;

import com.qpso.traffic.graph.model.Node;
import com.qpso.traffic.graph.model.TransportGraph;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record Customer(
        String id,
        String nodeId,
        double demandKg,
        double serviceTimeMin,
        Optional<TimeWindow> timeWindow
) {
    public record TimeWindow(Instant open, Instant close) {
        public boolean violated(Instant arrival) {
            return arrival.isBefore(open) || arrival.isAfter(close);
        }
        public static TimeWindow ofMinutesFromNow(int openMin, int closeMin) {
            Instant now = Instant.now();
            return new TimeWindow(now.plus(Duration.ofMinutes(openMin)),
                    now.plus(Duration.ofMinutes(closeMin)));
        }
    }

    public static Customer atNode(String id, Node node, double demandKg) {
        return new Customer(id, node.id(), demandKg, 5.0, Optional.empty());
    }
    public static Customer atNode(String id, Node node, double demandKg, double serviceTimeMin) {
        return new Customer(id, node.id(), demandKg, serviceTimeMin, Optional.empty());
    }
}
