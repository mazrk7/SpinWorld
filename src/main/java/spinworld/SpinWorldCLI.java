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
import spinworld.db.SpinWorldStorage;
import spinworld.gui.SpinWorldGUI;
import uk.ac.imperial.presage2.core.cli.Presage2CLI;
import uk.ac.imperial.presage2.core.db.DatabaseService;
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
		experiments.put("cheat_strat", "Test different cheating strategies.");
		experiments.put("base", "Base case scenario.");
		experiments.put("graduated_sanctions", "Test different sanction levels.");
		experiments.put("deterrence", "Test different severity scales.");
		experiments.put("monitoring_cost", "Test different network monitoring costs.");
		experiments.put("conflict_resolution", "Test different forgiveness levels.");
		experiments.put("optimal", "Test different mechanisms.");

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
		else if (args[1].equalsIgnoreCase("cheat_strat"))
			cheat_strat(repeats, seed);
		else if (args[1].equalsIgnoreCase("base"))
			base(repeats, seed);
		else if (args[1].equalsIgnoreCase("graduated_sanctions"))
			graduated_sanctions(repeats, seed);
		else if (args[1].equalsIgnoreCase("deterrence"))
			deterrence(repeats, seed);
		else if (args[1].equalsIgnoreCase("monitoring_cost"))
			monitoring_cost(repeats, seed);
		else if (args[1].equalsIgnoreCase("conflict_resolution"))
			conflict_resolution(repeats, seed);
		else if (args[1].equalsIgnoreCase("size"))
			size(repeats, seed);
		else if (args[1].equalsIgnoreCase("optimal"))
			optimal(repeats, seed);
	}

	void large_pop(int repeats, int seed) {
		int rounds = 2002;
		int agents = 100;

		for (int i = 0; i < repeats; i++) {
			for (double ncProp : new double[] { .0, .2, .4, .6, .8, 1.0 }) {
				int nc = (int) Math.round(agents * ncProp);

				PersistentSimulation sim = getDatabase().createSimulation(
						"POP_" + String.format("%03d", nc) + "_NC", "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(agents - nc));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(nc));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(0.3));
				sim.addParameter("monitoringLevel", Double.toString(0.5));

				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}

	void cheat_strat(int repeats, int seed) {
		Cheat[] cheatMethods = { Cheat.DEMAND, Cheat.PROVISION, Cheat.APPROPRIATE };
		int rounds = 2002;

		for (int i = 0; i < repeats; i++) {
			for (Cheat ch : cheatMethods) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"CH_" + ch.name().substring(0, 3), "spinworld.SpinWorldSimulation", "AUTO START",
						rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(10));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", ch.name());
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(0.3));
				sim.addParameter("monitoringLevel", Double.toString(0.5));

				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}
	
	void base(int repeats, int seed) {
		int rounds = 502;

		for (int i = 0; i < repeats; i++) {
			PersistentSimulation sim = getDatabase().createSimulation(
					"BASE", "spinworld.SpinWorldSimulation",
					"AUTO START", rounds);

			sim.addParameter("finishTime", Integer.toString(rounds));
			sim.addParameter("size", Integer.toString(5));
			sim.addParameter("alpha", Double.toString(0.1));
			sim.addParameter("beta", Double.toString(0.1));
			sim.addParameter("theta", Double.toString(0.1));
			sim.addParameter("phi", Double.toString(0.1));
			sim.addParameter("a", Double.toString(2));
			sim.addParameter("b", Double.toString(1));
			sim.addParameter("c", Double.toString(3));
			sim.addParameter("cAgents", Integer.toString(10));
			sim.addParameter("cPCheat", Double.toString(0.025));
			sim.addParameter("ncAgents", Integer.toString(10));
			sim.addParameter("ncPCheat", Double.toString(0.4));
			sim.addParameter("seed", Integer.toString(seed + i));
			sim.addParameter("cheatOn", Cheat.PROVISION.name());
			
			logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
		}

		stopDatabase();
	}
	
	void graduated_sanctions(int repeats, int seed) {
		int rounds = 502;

		for (int i = 0; i < repeats; i++) {
			for (int sc : new int[] { 0, 1, 3, 5, 7, 10, 15, 25 }) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"L_" + sc, "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(20));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(20));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("sanctionLevel", Integer.toString(sc));
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(0.3));
				sim.addParameter("monitoringLevel", Double.toString(0.5));

				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}
	
	void deterrence(int repeats, int seed) {
		int rounds = 502;

		for (int i = 0; i < repeats; i++) {
			for (double ub : new double[] { .2, .5, .8, 1.0 }) {
				for (double lb : new double[] { .0, .2, .5, .8 }) {
					if (lb >= ub)
						break;
					
					PersistentSimulation sim = getDatabase().createSimulation(
							"S_" + String.format("%02d", Math.round((lb*10))) + "_" 
									+ String.format("%02d", Math.round((ub*10))), "spinworld.SpinWorldSimulation",
							"AUTO START", rounds);
	
					sim.addParameter("finishTime", Integer.toString(rounds));
					sim.addParameter("size", Integer.toString(5));
					sim.addParameter("alpha", Double.toString(0.1));
					sim.addParameter("beta", Double.toString(0.1));
					sim.addParameter("theta", Double.toString(0.1));
					sim.addParameter("phi", Double.toString(0.1));				
					sim.addParameter("a", Double.toString(2));
					sim.addParameter("b", Double.toString(1));
					sim.addParameter("c", Double.toString(3));
					sim.addParameter("cAgents", Integer.toString(10));
					sim.addParameter("cPCheat", Double.toString(0.025));
					sim.addParameter("ncAgents", Integer.toString(10));
					sim.addParameter("ncPCheat", Double.toString(0.4));
					sim.addParameter("seed", Integer.toString(seed + i));
					sim.addParameter("cheatOn", Cheat.PROVISION.name());
					sim.addParameter("severityLB", Double.toString(lb));
					sim.addParameter("severityUB", Double.toString(ub));
					sim.addParameter("monitoringCost", Double.toString(0.3));
					sim.addParameter("monitoringLevel", Double.toString(0.5));

					logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
				}
			}
		}

		stopDatabase();
	}
	
	void monitoring_cost(int repeats, int seed) {
		int rounds = 502;

		for (int i = 0; i < repeats; i++) {
			for (double mc : new double[] { .0, .1, .3, .5, .7, .9, 1.0 }) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"MC_" + String.format("%03d", (Math.round(mc*100))), "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(10));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("sanctionLevel", Integer.toString(5));
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(mc));
				sim.addParameter("monitoringLevel", Double.toString(0.5));
				
				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}
	
	void conflict_resolution(int repeats, int seed) {
		int rounds = 502;

		for (int i = 0; i < repeats; i++) {
			for (double ff : new double[] { .0, .2, .5, .8, 1.0 }) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"F_" + String.format("%03d", (Math.round(ff*100))), "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(5));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(10));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("sanctionLevel", Integer.toString(5));
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(0.3));
				sim.addParameter("monitoringLevel", Double.toString(0.5));
				sim.addParameter("forgiveness", Double.toString(ff));
				
				logger.info("Created sim: " + sim.getID() + " - " + sim.getName());
			}
		}

		stopDatabase();
	}
	
	void optimal(int repeats, int seed) {
		int rounds = 2002;

		for (int i = 0; i < repeats; i++) {
			PersistentSimulation base = getDatabase().createSimulation("Base", "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

			base.addParameter("finishTime", Integer.toString(rounds));
			base.addParameter("size", Integer.toString(7));
			base.addParameter("alpha", Double.toString(0.1));
			base.addParameter("beta", Double.toString(0.1));
			base.addParameter("theta", Double.toString(0.1));
			base.addParameter("phi", Double.toString(0.1));
			base.addParameter("a", Double.toString(2));
			base.addParameter("b", Double.toString(1));
			base.addParameter("c", Double.toString(3));
			base.addParameter("cAgents", Integer.toString(20));
			base.addParameter("cPCheat", Double.toString(0.025));
			base.addParameter("ncAgents", Integer.toString(20));
			base.addParameter("ncPCheat", Double.toString(0.25));
			base.addParameter("seed", Integer.toString(seed + i));
			base.addParameter("cheatOn", Cheat.PROVISION.name());
				
			logger.info("Created sim: " + base.getID() + " - " + base.getName());
			
			PersistentSimulation retr = getDatabase().createSimulation("Principles 4|5", "spinworld.SpinWorldSimulation",
					"AUTO START", rounds);

			retr.addParameter("finishTime", Integer.toString(rounds));
			retr.addParameter("size", Integer.toString(7));
			retr.addParameter("alpha", Double.toString(0.1));
			retr.addParameter("beta", Double.toString(0.1));
			retr.addParameter("theta", Double.toString(0.1));
			retr.addParameter("phi", Double.toString(0.1));
			retr.addParameter("a", Double.toString(2));
			retr.addParameter("b", Double.toString(1));
			retr.addParameter("c", Double.toString(3));
			retr.addParameter("cAgents", Integer.toString(20));
			retr.addParameter("cPCheat", Double.toString(0.025));
			retr.addParameter("ncAgents", Integer.toString(20));
			retr.addParameter("ncPCheat", Double.toString(0.25));
			retr.addParameter("seed", Integer.toString(seed + i));
			retr.addParameter("cheatOn", Cheat.PROVISION.name());
			retr.addParameter("sanctionLevel", Integer.toString(3));
			retr.addParameter("severityLB", Double.toString(0.3));
			retr.addParameter("monitoringCost", Double.toString(0.2));
			retr.addParameter("monitoringLevel", Double.toString(0.4));
				
			logger.info("Created sim: " + retr.getID() + " - " + retr.getName());
			
			PersistentSimulation conflict = getDatabase().createSimulation("Principles 4|5|6", "spinworld.SpinWorldSimulation",
					"AUTO START", rounds);

			conflict.addParameter("finishTime", Integer.toString(rounds));
			conflict.addParameter("size", Integer.toString(7));
			conflict.addParameter("alpha", Double.toString(0.1));
			conflict.addParameter("beta", Double.toString(0.1));
			conflict.addParameter("theta", Double.toString(0.1));
			conflict.addParameter("phi", Double.toString(0.1));
			conflict.addParameter("a", Double.toString(2));
			conflict.addParameter("b", Double.toString(1));
			conflict.addParameter("c", Double.toString(3));
			conflict.addParameter("cAgents", Integer.toString(20));
			conflict.addParameter("cPCheat", Double.toString(0.025));
			conflict.addParameter("ncAgents", Integer.toString(20));
			conflict.addParameter("ncPCheat", Double.toString(0.25));
			conflict.addParameter("seed", Integer.toString(seed + i));
			conflict.addParameter("cheatOn", Cheat.PROVISION.name());
			conflict.addParameter("sanctionLevel", Integer.toString(3));
			conflict.addParameter("severityLB", Double.toString(0.3));
			conflict.addParameter("monitoringCost", Double.toString(0.2));
			conflict.addParameter("monitoringLevel", Double.toString(0.4));
			conflict.addParameter("forgiveness", Double.toString(0.7));

			logger.info("Created sim: " + conflict.getID() + " - " + conflict.getName());
			
			PersistentSimulation det = getDatabase().createSimulation("Deterrence", "spinworld.SpinWorldSimulation",
					"AUTO START", rounds);

			det.addParameter("finishTime", Integer.toString(rounds));
			det.addParameter("size", Integer.toString(7));
			det.addParameter("alpha", Double.toString(0.1));
			det.addParameter("beta", Double.toString(0.1));
			det.addParameter("theta", Double.toString(0.1));
			det.addParameter("phi", Double.toString(0.1));
			det.addParameter("a", Double.toString(2));
			det.addParameter("b", Double.toString(1));
			det.addParameter("c", Double.toString(3));
			det.addParameter("cAgents", Integer.toString(20));
			det.addParameter("cPCheat", Double.toString(0.025));
			det.addParameter("ncAgents", Integer.toString(20));
			det.addParameter("ncPCheat", Double.toString(0.25));
			det.addParameter("seed", Integer.toString(seed + i));
			det.addParameter("cheatOn", Cheat.PROVISION.name());
			det.addParameter("severityLB", Double.toString(0.4));
			det.addParameter("severityUB", Double.toString(0.6));
			det.addParameter("monitoringCost", Double.toString(0.2));
			det.addParameter("monitoringLevel", Double.toString(0.4));
				
			logger.info("Created sim: " + det.getID() + " - " + det.getName());
		}

		stopDatabase();
	}
	
	void size(int repeats, int seed) {
		int rounds = 2002;

		for (int i = 0; i < repeats; i++) {
			for (int sz : new int[] { 1, 5, 10, 50, 100 }) {
				PersistentSimulation sim = getDatabase().createSimulation(
						"SIZE_" + sz, "spinworld.SpinWorldSimulation",
						"AUTO START", rounds);

				sim.addParameter("finishTime", Integer.toString(rounds));
				sim.addParameter("size", Integer.toString(sz));
				sim.addParameter("alpha", Double.toString(0.1));
				sim.addParameter("beta", Double.toString(0.1));
				sim.addParameter("theta", Double.toString(0.1));
				sim.addParameter("phi", Double.toString(0.1));
				sim.addParameter("a", Double.toString(2));
				sim.addParameter("b", Double.toString(1));
				sim.addParameter("c", Double.toString(3));
				sim.addParameter("cAgents", Integer.toString(10));
				sim.addParameter("cPCheat", Double.toString(0.025));
				sim.addParameter("ncAgents", Integer.toString(10));
				sim.addParameter("ncPCheat", Double.toString(0.4));
				sim.addParameter("seed", Integer.toString(seed + i));
				sim.addParameter("cheatOn", Cheat.PROVISION.name());
				sim.addParameter("sanctionLevel", Integer.toString(5));
				sim.addParameter("severityLB", Double.toString(0.2));
				sim.addParameter("monitoringCost", Double.toString(0.3));
				sim.addParameter("monitoringLevel", Double.toString(0.5));
				
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

					// Calculate c and nc remaining
					int crem = 0;
					int ncrem = 0;

					remaining.setLong(1, id);
					remaining.setString(2, "c%");
					remaining.setInt(3, cutoff);
					remaining.setInt(4, network);
					ResultSet rs = remaining.executeQuery();
					if (rs.next()) {
						crem = rs.getInt(1);
					}

					remaining.setString(2, "nc%");
					rs = remaining.executeQuery();
					if (rs.next()) {
						ncrem = rs.getInt(1);
					}

					// Insert summary
					insertSummary.setLong(1, id);
					insertSummary.setString(2, name);
					insertSummary.setInt(3, network);
					insertSummary.setDouble(4, networks.getDouble(2));
					insertSummary.setDouble(5, networks.getDouble(3));
					insertSummary.setDouble(6, networks.getDouble(4));
					insertSummary.setDouble(7, networks.getDouble(5));
					insertSummary.setDouble(8, networks.getDouble(6));
					insertSummary.setInt(9, crem);
					insertSummary.setInt(10, ncrem);
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

		SpinWorldStorage storage = (SpinWorldStorage) getDatabase();
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
		SpinWorldGUI.main(args);
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
