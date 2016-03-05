package spinworld;

import java.util.Set;
import java.util.UUID;

import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.simulator.SimTime;
import uk.ac.imperial.presage2.core.util.random.Random;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.ParticipantLocationService;
import uk.ac.imperial.presage2.util.participant.AbstractParticipant;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import spinworld.RoundType;
import spinworld.actions.Appropriate;
import spinworld.actions.Demand;
import spinworld.actions.Provision;
import spinworld.Cheat;
import spinworld.ResourceAllocationService;
import spinworld.network.Network;

/*
 * Mobile agent that moves around the environment until it forms part of a network
 */
public class SpinWorldAgent extends AbstractParticipant {

	// MOBILITY FIELDS
	Location myLocation;
	int velocity;
	int noCollisions = 0;
	double radius = 1;
	
	// Variable to store the mobility service
	SpinWorldService mobilityService;
	
	// Injection is the process of setting dependencies into an object
	@Inject
	@Named("params.size")
	int size;
	
	// RESOURCE ALLOCATION FIELDS 
	double g = 0; // Resources generated
	double q = 0; // Resources needed
	double d = 0; // Resources demanded
	double p = 0; // Resources provisioned
	double pCheat = 0.0;
	
	Cheat cheatOn = Cheat.PROVISION;
	
	Network network = null;
		
	protected ResourceAllocationService resourcesGame;
	protected java.util.Random rnd;
	
	boolean compliantRound = true;

	public SpinWorldAgent(UUID id, String name, Location myLocation, int velocity,
			double radius, double pCheat, Cheat cheatOn, long rndSeed) {
		super(id, name);
		this.myLocation = myLocation;
		this.velocity = velocity;
		this.radius = radius;
		this.pCheat = pCheat;
		this.cheatOn = cheatOn;
		this.rnd = new java.util.Random(rndSeed);
	}
	
	@Override
	protected void processInput(Input in) {
		logger.info("Agent " + this.getName() + " not processing input: " + in.toString());
	}
	
	// Location is shared state, so share with the environment upon registration
	@Override
	protected Set<ParticipantSharedState> getSharedState() {
		Set<ParticipantSharedState> ss = super.getSharedState();
		ss.add(ParticipantLocationService.createSharedState(getID(), myLocation));
		return ss;
	}

	@Override
	public void initialise() {
		super.initialise();
		
		// Get the MobilityService
		try {
			this.mobilityService = getEnvironmentService(SpinWorldService.class);
			this.resourcesGame = this.getEnvironmentService(ResourceAllocationService.class);
		} catch (UnavailableServiceException e) {
			logger.warn(e);
		}
		if (this.persist != null) {
			this.persist.setProperty("pCheat", Double.toString(this.pCheat));
			/*this.persist.setProperty("a", Double.toString(this.ut.a));
			this.persist.setProperty("b", Double.toString(this.ut.b));
			this.persist.setProperty("c", Double.toString(this.ut.c));
			this.persist.setProperty("alpha", Double.toString(this.alpha));
			this.persist.setProperty("beta", Double.toString(this.beta));*/
			this.persist.setProperty("cheatOn", this.cheatOn.name());
		}
	}

	// At every time cycle, execute function of agent - core workings
	@SuppressWarnings("deprecation")
	@Override
	public void execute() {
		super.execute();
		
		this.network = this.resourcesGame.getNetwork(getID());
		
		if (resourcesGame.getRound() == RoundType.DEMAND) {				
			// Update g and q for this round
			g = resourcesGame.getG(getID());
			q = resourcesGame.getQ(getID());

			// If not part of a cluster, quit contribution to round
			if (this.network == null) {
				return;
			}
			
			this.compliantRound = chooseStrategy();

			// Facilitate cheating in the provision or demand of resources
			if (!compliantRound && 
			   (this.cheatOn == Cheat.PROVISION || this.cheatOn == Cheat.DEMAND)) {
				switch (this.cheatOn) {
				case PROVISION:
				default:
					// cheat: provision less than g
					provision(g * rnd.nextDouble());
					demand(q);
					break;
				case DEMAND:
					provision(g);
					demand(q + rnd.nextDouble() * (1 - q));
					break;
				}
			} else {
				// Provision and demand g and q resources
				provision(g);
				demand(q);
			}
		} else if (resourcesGame.getRound() == RoundType.APPROPRIATE) {
			if (!compliantRound && this.cheatOn == Cheat.APPROPRIATE) {
				// double allocated = game.getAllocated(getID());
				appropriate(q + rnd.nextDouble() * (1 - q));
			} else {
				appropriate(resourcesGame.getAllocated(getID()));
			}
		}
		
		myLocation = mobilityService.getAgentLocation(getID());
		velocity = mobilityService.getAgentVelocity(getID());
		noCollisions = mobilityService.getAgentNoCollisions(getID());
	 
		logger.info("My location is: " + this.myLocation + " and my velocity is " + this.velocity);
		saveDataToDB();
	 	
		if (velocity != 0) {
			Move m = new Move(velocity*(Random.randomInt(3) - 1), velocity*(Random.randomInt(3) - 1));
			submitMove(m);
		}
	}
	
	// Probabilistic cheating.
	protected boolean chooseStrategy() {
		return rnd.nextDouble() >= pCheat;
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

	
	// Submit move action to the environment
	private void submitMove(Move move) {
		try {
			logger.debug("Agent " + getName() + " attempting move: " + move);
			environment.act(move, getID(), authkey);
		} catch (ActionHandlingException e) {
			logger.warn("Error trying to move", e);
		}
	}
	
	private void saveDataToDB() {
		// Get current simulation time
		int time = SimTime.get().intValue();
		// Check DB is available
		if (this.persist != null) {
			// Save velocity for this time step
			this.persist.getState(time).setProperty("velocity", ((Integer)(this.velocity)).toString());
			this.persist.getState(time).setProperty("noCollisions", ((Integer)(this.noCollisions)).toString());
		}
	}
	
}