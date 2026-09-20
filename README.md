# QPSO Traffic Route Optimization Platform
## Quantum-Inspired Intelligent Traffic Route Optimization in Transportation Systems Using Metaheuristic Optimization

> **Important disclaimer (scientific honesty):** This software does **not** run on a quantum computer. *QPSO* here means **quantum-inspired Particle Swarm Optimization**, a classical metaheuristic that borrows probability-distribution ideas from the quantum delta-potential-well model. No claims of global-optimum guarantees are made — results are empirically observed **near-optimal / high-quality solutions**. We do, however, **fairly compare** QPSO against classical baselines (Dijkstra, standard PSO, real-coded GA) using statistical multi-trial benchmarks.

---

## 1. Problem Formulation

We seek a near-optimal path through a transportation network `G = (V, E)` under **dynamic edge travel times**. Each edge has a base (free-flow) travel time and a real-time congestion value `c ∈ [0, 1]`. A vehicle must travel from a source node `s ∈ V` to a destination node `d ∈ V` while minimizing a configurable multi-objective cost:

`F(R) = α · totalTravelTime(R) + β · totalDistance(R) + γ · totalCongestion(R) + Σ penalties(R)`

(See §4 for the exact objective-function interface.)

The Vehicle Routing Problem (VRP) variant additionally assigns a fleet of vehicles with capacity limits and customer demands.

---

## 2. Graph Model (backend/graph)

* `Node(id, lat, lon, metadata)` — intersections or any network junction.
* `RoadEdge(id, source, destination, distanceKm, freeFlowTravelTimeMin, capacity, freeFlowSpeed)` — **directed** roads. One-way streets are encoded as a single directed edge; two-way streets as a symmetric pair of directed edges.
* `TransportGraph` — adjacency list with: `outgoingEdges(u)`, `findEdge(u, v)`, node/edge lookups.
* `SampleNetwork` — deterministic 4×4 Berlin landmark network with 16 named nodes and multiple competing routes.

```
    A -- B -- C
    |    |    |
    D -- E -- F
```

Additional faster diagonal links create alternative routes. A grid generator is also provided for scalability tests (`SampleNetwork.createGrid(rows, cols, …)`).

---

## 3. Traffic Model (backend/traffic)

* `TrafficState(edgeId, congestionLevel, currentTravelTimeMin, timestamp)` — edge-level conditions.
* `CongestionModel(k, n)` — BPR-style curve:
  `factor(c) = 1 + k · c^n`,  `c ∈ [0, 1]`.
  Default `k=5, n=2`; this is configurable.
* `NetworkTrafficState` — holds per-edge states and exposes:
  * `currentTravelTime(edgeId, freeFlow) = freeFlow · factor(c)`
  * `congestionCostOfEdge(edgeId)` — used by the objective function
* `TrafficScenario` enum + `TrafficSimulator.apply(state, scenario)`:
  * `NORMAL` — light congestion `c ∈ [0.05, 0.20]`
  * `MORNING_PEAK` / `EVENING_PEAK` — medium-high `c ∈ [0.30, 0.70]`
  * `ACCIDENT` — one edge forced to `c ≥ 0.98`; neighborhood edges elevated
  * `RANDOM_DYNAMIC` — `c = U(0,1)^2.5` (light-biased, occasional spikes)

**Veridical fitness effect:** changes in `TrafficState` propagate into the `EdgeCostFunction` used by Dijkstra, the `ContinuousRouteDecoder`, and the `RouteFitnessFunction`. Congestion is not just UI decoration — it actually perturbs the fitness landscape and the optimizer adapts.

### Using a small OpenStreetMap area

The API can load a bounded `.osm` XML extract while keeping the existing simulated traffic scenarios. To try a small city neighborhood:

1. Use the OpenStreetMap Export function to download a small bounding box. Keep the extract to a neighborhood or a few city blocks for fast optimization.
2. Save the file as `backend/api/src/main/resources/city.osm`.
3. Set this property in `backend/api/src/main/resources/application.properties`:

  `app.graph.osm-resource=city.osm`

4. Rebuild and restart the backend from the repository root:

  `mvn clean install`

  `mvn -pl backend/api spring-boot:run`

The importer reads drivable `highway` ways, respects `oneway` and `maxspeed` tags, calculates geographic edge distances, and maps the result into the same `TransportGraph` used by QPSO, PSO, GA, and Dijkstra. Leave the property blank to use the deterministic 4×4 sample graph. Traffic remains simulated through the existing dashboard scenarios.

---

## 4. Objective Function

Interface: `FitnessFunction<S>` in `backend/optimization/core/FitnessFunction.java`.
For routing we use the specialized `RouteFitnessFunction` which produces a `FitnessBreakdown` exposing every term:

| Component | Formula | Default weight |
|---|---|---|
| Travel-time cost | α · Σ_e currentTravelTime(e) | α = 1.0 |
| Distance cost    | β · Σ_e distance(e)           | β = 0.5 |
| Congestion cost  | γ · Σ_e congestionLevel(e)    | γ = 2.0 |
| Penalties        | (per-constraint penalties)    | — |
| **Total fitness (minimize)** | sum of the above | — |

Each component is stored separately and returned through the REST API so the UI can render stacked cost breakdowns.

---

## 5. QPSO Mathematical Formulation (backend/qpso)

### Why QPSO runs on classical hardware
"Quantum" describes the *inspiration* only: we sample each particle's next position from a probability distribution (inverse-CDF of a double-exponential / delta-potential-well), which we compute classically. No quantum gates or qubits.

### State variables for particle `i`, dimension `d`
- `x_{i,d}(t)` — current position (continuous value in `[lower, upper]`)
- `pbest_{i,d}` — personal best position ever visited
- `gbest_d` — global best position across the whole swarm (only personal bests considered)
- `mbest_d` — **mean best position** = centroid of the personal bests:
  ```
  mbest_d = (1 / N) · Σ_i pbest_{i,d}
  ```

### Update equations (Sun et al., 2004)

**1. Local attractor (per particle, per dim, per iteration):**
```
u ~ Uniform(0,1)
p_{i,d} = u · pbest_{i,d} + (1 − u) · gbest_d
```

**2. Contraction–Expansion (CE) coefficient schedule (linear):**
```
α(t) = α_start − (α_start − α_end) · (t / T_max)
Default: α_start = 1.0, α_end = 0.5
```

**3. Quantum-inspired sampling (core difference from PSO):**
```
L_{i,d} = α(t) · | mbest_d − x_{i,d}(t) |

sgn  = +1   with probability 0.5,
       −1   with probability 0.5

r    ~ Uniform(0, 1)   (re-sampled per (i,d,t))

x_{i,d}(t+1) = p_{i,d} + sgn · L_{i,d} · ln(1 / r)
```
Then clamp to box bounds `[lower, upper]`.

### Key structural differences vs. standard PSO
| Standard PSO (`backend/pso/StandardPSO.java`) | QPSO (`backend/qpso/QPSOOptimizer.java`) |
|---|---|
| **Velocity** vector exists and is integrated (`x ← x + v`) | **No velocity**. Position sampled directly from probability well |
| Three tuneable parameters: w, c₁, c₂ | Single tuneable parameter: α(t) |
| Update uses personal best + global best only | Also uses **mbest** (centroid of personal bests) and **local attractor** |
| Additive deterministic form (after r₁, r₂ sampled) | Stochastic via `ln(1/r)` + random sign |

### Validation against mathematical benchmarks (Phase 1 — mandatory)
The QPSO engine is first validated on Sphere, Rastrigin, Rosenbrock before being used for routing. See `backend/qpso/src/test/.../QPSOOptimizerTest.java`.

---

## 6. Discrete Encoding Layer (backend/routing/encoding)

PSO/QPSO/GA operate on **continuous positions** in `[0,1]^D`, but a route is a **discrete walk** over the graph. We use an explicit **random-key decoder** so the optimizer can still compute gradients-of-a-sort and explore locally:

* The search dimension D = user-configured `maxSteps` (default `2·|V|`).
* At each step `k ∈ [0, D)` the walker stands at node `u`.
* Let `C = sorted(outgoingEdges(u))` (sorted by id for determinism).
* Decoder chooses `C[ ⌊ position[k] · |C| ⌋ ]`, clamped.
* Simple cycle-avoidance: if the chosen destination has already been visited and an unvisited alternative exists, switch to the lowest-cost unvisited edge.
* If the walker has not reached the destination after `D` steps, we append a Dijkstra fallback from the last node back to the destination, plus a large penalty `1000 · (steps_exceeded)`. This guarantees feasibility of the decoded path while strongly biasing the optimizer toward shorter walks.

**Extensibility:** `SolutionDecoder<D>` is a generic interface — alternative encodings (priority-list for VRP, permutation-based TSP encoding, etc.) can be added without modifying the optimizer modules.

---

## 7. Constraint Handling (backend/constraints)

Implemented in `VRPConstraintChecker`. Hard constraints are relaxed into **soft penalties** so the metaheuristic can still score infeasible points during search. Violations are recorded and returned as part of the solution metadata rather than silently dropping infeasible solutions.

| Constraint | Penalty |
|---|---|
| Vehicle capacity exceeded | `1000 $/kg` × excess |
| Route duration > max allowed | `500 $/min` × excess |
| Route does not start/end at depot | `50,000` each |
| Customer time-window violated (optional) | `10,000` per customer |

---

## 8. Baseline Algorithms

| Name | Purpose | File |
|---|---|---|
| **Dijkstra** | Exact polynomial-time shortest path for single vehicles on static edge weights. Used as a reference solution whenever the edge weights form a standard deterministic shortest-path problem. | `backend/routing/algorithms/DijkstraSolver.java` |
| **Standard PSO** | Classical velocity-based PSO with linearly decaying inertia (`w: 0.9 → 0.4`, c₁ = c₂ = 2.0). Solves the same continuous encoding as QPSO. | `backend/pso/StandardPSO.java` |
| **Real-coded GA** | Tournament selection, BLX-α blend crossover, Gaussian mutation, elitism (1 survivor). Alternative evolutionary baseline. | `backend/ga/RealCodedGA.java` |

All three population-based optimizers implement the same `OptimizationAlgorithm<double[]>` interface, so they can be drop-in swapped in the routing facade and in benchmarks.

---

## 9. Benchmark Methodology

### Multi-trial statistics
Every benchmark runs the same algorithm × problem combination for N independent trials with incrementing seeds (`seed = baseSeed + trialIndex`). Reported per algorithm:

| Metric | Definition |
|---|---|
| **best** | best fitness seen across all trials |
| **worst** | worst fitness |
| **mean** | arithmetic mean |
| **stdDev** | population standard deviation |
| **meanRuntimeMs** | mean wall-clock time |
| **meanEvaluations** | mean number of fitness calls |
| **meanConvergence** | iteration-wise mean of best-so-far curve (for plotting) |

Implemented in `backend/benchmarking/BenchmarkStatistics.java` and `BenchmarkRunner.java`.

### Problem sizes / scalability tests
The REST endpoint `/api/benchmark` exposes the mathematical benchmarks. For routing-scale tests, the routing facade accepts an `iterations` parameter and the `SampleNetwork.createGrid` generator scales to arbitrary grid sizes (e.g., 10×10, 20×20 nodes). The framework deliberately exposes raw statistics — **it never hardcodes a winner**.

---

## 10. Results

Convergence curves and trial statistics are produced by the actual code in real time. The frontend `/benchmark` page runs a requested number of trials for QPSO/PSO/GA on Sphere/Rastrigin/Rosenbrock and renders mean convergence on a log-scale chart. The route-optimization UI shows a full convergence curve for the selected run, along with the per-component objective breakdown and a Dijkstra reference route.

---

## 11. Complexity & Scalability

* **Graph ops**: `Dijkstra` runs in `O(E + V log V)`; per-iteration walk decoding in the metaheuristic is `O(maxSteps · out-degree)`.
* **QPSO / PSO / GA per iteration**: `O(populationSize · dims)` plus `O(populationSize · fitness_evaluation)`.
* **Fitness evaluation for routing** is dominated by a single graph walk, roughly `O(maxSteps)` — independent of graph size after decoding.
* Memory footprint of a run: `O(populationSize · dims + convergenceHistory.length)` (≈ a few MBs per run for typical population sizes of 40–80).

---

## 12. Known Limitations (honest)

* QPSO and the other metaheuristics are **stochastic** — a single run might be unlucky; always look at the multi-trial benchmark statistics.
* The random-key decoder can encode the same walk using many positions (plateaus); adding local-search hill-climbing on decoded routes (e.g., 2-opt, 1-shift) would likely improve VRP solution quality.
* VRP customer time-window enforcement is simplified for the hackathon; a production VRP solver would accumulate stop-to-stop travel durations to validate windows per customer.
* Benchmark problems use synthetic graphs; real-world routing calibrations should be performed using real probe/GPS data before trusting in production.
* Dynamic re-routing is currently triggered on-demand; in a real deployment, one would subscribe to a live traffic stream and re-trigger QPSO whenever edge travel times change significantly.

---

## 13. Project Layout

```
backend/
  optimization/    Core abstractions + Sphere/Rastrigin/Rosenbrock benchmarks
  qpso/            Quantum-inspired PSO (main engine)
  pso/             Standard PSO baseline
  ga/              Real-coded GA baseline
  benchmarking/    Multi-trial statistics and runner
  graph/           Node, RoadEdge, TransportGraph, sample networks
  traffic/         TrafficState, simulator (5 scenarios), CongestionModel
  routing/         Dijkstra, Route, ContinuousRouteDecoder, RouteFitnessFunction, RoutingOptimizer
  constraints/     VRP data model (Vehicle/Customer) + VRPConstraintChecker
  simulation/      Reroute/demo orchestration layer
  api/             Spring Boot REST app + DTOs + endpoints + integration tests
frontend/
  Vite + React 18 + TypeScript + Leaflet (react-leaflet) + Recharts
  src/components/ NetworkMap, RoutePanel, TrafficPanel, ConvergencePanel, BenchmarkPanel, ReroutePanel
```

## 14. Run it locally

```bash
# Backend
mvn -f backend/api/pom.xml spring-boot:run      # listens on :8080

# Frontend
cd frontend && npm install && npm run dev        # listens on :5173, proxies /api → :8080
```

Open `http://localhost:5173` in a browser.
