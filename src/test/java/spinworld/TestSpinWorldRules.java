package spinworld;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import spinworld.actions.Appropriate;
import spinworld.actions.Demand;
import spinworld.actions.Generate;
import spinworld.actions.JoinNetwork;
import spinworld.actions.Provision;
import spinworld.allocators.RandomAllocator;
import spinworld.facts.Allocation;
import spinworld.facts.Particle;
import spinworld.facts.Round;
import spinworld.network.Network;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;
import uk.ac.imperial.presage2.util.location.Location;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestSpinWorldRules {

	final private Logger logger = Logger.getLogger(TestSpinWorldRules.class);

	Injector injector;
	RuleStorage rules;
	StatefulKnowledgeSession session;

	@Before
	public void setUp() throws Exception {
		injector = Guice.createInjector(new RuleModule()
				.addClasspathDrlFile("SpinWorld.drl")
				.addClasspathDrlFile("Institution.drl")
				.addClasspathDrlFile("RandomAllocation.drl"));
		
		rules = injector.getInstance(RuleStorage.class);
		
		session = injector.getInstance(StatefulKnowledgeSession.class);
		session.setGlobal("logger", this.logger);
		session.setGlobal("session", session);
		session.setGlobal("storage", null);
	}

	@After
	public void tearDown() throws Exception {
		session.dispose();
	}

	@Test
	public void testRoundPruning() {
		session.insert(new Round(1, RoundType.DEMAND));
		session.insert(new Round(1, RoundType.APPROPRIATE));
		session.insert(new Round(2, RoundType.DEMAND));

		rules.incrementTime();

		Collection<Object> rounds = session.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object object) {
				return object instanceof Round;
			}
		});
		
		assertEquals(1, rounds.size());
		Round r = (Round) rounds.iterator().next();
		assertEquals(2, r.getNumber());
		assertEquals(RoundType.DEMAND, r.type);

		session.insert(new Round(2, RoundType.APPROPRIATE));
		rules.incrementTime();

		rounds = session.getObjects(new ObjectFilter() {
			@Override
			public boolean accept(Object object) {
				return object instanceof Round;
			}
		});
		
		assertEquals(1, rounds.size());
		r = (Round) rounds.iterator().next();
		assertEquals(2, r.getNumber());
		assertEquals(RoundType.APPROPRIATE, r.type);
	}

	@Test
	public void testRandomSingleNetwork() {
		SimulatedWorld world = new SimulatedWorld();

		RandomAllocator.rnd = new java.util.Random();
		world.addNetwork(Allocation.RANDOM);
		int agents = Random.randomInt(30);
		int size = 20;
		char name = 'a';
		
		for (int n = 0; n < agents; n++) {
			world.addPlayer(String.valueOf(name), 0.1, 0.1, 0, 1, 
					new Location(Random.randomInt(size), Random.randomInt(size)));
			// Next unicode point
			name++;
		}
		
		world.initRound();

		for (int i = 0; i < 5; i++) {
			for (Particle p : world.particles) {
				Provision provision = new Provision(world.currentRound, p, p.getG());
				Demand demand = new Demand(world.currentRound, p, p.getQ());
				session.insert(provision);
				session.insert(demand);
			}
			
			world.demandRound();

			int incorrectCount = 0;
			for (Particle p : world.particles) {
				if (Math.abs(p.getQ() - p.getAllocated()) > 0.00001
						&& p.getAllocated() != 0) {
					incorrectCount++;
				}
			}
			
			logger.info(incorrectCount + " non 0 or q allocations");
			assertFalse("No more than one non 0 or q allocation",
					incorrectCount > 1);

			for (Particle p : world.particles) {
				Appropriate app = new Appropriate(world.currentRound, p, p.getAllocated());
				session.insert(app);
			}
			
			world.appropriateRound();

			for (Particle p : world.particles) {
				assertEquals(p.getAllocated(), p.getAppropriated(), 0.000001);
			}
		}
	}

	class SimulatedWorld {

		List<Network> networks = new ArrayList<Network>();
		List<Particle> particles = new ArrayList<Particle>();
		Map<UUID, Generate> generated = new HashMap<UUID, Generate>();
		Map<UUID, Provision> provisioned = new HashMap<UUID, Provision>();
		Map<UUID, Demand> demanded = new HashMap<UUID, Demand>();
		int currentRound = 1;

		SimulatedWorld() {
			super();
			logger.info("New world:");
		}

		SimulatedWorld addNetwork(Allocation all) {
			Network n = new Network(networks.size(), all);
			networks.add(n);
			session.insert(n);
			return this;
		}

		SimulatedWorld addPlayer(String name, double alpha, double beta, int network,
				int velocity, Location loc) {
			Particle p = new Particle(Random.randomUUID(), name, alpha, beta, velocity, loc);
			particles.add(p);
			session.insert(p);
			
			try {
				Network n = networks.get(network);
				session.insert(new JoinNetwork(p, n));
			} catch (IndexOutOfBoundsException e) {
			}
			
			return this;
		}

		void initRound() {
			for (Particle p : particles) {
				Generate g = new Generate(p, currentRound);
				generated.put(p.getId(), g);
				session.insert(g);
			}
			
			rules.incrementTime();
		}

		void demandRound() {
			session.insert(new Round(currentRound, RoundType.DEMAND));
			
			for (Particle p : particles) {
				Generate g = generated.get(p.getId());
				assertEquals(g.getG(), p.getG(), 0.000001);
				assertEquals(g.getQ(), p.getQ(), 0.000001);
			}

			rules.incrementTime();
		}

		void appropriateRound() {
			session.insert(new Round(currentRound, RoundType.APPROPRIATE));
			rules.incrementTime();
			currentRound++;
		}

	}

}
