package spinworld.actions;

import spinworld.facts.Particle;
import spinworld.network.Network;

public class LeaveNetwork extends ParticleAction {
	
	final Network network;

	public LeaveNetwork(Network network) {
		super();
		this.network = network;
	}
	
	public LeaveNetwork(Particle particle, Network network) {
		super();
		this.particle = particle;
		this.network = network;
	}

	public Network getNetwork() {
		return network;
	}

}
