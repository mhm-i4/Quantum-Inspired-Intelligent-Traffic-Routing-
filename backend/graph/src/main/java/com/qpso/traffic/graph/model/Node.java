package com.qpso.traffic.graph.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record Node(
        String id,
        double latitude,
        double longitude,
        Map<String, String> metadata
) {
    public Node(String id, double latitude, double longitude) {
        this(id, latitude, longitude, Collections.emptyMap());
    }

    public Node withMeta(String key, String value) {
        Map<String, String> m = new HashMap<>(metadata);
        m.put(key, value);
        return new Node(id, latitude, longitude, Collections.unmodifiableMap(m));
    }
}
