package spinworld.actions;

import spinworld.facts.Allocation;
import spinworld.facts.Particle;
import spinworld.network.Network;

public class CreateNetwork extends ParticleAction {
	
	final Network network;
	final Particle collidedParticle;

	public CreateNetwork(Network network, Particle collidedParticle) {
		super();
		this.network = network;
		this.collidedParticle = collidedParticle;
	}

	public Network getNetwork() {
		return this.network;
	}
	
	public Particle getCollidedParticle() {
		return this.collidedParticle;
	}

	public Allocation getAllocationMethod() {
		return this.network.getAllocationMethod();
	}
	
}

