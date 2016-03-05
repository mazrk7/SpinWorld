package spinworld.network;

import spinworld.facts.Particle;
import spinworld.facts.Tuple;

public class Connection {

	Tuple<Particle, Particle> connectedParticles;
	
	public Connection(Particle p1, Particle p2) {
		super();
		this.connectedParticles = new Tuple<Particle, Particle>(p1, p2);
	}
	
	@Override
	public String toString() {
		return "Connection [X=" + connectedParticles.x + ", Y=" + connectedParticles.y + "]";
	}
	
	// TODO Fix this hacky way of accessing connection pairs
	public Particle getParticleX() {
		return connectedParticles.x;
	}

	public Particle getParticleY() {
		return connectedParticles.y;
	}
	
	public boolean containsParticle(Particle p) {
		if (connectedParticles.x.equals(p) || connectedParticles.y.equals(p))
			return true;
		else
			return false;
	}
		
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Connection other = (Connection) obj;
		if (connectedParticles.x == null) {
			if (other.connectedParticles.x != null)
				return false;
		} else if (!connectedParticles.x.equals(other.connectedParticles.x))
			return false;
		if (connectedParticles.y == null) {
			if (other.connectedParticles.y != null)
				return false;
		} else if (!connectedParticles.y.equals(other.connectedParticles.y))
			return false;
		return true;
	}

}
