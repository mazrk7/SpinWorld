package spinworld.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import spinworld.facts.Allocation;
import spinworld.facts.Particle;

public class Network {
	
	// Unique network ID
	final int id;
		
	// Allocation method for a network
	Allocation allocationMethod;
	
	// DoF to have no monitoring
	double monitoringLevel = 0.0;
	double monitoringCost = 1.0;
	
	double severityLB = 0.2;
	double severityUB = 1.0;
	
	// Number of levels before expulsion
	int noWarnings = Integer.MAX_VALUE;
	
	// No conflict-resoluton mechanism
	double forgiveness = 1.0;
	
	int compliantRounds = 0;
	
	int lifespan = 0;
	
	final SummaryStatistics utilityData;
	
	// List of particles that are banned from the network
	Set<Particle> bannedParticles = new HashSet<Particle>();
	Map<Particle, Integer> warnHistory = new HashMap<Particle, Integer>();
	
	public Network(int id, Allocation allocationMethod) {
		super();
		this.id = id;
		this.allocationMethod = allocationMethod;
		this.utilityData = new SummaryStatistics();
	}
	
	public Network(int id, Allocation allocationMethod, 
			double monitoringLevel, double monitoringCost, int noWarnings, 
			double severityLB, double severityUB, double forgiveness) {
		this(id, allocationMethod);
		this.monitoringLevel = monitoringLevel;
		this.monitoringCost = monitoringCost;
		this.noWarnings = noWarnings;
		this.severityLB = severityLB;
		this.severityUB = severityUB;
		this.forgiveness = forgiveness;
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
	
	public int getNoWarnings() {
		return this.noWarnings;
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
		
	public void warn(Particle p) {
		if (warnHistory.containsKey(p))
			warnHistory.put(p, warnHistory.get(p) + 1);
		else
			warnHistory.put(p, 1);
	}
	
	public void removeWarning(Particle p) {
		if (warnHistory.containsKey(p) && warnHistory.get(p) > 1)
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
	
	public double getForgiveness() {
		return this.forgiveness;
	}
	
	public int getCompliantRounds() {
		return this.compliantRounds;
	}
	
	public void setCompliantRounds(int count) {
		this.compliantRounds = count;
	}
	
	public int getLongevity() {
		return this.lifespan;
	}
	
	public void incrementLongevity() {
		this.lifespan++;
	}
	
	public SummaryStatistics getUtilityData() {
		return utilityData;
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
