package spinworld;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import spinworld.db.ConnectionlessStorage;
import spinworld.db.Queries;
import spinworld.gui.SpinWorldGUI;
import uk.ac.imperial.presage2.core.cli.Presage2CLI;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;
import uk.ac.imperial.presage2.core.simulator.RunnableSimulation;

public class SpinWorldCLI extends Presage2CLI {

	private final Logger logger = Logger.getLogger(SpinWorldCLI.class);

	protected SpinWorldCLI() {
		super(SpinWorldCLI.class);
	}

	public static void main(String[] args) {
		Presage2CLI cli = new SpinWorldCLI();
		cli.invokeCommand(args);
	}

	@Command(name = "insert", description = "Insert a batch of simulations to run.")
	public void insert_batch(String[] args) {
		Options options = new Options();

		// Generate experiment types
		Map<String, String> experiments = new HashMap<String, String>();
		experiments.put("large_pop", "Large population.");
		experiments.put("optimal", "Find the optimal cheat strategy.");
		experiments.put("cheat", "Test different cheating strategies.");

		OptionGroup exprOptions = new OptionGroup();
		for (String key : experiments.keySet()) {
			exprOptions.addOption(new Option(key, experiments.get(key)));
		}

		// Check for experiment type argument
		if (args.length < 2 || !experiments.containsKey(args[1])) {
			options.addOptionGroup(exprOptions);
			HelpFormatter formatter = new HelpFormatter();
			formatter.setOptPrefix("");
			formatter.printHelp("presage2cli insert <experiment>", options, false);

			return;
		}

		// Optional random seed arg
		options.addOption("s", "seed", true,
				"Random seed to start with (subsequent repeats use incrementing seeds from this value)");

		int repeats = 0;
		try {
			repeats = Integer.parseInt(args[2]);
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.warn("REPEATS argument missing");
		} catch (NumberFormatException e) {
			logger.warn("REPEATS argument is not a valid integer");
		}

		if (repeats <= 0) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("presage2cli insert " + args[1] + " REPEATS", options, true);

			return;
		}

		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		int seed = 0;

		try {
			cmd = parser.parse(options, args);
			seed = Integer.parseInt(cmd.getOptionValue("seed"));
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		} catch (NumberFormatException e) {
		} catch (NullPointerException e) {
		}

		if (args[1].equalsIgnoreCase("large_pop"))
			large_pop(repeats, seed);
		else if (args[1].equalsIgnoreCase("optimal"))
			optimal(repeats, seed);
		else if (args[1].equalsIgnoreCase("cheat"))
			cheatOnAppropriate(repeats, seed);
	}

	void large_pop(int repeats, int seed) {
		// int rounds = 2002;
		int rounds = 200;
		int agents = 100;

		for (int i = 0; i < repeats; i++) {
			for (double pProp : new double[] { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 }) {
				int pc = (int) Math.round(agents * pProp);

				PersistentSimulation sim = getDatabase().createSimulation(
						"LARGE_POP_" + String.format("%03d", pc) + "_pro", "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("agents", Integer.toString(agents - pc));
				sim.addParameter("cheat", Double.toString(0.02));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());

				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}

	void optimal(int repeats, int seed) {
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION, Cheat.APPROPRIATE };
		// int rounds = 2002;
		int rounds = 200;
		
		// Minority optimal
		for (int i = 0; i < repeats; i++) {
			for (Cheat ch : cheatMethods) {
				double ncStrat = 0.0;

				while (ncStrat <= 1.0) {
					String stratStr = Double.toString(ncStrat);
					stratStr = stratStr.substring(0, Math.min(4, stratStr.length()));

					PersistentSimulation sim = getDatabase().createSimulation(
							"MIN_" + stratStr + "_" + ch.name().substring(0, 3),
							"spinworld.SpinWorldSimulation", "AUTO START", rounds);

					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("size", Integer.toString(5));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("theta", Double.toString(0.1));
					sim.addParameter("phi", Double.toString(0.1));
					sim.addParameter("agents", Integer.toString(20));
					sim.addParameter("cheat", Double.toString(0.02));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("cheatOn", ch.name());

					ncStrat += 0.05;
				}
			}
		}

		// Majority optimal
		for (int i = 0; i < repeats; i++) {
			for (Cheat ch : cheatMethods) {
				double cStrat = 0.0;

				while (cStrat <= 1.0) {
					String stratStr = Double.toString(cStrat);
					stratStr = stratStr.substring(0, Math.min(4, stratStr.length()));

					PersistentSimulation sim = getDatabase().createSimulation(
							"MAJ_" + stratStr + "_" + ch.name().substring(0, 3),
							"spinworld.SpinWorldSimulation", "AUTO START", rounds);

					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("size", Integer.toString(5));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("gamma", Double.toString(0.1));
					sim.addParameter("theta", Double.toString(0.1));
					sim.addParameter("phi", Double.toString(0.1));
					sim.addParameter("agents", Integer.toString(20));
					sim.addParameter("cheat", Double.toString(cStrat));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("cheatOn", ch.name());

					cStrat += 0.05;
				}
			}
		}

		stopDatabase();
	}

	void cheatOnAppropriate(int repeats, int seed) {
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION, Cheat.APPROPRIATE };
		// int rounds = 1002;
		int rounds = 200;

		for (int i = 0; i < repeats; i++) {
			for (Cheat ch : cheatMethods) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"CHEAT_" + ch.name().substring(0, 3), "spinworld.SpinWorldSimulation", "AUTO START",
						rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("gamma", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("agents", Integer.toString(20));
				sim.addParameter("cheat", Double.toString(0.02));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", ch.name());

				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}

	@Command(name = "summarise", description = "Process raw simulation data to generate evaluation metrics.")
	public void summarise(String[] args) {
		logger.warn("This implementation assumes you are using postgresql >= 9.1 with hstore, it will fail otherwise.");

		// Get database to trigger injector creation
		getDatabase();
		// Pull JDBC connection from injector
		Connection conn = injector.getInstance(Connection.class);

		try {
			logger.info("Creating tables and views. ");

			logger.info("CREATE VIEW allocationRatios");
			conn.createStatement().execute(Queries.getQuery("create_allocationratios"));

			logger.info("CREATE TABLE simulationSummary");
			conn.createStatement().execute(Queries.getQuery("create_simulationsummary"));

			logger.info("CREATE VIEW aggregatedSimulations");
			conn.createStatement().execute(Queries.getQuery("create_aggregatedsimulations"));

			logger.info("CREATE TABLE aggregatedParticleScore");
			conn.createStatement().execute(Queries.getQuery("create_aggregatedparticlescore"));

			logger.info("Vacuuming database...");
			conn.createStatement().execute("VACUUM FULL");

			logger.info("Processing simulations...");

			// Prepare statements
			PreparedStatement aggregatedParticleScore = conn
					.prepareStatement(Queries.getQuery("insert_aggregatedparticlescore"));
			PreparedStatement networkStats = conn.prepareStatement(Queries.getQuery("select_networks"));
			PreparedStatement remaining = conn.prepareStatement(Queries.getQuery("select_agentsremaining"));
			PreparedStatement insertSummary = conn.prepareStatement(Queries.getQuery("insert_simulationsummary"));

			// Get subset to process
			ResultSet unprocessed = conn.createStatement()
					.executeQuery(Queries.getQuery("select_unprocessedsimulations"));

			while (unprocessed.next()) {
				long id = unprocessed.getLong(1);
				String name = unprocessed.getString(2);
				int finishTime = unprocessed.getInt(3);
				int cutoff = (int) (Math.floor(finishTime / 2)) - 1;

				logger.info(id + ": " + name);

				// START TRANSACTION
				conn.setAutoCommit(false);

				// Generate particle scores per network
				aggregatedParticleScore.setLong(1, id);
				aggregatedParticleScore.setLong(2, id);
				aggregatedParticleScore.execute();

				networkStats.setLong(1, id);
				ResultSet networks = networkStats.executeQuery();
				logger.debug("Cutoff: " + cutoff);
				while (networks.next()) {
					int network = networks.getInt(1);
					logger.debug("Network " + network);

					// Calculate particles remaining
					int rem = 0;

					remaining.setLong(1, id);
					remaining.setInt(2, cutoff);
					remaining.setInt(3, network);
					ResultSet rs = remaining.executeQuery();

					if (rs.next()) {
						rem = rs.getInt(1);
					}

					// Insert summary
					insertSummary.setLong(1, id);
					insertSummary.setString(2, name);
					insertSummary.setInt(3, network);
					insertSummary.setDouble(4, networks.getDouble(1));
					insertSummary.setDouble(5, networks.getDouble(2));
					insertSummary.setDouble(6, networks.getDouble(3));
					insertSummary.setInt(7, rem);
					insertSummary.execute();

				}

				// COMMIT TRANSACTION
				conn.commit();
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			stopDatabase();
		}
	}

	@Command(name = "verify", description = "Check a result set for errors.")
	public void verify(String[] args) {
		long simulationID = 0;
		try {
			simulationID = Long.parseLong(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Simulation ID should be an integer.");
			return;
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("Please specify a simulation ID.");
		}

		StorageService storage = getDatabase();
		PersistentSimulation sim = storage.getSimulationById(simulationID);

		logger.info("Check for missing round data");
		for (PersistentAgent a : sim.getAgents()) {
			logger.info("Agent " + a.getName() + "...");
			boolean current;
			boolean next = hasMissingData(a.getState(1));
			for (int t = 2; t < (sim.getFinishTime() / 2); t++) {
				current = next;
				next = hasMissingData(a.getState(t));
				if (current && !next) {
					logger.warn("Missing round for agent " + a.getName() + " timestep " + (t - 1));
				} else if (!current && next) {
					logger.info(a.getName() + " didn't play at round " + t);
				}
			}
		}

		stopDatabase();
	}

	private static boolean hasMissingData(TransientAgentState s) {
		Map<String, String> p = s.getProperties();
		return !p.containsKey("g") || !p.containsKey("d") || !p.containsKey("p") || !p.containsKey("q")
				|| !p.containsKey("r") || !p.containsKey("r'") || !p.containsKey("RTotal") || !p.containsKey("U");
	}

	@Command(name = "graph", description = "Export graphs for simulation.")
	public void export_graphs(String[] args) throws Exception {
		if (args.length > 1) {
			args = new String[] { args[1], Boolean.toString(true) };
			SpinWorldGUI.main(args);
		}
	}

	@SuppressWarnings("static-access")
	@Command(name = "run_hpc", description = "Run sim in hpc mode (reduced db connections).")
	public void run_connectionless(String[] args) throws Exception {

		int threads = 4;
		int retries = 3;

		Options options = new Options();
		options.addOption(
				OptionBuilder.withArgName("url").hasArg().withDescription("Database url.").isRequired().create("url"));
		options.addOption(OptionBuilder.withArgName("user").hasArg().withDescription("Database user.").isRequired()
				.create("user"));
		options.addOption(OptionBuilder.withArgName("password").hasArg().withDescription("Database user's password.")
				.isRequired().create("password"));
		options.addOption("r", "retry", true, "Number of times to attempt db reconnect.");
		options.addOption("t", "threads", true, "Number of threads for the simulator (default " + threads + ").");
		options.addOption("h", "help", false, "Show help");

		CommandLineParser parser = new GnuParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println(e.getMessage());
			new HelpFormatter().printHelp("presage2cli run <ID>", options, true);
			return;
		}
		if (cmd.hasOption("h") || args.length < 2) {
			new HelpFormatter().printHelp("presage2cli run <ID>", options, true);
			return;
		}
		if (cmd.hasOption("t")) {
			try {
				threads = Integer.parseInt(cmd.getOptionValue("t"));
			} catch (NumberFormatException e) {
				System.err.println("Thread no. should be in integer.");
				return;
			}
		}
		if (cmd.hasOption("r")) {
			try {
				retries = Integer.parseInt(cmd.getOptionValue("r"));
			} catch (NumberFormatException e) {
				System.err.println("Retries no. should be in integer.");
				return;
			}
		}

		long simulationID;
		try {
			simulationID = Long.parseLong(args[1]);
		} catch (NumberFormatException e) {
			System.err.println("Simulation ID should be an integer.");
			return;
		}

		Properties jdbcInfo = new Properties();
		jdbcInfo.put("driver", "com.mysql.jdbc.Driver");
		jdbcInfo.put("url", cmd.getOptionValue("url"));
		jdbcInfo.put("user", cmd.getOptionValue("user"));
		jdbcInfo.put("password", cmd.getOptionValue("password"));
		ConnectionlessStorage storage = new ConnectionlessStorage(jdbcInfo, retries);
		DatabaseService db = storage;
		db.start();

		RunnableSimulation.runSimulationID(simulationID, threads);

		db.stop();
	}

}
