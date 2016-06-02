package spinworld.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import spinworld.facts.Allocation;
import spinworld.facts.Particle;

public class Network {
	
	// Unique network ID
	final int id;
		
	// Allocation method for a network
	Allocation allocationMethod;
	
	// Monitoring parameters within network
	double monitoringLevel = 0.0;
	double monitoringCost = 0.0;
	
	double severityLB = 0.2;
	double severityUB = 0.8;
	
	// Number of warnings a network will give to a particle as punishment
	int noWarnings = 3;
	
	// List of particles that are banned from the network
	Set<Particle> bannedParticles = new HashSet<Particle>();
	Map<Particle, Integer> warnHistory = new HashMap<Particle, Integer>();
	
	public Network(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
	}
	
	public Network(int id, Allocation allocationMethod, 
			double monitoringLevel, double monitoringCost, int noWarnings) {
		this(id, allocationMethod);
		this.monitoringLevel = monitoringLevel;
		this.monitoringCost = monitoringCost;
		this.noWarnings = noWarnings;
	}
	
	public Network(int id, Allocation allocationMethod, double monitoringLevel,
			double monitoringCost, int noWarnings, double severityLB, double severityUB) {
		this(id, allocationMethod);
		this.monitoringLevel = monitoringLevel;
		this.monitoringCost = monitoringCost;
		this.noWarnings = noWarnings;
		this.severityLB = severityLB;
		this.severityUB = severityUB;
	}
	
	@Override
	public String toString() {
		return "Network " + id;
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
	
	public void setMonitoringLevel(double mFreq) {
		this.monitoringLevel = mFreq;
	}

	// Normalise against average provision
	public double getMonitoringCost() {
		return monitoringCost * 0.5;
	}
	
	public int getNoWarnings() {
		return this.noWarnings;
	}
	
	public void warn(Particle p) {
		if (warnHistory.containsKey(p))
			warnHistory.put(p, warnHistory.get(p) + 1);
		else
			warnHistory.put(p, 1);
	}
	
	public void removeWarning(Particle p) {
		if (warnHistory.containsKey(p))
			warnHistory.put(p, warnHistory.get(p) - 1);
		else
			warnHistory.remove(p);
	}
	
	public int getWarningCount(Particle p) {
		if (warnHistory.containsKey(p))
			return warnHistory.get(p);
		else
			return 0;
	}
	
	public double getSeverityLB() {
		return this.severityLB;
	}
	
	public double getSeverityUB() {
		return this.severityUB;
	}
	
	public void banParticle(Particle p) {
		bannedParticles.add(p);
	}
	
	public int getNoBannedParticles() {
		if (!bannedParticles.isEmpty())
			return bannedParticles.size();
		else
			return 0;
	}
	
	public boolean isBanned(Particle p) {
		if (bannedParticles.contains(p))
			return true;
		else
			return false;
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
