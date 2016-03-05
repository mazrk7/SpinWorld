package spinworld;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.QueryResults;
import org.drools.runtime.rule.QueryResultsRow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import spinworld.facts.Particle;
import spinworld.network.MemberOf;
import spinworld.network.Network;
import spinworld.facts.Round;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

@Singleton
public class ResourceAllocationService extends EnvironmentService {
	
	private final Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;
	
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	Map<UUID, MemberOf> members = new HashMap<UUID, MemberOf>();
	Set<Network> networks = new CopyOnWriteArraySet<Network>();
	
	RoundType round = RoundType.INIT;
	int roundNumber = 0;
	private int numNetworks = 0;
	
	@Inject
	protected ResourceAllocationService(EnvironmentSharedStateAccess sharedState,
			StatefulKnowledgeSession session, EventBus eb) {
		super(sharedState);
		this.session = session;
		eb.subscribe(this);
	}
	
	// Move to next stage of round or next round altogether
	@EventListener
	public void onIncrementTime(EndOfTimeCycle e) {
		if (round == RoundType.DEMAND) {
			round = RoundType.APPROPRIATE;
			session.insert(new Round(roundNumber, RoundType.APPROPRIATE));
		} else {
			round = RoundType.DEMAND;
			session.insert(new Round(++roundNumber, RoundType.DEMAND));
		}
		
		logger.info("Next round: " + round);
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

	// Similarly query session for access to MemberOf structure
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

	public RoundType getRound() {
		return round;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public double getG(UUID particle) {
		return getParticle(particle).getG();
	}

	public double getQ(UUID particle) {
		return getParticle(particle).getQ();
	}

	public double getAllocated(UUID particle) {
		return getParticle(particle).getAllocated();
	}

	public double getAppropriated(UUID particle) {
		return getParticle(particle).getAppropriated();
	}

	public Network getNetwork(final UUID particle) {
		MemberOf m = getMemberOf(particle);
		if (m != null)
			return m.getNetwork();
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
			
			// Add clusters to list
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

	// Get particles that are not part of a network - I think (not called now)
	public Set<UUID> getOrphanParticles(){
		Set<UUID> op = new HashSet<UUID>();
		for (Entry<UUID, Particle> p : this.particles.entrySet()){
			if (getNetwork(p.getKey()) == null){
				op.add(p.getKey());
			}
		}
		return op;
	}
	
}
