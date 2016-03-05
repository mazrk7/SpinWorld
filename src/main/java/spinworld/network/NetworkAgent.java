package spinworld.network;

import java.util.Set;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import spinworld.Cheat;
import spinworld.SpinWorldAgent;
import uk.ac.imperial.presage2.core.environment.ParticipantSharedState;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.core.simulator.SimTime;
import uk.ac.imperial.presage2.util.location.Location;

public class NetworkAgent extends SpinWorldAgent {
	
	int noLinks = 0;
	Network net;
	
	// Variable to store the network service
	NetworkService networkService;
	
	@Inject
	@Named("params.size")
	int size;

	public NetworkAgent(UUID id, String name, Location myLocation, int velocity,
			double radius, double pCheat, Cheat cheatOn, long rndSeed) {
		super(id, name, myLocation, velocity, radius, pCheat, cheatOn, rndSeed);
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
		
		// Get the NetworkService
		try {
			this.networkService = getEnvironmentService(NetworkService.class);
		} catch (UnavailableServiceException e) {
			logger.warn(e);
		}
	}

	@Override
	public void execute() {
		super.execute();
		
		noLinks = networkService.getAgentNoLinks(getID());
		net = networkService.getAgentNetwork(getID());
		
		if (net != null)
			logger.info("I am connected to a total of " + this.noLinks + " network agent(s) in " + this.net.toString());
		
		saveDataToDB();
	}
	
	private void saveDataToDB() {
		// Get current simulation time
		int time = SimTime.get().intValue();
		// Check DB is available
		if (this.persist != null) {
			// Save number of social contacts of this agent
			this.persist.getState(time).setProperty("noLinks", ((Integer)(this.noLinks)).toString());
			if (this.net != null)
				this.persist.getState(time).setProperty("network", ((Integer)(this.net.getId())).toString());
		}
	}

}
