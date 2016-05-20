package spinworld.facts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import spinworld.GraduationLevel;
import spinworld.network.Network;
import uk.ac.imperial.presage2.util.location.Location;

// Mobile particle in a linear square environment
public class Particle {

	UUID id;
	final String name;
	Location loc;
	
	int velocity = 1;
	
	// Resources generated
	double g = 0;
	// Resources needed
	double q = 0;
	// Resources demanded
	double d = 0;
	
	// Resources allocated and appropriated (used)
	double allocated = 0;
	double appropriated = 0;

	// Alpha and beta are coefficients determining rate of reinforcement
	// of satisfaction and dissatisfaction, respectively
	// Similar concept should be extended for cheating
	double alpha = 0.1;
	double beta = 0.1;
		
	// Size multiplier a.k.a. radius of particle acts as a weighting for resource allocation
	double radius = 1;
	
	Particle toJoin = null;
	Set<Particle> links = new CopyOnWriteArraySet<Particle>();
			
	Map<Network, ArrayList<GraduationLevel>> observedSanctionHistory = new HashMap<Network, ArrayList<GraduationLevel>>();
	Map<Network, ArrayList<Boolean>> observedCatchHistory = new HashMap<Network, ArrayList<Boolean>>();
		
	public Particle(UUID id) {
		super();
		this.id = id;
		this.name = "n/a";
	}
	
	public Particle(UUID id, String name, double alpha, 
			double beta, int velocity, Location loc) {
		super();
		this.id = id;
		this.name = name;
		this.alpha = alpha;
		this.beta = beta;
		this.velocity = velocity;
		this.loc = loc;
	}

	public Particle(UUID id, String name, double alpha, double beta, 
			double radius, int velocity, Location loc) {
		this(id, name, alpha, beta, velocity, loc);
		this.radius = radius;
	}

	@Override
	public String toString() {
		return "Particle [" + name + ", velocity=" + velocity +", g=" + g + ", q=" + q + "]";
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	
	public Location getLoc() {
		return loc;
	}
	
	public void setLoc(Location loc) {
		this.loc = loc;
	}
	
	public int getVelocity() {
		return velocity;
	}
	
	public void setVelocity(int velocity) {
		this.velocity = velocity;
	}
	
	public double getRadius() {
		return radius;
	}
	
	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	public double getQ() {
		return q;
	}

	public void setQ(double q) {
		this.q = q;
	}

	public double getD() {
		return d;
	}

	public void setD(double d) {
		this.d = d;
	}

	public double getAllocated() {
		return allocated;
	}

	public void setAllocated(double allocated) {
		this.allocated = allocated;
	}

	public double getAppropriated() {
		return appropriated;
	}

	public void setAppropriated(double appropriated) {
		this.appropriated = appropriated;
	}
	
	public double getAlpha() {
		return alpha;
	}

	public double getBeta() {
		return beta;
	}
	
	public void toJoin(Particle p) {
		this.toJoin = p;
	}
	
	public Particle getToJoin() {
		return this.toJoin;
	}
	
	public void addLink(Particle p) {
		if (!links.contains(p))
			links.add(p);
	}
	
	public void detachLink(Particle p) {
		if (links.contains(p))
			links.remove(p);
	}
	
	public void detachLinks() {
		if (!links.isEmpty()) {
			for (Particle p : links) {
				p.detachLink(this);
				links.remove(p);
			}
		}
	}
	
	public Set<Particle> getLinks() {
		if (!links.isEmpty())
			return links;
		else
			return null;
	}
	
	public int getNoLinks() {
		if (!links.isEmpty())
			return links.size();
		else
			return 0;
	}
	
	public void updateObservedSanctionHistory(Network net, GraduationLevel sanction) {
		if (observedSanctionHistory.containsKey(net)) 
			observedSanctionHistory.get(net).add(sanction);
		else
			observedSanctionHistory.put(net, new ArrayList<GraduationLevel>(Arrays.asList(sanction)));
	}
	
	public double getObservedRiskRate(Network net) {	
		double riskRate = 0.0;
		
		if (observedSanctionHistory.containsKey(net)) {
			int sanctionCount = Collections.frequency(observedSanctionHistory.get(net), GraduationLevel.WARNING)
					+ Collections.frequency(observedSanctionHistory.get(net), GraduationLevel.EXPULSION);	
			
			riskRate = ((double) sanctionCount)/observedSanctionHistory.get(net).size();
		}
		
		return riskRate;
	}
	
	public void updateObservedCatchHistory(Network net, Boolean cheat) {
		if (observedCatchHistory.containsKey(net)) 
			observedCatchHistory.get(net).add(cheat);
		else
			observedCatchHistory.put(net, new ArrayList<Boolean>(Arrays.asList(cheat)));
	}
	
	public double getObservedCatchRate(Network net) {
		double catchRate = 0.0;
		
		if (observedCatchHistory.containsKey(net)) {
			int catchCount = Collections.frequency(observedCatchHistory.get(net), true);			
			catchRate = ((double) catchCount)/observedCatchHistory.get(net).size();
		}
		
		return catchRate;
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Particle other = (Particle) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}

