package spinworld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.drools.runtime.ObjectFilter;
import org.drools.runtime.StatefulKnowledgeSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import spinworld.facts.Particle;
import spinworld.facts.Round;
import spinworld.network.Network;
import uk.ac.imperial.presage2.core.environment.EnvironmentRegistrationRequest;
import uk.ac.imperial.presage2.core.environment.EnvironmentService;
import uk.ac.imperial.presage2.core.environment.EnvironmentSharedStateAccess;
import uk.ac.imperial.presage2.core.event.EventBus;
import uk.ac.imperial.presage2.core.event.EventListener;
import uk.ac.imperial.presage2.core.simulator.EndOfTimeCycle;

@Singleton
public class SpinWorldService extends EnvironmentService {
	
	private final Logger logger = Logger.getLogger(this.getClass());
	final StatefulKnowledgeSession session;
	
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	
	// Initialised round
	RoundType round = RoundType.INIT;
	int roundNumber = 0;
	
	@Inject
	protected SpinWorldService(EnvironmentSharedStateAccess sharedState,
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

	public RoundType getRound() {
		return round;
	}

	public int getRoundNumber() {
		return roundNumber;
	}

	public double getG(UUID pId) {
		return getParticle(pId).getG();
	}

	public double getQ(UUID pId) {
		return getParticle(pId).getQ();
	}

	public double getAllocated(UUID pId) {
		return getParticle(pId).getAllocated();
	}

	public double getAppropriated(UUID pId) {
		return getParticle(pId).getAppropriated();
	}
	
	public double getObservedCatchRate(UUID pId, Network net) {
		return getParticle(pId).getObservedCatchRate(net);
	}
	
	public double getRiskRate(UUID pId, Network net) {
		return getParticle(pId).getRiskRate(net);
	}
	
}
