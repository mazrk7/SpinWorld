package spinworld.actions;

import spinworld.facts.Particle;

public class Appropriate extends ParticleAction {

	double quantity;

	public Appropriate(double quantity) {
		this.quantity = quantity;
	}

	public Appropriate(int t, Particle particle, double quantity) {
		super(t, particle);
		this.quantity = quantity;
	}
	
	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "Appropriate [quantity=" + quantity + ", particle="
				+ particle.getName() + ", t=" + t + "]";
	}
	
}