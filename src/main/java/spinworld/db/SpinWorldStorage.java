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
					.execute("CREATE TABLE IF NOT EXISTS `particleScore` ("
							+ "`simID` bigint(20) NOT NULL,"
							+ "`particle` varchar(10) NOT NULL,"
							+ "`round` int(11) NOT NULL,"
							+ "`g` double NOT NULL,"
							+ "`q` double NOT NULL,"
							+ "`d` double NOT NULL,"
							+ "`p` double NOT NULL,"
							+ "`r` double NOT NULL,"
							+ "`rP` double NOT NULL,"
							+ "`rTotal` double NOT NULL,"
							+ "`satisfaction` double NOT NULL,"
							+ "`U` double NOT NULL,"
							+ "`network` int(11) NOT NULL,"
							+ "`pCheat` double NOT NULL,"
							+ "`catchRate` double NOT NULL,"
							+ "`risk` double NOT NULL,"
							+ "PRIMARY KEY (`simID`,`particle`,`round`),"
							+ "KEY `simID` (`simID`),"
							+ "KEY `particle` (`particle`),"
							+ "KEY `round` (`round`),"
							+ "KEY `network` (`network`),"
							+ "FOREIGN KEY (`simID`) REFERENCES `simulations` (`ID`) ON DELETE CASCADE"
							+ ")");

			createTables
					.execute("CREATE TABLE IF NOT EXISTS `aggregatedParticleScore` ("
							+ "`simID` bigint(20) NOT NULL,"
							+ "`particle` varchar(10) NOT NULL,"
							+ "`network` int(11) NOT NULL,"
							+ "`USum` double NOT NULL,"
							+ "PRIMARY KEY (`simID`, `particle`,`network`),"
							+ "KEY `simID` (`simID`),"
							+ "KEY `particle` (`particle`),"
							+ "KEY `network` (`network`),"
							+ "KEY `simID-network` (`simID`, `network`),"
							+ "FOREIGN KEY (`simID`) REFERENCES `simulations` (`ID`) ON DELETE CASCADE );");

			createTables
					.execute("CREATE TABLE IF NOT EXISTS `simulationSummary` ("
							+ "`ID` bigint(20) NOT NULL,"
							+ "`Name` varchar(255) NOT NULL,"
							+ "`network` int(11) NOT NULL,"
							+ "`ut.` double NOT NULL,"
							+ "`stddev ut.` double NOT NULL,"
							+ "`total ut.` double NOT NULL,"
							+ "`rem.` int NOT NULL,"
							+ "PRIMARY KEY (`ID`, `network`),"
							+ "KEY `Name` (`Name`),"
							+ "FOREIGN KEY (`ID`) REFERENCES `simulations` (`ID`) ON DELETE CASCADE );");
			
			createTables.execute("CREATE TABLE IF NOT EXISTS `particles` ("
					+ "`simID` bigint(20) NOT NULL,"
					+ "`name` varchar(10) NOT NULL,"
					+ "`pCheat` double NOT NULL,"
					+ "`cheatOn` char(1) NOT NULL,"
					+ "PRIMARY KEY (`simID`,`name`))");
			
			createTables
					.execute("CREATE TABLE IF NOT EXISTS `networks` ("
							+ "`simID` bigint(20) NOT NULL,"
							+ "`network` int(11) NOT NULL,"
							+ "`method` varchar(255) NOT NULL,"
							+ "`created` int(11) NOT NULL,"
							+ "PRIMARY KEY (`simID`, `network`),"
							+ "FOREIGN KEY (`simID`) REFERENCES `simulations` (`ID`) ON DELETE CASCADE );");
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

		/* Insert network information */
		try {
			insertNetwork = conn
					.prepareStatement("INSERT IGNORE INTO networks "
							+ "(simID, network, method, created) "
							+ "VALUES (?, ?, ?, ?) ");
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
			}

			batchQueryQ.put(insertNetwork);
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
		
		try {
			insertParticle = conn
					.prepareStatement("REPLACE INTO particleScore "
							+ "(simID, particle, round, g, q, d, p, r, rP, rTotal, satisfaction, U, network, pCheat, catchRate, risk)  "
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
			insertParticle = conn.prepareStatement("INSERT INTO particles "
					+ "(simID, name, pCheat, cheatOn)  "
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
