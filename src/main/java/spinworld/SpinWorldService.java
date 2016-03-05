package spinworld;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;

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
import uk.ac.imperial.presage2.util.location.Location;
import uk.ac.imperial.presage2.util.location.LocationService;
import uk.ac.imperial.presage2.util.location.area.AreaService;

@ServiceDependencies({ LocationService.class, AreaService.class })
@Singleton
public class SpinWorldService extends EnvironmentService {

	final private Logger logger = Logger.getLogger(this.getClass());
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	
	final protected EnvironmentServiceProvider serviceProvider;
	LocationService locationService = null;
	
	// Influential variability factor of velocity 
	@Inject
	@Named("params.vConst")
	private int vConst;
	
	// Radius of agents
	@Inject
	@Named("params.radius")
	private double radius;
		
	@Inject
	protected SpinWorldService(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider) {
		super(sharedState);
		this.serviceProvider = serviceProvider;
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
		UUID id = req.getParticipantID();
		SpinWorldAgent ag = (SpinWorldAgent) req.getParticipant();
		Location loc = getAgentLocation(id);
		Particle p = new Particle(id, ag.getName(), radius);
		p.setLoc(loc);
		p.setVelocity(ag.velocity);
		this.particles.put(id, p);
	}
	
	private synchronized Particle getParticle(final UUID id) {
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
	
	public void incrementAgentCollisionCount(UUID particle) {
		getParticle(particle).incrementCollisionCount();
	}
	
	public void onAgentCollision(UUID particle, UUID otherParticle) {
		incrementAgentCollisionCount(particle);
		incrementAgentCollisionCount(otherParticle);
		logger.info("Collision between particles " + getAgentName(particle) + " and " + getAgentName(otherParticle));
	}
	
	public void updateAgentVelocities(UUID particle, UUID otherParticle) {
		setAgentVelocity(particle, getAgentVelocity(particle) + vConst*getAgentNoCollisions(particle));
		setAgentVelocity(otherParticle, getAgentVelocity(otherParticle) + vConst*getAgentNoCollisions(otherParticle));
	}
	
	public Set<Particle> getCollidedAgents(final UUID pId, final Location target) {
		Set<Particle> collidedAgents = new CopyOnWriteArraySet<Particle>();
		Particle particle = getParticle(pId);
		
		for (UUID otherId : this.particles.keySet()) {
			Particle otherParticle = getParticle(otherId);
			
			if (!particle.equals(otherParticle) && target.equals(getAgentLocation(otherId))) {
				onAgentCollision(pId, otherId);
				updateAgentVelocities(pId, otherId);
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
