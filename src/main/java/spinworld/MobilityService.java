package spinworld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
	
	@Inject
	@Named("params.size")
	private int size;
	
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
	
	public Location getLocation(UUID particle) {
		locationService = getLocationService();
		return locationService.getAgentLocation(particle);
	}
	
	public void setLocation(final UUID particle, final Location loc) {
		locationService = getLocationService();
		locationService.setAgentLocation(particle, loc);
		getParticle(particle).setLoc(loc);
	}
	
	public int getVelocity(UUID particle) {
		return getParticle(particle).getVelocity();
	}
	
	public void setVelocity(final UUID particle, final int velocity) {
		getParticle(particle).setVelocity(velocity);
	}
	
	// Limited velocity change to dimensions of environment
	private void updateVelocity(final UUID particle) {
		int updateVelocity = getVelocity(particle) + vConst * getNoCollisions(particle);	
		
		if (updateVelocity > size)
			setVelocity(particle, size);
		else
			setVelocity(particle, updateVelocity);
	}

	public int getNoCollisions(UUID particle) {
		return getParticle(particle).getNoCollisions();
	}
	
	public void collide(UUID particle, UUID otherParticle) {
		Particle a = getParticle(particle);
		Particle b = getParticle(otherParticle);
		
		a.collide(b);
		b.collide(a);
		logger.info("Collision between particles " + a.getName() + " and " + b.getName());
		
		updateVelocity(particle);
		updateVelocity(otherParticle);
	}
	
	public synchronized Set<Particle> getCollisions(UUID id) {	
		return getParticle(id).getCollisions();
	}	
	
	public void clearCollisions(UUID id) {
		getParticle(id).clearCollisions();
	}
	
	public void checkForCollisions(final UUID pId, final Location target) {
		Particle particle = getParticle(pId);
		
		for (UUID otherId : this.particles.keySet()) {
			Particle otherParticle = getParticle(otherId);
			
			if (!particle.equals(otherParticle) && target.equals(getLocation(otherId)))
				collide(pId, otherId);	
		}	
	}
	
	public void printCollisions(Time t) {	
		int totalCollisions = 0;
		
		for (UUID id : this.particles.keySet()) {
			logger.info(getNoCollisions(id) + " collision(s) for particle " + getParticle(id).getName());
			totalCollisions += getNoCollisions(id);
		}	
		
		logger.info("Total number of collisions at time cycle " + t + " is: " + totalCollisions);
	}
	
}
