package spinworld.network;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import spinworld.facts.Allocation;
import spinworld.facts.Particle;
import spinworld.network.Network;

public class Network {
	
	// Unique network ID
	final int id;
	Set<Connection> connections = new CopyOnWriteArraySet<Connection>();
	
	// Allocation method for a network
	Allocation allocationMethod;
	
	double monitoringLevel = 0.0;
	double monitoringCost = 0.0;
	
	public Network(int id) {
		super();
		this.id = id;
	}
	
	public Network(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
	}
	
	public Network(int id, Allocation allocationMethod, double monitoringLevel,
			double monitoringCost) {
		this(id, allocationMethod);
		this.monitoringLevel = monitoringLevel;
		this.monitoringCost = monitoringCost;
	}
	
	@Override
	public String toString() {
		return "Social Network [id=" + id + ", connections=" + connections.size() + "]";
	}

	public int getId() {
		return id;
	}
	
	public void addConnection(Connection conn) {
		connections.add(conn);
	}
	
	public void removeConnection(Connection conn) {
		connections.remove(conn);
	}
	
	public boolean containsParticle(Particle p) {
		boolean result = false;
		
		for (Connection conn : connections) {
			if (conn.containsParticle(p)) {
				result = true;
				break;
			}
			else {
				// Do nothing
			}	
		}
		
		return result;
	}
	
	public boolean connectionExists(Connection conn) {
		return connections.contains(conn);
	}
	
	public Allocation getAllocationMethod() {
		return allocationMethod;
	}
	
	public double getMonitoringLevel() {
		return monitoringLevel;
	}

	// Normalise against average provision
	public double getMonitoringCost() {
		return monitoringCost * 0.5;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Network other = (Network) obj;
		if (id != other.id)
			return false;
		return true;
	}

}
