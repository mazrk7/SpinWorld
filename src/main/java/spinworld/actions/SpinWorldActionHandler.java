package spinworld.actions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.Inject;

import spinworld.MobilityService;
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
public class SpinWorldActionHandler implements ActionHandler {

    final private Logger logger = Logger.getLogger(SpinWorldActionHandler.class);
	
	// Environmental properties for controlling movement of agents within global environment
	final protected EnvironmentServiceProvider serviceProvider;
	final protected HasArea environment;
	final protected EnvironmentSharedStateAccess sharedState;
	
	final StatefulKnowledgeSession session;
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();

	MobilityService mobilityService = null;
	NetworkService networkService = null;
	SpinWorldService spinWorldService = null;
	
	@Inject
	public SpinWorldActionHandler(StatefulKnowledgeSession session,
			HasArea environment,
			EnvironmentServiceProvider serviceProvider,
			EnvironmentSharedStateAccess sharedState)
			throws UnavailableServiceException {
		super();
		this.session = session;
		this.environment = environment;
		this.serviceProvider = serviceProvider;
		this.sharedState = sharedState;
	}
	
	@Override
	public boolean canHandle(Action action) {
		return (action instanceof ParticleAction) || (action instanceof Move);
	}
	
	MobilityService getMobilityService() {
		if (mobilityService == null) {
			try {
				this.mobilityService = serviceProvider
						.getEnvironmentService(MobilityService.class);
			} catch (UnavailableServiceException e) {
				logger.warn("Could not load mobility service", e);
			}
		}
		
		return mobilityService;
	}
	
	NetworkService getNetworkService() {
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
	
	SpinWorldService getSpinWorldService() {
		if (this.spinWorldService == null) {
			try {
				this.spinWorldService = serviceProvider.getEnvironmentService(SpinWorldService.class);
			} catch (UnavailableServiceException e) {
				logger.warn("Could not get SpinWorld service", e);
			}
		}
		
		return this.spinWorldService;
	}
	
	private synchronized Particle getParticle(final UUID id) {
		if (!particles.containsKey(id)) {
			Collection<Object> rawParticles = session
					.getObjects(new ObjectFilter() {
						@Override
						public boolean accept(Object object) {
							return object instanceof Particle;
						}
					});
			for (Object pObj : rawParticles) {
				Particle p = (Particle) pObj;
				particles.put(p.getId(), p);
			}
		}
		return particles.get(id);
	}
	
	@Override
	public Input handle(Action action, UUID actor)
			throws ActionHandlingException {
		getMobilityService();
		getNetworkService();
		
		Particle p = getParticle(actor);
		
		// Perform resource allocation action
		if (action instanceof ParticleAction) {
			if (logger.isDebugEnabled())
				logger.debug("Handling: " + action);

			((ParticleAction) action).setParticle(p);
		}
		
		// Time stamp resource allocation action
		if (action instanceof TimeStampedAction) {
			if (logger.isDebugEnabled())
				logger.debug("Handling: " + action);
			
			((TimeStampedAction) action).setT(getSpinWorldService().getRoundNumber());
		}
	
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
			Set<Particle> collidedParticles = this.mobilityService.getCollidedAgents(actor, target);

			if (!collidedParticles.isEmpty()) {
				for (Particle cp : collidedParticles) {
					this.networkService.assignLink(actor, cp);	
				}
			}
		}
		
		session.insert(action);

		return null;
	}

}
