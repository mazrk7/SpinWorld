package spinworld.actions;

import spinworld.facts.Allocation;
import spinworld.network.Network;

public class CreateNetwork extends ParticleAction {
	
	final Network network;

	public CreateNetwork(Network network) {
		super();
		this.network = network;
	}

	public Network getNetwork() {
		return this.network;
	}

	public Allocation getAllocationMethod() {
		return this.network.getAllocationMethod();
	}
	
}

