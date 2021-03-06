package spinworld.facts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import spinworld.GraduationLevel;
import uk.ac.imperial.presage2.util.location.Location;

// Mobile particle in a linear square environment
public class Particle {

	UUID id;
	final String name;
	Location loc;
	String type = "C";

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
					
	ArrayList<GraduationLevel> observedSanctionHistory = new ArrayList<GraduationLevel>();
	ArrayList<Boolean> observedCatchHistory = new ArrayList<Boolean>();
	
	double utility = 0.0;
		
	public Particle(UUID id) {
		super();
		this.id = id;
		this.name = "n/a";
	}
	
	public Particle(UUID id, String name, String type,
			double alpha, double beta, int velocity, Location loc) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.alpha = alpha;
		this.beta = beta;
		this.velocity = velocity;
		this.loc = loc;
	}

	public Particle(UUID id, String name, String type, double alpha, double beta, 
			double radius, int velocity, Location loc) {
		this(id, name, type, alpha, beta, velocity, loc);
		this.radius = radius;
	}

	@Override
	public String toString() {
		return "Particle [" + name + ", type=" + type +
				", velocity=" + velocity + ", g=" + g + ", q=" + q + "]";
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
	
	public double getUtility() {
		return utility;
	}
	
	public void setUtility(double util) {
		this.utility = util;
	}
	
	public void updateObservedSanctionHistory(GraduationLevel sanction) {
		observedSanctionHistory.add(sanction);
	}
	
	public double getObservedRiskRate() {	
		if (!observedSanctionHistory.isEmpty()) {
			int sanctionCount = 
					(int)(Collections.frequency(observedSanctionHistory, GraduationLevel.WARNING) * 0.5)
				+ Collections.frequency(observedSanctionHistory, GraduationLevel.EXPULSION);
			double risk = ((double) sanctionCount)/observedSanctionHistory.size();
			
			return risk;
		} else
			return 0;	
	}
	
	public void updateObservedCatchHistory(Boolean caught) {
			observedCatchHistory.add(caught);
	}
	
	public double getObservedCatchRate() {
		if (!observedCatchHistory.isEmpty()) {
			int catchCount = Collections.frequency(observedCatchHistory, true);			
			double catchRate = ((double) catchCount)/observedCatchHistory.size();
			
			return catchRate;
		}
		else
			return 0;
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

