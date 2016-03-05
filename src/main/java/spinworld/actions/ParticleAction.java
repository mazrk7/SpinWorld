package spinworld.actions;

import spinworld.facts.Particle;

abstract class ParticleAction extends TimeStampedAction {

	Particle particle;

	ParticleAction() {
		super();
	}

	ParticleAction(int t, Particle particle) {
		super(t);
		this.particle = particle;
	}

	public Particle getPlayer() {
		return particle;
	}

	public void setPlayer(Particle particle) {
		this.particle = particle;
	}

}
