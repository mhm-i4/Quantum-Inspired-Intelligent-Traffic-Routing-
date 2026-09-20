package com.qpso.traffic.traffic.model;

/**
 * Named traffic scenarios that can be applied to a {@link NetworkTrafficState}.
 */
public enum TrafficScenario {
    NORMAL,
    MORNING_PEAK,
    EVENING_PEAK,
    ACCIDENT,
    RANDOM_DYNAMIC
}
