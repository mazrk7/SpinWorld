package spinworld.actions;

import spinworld.facts.Particle;

public class Provision extends ParticleAction {

	// Quantity to provision p
	public double quantity;

	public Provision(double quantity) {
		this.quantity = quantity;
	}

	// Player provisions quantity in round t
	public Provision(int t, Particle particle, double quantity) {
		super(t, particle);
		this.quantity = quantity;
	}
	
	public double getQuantity() {
		return quantity;
	}

	@Override
	public String toString() {
		return "Provision [quantity=" + quantity + ", particle="
				+ particle.getName() + ", t=" + t + "]";
	}
	
}
