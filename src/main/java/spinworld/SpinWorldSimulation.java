package spinworld;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.core.util.StringUtils;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

import spinworld.actions.Generate;
import spinworld.actions.SpinWorldHandler;
import spinworld.allocators.RandomAllocator;
import spinworld.facts.Allocation;
import spinworld.facts.Particle;
import spinworld.network.Network;
import spinworld.network.NetworkAgent;
import spinworld.network.NetworkService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
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
	
	private final Logger logger = Logger.getLogger(this.getClass());
	// Allows the application to establish an iterative conversation with the
	// engine, where the state of the session is kept across invocations
	private StatefulKnowledgeSession session;
	
	private Set<Particle> particles = new HashSet<Particle>();
	private Scenario scenario;
	private SpinWorldService mobilityService;
	private NetworkService networkService;	
	private ResourceAllocationService resourcesGame;
	private java.util.Random rnd;
	
	protected int playerCtr = 0;
	protected int genCtr = 0;
	
	// Parameter for 2D size of environment
	@Parameter(name = "size")
	public int size;

	// Number of agents contained within environment
	@Parameter(name = "agents", optional = true)
	public int agents = 1;
	
	// Initial velocity v0 of each of the agents
	@Parameter(name = "initVelocity", optional = true)
	public int initVelocity = 1;
	
	// Influential variability factor of velocity 
	@Parameter(name = "vConst", optional = true)
	public int vConst = 1;
	
	// Radius of an agent
	@Parameter(name = "radius", optional = true)
	public int radius = 1;
	
	@Parameter(name = "aCheat", optional = true)
	public double aCheat;
	@Parameter(name = "aSize", optional = true)
	public double aSize = 1;
	
	@Parameter(name = "networks")
	public String networks;
	
	@Parameter(name = "seed")
	public int seed;
	
	@Parameter(name = "monitoringLevel", optional = true)
	public double monitoringLevel = 1.0;
	@Parameter(name = "monitoringCost", optional = true)
	public double monitoringCost = 0.0;
	
	@Parameter(name = "cheatOn", optional = true)
	public String cheatOn = "provision";
	
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
			this.mobilityService = serviceProvider.getEnvironmentService(SpinWorldService.class);
			this.networkService = serviceProvider.getEnvironmentService(NetworkService.class);
			this.resourcesGame = serviceProvider.getEnvironmentService(ResourceAllocationService.class);
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
				.addActionHandler(SpinWorldHandler.class)
				.addParticipantEnvironmentService(ParticipantLocationService.class)
				.addParticipantGlobalEnvironmentService(SpinWorldService.class)
				.addParticipantGlobalEnvironmentService(NetworkService.class)
				.addParticipantGlobalEnvironmentService(ResourceAllocationService.class)
				.setStorage(RuleStorage.class));
		modules.add(new RuleModule().addClasspathDrlFile("ResourceAllocationMaster.drl")
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

		Network[] networkArr = initNetworks();

		for (int n = 0; n < agents; n++) {
			createParticle("p" + n,  
					new Location(Random.randomInt(size), Random.randomInt(size)),
					initVelocity, aCheat, aSize, getCheatOn(),
					networkArr[n % networkArr.length], scenario);
		}
		
		// Generate resources and needed
		for (Particle p : particles) {
			session.insert(new Generate(p, resourcesGame.getRoundNumber() + 1, rnd));
		}
	}
	
	protected Network[] initNetworks() {
		String[] networkNames = StringUtils.split(this.networks, ',');
		Network[] networks = new Network[networkNames.length];
		
		for (int i = 0; i < networkNames.length; i++) {
			Allocation method = null;
			// Assign allocation methods to initialised networks
			for (Allocation a : Allocation.values()) {
				if (networkNames[i].equalsIgnoreCase(a.name())) {
					method = a;
					break;
				}
			}
			
			if (method == null)
				throw new RuntimeException("Unknown allocation method '"
						+ networkNames[i] + "', could not create network!");
			
			Network c = new Network(this.resourcesGame.getNextNumNetwork(), method,
					this.monitoringLevel, this.monitoringCost);
			session.insert(c);

			networks[i] = c;
		}
		
		return networks;
	}
	
	protected Cheat getCheatOn() {
		for (Cheat c : Cheat.values()) {
			if (this.cheatOn.equalsIgnoreCase(c.name())) {
				return c;
			}
		}
		Cheat[] cs = Cheat.values();
		Cheat c = cs[rnd.nextInt(cs.length)];
		logger.debug("Cheat on: " + c);
		return c;
	}
	
	protected NetworkAgent createParticle(String name, Location loc, int velocity, 
			double radius, double pCheat, Cheat cheatOn, Network network, Scenario scenario) {
		UUID pid = UUID.randomUUID();
		NetworkAgent ag = new NetworkAgent(pid, name, loc, velocity, radius,
				pCheat, cheatOn, rnd.nextLong());
		scenario.addParticipant(ag);	
		Particle p = new Particle(pid, name, radius);
		p.setLoc(loc);
		p.setVelocity(velocity);		
		particles.add(p);
		
		session.insert(p);
		playerCtr++;
		
		return ag;
	}
	
	@EventListener
	public void incrementTime(EndOfTimeCycle e) {
		// Generate new g and q
		for (Particle p : particles) {
			session.insert(new Generate(p, resourcesGame.getRoundNumber() + 1, rnd));
		}
		
		mobilityService.printCollisions(e.getTime());
		networkService.printNetworks(e.getTime());
		// mobilityService.setStaticNetworkAgents();
	}
	
}

