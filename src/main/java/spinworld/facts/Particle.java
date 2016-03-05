package spinworld.facts;

import java.util.UUID;

import uk.ac.imperial.presage2.util.location.Location;

/*
 * Moving particle in a linear square environment
 */
public class Particle {

	UUID id;
	final String name;
	Location loc;
	
	// Number of collisions, represents number of particle's collisions
	int noCollisions = 0;
	// Number of collisions, represents number of particle's social connections
	int noLinks = 0;
	int velocity;
	
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
	// double alpha = 0.1;
	// double beta = 0.1;
	
	// Role role = Role.PROSUMER;
	
	// Player history may be worth including
	// Map<Cluster, PlayerHistory> history = new HashMap<Cluster, PlayerHistory>();

	// Size multiplier a.k.a. radius gives weighting to size of player
	double radius = 1;

	public Particle(UUID id) {
		super();
		this.id = id;
		this.name = "n/a";
	}

	public Particle(UUID id, String name, double radius) {
		super();
		this.id = id;
		this.name = name;
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
	
	// Increment collision count of particle
	public void incrementCollisionCount() {
		this.noCollisions++;
	}
	
	public int getNoCollisions() {
		return noCollisions;
	}
	
	// Increment number of social connections of particle
	public void incrementNoLinks() {
		this.noLinks++;
	}
	
	public int getNoLinks() {
		return noLinks;
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
	
	public double getRadius() {
		return radius;
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

