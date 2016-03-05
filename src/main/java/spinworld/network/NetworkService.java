package spinworld.network;

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

@Singleton
public class NetworkService extends EnvironmentService {

	final private Logger logger = Logger.getLogger(this.getClass());
	Map<UUID, Particle> particles = new HashMap<UUID, Particle>();
	Set<Network> networks = new CopyOnWriteArraySet<Network>();
	
	private int numNetworks = 0;
	final protected EnvironmentServiceProvider serviceProvider;
	
	// Radius of an agents
	@Inject
	@Named("params.radius")
	private int radius;
	
	// Type of allocation between networks
	@Inject
	@Named("params.allocation")
	private String allocation;
	
	@Inject
	protected NetworkService(EnvironmentSharedStateAccess sharedState,
			EnvironmentServiceProvider serviceProvider) {
		super(sharedState);
		this.serviceProvider = serviceProvider;
	}
	
	@Override
	public void registerParticipant(EnvironmentRegistrationRequest req) {
		UUID id = req.getParticipantID();
		NetworkAgent ag = (NetworkAgent) req.getParticipant();
		Particle p = new Particle(id, ag.getName(), radius);
		particles.put(id, p);
	}
	
	private synchronized Particle getParticle(final UUID id) {
		return particles.get(id);
	}
	
	public int getAgentNoLinks(UUID particle) {
		return getParticle(particle).getNoLinks();
	}
	
	public Network getAgentNetwork(UUID particle) {
		Particle p = getParticle(particle);
		Network result = null;
		
		for (Network net : this.networks) {
			if (net.containsParticle(p)) {
				result = net;
				break;
			}
			else {
				// Do Nothing
			}
		}
		
		return result;
	}
	
	public void incrementAgentNoLinks(UUID particle) {
		getParticle(particle).incrementNoLinks();
	}
	
	private int getNextNumNetwork(){
		// Clear?
		return this.numNetworks++;
	}
	
	public void formLinks(UUID id, Set<Particle> collidedAgents) {
		Particle particle = getParticle(id);
		
		for (Particle p : collidedAgents) {
			// Establish a connection
			logger.info("Establishing a connection between particles " + particle.getName() 
				+ " and " + p.getName());
			establishConnection(particle, p);
		}	
	}
	
	private void establishConnection(Particle particle, Particle otherParticle) {
		Connection conn = new Connection(particle, otherParticle);
		boolean isConnected = false;
		
		if (this.networks.isEmpty()) {
			logger.info("No network currently exists, creating the first one.");
			int id = getNextNumNetwork();
			Network newNet = new Network(id);
			addNetworkConnections(newNet, conn);
			this.networks.add(newNet);
			isConnected = true;
		}
		else {
			for (Network net : this.networks) {
				if (net.connectionExists(conn)) {
					logger.info("Connection already exists in network " + net.getId());
					isConnected = true;
					break;
				}
				else if ((net.containsParticle(particle) || net.containsParticle(otherParticle))
						&& !net.connectionExists(conn)) {
					logger.info("Adding " + conn.toString() + " to network " + net.getId());
					addNetworkConnections(net, conn);
					isConnected = true;
					break;
				}
			}
		}
		
		if (!isConnected) {
			int id = getNextNumNetwork();
			Network newNet = new Network(id);
			logger.info("Creating a new network with id " + id + " for " + conn.toString());
			addNetworkConnections(newNet, conn);
			this.networks.add(newNet);
		}		
	}
	
	private void addNetworkConnections(Network net, Connection conn) {
		Particle x = conn.getParticleX();
		Particle y = conn.getParticleY();
		incrementAgentNoLinks(x.getId());
		incrementAgentNoLinks(y.getId());
		net.addConnection(conn);
	}
	
	public void printNetworks(Time t) {	
		logger.info("Total number of networks existing at time cycle " + t + " is: ");
		for (Network net : this.networks) {
			logger.info(net.toString());
		}	
	}
	
	// For all agents that have formed a network larger than threshold
	// in terms of population, make them have 0 velocity i.e. static
	/*public void setStaticNetworkAgents() {
		for (UUID id : this.particles.keySet()) {
			if (getAgentNoCollisions(id) >= (int)(networkThreshold * particles.size())) {
				logger.info("Agent " + getAgentName(id) + " is part of a sufficient social network and is therefore static.");
				setAgentVelocity(id, 0);
			}
		}
	}*/

}
