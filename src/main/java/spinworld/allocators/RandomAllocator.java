package spinworld.allocators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.drools.runtime.StatefulKnowledgeSession;

import spinworld.actions.Allocate;
import spinworld.facts.Particle;

// Allocate resources to players in a random order, possibly leading to depletion
public class RandomAllocator {

	public static Random rnd;
	
	public static void allocate(StatefulKnowledgeSession session,
			List<Particle> particles, double poolSize, int t) {
		particles = new ArrayList<Particle>(particles);
		Collections.shuffle(particles, rnd);
		
		for (Particle p : particles) {
			double allocation = Math.min(p.getD(), poolSize);
			session.insert(new Allocate(p, allocation, t));
			poolSize -= allocation;
		}
	}
	
}
