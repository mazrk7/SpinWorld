package spinworld.mobility;

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
	Map<UUID, Set<Particle>> collisions = new HashMap<UUID, Set<Particle>>();
	int noCollisions = 0;

	final protected EnvironmentServiceProvider serviceProvider;
	LocationService locationService = null;
	AreaService areaService = null;
	
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
	
	public Location getLocation(UUID pId) {
		locationService = getLocationService();
		return locationService.getAgentLocation(pId);
	}
	
	public void setLocation(final UUID pId, final Location loc) {
		locationService = getLocationService();
		locationService.setAgentLocation(pId, loc);
		getParticle(pId).setLoc(loc);
	}
	
	public int getVelocity(UUID pId) {
		return getParticle(pId).getVelocity();
	}
	
	public void setVelocity(final UUID pId, final int velocity) {
		getParticle(pId).setVelocity(velocity);
	}
	
	// Limited velocity change to dimensions of environment
	public void updateVelocity(final UUID pId, int noLinks) {
		int updateVelocity = getVelocity(pId) + vConst * noLinks;	
		
		if (updateVelocity >= size)
			setVelocity(pId, size);
		else
			setVelocity(pId, updateVelocity);
	}
	
	public int getNoCollisions(final UUID pId) {
		if (this.collisions.containsKey(pId))
			return this.collisions.get(pId).size();
		else
			return 0;
	}
	
	public synchronized Set<Particle> getCollisions(final UUID pId) {	
		if (this.collisions.containsKey(pId))
			return this.collisions.get(pId);
		else
			return null;
	}	
	
	private void addCollision(UUID pId, Set<Particle> collisionCandidates) {		
		collisions.put(pId, collisionCandidates);
	}
	
	public void checkForCollisions(final UUID pId, final Location target) {		
		Set<Particle> collisionCandidates = new CopyOnWriteArraySet<Particle>();
		boolean collisionHappened = false;
		
		for (UUID otherId : this.particles.keySet()) {			
			if (!getParticle(pId).equals(getParticle(otherId)) && target.equals(getLocation(otherId))) {
				collisionCandidates.add(getParticle(otherId));
				
				logger.info("Collision between particles " + getParticle(pId).getName() + 
						" and " + getParticle(otherId).getName());
				this.noCollisions++;
				
				collisionHappened = true;
			}
		}
		
		if (collisionHappened)
			addCollision(pId, collisionCandidates);
	}
	
	public void clearCollisions() {
		this.collisions.clear();
	}
	
}
