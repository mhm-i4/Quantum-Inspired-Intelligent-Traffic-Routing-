package com.qpso.traffic.graph.model;

/**
 * Deterministic 4x4 Berlin landmark network used for repeatable tests.
 *
 * <pre>
 *   Alexanderplatz -- MuseumIsland -- HackescherMarkt -- PrenzlauerBerg
 *          |              |                 |                 |
 *   Nikolaiviertel -- UnterDenLinden -- Tiergarten -- Charlottenburg
 *          |              |                 |                 |
 *   Gendarmenmarkt -- PotsdamerPlatz -- Kreuzberg -- Tempelhof
 *          |              |                 |                 |
 *   Friedrichshain -- Oberbaumbruecke -- TreptowerPark -- Neukoelln
 * </pre>
 *
 * All roads are two-way (both directions inserted) with realistic speeds/distances.
 */
public final class SampleNetwork {
    private SampleNetwork() {}

    public static TransportGraph create() {
        TransportGraph g = new TransportGraph();

        String[][] names = {
                {"Alexanderplatz", "MuseumIsland", "HackescherMarkt", "PrenzlauerBerg"},
                {"Nikolaiviertel", "UnterDenLinden", "Tiergarten", "Charlottenburg"},
                {"Gendarmenmarkt", "PotsdamerPlatz", "Kreuzberg", "Tempelhof"},
                {"Friedrichshain", "Oberbaumbruecke", "TreptowerPark", "Neukoelln"}
        };
        double[][] latitudes = {
                {52.5200, 52.5220, 52.5240, 52.5320},
                {52.5200, 52.5160, 52.5140, 52.5050},
                {52.5130, 52.5090, 52.5000, 52.4800},
                {52.5150, 52.5010, 52.4930, 52.4800}
        };
        double[][] longitudes = {
                {13.4050, 13.4080, 13.4110, 13.4400},
                {13.4080, 13.3880, 13.3500, 13.3050},
                {13.3920, 13.3750, 13.4200, 13.4000},
                {13.4550, 13.4450, 13.4600, 13.4350}
        };
        for (int row = 0; row < names.length; row++) {
            for (int column = 0; column < names[row].length; column++) {
                g.addNode(new Node(names[row][column], latitudes[row][column], longitudes[row][column]));
            }
        }
        for (int row = 0; row < names.length; row++) {
            for (int column = 0; column < names[row].length; column++) {
                if (column + 1 < names[row].length) {
                    addTwoWay(g, names[row][column] + "-" + names[row][column + 1],
                            names[row][column], names[row][column + 1], 0.45 + column * 0.08, 45.0 + row * 5.0);
                }
                if (row + 1 < names.length) {
                    addTwoWay(g, names[row][column] + "-" + names[row + 1][column],
                            names[row][column], names[row + 1][column], 0.55 + row * 0.07, 40.0 + column * 4.0);
                }
            }
        }
        // A few faster diagonals create competing routes for the optimizer to explore.
        addTwoWay(g, "Alexanderplatz-UnterDenLinden", "Alexanderplatz", "UnterDenLinden", 0.70, 65.0);
        addTwoWay(g, "MuseumIsland-Tiergarten", "MuseumIsland", "Tiergarten", 0.85, 65.0);
        addTwoWay(g, "Gendarmenmarkt-Kreuzberg", "Gendarmenmarkt", "Kreuzberg", 0.80, 60.0);
        addTwoWay(g, "PotsdamerPlatz-TreptowerPark", "PotsdamerPlatz", "TreptowerPark", 1.10, 60.0);

        return g;
    }

    public static TransportGraph createGrid(int rows, int cols,
                                            double originLat, double originLon,
                                            double stepKm) {
        TransportGraph g = new TransportGraph();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String id = "N" + r + "_" + c;
                double lat = originLat + (r * stepKm / 111.0);
                double lon = originLon + (c * stepKm / 111.0);
                g.addNode(new Node(id, lat, lon));
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String here = "N" + r + "_" + c;
                if (c + 1 < cols) {
                    String right = "N" + r + "_" + (c + 1);
                    addTwoWay(g, here + "-" + right, here, right, stepKm, 50.0);
                }
                if (r + 1 < rows) {
                    String down = "N" + (r + 1) + "_" + c;
                    addTwoWay(g, here + "-" + down, here, down, stepKm, 40.0);
                }
            }
        }
        return g;
    }

    private static void addTwoWay(TransportGraph g, String baseId,
                                  String from, String to, double distKm, double speed) {
        g.addEdge(RoadEdge.create(baseId + "_F", from, to, distKm, speed));
        g.addEdge(RoadEdge.create(baseId + "_B", to, from, distKm, speed));
    }
}
