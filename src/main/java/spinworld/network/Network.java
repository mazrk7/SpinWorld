package spinworld.network;

import spinworld.facts.Allocation;

public class Network {
	
	// Unique network ID
	final int id;
	
	// Allocation method for a network
	Allocation allocationMethod;
	
	// Monitoring parameters within network
	double monitoringLevel = 0.0;
	double monitoringCost = 0.0;
	
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
		return "Network " + id + "";
	}

	public int getId() {
		return id;
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
