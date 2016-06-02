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
	
	// Similarly query session for access to MemberOf structures
	private synchronized Set<MemberOf> getMembersOfNet(final Network net) {	
		Set<MemberOf> netMembers = new CopyOnWriteArraySet<MemberOf>();
		
		QueryResults results = session.getQueryResults("networkMembers",
				new Object[] { net });
		
		for (QueryResultsRow row : results) {
			netMembers.add((MemberOf) row.get("m"));
		}
		
		return Collections.unmodifiableSet(netMembers);
	}
	
	public synchronized Network getNetwork(final UUID pId) {
		MemberOf m = getMemberOf(pId);
					
		if (m != null)
			return m.getNetwork();
		else
			return null;
	}

	public synchronized Set<Network> getNetworks() {
		networks.clear();
		
		// If we allow for dynamic creation of clusters,
		// we must check which ones exist every time
		Collection<Object> netSearch = session
				.getObjects(new ObjectFilter() {

					@Override
					public boolean accept(Object object) {
						return object instanceof Network;
					}
				});
			
		for (Object object : netSearch) {
			networks.add((Network) object);
		}
		
		return Collections.unmodifiableSet(networks);
	}
	
	public int getNumNetworks(){
		return getNetworks().size();
	}
	
	// Update network integer number
	public int getNextNumNetwork(){
		return numNetworks++;
	}
	
	public int getNoLinks(final UUID pId, final Network net) {
		if (!getMembersOfNet(net).isEmpty())
			return getMembersOfNet(net).size();
		else
			return 0;
	}
	
	// Create membership for the two particles forming a network
	public void createMembership(final UUID pId, final Particle collision, final Network net) {
		session.insert(new MemberOf(getParticle(pId), net));	
		session.insert(new MemberOf(collision, net));
	}
	
	// Join membership of this particle to a network
	public void joinMembership(final UUID pId, final UUID jpId, final Network net) {
		session.insert(new MemberOf(getParticle(pId), net));	
	}
	
	// Retract membership of this particle from a network
	public void retractMembership(final UUID pId) {
		if (session.getFactHandle(getMemberOf(pId)) != null)
			session.retract(session.getFactHandle(getMemberOf(pId)));
	}

	public boolean isBanned(final UUID pId, final Network net) {
		return net.isBanned(getParticle(pId));
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

		logger.info("Total number of networks existing at time cycle " + t + " is: " + roundNetworks.size());

		if (!roundNetworks.isEmpty()) {
			for (Network net : roundNetworks) {
				logger.info("For " + net + ", the particles are:");
				for (MemberOf mem : getMembersOfNet(net)){
					logger.info(mem);
				}
			}
		}
	}
	
}
