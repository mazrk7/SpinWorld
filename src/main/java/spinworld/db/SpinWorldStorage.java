package spinworld.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import uk.ac.imperial.presage2.core.environment.EnvironmentServiceProvider;
import uk.ac.imperial.presage2.core.environment.UnavailableServiceException;
import uk.ac.imperial.presage2.db.sql.Agent;
import uk.ac.imperial.presage2.db.sql.SqlStorage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import spinworld.SpinWorldService;
import spinworld.network.Network;
import spinworld.network.NetworkService;

public class SpinWorldStorage extends SqlStorage {

	int maxRound = -1;

	boolean shutdown = false;

	protected SpinWorldService world = null;
	protected NetworkService netWorld = null;
	protected Set<Network> added = new HashSet<Network>();

	@Inject
	public SpinWorldStorage(@Named(value = "sql.info") Properties jdbcInfo) {
		super(jdbcInfo);
	}

	@Inject(optional = true)
	public void setWorld(EnvironmentServiceProvider serviceProvider) {
		try {
			this.world = serviceProvider.getEnvironmentService(SpinWorldService.class);
			this.netWorld = serviceProvider.getEnvironmentService(NetworkService.class);
		} catch (UnavailableServiceException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void initTables() {
		super.initTables();
		Statement createTables = null;
		
		try {
			createTables = conn.createStatement();
			
			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"particleScore\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"particle\" varchar(10) NOT NULL,"
							+ "\"round\" int NOT NULL,"
							+ "\"g\" float NOT NULL,"
							+ "\"q\" float NOT NULL,"
							+ "\"d\" float NOT NULL,"
							+ "\"p\" float NOT NULL,"
							+ "\"r\" float NOT NULL,"
							+ "\"rP\" float NOT NULL,"
							+ "\"rTotal\" float NOT NULL,"
							+ "\"satisfaction\" float NOT NULL,"
							+ "\"U\" float NOT NULL,"
							+ "\"network\" int NOT NULL,"
							+ "\"pCheat\" float NOT NULL,"
							+ "\"catchRate\" float NOT NULL,"
							+ "\"risk\" float NOT NULL,"
							+ "PRIMARY KEY (\"simId\", \"particle\", \"round\"),"
							+ "FOREIGN KEY (\"simId\") REFERENCES \"simulations\" (\"id\") ON DELETE CASCADE"
							+ ");");
			
			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"networkScore\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"network\" int NOT NULL,"
							+ "\"round\" int NOT NULL,"
							+ "\"type\" char(1) NOT NULL,"
							+ "\"monitoringLevel\" float NOT NULL,"
							+ "\"banCount\" int NOT NULL,"
							+ "PRIMARY KEY (\"simId\", \"network\", \"round\"),"
							+ "FOREIGN KEY (\"simId\") REFERENCES \"simulations\" (\"id\") ON DELETE CASCADE"
							+ ");");

			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"aggregatedParticleScore\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"particle\" varchar(10) NOT NULL,"
							+ "\"network\" int NOT NULL,"
							+ "\"USum\" float NOT NULL,"
							+ "PRIMARY KEY (\"simId\", \"particle\",\"network\"),"
							+ "FOREIGN KEY (\"simId\") REFERENCES \"simulations\" (\"id\") ON DELETE CASCADE );");
		
			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"simulationSummary\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"name\" varchar(255) NOT NULL,"
							+ "\"network\" int NOT NULL,"
							+ "\"ut. C\" float NOT NULL,"
							+ "\"stddev ut. C\" float NOT NULL,"
							+ "\"ut. NC\" float NOT NULL,"
							+ "\"stddev ut. NC\" float NOT NULL,"
							+ "\"total ut.\" float NOT NULL,"
							+ "\"rem. C\" int NOT NULL,"
							+ "\"rem. NC\" int NOT NULL,"
							+ "PRIMARY KEY (\"simId\", \"network\"),"
							+ "FOREIGN KEY (\"simId\") REFERENCES \"simulations\" (\"id\") ON DELETE CASCADE );");
			
			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"particles\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"name\" varchar(10) NOT NULL,"
							+ "\"pCheat\" float NOT NULL,"
							+ "\"cheatOn\" char(1) NOT NULL,"
							+ "PRIMARY KEY (\"simId\",\"name\"));");
			
			createTables
					.execute("CREATE TABLE IF NOT EXISTS \"networks\" ("
							+ "\"simId\" bigint NOT NULL,"
							+ "\"network\" int NOT NULL,"
							+ "\"method\" varchar(255) NOT NULL,"
							+ "\"created\" int NOT NULL,"
							+ "PRIMARY KEY (\"simId\", \"network\"),"
							+ "FOREIGN KEY (\"simId\") REFERENCES \"simulations\" (\"id\") ON DELETE CASCADE );");
		} catch (SQLException e) {
			logger.warn("", e);
			throw new RuntimeException(e);
		} finally {
			if (createTables != null) {
				try {
					createTables.close();
				} catch (SQLException e) {
					logger.warn(e);
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	protected synchronized void updateTransientEnvironment() {
		PreparedStatement insertNetwork = null;
		PreparedStatement insertNetworkSummary = null;
		PreparedStatement deleteNetworkSummary = null;

		/* Insert network information */
		try {
			insertNetwork = conn
					.prepareStatement("INSERT INTO \"networks\" "
							+ "(\"simId\", \"network\", \"method\", \"created\") "
							+ "VALUES (?, ?, ?, ?) "
							+ "ON CONFLICT (\"simId\", \"network\")"
							+ "DO NOTHING");
			
			deleteNetworkSummary = conn.
					prepareStatement("DELETE FROM \"networkScore\" WHERE \"simId\" = ?"
							+ "AND \"network\" = ? AND \"round\" = ?");
			
			insertNetworkSummary = conn
					.prepareStatement("INSERT INTO \"networkScore\" "
							+ "(\"simId\", \"network\", \"round\", \"type\", \"monitoringLevel\", \"banCount\")  "
							+ "VALUES (?, ?, ?, ?, ?, ?) ");
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		}

		try {
			for (Network n : this.netWorld.getNetworks()) {
				insertNetwork.setLong(1, this.simId);
				insertNetwork.setInt(2, n.getId());
				insertNetwork.setString(3, n.getAllocationMethod().toString());
				insertNetwork.setInt(4, this.world.getRoundNumber());

				insertNetwork.addBatch();

				deleteNetworkSummary.setLong(1, this.simId);
				deleteNetworkSummary.setInt(2, n.getId());
				deleteNetworkSummary.setInt(3, this.world.getRoundNumber());
				deleteNetworkSummary.addBatch();

				insertNetworkSummary.setLong(1, this.simId);
				insertNetworkSummary.setInt(2, n.getId());
				insertNetworkSummary.setInt(3, this.world.getRoundNumber());
				insertNetworkSummary.setString(4, n.getType());
				insertNetworkSummary.setDouble(5, n.getMonitoringLevel());
				insertNetworkSummary.setInt(6, n.getNoBannedParticles());
				
				insertNetworkSummary.addBatch();
			}

			batchQueryQ.put(insertNetwork);
			batchQueryQ.put(deleteNetworkSummary);
			batchQueryQ.put(insertNetworkSummary);
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

	}

	protected double getProperty(Map<String, String> properties, String key,
			double defaultValue) {
		if (properties.containsKey(key))
			return Double.parseDouble(properties.get(key));
		else
			return defaultValue;
	}

	@Override
	protected synchronized void updateTransientAgents() {
		PreparedStatement insertParticle = null;
		PreparedStatement deleteParticle = null;

		try {
			deleteParticle = conn.
					prepareStatement("DELETE FROM \"particleScore\" WHERE \"simId\" = ?"
							+ "AND \"particle\" = ? AND \"round\" = ?");
			
			insertParticle = conn
					.prepareStatement("INSERT INTO \"particleScore\" "
							+ "(\"simId\", \"particle\", \"round\", \"g\", \"q\", \"d\", \"p\", \"r\", \"rP\", "
							+ "\"rTotal\", \"satisfaction\", \"U\", \"network\", \"pCheat\", \"catchRate\", \"risk\")  "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ");
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		}

		try {
			Set<Agent> notfullyProcessed = new HashSet<Agent>();
			
			for (Agent a : agentTransientQ) {
				List<Integer> forRemoval = new LinkedList<Integer>();
				for (Map.Entry<Integer, Map<String, String>> round : a.transientProperties
						.entrySet()) {
					if (!shutdown && world != null
							&& round.getKey() >= world.getRoundNumber() - 2) {
						notfullyProcessed.add(a);
						continue;
					}

					Map<String, String> props = round.getValue();

					if (!props.containsKey("g"))
						continue;

					deleteParticle.setLong(1, this.simId);
					deleteParticle.setString(2, a.getName());
					deleteParticle.setInt(3, round.getKey() - 1);
					deleteParticle.addBatch();

					insertParticle.setLong(1, a.simId);
					insertParticle.setString(2, a.getName());
					insertParticle.setInt(3, round.getKey() - 1);

					insertParticle.setDouble(4, getProperty(props, "g", 0.0));
					insertParticle.setDouble(5, getProperty(props, "q", 0.0));
					insertParticle.setDouble(6, getProperty(props, "d", 0.0));
					insertParticle.setDouble(7, getProperty(props, "p", 0.0));
					insertParticle.setDouble(8, getProperty(props, "r", 0.0));
					insertParticle.setDouble(9, getProperty(props, "r'", 0.0));
					insertParticle.setDouble(10,
							getProperty(props, "RTotal", 0.0));
					insertParticle.setDouble(11, getProperty(props, "o", 0.0));
					insertParticle.setDouble(12, getProperty(props, "U", 0.0));
					insertParticle.setDouble(14,
							getProperty(props, "pCheat", 0.0));
					insertParticle.setDouble(15,
							getProperty(props, "catchRate", 0.0));
					insertParticle.setDouble(16,
							getProperty(props, "risk", 0.0));
					
					if (props.containsKey("network"))
						insertParticle.setInt(13,
								Integer.parseInt(props.get("network")));
					else
						insertParticle.setInt(13, 0);

					insertParticle.addBatch();
					
					forRemoval.add(round.getKey());
				}
				
				for (Integer round : forRemoval) {
					a.transientProperties.remove(round);
				}
			}
			
			batchQueryQ.put(deleteParticle);
			batchQueryQ.put(insertParticle);
			agentTransientQ.clear();
			agentTransientQ.addAll(notfullyProcessed);
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected synchronized void updateAgents() {
		PreparedStatement insertParticle = null;
		
		try {
			insertParticle = conn.prepareStatement("INSERT INTO \"particles\" "
					+ "(\"simId\", \"name\", \"pCheat\", \"cheatOn\")  "
					+ "VALUES (?, ?, ?, ?) ");
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		}

		try {
			for (Agent a : agentQ) {
				insertParticle.setLong(1, simId);
				insertParticle.setString(2, a.getName());
				insertParticle.setDouble(3,
						getProperty(a.properties, "pCheat", 0.0));
				insertParticle.setString(4, a.properties.get("cheatOn")
						.substring(0, 1));
				
				insertParticle.addBatch();
			}
			
			batchQueryQ.put(insertParticle);
			agentQ.clear();
		} catch (SQLException e) {
			logger.warn(e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void stop() {
		this.shutdown = true;
		super.stop();
	}

}
