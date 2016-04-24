package spinworld.network;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.QueryResultsRow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import spinworld.facts.Particle;
import uk.ac.imperial.presage2.core.Time;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventBus;

@Singleton
public class NetworkService extends EnvironmentService {

	final private Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;

	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	Map<UUID, MemberOf> members = new HashMap<UUID, MemberOf>();
	Set<Network> networks = new CopyOnWriteArraySet<Network>();
	
	private int numNetworks = 0;
	final protected EnvironmentServiceProvider serviceProvider;
	
	@Inject
	protected NetworkService(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider,
			StatefulKnowledgeSession session, EventBus eb) {
		super(sharedState);
		this.serviceProvider = serviceProvider;
		this.session = session;
		eb.subscribe(this);
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
	
	// Similarly query session for access to MemberOf structures
	private synchronized MemberOf getMemberOf(final UUID id) {
		MemberOf m = members.get(id);
		
		if (m == null || session.getFactHandle(m) == null) {
			members.remove(id);
			QueryResults results = session.getQueryResults("getMemberOf",
					new Object[] { getParticle(id) });
			
			for (QueryResultsRow row : results) {
				members.put(id, (MemberOf) row.get("m"));
				return members.get(id);
			}
			
			return null;
		}
		
		return m;
	}
	
	public Network getNetwork(final UUID particle) {
		MemberOf m = getMemberOf(particle);
		
		if (m != null)
			return m.getNetwork();
		else if (isReserved(particle))
			return getReservedNetwork(particle);
		else
			return null;
	}

	public synchronized Set<Network> getNetworks() {
		// If we allow for dynamic creation of network
		// we must check which ones exist every time
		if (this.networks.isEmpty()) {
			// Check which ones currently exist
			Collection<Object> networkSearch = session
					.getObjects(new ObjectFilter() {
						@Override
						public boolean accept(Object object) {
							return object instanceof Network;
						}
					});
			
			// Add networks to list
			for (Object object : networkSearch) {
				this.networks.add((Network) object);
			}
		}
		
		return Collections.unmodifiableSet(this.networks);
	}
	
	public int getNumNetworks(){
		return getNetworks().size();
	}
	
	// Update network integer number
	public int getNextNumNetwork(){
		this.networks.clear();
		return numNetworks++;
	}
	
	public int getNoLinks(final UUID particle) {
		return getParticle(particle).getNoLinks();
	}
	
	public void assignLink(UUID id, Particle p) {	
		getParticle(id).assignLink(p);
		p.assignLink(getParticle(id));
	}
	
	public synchronized Set<Particle> getLinks(UUID id) {	
		return getParticle(id).getLinks();
	}
	
	public void removeLink(UUID id, Particle p) {	
		getParticle(id).removeLink(p);	
		p.removeLink(getParticle(id));
	}
	
	public void detachLinks(UUID id) {
		Set<Particle> linkedParticles = getLinks(id);
		Particle p = getParticle(id);
		
		for (Particle lp : linkedParticles) {
			removeLink(id, lp);
			removeLink(lp.getId(), p);
		}
		
		if (p.getNoLinks() != 0)
			logger.info("Error detaching links from particle " + p.getName());
	}
	
	public void reserveSlot(UUID id, Network net) {
		getParticle(id).reserveSlot(net);
	}
	
	public void occupySlot(UUID id) {
		getParticle(id).occupySlot();
	}
	
	public boolean isReserved(UUID id) {
		return getParticle(id).isReserved();
	}
	
	public Network getReservedNetwork(UUID id) {
		return getParticle(id).getReservation();
	}

	// Get particles that are not part of a network
	public Set<UUID> getOrphanParticles(){
		Set<UUID> op = new HashSet<UUID>();
		
		for (Entry<UUID, Particle> p : this.particles.entrySet()){
			if (getNetwork(p.getKey()) == null){
				op.add(p.getKey());
			}
		}
		
		return op;
	}
	
	public void printNetworks(Time t) {	
		Set<Network> roundNetworks = getNetworks();
		
		if(!roundNetworks.isEmpty()) {
			logger.info("Total number of networks existing at time cycle " + t + " is: " + roundNetworks.size());
			
			for (Network net : roundNetworks) {
				logger.info("For " + net + ", the particles are:");
				for (Entry<UUID, MemberOf> mem : this.members.entrySet()){
					if (mem.getValue().network != null && mem.getValue().network.equals(net))
						logger.info(mem.getValue());
				}
			}
		}
		else
			logger.info("Total number of networks existing at time cycle " + t + " is: 0");
	}
	
	// For all agents that have formed a network larger than threshold
	// in terms of population, make them have 0 velocity i.e. static
	/* public void setStaticNetworkAgents() {
		for (UUID id : this.particles.keySet()) {
			if (getAgentNoCollisions(id) >= (int)(networkThreshold * particles.size())) {
				logger.info("Agent " + getAgentName(id) + " is part of a sufficient social network and is therefore static.");
				setAgentVelocity(id, 0);
			}
		}
	} */

}
