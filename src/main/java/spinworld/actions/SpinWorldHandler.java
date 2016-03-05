package spinworld.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import spinworld.SpinWorldService;
import spinworld.facts.Particle;
import spinworld.network.NetworkService;
import uk.ac.imperial.presage2.core.Action;
import uk.ac.imperial.presage2.core.environment.ActionHandler;
import uk.ac.imperial.presage2.core.environment.ActionHandlingException;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.environment.ServiceDependencies;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.messaging.Input;
import uk.ac.imperial.presage2.util.location.CannotSeeAgent;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationService;
import uk.ac.imperial.presage2.util.location.Move;
import uk.ac.imperial.presage2.util.location.area.AreaService;
import uk.ac.imperial.presage2.util.location.area.EdgeException;
import uk.ac.imperial.presage2.util.location.area.HasArea;

@ServiceDependencies({ LocationService.class, AreaService.class })
public class SpinWorldHandler implements ActionHandler {

    final private Logger logger = Logger.getLogger(SpinWorldHandler.class);
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	
	final protected EnvironmentServiceProvider serviceProvider;
	final protected HasArea environment;
	final protected EnvironmentSharedStateAccess sharedState;
	SpinWorldService mobilityService = null;
	NetworkService networkService = null;
	
	@Inject
	public SpinWorldHandler(HasArea environment,
			EnvironmentServiceProvider serviceProvider,
			EnvironmentSharedStateAccess sharedState)
			throws UnavailableServiceException {
		super();
		this.environment = environment;
		this.serviceProvider = serviceProvider;
		this.sharedState = sharedState;
	}
	
	@Override
	public boolean canHandle(Action action) {
		return action instanceof Move;
	}
	
	protected SpinWorldService getMobilityService() {
		if (mobilityService == null) {
			try {
				this.mobilityService = serviceProvider
						.getEnvironmentService(SpinWorldService.class);
			} catch (UnavailableServiceException e) {
				logger.warn("Could not load mobility service", e);
			}
		}
		return mobilityService;
	}
	
	protected NetworkService getNetworkService() {
		if (networkService == null) {
			try {
				this.networkService = serviceProvider
						.getEnvironmentService(NetworkService.class);
			} catch (UnavailableServiceException e) {
				logger.warn("Could not load network service", e);
			}
		}
		return networkService;
	}
	
	@Override
	public Input handle(Action action, UUID actor)
			throws ActionHandlingException {
		getMobilityService();
		getNetworkService();
		
		// If mobile agent action is to move
		if (action instanceof Move) {
			if (logger.isDebugEnabled())
				logger.debug("Handling move " + action + " from " + mobilityService.getAgentName(actor));
			
			final Move m = (Move) action;
			Location loc = null;
			
			// Get the agent's current location
			try {
				loc = mobilityService.getAgentLocation(actor);
			} catch (CannotSeeAgent e) {
				throw new ActionHandlingException(e);
			}
			
			// Compute the target location for this mobile agent
			Location target = new Location(loc.add(m));
			
			// Check whether the target location fits the environment's bounds
			// See if the move is valid
			if (!target.in(environment.getArea())) {
				try {
					final Move mNew = environment.getArea()
							.getValidMove(loc, m);
					target = new Location(loc.add(mNew));
				} catch (EdgeException e) {
					throw new ActionHandlingException(e);
				}
			}
			
			// If the move is valid, update the agent's location to target
			this.mobilityService.setAgentLocation(actor, target);

			// Check if any collisions occurred at this target location
			Set<Particle> collidedAgents = this.mobilityService.getCollidedAgents(actor, target);

			if (!collidedAgents.isEmpty())
				this.networkService.formLinks(actor, collidedAgents);
				
			return null;
		}
		
		throw new ActionHandlingException("MoveHandler was asked to handle non Move action!");
	}

}
