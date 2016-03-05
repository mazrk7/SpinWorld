package spinworld.actions;

import spinworld.facts.Particle;
import uk.ac.imperial.presage2.core.util.random.Random;

public class Allocate extends TimeStampedAction {

	Particle particle;
	double quantity;
	double order = 0;

	// Allocate in a random order
	public Allocate(Particle p, double quantity, int time) {
		super(time);
		this.particle = p;
		this.quantity = quantity;
		this.order = Random.randomDouble();
	}

	// Allocate resources in a specified order
	public Allocate(Particle p, double quantity, int time, double order) {
		this(p, quantity, time);
		this.order = order;
	}

	public Particle getPlayer() {
		return particle;
	}

	public double getQuantity() {
		return quantity;
	}

	public double getOrder() {
		return order;
	}

	@Override
	public String toString() {
		return "Allocate [particle=" + particle.getName() + ", quantity="
				+ quantity + ", t=" + t + "]";
	}

}

