package spinworld.actions;

import java.util.Random;

import spinworld.facts.Particle;

public class Generate extends TimeStampedAction {

	public final Particle particle;
	// Resources generated
	public final double g;
	// Resources needed
	public final double q;

	// Player generates a random amount relative to size multiplier
	// Player needs an amount relative to resources generated and size multiplier
	public Generate(Particle particle, int time, Random rnd) {
		super(time);
		this.particle = particle;
		this.g = rnd.nextDouble() * particle.getRadius();
		this.q = this.g
				+ (rnd.nextDouble() * (particle.getRadius() - this.g));
	}

	// Generate constructor without random seed parameter
	public Generate(Particle particle, int time) {
		super(time);
		this.particle = particle;
		this.g = uk.ac.imperial.presage2.core.util.random.Random.randomDouble()
				* particle.getRadius();
		this.q = this.g
				+ (uk.ac.imperial.presage2.core.util.random.Random
						.randomDouble() * (particle.getRadius() - this.g));
	}

	public Particle getParticle() {
		return particle;
	}

	public double getG() {
		return g;
	}

	public double getQ() {
		return q;
	}

	@Override
	public String toString() {
		return "Generate [particle=" + particle.getName() + ", g=" + g + ", q=" + q
				+ ", t=" + t + "]";
	}

}
