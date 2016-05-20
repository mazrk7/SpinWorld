package spinworld;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import spinworld.SpinWorldAgent.NetworkLeaveAlgorithm;
import spinworld.actions.Generate;
import spinworld.actions.SpinWorldActionHandler;
import spinworld.allocators.RandomAllocator;
import spinworld.facts.Particle;
import spinworld.mobility.MobilityService;
import spinworld.network.NetworkService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.participant.Participant;
import uk.ac.imperial.presage2.core.plugin.PluginModule;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;
import uk.ac.imperial.presage2.core.simulator.InjectedSimulation;
import uk.ac.imperial.presage2.core.simulator.Parameter;
import uk.ac.imperial.presage2.core.simulator.Scenario;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.rules.RuleModule;
import uk.ac.imperial.presage2.rules.RuleStorage;
import uk.ac.imperial.presage2.rules.facts.SimParticipantsTranslator;
import uk.ac.imperial.presage2.util.environment.AbstractEnvironmentModule;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationStoragePlugin;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.location.area.Area;
import uk.ac.imperial.presage2.util.location.area.WrapEdgeHandler;
import uk.ac.imperial.presage2.util.network.NetworkModule;

public class SpinWorldSimulation extends InjectedSimulation {
	
	private final Logger logger = Logger.getLogger("spinworld.RuleEngine");
	// Allows the application to establish an iterative conversation with the
	// engine, where the state of the session is kept across invocations
	private StatefulKnowledgeSession session;
	
	private Set<Particle> particles = new HashSet<Particle>();
	private Scenario scenario;
	private MobilityService mobilityService;
	private NetworkService networkService;	
	private SpinWorldService resourcesGame;
	private java.util.Random rnd;
	
	protected int particleCtr = 0;
	protected int genCtr = 0;
	
	// Parameter for 2D size of environment
	@Parameter(name = "size")
	public int size;

	// Number of agents contained within environment
	@Parameter(name = "agents", optional = true)
	public int agents = 1;
	
	// Propensity for an agent to cheat
	@Parameter(name = "cheat", optional = true)
	public double cheat;
	
	// Radius of an agent
	@Parameter(name = "radius", optional = true)
	public int radius = 1;
	
	// Initial velocity v0 of each of the agents
	@Parameter(name = "initVelocity", optional = true)
	public int initVelocity = 1;
	
	// Influential variability factor of velocity 
	@Parameter(name = "vConst", optional = true)
	public int vConst = 1;
	
	// Utility parameters
	@Parameter(name = "a", optional = true)
	public double a = 2;
	@Parameter(name = "b", optional = true)
	public double b = 1;
	@Parameter(name = "c", optional = true)
	public double c = 1;
	
	// Satisfaction parameters
	@Parameter(name = "alpha")
	public double alpha;
	@Parameter(name = "beta")
	public double beta;
	
	// Cheating benefit parameters
	@Parameter(name = "theta")
	public double theta;
	@Parameter(name = "phi")
	public double phi;
	
	// Rnd seed
	@Parameter(name = "seed")
	public int seed;
	
	// Threshold rate at which agent will look for new networks
	@Parameter(name = "t1", optional = true)
	public double t1 = 0.1;
	// Threshold rate at which agent will leave a network permanently
	@Parameter(name = "t2", optional = true)
	public double t2 = 0.5;
	
	// Round type for which agents will cheat on
	@Parameter(name = "cheatOn", optional = true)
	public String cheatOn = "provision";

	// Network leave algorithm
	@Parameter(name = "networkLeave", optional = true)
	public NetworkLeaveAlgorithm networkLeave = NetworkLeaveAlgorithm.THRESHOLD;

	@Parameter(name = "resetSatisfaction", optional = true)
	public boolean resetSatisfaction = false;
	
	@Parameter(name = "monitoringLevel", optional = true)
	public double monitoringLevel = 1.0;
	@Parameter(name = "monitoringCost", optional = true)
	public double monitoringCost = 0.0;
	@Parameter(name = "noWarnings", optional = true)
	public int noWarnings = 3;
	
	@Parameter(name = "severityUB", optional = true)
	public double severityUB = 0.8;
	@Parameter(name = "severityLB", optional = true)
	public double severityLB = 0.2;
	
	public SpinWorldSimulation(Set<AbstractModule> modules) {
		super(modules);
	}
	
	@Inject
	public void setSession(StatefulKnowledgeSession session) {
		this.session = session;
	}
	
	@Inject
	public void setServiceProvider(EnvironmentServiceProvider serviceProvider) {
		try {
			this.mobilityService = serviceProvider.getEnvironmentService(MobilityService.class);
			this.networkService = serviceProvider.getEnvironmentService(NetworkService.class);
			this.resourcesGame = serviceProvider.getEnvironmentService(SpinWorldService.class);
		} catch (UnavailableServiceException e) {
			logger.warn("", e);
		}
	}
	
	@Inject
	public void setEventBus(EventBus eb) {
		eb.subscribe(this);
	}
	
	/* 
	 * Initialise scenario with 2D area of parameterised size with wrap-around effect
	 * Create the abstract environment
	 */
	@Override
	protected Set<AbstractModule> getModules() {
		Set<AbstractModule> modules = new HashSet<AbstractModule>();
		
		modules.add(Area.Bind.area2D(size, size).edgeHandler(WrapEdgeHandler.class));

		modules.add(new AbstractEnvironmentModule()
				.addActionHandler(SpinWorldActionHandler.class)
				.addParticipantEnvironmentService(ParticipantLocationService.class)
				.addParticipantGlobalEnvironmentService(MobilityService.class)
				.addParticipantGlobalEnvironmentService(NetworkService.class)
				.addParticipantGlobalEnvironmentService(SpinWorldService.class)
				.setStorage(RuleStorage.class));
		
		modules.add(new RuleModule().addClasspathDrlFile("SpinWorld.drl")
				.addClasspathDrlFile("Institution.drl")
				.addClasspathDrlFile("RandomAllocation.drl")
				.addStateTranslator(SimParticipantsTranslator.class));
		
		// Fully connected network
		modules.add(NetworkModule.fullyConnectedNetworkModule());	
		// Location plugin
		modules.add(new PluginModule().addPlugin(LocationStoragePlugin.class));
		
		return modules;
	}
	
	@Override
	protected void addToScenario(Scenario s) {
		this.scenario = s;
		
		// Set up range for random seed
		this.rnd = new java.util.Random(this.seed);
		RandomAllocator.rnd = new java.util.Random(rnd.nextLong());

		// Initialise globals from parameters
		session.setGlobal("logger", this.logger);
		session.setGlobal("session", session);
		session.setGlobal("storage", this.storage);
		session.setGlobal("rnd", new java.util.Random(rnd.nextLong()));
		
		for (int n = 0; n < agents; n++) {
			createParticle("p" + n,  
					new Location(Random.randomInt(size), Random.randomInt(size)),
					initVelocity, radius, cheat, getCheatOn(), scenario);
		}
		
		// Generate resources needed
		for (Particle p : particles) {
			session.insert(new Generate(p, resourcesGame.getRoundNumber() + 1, rnd));
		}
	}
	
	protected Cheat getCheatOn() {
		for (Cheat c : Cheat.values()) {
			if (this.cheatOn.equalsIgnoreCase(c.name()))
				return c;
		}
		
		Cheat[] cs = Cheat.values();
		Cheat c = cs[rnd.nextInt(cs.length)];
		logger.debug("Cheat on: " + c);
		
		return c;
	}
	
	protected SpinWorldAgent createParticle(String name, Location loc, int velocity, 
			int radius, double pCheat, Cheat cheatOn, Scenario scenario) {
		UUID pid = UUID.randomUUID();
		
		SpinWorldAgent ag = new SpinWorldAgent(pid, name, loc, velocity, radius,
				a, b, c, pCheat, alpha, beta, cheatOn, getNetworkLeave(), 
				resetSatisfaction, rnd.nextLong(), t1, t2, theta, phi);
		scenario.addParticipant(ag);
		
		Particle p = new Particle(pid, name, alpha, beta, radius, velocity, loc);		
		particles.add(p);
		
		session.insert(p);
		particleCtr++;
		
		return ag;
	}
	
	public NetworkLeaveAlgorithm getNetworkLeave() {
		return networkLeave;
	}
	
	@EventListener
	public void incrementTime(EndOfTimeCycle e) {
		// Generate new g and q
		for (Particle p : particles) {
			session.insert(new Generate(p, resourcesGame.getRoundNumber() + 1, rnd));
		}
		
		mobilityService.printCollisions(e.getTime());
		mobilityService.clearCollisions();
		networkService.printNetworks(e.getTime());
		
		if (resourcesGame.getRound() == RoundType.DEMAND) {
			for (Participant part : scenario.getParticipants()) {
				SpinWorldAgent ag = (SpinWorldAgent) part;
				ag.updateNetworkLinks();
			}
		}
	}
	
}

