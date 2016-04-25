package spinworld.actions;

import spinworld.facts.Particle;
import uk.ac.imperial.presage2.core.Action;

public class Collide implements Action {
	
	Particle a;
	Particle b;

	public Collide(Particle a, Particle b) {
		super();
		this.a = a;
		this.b = b;
	}
	
	public Particle getParticleA() {
		return a;
	}
	
	public Particle getParticleB() {
		return b;
	}
	
	@Override
	public String toString() {
		return "Collide [particle A=" + a.getName() + ", particle B=" + b.getName() + "]";
	}

}
