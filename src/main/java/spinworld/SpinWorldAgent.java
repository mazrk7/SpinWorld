package spinworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.location.Location;

import spinworld.RoundType;
import spinworld.actions.Appropriate;
import spinworld.actions.CreateNetwork;
import spinworld.actions.Demand;
import spinworld.actions.JoinNetwork;
import spinworld.actions.LeaveNetwork;
import spinworld.actions.Provision;
import spinworld.facts.Allocation;
import spinworld.facts.Particle;
import spinworld.mobility.MobileAgent;
import spinworld.mobility.MobilityService;
import spinworld.Cheat;
import spinworld.SpinWorldService;
import spinworld.network.Network;
import spinworld.network.NetworkService;

/*
 * Mobile agent that moves around the environment until it forms part of a network
 */
public class SpinWorldAgent extends MobileAgent {

	enum NetworkLeaveAlgorithm {
		THRESHOLD, UTILITY, AGE
	};

	// Monitoring level of strict networks
	@Inject
	@Named("params.sMonitoringLevel")
	private double sMonitoringLevel;
	
	// Monitoring level of lenient networks
	@Inject
	@Named("params.lMonitoringLevel")
	private double lMonitoringLevel;
	
	@Inject
	@Named("params.strictNets")
	private double strictNets;

	// Monitoring cost of created networks
	@Inject
	@Named("params.monitoringCost")
	private double monitoringCost;
	
	// Number of warnings in created networks
	@Inject
	@Named("params.noWarnings")
	private int noWarnings;
	
	// Upper and lower bound of severity scale for networks
	@Inject
	@Named("params.severityLB")
	private double severityLB;
	@Inject
	@Named("params.severityUB")
	private double severityUB;

	double g = 0; // Resources generated
	double q = 0; // Resources needed
	double d = 0; // Resources demanded
	double p = 0; // Resources provisioned

	final UtilityFunction ut;

	double pCheat = 0.0;
	double catchRate = 0.0;
	double risk = 0.0;

	double alpha = .1;
	double beta = .1;
	double satisfaction = 0.5;
	double tau = .1;

	Cheat cheatOn = Cheat.PROVISION;

	Network network = null;
	Set<Particle> collisions = new CopyOnWriteArraySet<Particle>();
	Map<Network, DescriptiveStatistics> networkUtilities = new HashMap<Network, DescriptiveStatistics>();
	DescriptiveStatistics rollingUtility = new DescriptiveStatistics(100);
	SummaryStatistics overallUtility = new SummaryStatistics();
	DescriptiveStatistics scarcity = new DescriptiveStatistics(100);
	DescriptiveStatistics need = new DescriptiveStatistics(100);

	protected SpinWorldService resourcesGame;
	protected NetworkService networkService;
	protected MobilityService mobilityService;
	protected java.util.Random rnd;

	NetworkLeaveAlgorithm networkLeave = NetworkLeaveAlgorithm.THRESHOLD;
	NetworkAssess networkEvaluation;
	boolean resetSatisfaction = false;
	boolean permCreateNetwork = false;
	boolean dead = false;
	boolean compliantRound = true;
	
	// Sort of like weekly evaluation for strategy
	final int strategyLength = 7;
	boolean[] strategy;
	int strategyPtr = 0;

	double prevUtility = 0;
	double[] benefits;

	int complyCount = 0;
	int defectCount = 0;
	
	double theta = .1;
	double phi = .1;
				
	public SpinWorldAgent(UUID id, String name, Location myLocation, int velocity, double radius, 
			double a, double b, double c, double pCheat, double alpha, double beta, Cheat cheatOn, 
			NetworkLeaveAlgorithm netLeave, boolean resetSatisfaction, long rndSeed, double t1, 
			double t2, double theta, double phi) {
		super(id, name, myLocation, velocity, radius);
		this.pCheat = pCheat;
		this.alpha = alpha;
		this.beta = beta;
		this.ut = new UtilityFunction(a, b, c);
		this.cheatOn = cheatOn;
		this.resetSatisfaction = resetSatisfaction;
		this.rnd = new java.util.Random(rndSeed);
		
		this.networkLeave = netLeave;
		int leaveThreshold = 100;

		switch (netLeave) {
		case THRESHOLD:
			this.networkEvaluation = new SatisfiedNetworking(leaveThreshold);
			break;
		case UTILITY:
			this.networkEvaluation = new UtilityNetworking(t1, t2);
			break;
		case AGE:
			this.networkEvaluation = new LimitLife(t1, t2);
		}
		
		this.theta = theta;
		this.phi = phi;
		
		// Generate strategy
		this.strategy = new boolean[strategyLength];
		
		// Probabilistic cheating depending on agent likelihood to cheat
		// Defect the round if deciding to cheat
		// Else comply with the round
		for (int i = 0; i < strategyLength; i++) {
			if (rnd.nextDouble() < this.pCheat) {
				this.strategy[i] = false;
				defectCount++;
			} else {
				this.strategy[i] = true;
				complyCount++;
			}
		}
		
		this.benefits = new double[strategyLength-1];
		this.rollingUtility = new DescriptiveStatistics(strategyLength);
		
		// Propensity to cheat updated according to number of rounds defected
		this.pCheat = ((double) defectCount) / strategyLength;
		
		logger.info("Initial strategy: " + strategyToString(strategy));
	}

	@Override
	protected void processInput(Input in) {
		logger.info("Agent " + this.getName() + " not processing input: " + in.toString());
	}

	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> ss = super.getSharedState();
		return ss;
	}

	@Override
	public void initialise() {
		super.initialise();

		// Get the resource allocation & network service for SpinWorld
		try {
			this.resourcesGame = this.getEnvironmentService(SpinWorldService.class);
			this.networkService = this.getEnvironmentService(NetworkService.class);
			this.mobilityService = this.getEnvironmentService(MobilityService.class);
		} catch (UnavailableServiceException e) {
			logger.warn(e);
		}

		if (this.persist != null) {
			this.persist.setProperty("pCheat", Double.toString(this.pCheat));
			this.persist.setProperty("a", Double.toString(this.ut.a));
			this.persist.setProperty("b", Double.toString(this.ut.b));
			this.persist.setProperty("c", Double.toString(this.ut.c));
			this.persist.setProperty("alpha", Double.toString(this.alpha));
			this.persist.setProperty("beta", Double.toString(this.beta));
			this.persist.setProperty("cheatOn", this.cheatOn.name());
		}
	}

	// At every time cycle, execute function of agent - core workings
	@Override
	public void execute() {
		super.execute();
		
		this.network = this.networkService.getNetwork(getID());
		this.collisions = this.mobilityService.getCollisions(getID());

		if (this.collisions != null) {					
			for (Particle p : collisions) {
				formNetworks(p);
			}
			
			this.mobilityService.updateVelocity(getID(), this.networkService.getNoLinks(getID(), this.network));
		}
		
		if (!dead && permCreateNetwork && this.network == null && resourcesGame.getRoundNumber() > 1
				&& resourcesGame.getRound() == RoundType.DEMAND) {
			networkEvaluation.evaluateNetworks();
		}

		if (resourcesGame.getRound() == RoundType.DEMAND) {
			if (resourcesGame.getRoundNumber() > 1) {
				// Determine utility gained from last round
				calculateScores();
			}

			if (resourcesGame.getRoundNumber() % 20 == 0) {
				networkEvaluation.evaluateNetworks();
			}

			// Update g and q for this round
			g = resourcesGame.getG(getID());
			q = resourcesGame.getQ(getID());

			// If not part of a network, quit contribution to round
			if (this.network == null)
				return;
			
			if (resourcesGame.getRoundNumber() > 1)
				this.compliantRound = chooseStrategy();
			else
				this.compliantRound = (rnd.nextDouble() >= pCheat);

			// Facilitate cheating in the provision or demand of resources
			if (!compliantRound && (this.cheatOn == Cheat.PROVISION || this.cheatOn == Cheat.DEMAND)) {
				switch (this.cheatOn) {
				case PROVISION:
				default:
					// Cheat: Provision less than g
					provision(g * rnd.nextDouble());
					demand(q);
					break;
				case DEMAND:
					// Cheat: Demand more than q
					provision(g);
					demand(q + rnd.nextDouble() * (1 - q));
					break;
				}
			} else {
				provision(g);
				demand(q);
			}
		} else if (resourcesGame.getRound() == RoundType.APPROPRIATE) {
			if (!compliantRound && this.cheatOn == Cheat.APPROPRIATE) {
				// Cheat: Appropriate more than q
				appropriate(q + rnd.nextDouble() * (1 - q));
			} else {
				appropriate(resourcesGame.getAllocated(getID()));
			}
		}
	}

	protected boolean chooseStrategy() {
		// Choose strategies for each round
		boolean strat = this.strategy[strategyPtr++];
				
		// If 7 rounds have passed
		if (strategyPtr >= strategyLength) {	
			double currentUtility = this.rollingUtility.getMean();
			this.benefits[strategyPtr-2] = currentUtility - prevUtility;
					
			// Update previous round's utility
			this.prevUtility = currentUtility;
			
			if (defectCount == 0 || complyCount == 0)
				logger.info("Cannot modify strategy, already pure.");
			else {
				double defectBenefit = 0.0;
				double complyBenefit = 0.0; 
			
				for (int i = 0; i < strategyLength-1; i++) {				
					if (this.strategy[i+1])
						complyBenefit += this.benefits[i];
					else	
						defectBenefit += this.benefits[i];
				}
			
				defectBenefit = defectBenefit/defectCount;
				complyBenefit = complyBenefit/complyCount;
			
				if (defectBenefit > complyBenefit)
					reinforcementToCheat(defectBenefit - complyBenefit);
				else
					this.pCheat = this.pCheat - phi * this.pCheat;

				
				// Modify strategy depending on the reinforcement of propensity to cheat
				modifyStrategy();
			}
			
			// Reset the strategy pointer for another 7 rounds
			this.strategyPtr = 0;
			this.rollingUtility.clear();
		}
		else {
			double currentUtility = this.rollingUtility.getMean();
			this.benefits[strategyPtr-1] = currentUtility - prevUtility;
					
			// Update previous round's utility
			this.prevUtility = currentUtility;
		}
		
		return strat;
	}

	private void modifyStrategy() {
		defectCount = 0;
		complyCount = 0;
		
		// Probabilistic cheating updated with reinforcement factor
		for (int i = 0; i < strategyLength; i++) {
			if (rnd.nextDouble() < this.pCheat) {
				this.strategy[i] = false;
				defectCount++;
			} else {
				this.strategy[i] = true;
				complyCount++;
			}
		}
		
		// Re-compute the agent's propensity to cheat
		this.pCheat = ((double) defectCount) / strategyLength;
		
		logger.info("Updated strategy: " + strategyToString(strategy));
	}
	
	private void reinforcementToCheat(double benefit) {
		this.risk = this.resourcesGame.getObservedRiskRate(getID(), this.network);
		this.catchRate = this.resourcesGame.getObservedCatchRate(getID(), this.network);
		
		double normBenefit = benefit/(benefit + risk + catchRate);
		double normRisk = risk/(benefit + risk + catchRate);
		double normCatchRate = catchRate/(benefit + risk + catchRate);
		double reinforcement = theta * (normBenefit - normRisk - normCatchRate);
			
		if (reinforcement > 0.0)
			this.pCheat = this.pCheat + reinforcement * (1 - pCheat);
		else
			this.pCheat = this.pCheat + reinforcement * pCheat;
	}
	
	// Prints the strategy plan of the agent
	private static String strategyToString(boolean[] strategy) {
		StringBuilder s = new StringBuilder();
		
		for (int i = 0; i < strategy.length; i++) {
			s.append(strategy[i] ? 'C' : 'D');
		}
		
		return s.toString();
	}

	// Demand amount d, act upon environment
	protected void demand(double d) {
		try {
			environment.act(new Demand(d), getID(), authkey);
			this.d = d;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to demand", e);
		}
	}

	// Provision amount p, act upon environment
	protected void provision(double p) {
		try {
			environment.act(new Provision(p), getID(), authkey);
			this.p = p;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to provision", e);
		}
	}

	// Appropriate amount r, act upon environment
	protected void appropriate(double r) {
		try {
			environment.act(new Appropriate(r), getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn("Failed to appropriate", e);
		}
	}
	
	private void formNetworks(Particle p) {
		synchronized(this.networkService) {
			this.network = this.networkService.getNetwork(getID());
			Network otherNetwork = this.networkService.getNetwork(p.getId());
	
			if (network == null) {
				if (otherNetwork == null)
					createNetwork(p);
				else if (otherNetwork != null)
					joinNetwork(p, otherNetwork);
			}
			else if (network != null) {
				if (otherNetwork == null) {
					logger.info("Reserved particle " + p.getName() + " joined network: " + network.toString());
					this.networkService.joinMembership(p.getId(), getID(), network);
				}
				else if (otherNetwork != null && !network.equals(otherNetwork))
					assessNetwork(p, otherNetwork);
			}
		}
	}
	
	protected void createNetwork(Particle p) {
		try {	
			Allocation method = Allocation.RANDOM;
			if (rnd.nextDouble() < this.strictNets) {
				Network net = new Network(this.networkService.getNextNumNetwork(), "S", method, 
						this.sMonitoringLevel, this.monitoringCost, this.noWarnings, this.severityLB, this.severityUB);
				this.network = net;
			} else {
				Network net = new Network(this.networkService.getNextNumNetwork(), "L", method, 
						this.lMonitoringLevel, this.monitoringCost, this.noWarnings, this.severityLB, this.severityUB);
				this.network = net;
			}
			
			this.networkService.createMembership(getID(), p, this.network);
			this.resourcesGame.session.insert(this.network);
			environment.act(new CreateNetwork(this.network, p), getID(), authkey);		
		} catch (ActionHandlingException e) {
			logger.warn("Failed to create network", e);
		}
	}

	protected void joinNetwork(Particle p, Network net) {
		if (this.networkService.isBanned(getID(), net))
			return;
		try {
	    	this.networkService.joinMembership(getID(), p.getId(), net);
	    	environment.act(new JoinNetwork(net), getID(), authkey);
		   	this.network = net;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to join network", e);
		}
	}
	
	protected void leaveNetwork() {
		if (this.network == null)
			return;
		try {
		    this.networkService.retractMembership(getID());
		    environment.act(new LeaveNetwork(this.network), getID(), authkey);
			this.network = null;
		} catch (ActionHandlingException e) {
			logger.warn("Failed to leave network", e);
		}
	}

	protected void assessNetwork(Particle p, Network otherNet) {
		networkEvaluation.checkNewNetworks();
		Network preferred = networkEvaluation.preferredNetwork(otherNet);

		// If the networks differ by preference
		if (preferred != this.network && preferred != null) {
			leaveNetwork();
			joinNetwork(p, preferred);
		}
	}

	protected void calculateScores() {
		double r = resourcesGame.getAllocated(getID());
		double rP = resourcesGame.getAppropriated(getID());
		this.risk = this.resourcesGame.getObservedRiskRate(getID(), this.network);
		this.catchRate = this.resourcesGame.getObservedCatchRate(getID(), this.network);
		
		if (g == 0 && q == 0)
			return;

		// Playing the game outside of a network
		if (this.network == null) {
			r = 0;
			rP = 0;
			this.p = 0;
			this.d = 0;
		}

		// Total resources of agent
		double rTotal = rP + (g - p);
		// Total utility in this round
		double u = ut.getUtility(g, q, d, p, r, rP);

		if (rP >= d)
			satisfaction = satisfaction + alpha * (1 - satisfaction);
		else
			satisfaction = satisfaction - beta * satisfaction;

		logger.info("[" + network + ", g=" + g + ", q=" + q + ", d=" + d + ", p=" + p + ", r=" + r + ", r'=" + rP
				+ ", R=" + rTotal + ", U=" + u + ", o=" + satisfaction + "]");

		if (this.persist != null) {
			TransientAgentState state = this.persist.getState(resourcesGame.getRoundNumber() - 1);
			state.setProperty("g", Double.toString(g));
			state.setProperty("q", Double.toString(q));
			state.setProperty("d", Double.toString(d));
			state.setProperty("p", Double.toString(p));
			state.setProperty("r", Double.toString(r));
			state.setProperty("r'", Double.toString(rP));
			state.setProperty("RTotal", Double.toString(rTotal));
			state.setProperty("U", Double.toString(u));
			state.setProperty("o", Double.toString(satisfaction));
			state.setProperty("network", Integer.toString(this.network != null ? this.network.getId() : -1));
			state.setProperty("pCheat", Double.toString(pCheat));
			state.setProperty("catchRate", Double.toString(catchRate));
			state.setProperty("risk", Double.toString(risk));
		}

		if (!networkUtilities.containsKey(this.network)) {
			DescriptiveStatistics s = new DescriptiveStatistics(50);
			networkUtilities.put(network, s);
		}

		networkUtilities.get(this.network).addValue(u);
		rollingUtility.addValue(u);
		overallUtility.addValue(u);

		// Observed scarcity for this agent
		scarcity.addValue(this.g/this.q);

		// Observed need for this agent
		need.addValue(this.q);
	}

	interface NetworkAssess {
		void evaluateNetworks();
		Network preferredNetwork(Network otherNetwork);
		void checkNewNetworks();
	}

	class SatisfiedNetworking implements NetworkAssess {

		int dissatisfactionCount = 0;
		int leaveThreshold = 3;

		Map<Network, Double> networkSatisfaction = new HashMap<Network, Double>();
		Set<Network> definitelyLeftNetworks = new HashSet<Network>();

		SatisfiedNetworking() {
			super();
		}

		SatisfiedNetworking(int leaveThreshold) {
			super();
			this.leaveThreshold = leaveThreshold;
		}
		
		public void evaluateNetworks() {
			checkNewNetworks();

			if (network != null)
				networkSatisfaction.put(network, satisfaction);
			else
				return;

			Network optimal = optimalNetwork();
			
			if (optimal == null)
				return;
			
			double maxSatisfaction = networkSatisfaction.get(optimal);

			// If satisfaction is below threshold or other network's satisfaction, 
			// then increment consecutive dissatisfaction count, otherwise reset it to 0
			if (maxSatisfaction < tau || !optimal.equals(network))
				this.dissatisfactionCount++;
			else
				this.dissatisfactionCount = 0;

			boolean leaveNetwork = this.dissatisfactionCount >= this.leaveThreshold;
			if (leaveNetwork) {
				if (networkSatisfaction.get(network) < tau)
					definitelyLeftNetworks.add(network);

				leaveNetwork();
			}

		}

		Network optimalNetwork() {
			Network optimal = network;
			double maxSatisfaction = satisfaction;

			for (Map.Entry<Network, Double> e : networkSatisfaction.entrySet()) {
				if (e.getValue() > maxSatisfaction) {
					maxSatisfaction = e.getValue();
					optimal = e.getKey();
				}
			}

			return optimal;
		}

		public Network preferredNetwork(Network otherNetwork) {
			if (definitelyLeftNetworks.contains(otherNetwork))
				return network;
			else if(definitelyLeftNetworks.contains(network))
				return otherNetwork;
			else if (networkSatisfaction.get(otherNetwork) <= satisfaction)
				return network;
			else if (networkSatisfaction.get(otherNetwork) > satisfaction)
				return otherNetwork;
			else
				return null;
		}
		
		public void checkNewNetworks() {
			// Add new available networks
			Set<Network> availableNetworks = networkService.getNetworks();

			for (Network n : availableNetworks) {
				if (!networkSatisfaction.containsKey(n) && !definitelyLeftNetworks.contains(n))
					networkSatisfaction.put(n, 0.5);
				if (definitelyLeftNetworks.contains(n))
					networkSatisfaction.remove(n);
			}

			// Remove non-existing networks -- use iterator to avoid
			// concurrentModificationException
			Iterator<Entry<Network, Double>> it = networkSatisfaction.entrySet().iterator();

			while (it.hasNext()) {
				Entry<Network, Double> entry = it.next();
				if (!availableNetworks.contains(entry.getKey()))
					it.remove();
			}
		}

	}

	class UtilityNetworking implements NetworkAssess {

		int acclimatisationRounds = 50;
		double tolerance1;
		double tolerance2;

		// Threshold rate at which agent will look for new networks
		double t1 = 0.0;
		// Threshold rate at which agent will leave a network permanently
		double t2 = 0.0;
		// Threshold rate at which agent will stop playing the game
		double t3 = 0.0;

		UtilityNetworking(double tolerance1, double tolerance2) {
			super();
			this.tolerance1 = tolerance1;
			this.tolerance2 = tolerance2;
		}

		@Override
		public void evaluateNetworks() {
			determineTargetRates();

			// Death
			if (overallUtility.getN() > 50 && overallUtility.getMean() < t3) {
				if (network != null)
					leaveNetwork();
				
				dead = true;
				return;
			}
			// Acclimatisation period in new network
			if (network != null && --acclimatisationRounds > 0)
				return;

			checkNewNetworks();

			// Get our current rate of utility generation in this network, or
			// lowest possible value if we are network-less
			final double currentRate = network != null && networkUtilities.containsKey(network)
					? networkUtilities.get(network).getMean() : t3;
			if (networkUtilities.size() > 1) {
				if (currentRate < t2) {
					// Below low threshold, leave network.
					leaveNetwork();
				} else if (currentRate < t1) {
					// Below high threshold, check for a possibly better
					// network,
					// where ut > t1 or unknown
					Network chosen = chooseNetworkFromSubset(new NetworkFilter() {
						@Override
						public boolean isSubsetMember(Network n) {
							DescriptiveStatistics s = networkUtilities.get(n);
							return s != null && (s.getN() < 2 || s.getMean() > t1);
						}
					});

					if (chosen != null) {
						leaveNetwork();
						acclimatisationRounds = 50;
					}
				} else {
					// Above high threshold, so look only for preferred networks
					Network chosen = chooseNetworkFromSubset(new NetworkFilter() {
						@Override
						public boolean isSubsetMember(Network n) {
							DescriptiveStatistics s = networkUtilities.get(n);
							return s != null && s.getMean() > currentRate;
						}
					});

					if (chosen != null) {
						leaveNetwork();
						acclimatisationRounds = 50;
					}
				}
			} else if (network != null && currentRate < t2) {
				leaveNetwork();
				acclimatisationRounds = 50;
			}
		}

		void determineTargetRates() {
			// Calculate expected utility rates
			double bestCase = ut.estimateFullComplyUtility(scarcity.getMean());
			double worstCase = ut.estimateFullDefectUtility(scarcity.getMean());

			t3 = worstCase;
			// alpha and beta are acceptable inefficiency 
			// proportions for t1 and t2 respectively
			t2 = bestCase - tolerance2 * (bestCase - worstCase);
			t1 = bestCase - tolerance1 * (bestCase - worstCase);
		}

		private Network chooseNetworkFromSubset(NetworkFilter f) {
			List<Network> candidateNetworks = new ArrayList<Network>();
			for (Entry<Network, DescriptiveStatistics> e : networkUtilities.entrySet()) {
				if (f.isSubsetMember(e.getKey()))
					candidateNetworks.add(e.getKey());
			}
			// If we found one, join randomly, else wait for something new
			if (candidateNetworks.size() > 0)
				return candidateNetworks.get(rnd.nextInt(candidateNetworks.size()));
			else
				return null;
		}

		public Network preferredNetwork(Network otherNetwork) {
			if (networkUtilities.get(otherNetwork).getMean() <= networkUtilities.get(network).getMean())
				return network;
			else if (networkUtilities.get(otherNetwork).getMean() > networkUtilities.get(network).getMean())
				return otherNetwork;
			else
				return null;
		}

		public void checkNewNetworks() {
			// Initialise/update network utilities
			// Add new available network
			Set<Network> availableNetworks = networkService.getNetworks();

			for (Network n : availableNetworks) {
				if (!networkUtilities.containsKey(n)) {
					DescriptiveStatistics s = new DescriptiveStatistics(50);
					networkUtilities.put(n, s);
				}
			}

			// Remove non-existing networks -- use iterator to avoid
			// concurrentModificationException
			Iterator<Entry<Network, DescriptiveStatistics>> it = networkUtilities.entrySet().iterator();

			while (it.hasNext()) {
				Entry<Network, DescriptiveStatistics> entry = it.next();

				if (!availableNetworks.contains(entry.getKey()))
					it.remove();
			}
		}

	}

	class LimitLife extends UtilityNetworking {

		LimitLife(double tolerance1, double tolerance2) {
			super(tolerance1, tolerance2);
		}

		int age = 0;
		int baselifespan = 200;

		@Override
		public void evaluateNetworks() {
			if (networkUtilities.size() > 1)
				super.evaluateNetworks();
			else
				determineTargetRates();

			age++;

			if (network != null && --acclimatisationRounds > 0)
				return;

			int expected = (int) (baselifespan + (overallUtility.getMean() - t2) * baselifespan);
			
			if (age > expected) {
				// Death of old age
				if (network != null)
					leaveNetwork();
				
				dead = true;
				logger.info("Died at age " + age);
			}
		}

	}

	class UtilityFunction {

		double a;
		double b;
		double c;

		UtilityFunction(double a, double b, double c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public double getUtility(double g, double q, double d, double p, double r, double rP) {
			double rTotal = rP + (g - p);
			
			if (rTotal >= q)
				return a + b * ((rTotal/q) - 1);
			else
				return c * (rTotal/q);
		}

		public double estimateFullComplyUtility(double scarcity) {
			return a * scarcity;
		}

		public double estimateFullDefectUtility(double scarcity) {
			return c * scarcity;
		}

	}

	interface NetworkFilter {
		boolean isSubsetMember(Network n);
	}

}