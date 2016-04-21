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

// Mobile agent that moves around the environment until it forms part of a network
public class MobileAgent extends AbstractParticipant {

	Location myLocation;
	int velocity;
	int noCollisions = 0;
	double radius = 1;

	// Variable to store the mobility service
	MobilityService mobilityService;
	
	// Injection is the process of setting dependencies into an object
	@Inject
	@Named("params.size")
	int size;

	public MobileAgent(UUID id, String name, Location myLocation, int velocity, double radius) {
		super(id, name);
		this.myLocation = myLocation;
		this.velocity = velocity;
		this.radius = radius;
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
			this.mobilityService = getEnvironmentService(MobilityService.class);
		} catch (UnavailableServiceException e) {
			logger.warn(e);
		}
	}

	// At every time cycle, execute function of agent - core workings
	@Override
	public void execute() {
		myLocation = mobilityService.getLocation(getID());
		velocity = mobilityService.getVelocity(getID());
		noCollisions = mobilityService.getNoCollisions(getID());
	 
		logger.info("My location is: " + this.myLocation + " and my velocity is " + this.velocity);
		saveDataToDB();
	 	
		if (velocity != 0) {
			Move m = new Move(velocity*(Random.randomInt(3) - 1), velocity*(Random.randomInt(3) - 1));
			submitMove(m);
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