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

	public Particle getParticle() {
		return particle;
	}

	public void setParticle(Particle particle) {
		this.particle = particle;
	}

}
