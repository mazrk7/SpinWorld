package spinworld.gui;

import java.awt.Container;
import java.util.ArrayList;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class NetworkGraph extends Container {
    
    static final long serialVersionUID = 420007L;

	final PersistentSimulation sim;
	
    // Graph<V, E> where V is the type of the vertices and E is the type of the edges
	private UndirectedSparseGraph<String, String> graph;
		  	
	NetworkGraph(PersistentSimulation sim) {
		this.sim = sim;		
		graph = new UndirectedSparseGraph<String, String>();
	}
	
	public UndirectedSparseGraph<String, String> getGraph() {
		return graph;
	}
	
	public void updateGraph(int t, FRLayout<String, String> layout) {		
		// Clear graph of vertices
		ArrayList<String> toRemoveV = new ArrayList<String>(graph.getVertices());
		for (String v : toRemoveV) {
		  graph.removeVertex(v);
		}
		
		// Clear graph of edges
		ArrayList<String> toRemoveE = new ArrayList<String>(graph.getEdges());
		for (String e : toRemoveE) {
		  graph.removeEdge(e);
		}
		
		// double relation = Double.parseDouble(sim.getParameters().get("size"));
		// double xScale = layout.getSize().getWidth();
		// double yScale = layout.getSize().getHeight();

		for (PersistentAgent a : sim.getAgents()) {
			TransientAgentState as = a.getState(t);
			
			if (as != null && as.getProperty("network") != null) {
				String aNet = as.getProperty("network");
				if (!graph.containsVertex(a.getName())) {
					graph.addVertex(a.getName());
					// IF PHYSICAL LOCATIONS
					/* double x = (Double.parseDouble(as.getProperty("x")) + Random.randomDouble())/relation;
					x = (x > relation) ? relation * xScale : x * xScale;
					double y = (Double.parseDouble(as.getProperty("y")) + Random.randomDouble())/relation;
					y = (y > relation) ? relation * yScale : y * yScale;
					layout.setLocation(a.getName(), x, y);
					layout.lock(a.getName(), true); */
				}

				if (!aNet.equals("-1")) {
					for (PersistentAgent b : sim.getAgents()) {
						if (!a.equals(b)) {
							TransientAgentState bs = b.getState(t);
							
							if (bs != null && bs.getProperty("network") != null
									&& bs.getProperty("network").equals(aNet)) {
								if (!graph.containsVertex(b.getName())) {
									graph.addVertex(b.getName());
									// IF PHYSICAL LOCATIONS
									/* double x = (Double.parseDouble(bs.getProperty("x")) + Random.randomDouble())/relation;
									x = (x > relation) ? relation * xScale : x * xScale;
									double y = (Double.parseDouble(bs.getProperty("y")) + Random.randomDouble())/relation;
									y = (y > relation) ? relation * yScale : y * yScale;
									layout.setLocation(b.getName(), x, y);
									layout.lock(b.getName(), true); */
								}
								
								String edge = a.getName() + "-N" + aNet + "-" + b.getName();
								if (!graph.containsEdge(edge))
									graph.addEdge(edge, a.getName(), b.getName());
							}
						}
					}
				}
			}	
		}
	}

}
