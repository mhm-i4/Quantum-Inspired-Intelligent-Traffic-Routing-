import java.util.*;
import com.qpso.traffic.graph.model.*;
import com.qpso.traffic.traffic.model.*;
import com.qpso.traffic.routing.encoding.*;
import com.qpso.traffic.routing.model.*;
import com.qpso.traffic.qpso.*;
public class CheckRoute {
  public static void main(String[] args) {
    TransportGraph g = SampleNetwork.create();
    NetworkTrafficState traffic = new NetworkTrafficState();
    new TrafficSimulator(g).apply(traffic, TrafficScenario.NORMAL);
    ObjectiveWeights w = ObjectiveWeights.DEFAULT;
    var decoder = new ContinuousRouteDecoder(g, "Alexanderplatz", "Neukoelln", 32, e -> {
      double tt = traffic.currentTravelTime(e.id(), e.freeFlowTravelTimeMin());
      double cong = traffic.congestionOf(e.id());
      return w.alphaTravelTime() * tt + w.betaDistance() * e.distanceKm() + w.gammaCongestion() * cong;
    });
    var fitFn = new RouteFitnessFunction(decoder, w, route -> { double s=0; for (String e : route.edgeIds()) s += traffic.congestionOf(e); return s; });
    QPSOOptimizer q = new QPSOOptimizer(40, QPSOOptimizer.QPSOParams.DEFAULT);
    var res = q.optimize(fitFn, decoder.dimensions(), 0.0, 1.0, 250, 7L);
    System.out.println("best=" + res.bestFitness());
    System.out.println("first=" + res.convergenceHistory().get(0));
    System.out.println("last=" + res.convergenceHistory().get(res.convergenceHistory().size()-1));
    System.out.println("histSize=" + res.convergenceHistory().size());
    System.out.println("unique=" + new java.util.HashSet<>(res.convergenceHistory()).size());
    System.out.println("dec=" + decoder.decode(res.bestSolution()));
  }
}
