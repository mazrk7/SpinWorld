package spinworld.actions;

import spinworld.facts.Particle;

public class Demand extends ParticleAction {

	double quantity;

	// Demand without any association to a player
	public Demand(double quantity) {
		this.quantity = quantity;
	}

	public Demand(int t, Particle particle, double quantity) {
		super(t, particle);
		this.quantity = quantity;
	}

	@Override
	public String toString() {
		return "Demand [quantity=" + quantity + ", particle=" + particle.getName()
				+ ", t=" + t + "]";
	}

	public double getQuantity() {
		return quantity;
	}

}