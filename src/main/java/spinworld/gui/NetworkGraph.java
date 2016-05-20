package spinworld.gui;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
	
	public void updateGraph(int t) {		
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
			
		for (PersistentAgent a : sim.getAgents()) {
			TransientAgentState s = a.getState(t);
			
			if (s != null && s.getProperty("links") != null
					&& s.getProperty("network") != null) {
				String links = s.getProperty("links");
				String netID = s.getProperty("network").toString();
				
				if (!links.equals("") && !netID.equals("-1")) {
					List<String> linksArr = Arrays.asList(links.split(","));
					
					if (!graph.containsVertex(a.getName()))
						graph.addVertex(a.getName());
					
			        for (String link : linksArr) {
						if (!graph.containsVertex(link))
							graph.addVertex(link);
						
						if (!graph.containsEdge(a.getName() + "_N" + netID + "_" + link))
							graph.addEdge(a.getName() + "_N" + netID + "_" + link, a.getName(), link);	
					}
				}
			}				
		}
	}

}
