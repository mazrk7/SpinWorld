package spinworld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import spinworld.facts.Particle;
import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.environment.ServiceDependencies;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationService;
import uk.ac.imperial.presage2.util.location.area.AreaService;

@ServiceDependencies({ LocationService.class, AreaService.class })
@Singleton
public class MobilityService extends EnvironmentService {

	final private Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;

	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	
	final protected EnvironmentServiceProvider serviceProvider;
	LocationService locationService = null;
	
	// Influential variability factor of velocity 
	@Inject
	@Named("params.vConst")
	private int vConst;
		
	@Inject
	protected MobilityService(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider,
			StatefulKnowledgeSession session, EventBus eb) {
		super(sharedState);
		this.serviceProvider = serviceProvider;
		this.session = session;
		eb.subscribe(this);
	}
	
	protected LocationService getLocationService() {
		if (locationService == null) {
			try {
				this.locationService = serviceProvider
						.getEnvironmentService(LocationService.class);
			} catch (UnavailableServiceException e) {
				logger.warn("Could not load location service", e);
			}
		}
		return locationService;
	}
	
	@Override
	public void registerParticipant(EnvironmentRegistrationRequest req) {

	}
	
	// Query session in order to access players within environment shared state
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
	
	public Location getAgentLocation(UUID particle) {
		locationService = getLocationService();
		return locationService.getAgentLocation(particle);
	}
	
	public void setAgentLocation(final UUID particle, final Location loc) {
		locationService = getLocationService();
		locationService.setAgentLocation(particle, loc);
		getParticle(particle).setLoc(loc);
	}
	
	public int getAgentVelocity(UUID particle) {
		return getParticle(particle).getVelocity();
	}
	
	public void setAgentVelocity(final UUID particle, final int velocity) {
		getParticle(particle).setVelocity(velocity);
	}
	
	public String getAgentName(UUID particle) {
		return getParticle(particle).getName();
	}

	public int getAgentNoCollisions(UUID particle) {
		return getParticle(particle).getNoCollisions();
	}
	
	public void onAgentCollision(UUID particle, UUID otherParticle) {
		getParticle(particle).incrementCollisionCount();
		getParticle(otherParticle).incrementCollisionCount();
		logger.info("Collision between particles " + getAgentName(particle) + " and " + getAgentName(otherParticle));
		
		updateAgentVelocity(particle);
		updateAgentVelocity(otherParticle);
	}
	
	public void updateAgentVelocity(UUID particle) {
		setAgentVelocity(particle, getAgentVelocity(particle) + vConst*getAgentNoCollisions(particle));
	}
	
	public Set<Particle> getCollidedAgents(final UUID pId, final Location target) {
		Set<Particle> collidedAgents = new CopyOnWriteArraySet<Particle>();
		Particle particle = getParticle(pId);
		
		for (UUID otherId : this.particles.keySet()) {
			Particle otherParticle = getParticle(otherId);
			
			if (!particle.equals(otherParticle) && target.equals(getAgentLocation(otherId))) {
				onAgentCollision(pId, otherId);			
				collidedAgents.add(otherParticle);
			}
		}
		
		return collidedAgents;
	}
	
	public void printCollisions(Time t) {	
		logger.info("Total number of collisions at time cycle " + t + " is: ");
		for (UUID id : this.particles.keySet()) {
			logger.info(getAgentNoCollisions(id) + " for particle " + getAgentName(id));
		}	
	}
	
}