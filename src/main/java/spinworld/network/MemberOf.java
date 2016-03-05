package spinworld.network;

import spinworld.facts.Particle;

// Validating whether a player is a member or not of a given cluster
// More or less equivalent to the connection relationship for a network and agent
public class MemberOf {

	public Particle particle;
	public Network network;

	public MemberOf(Particle particle, Network network) {
		super();
		this.particle = particle;
		this.network = network;
	}

	@Override
	public String toString() {
		return "MemberOf [particle=" + particle + ", network=" + network + "]";
	}

	public Particle getParticle() {
		return particle;
	}

	public Network getNetwork() {
		return network;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((network == null) ? 0 : network.hashCode());
		result = prime * result + ((particle == null) ? 0 : particle.hashCode());
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
		MemberOf other = (MemberOf) obj;
		if (network == null) {
			if (other.network != null)
				return false;
		} else if (!network.equals(other.network))
			return false;
		if (particle == null) {
			if (other.particle != null)
				return false;
		} else if (!particle.equals(other.particle))
			return false;
		return true;
	}

}

