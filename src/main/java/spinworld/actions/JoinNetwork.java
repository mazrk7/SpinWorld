package spinworld.actions;

import spinworld.network.Network;
import spinworld.facts.Particle;

public class JoinNetwork extends ParticleAction {
	
	final Network network;

	public JoinNetwork(Network network) {
		super();
		this.network = network;
	}

	public JoinNetwork(Particle particle, Network network) {
		super();
		this.particle = particle;
		this.network = network;
	}

	public Network getNetwork() {
		return network;
	}
	
}

