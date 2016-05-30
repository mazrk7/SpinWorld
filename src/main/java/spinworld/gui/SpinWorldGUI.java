package spinworld.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public class SpinWorldGUI {
	
	private final Logger logger = Logger.getLogger(this.getClass());

	final DatabaseService db;
	final StorageService sto;
	
	PersistentSimulation sim;
	int t = 5;
	int windowSize = 50;
	int t0 = -1;
	
	boolean exportMode = false;
	boolean outputSimCharts = false;
	boolean outputSingle = false;
	boolean outputList = false;
	boolean outputRange = false;
	boolean outputComparisonCharts = false;
	
	Long singleSimId;
	Long[] manySimIds;
	String methodComp = "";
	
	final static String imagePath = "/home/markzolotas7/Videos/";

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			SpinWorldGUI gui = injector.getInstance(SpinWorldGUI.class);
			
			try {		
				gui.init(args);
				
				List<Long> simIds = gui.getSimulationList();
				
				for (Long simId : simIds) {
					gui.buildChartsForSim(simId);
				}
				
				gui.buildForMethods(simIds);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			gui.finish();
			gui.logger.info("Finished.");
			if (gui.exportMode) System.exit(0);
		}
	}

	@Inject
	public SpinWorldGUI(DatabaseService db, StorageService sto) {
		super();
		this.db = db;
		this.sto = sto;
	}
	
	private void init(String[] args) throws Exception {
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		Options options = new Options();
		
		options.addOption("export", false, "Run in export mode ? (When specified, doesn't display anything to the screen) (default:false)");
		options.addOption("outputSimCharts", false, "Output a set of charts for each simulation during runtime ? (default:false)");
		options.addOption("outputSingle", true, "Output a set of charts for a single simulation ? (default:false)");
		
		Option optList = new Option("outputList", true, "Output a set of charts for a list of simulations ? (default:false)");
		optList.setArgs(Option.UNLIMITED_VALUES);
		options.addOption(optList);
		
		Option optRange = new Option("outputRange", true, "Output a set of charts for a range of simulations ? (default:false)");
		optRange.setArgs(2);
		options.addOption(optRange);
		
		options.addOption("outputComparisonCharts", true, "Output a set of charts comparing each method ? (default:false)");

		CommandLineParser parser = new GnuParser();
		CommandLine cmd = parser.parse(options, args);
		
		if (cmd.hasOption("export")) {
			this.exportMode = true;
		}
		
		if (cmd.hasOption("outputSimCharts")) {
			this.outputSimCharts = true;
		}
		
		if (cmd.hasOption("outputSingle")) {
			this.outputSingle = true;
			this.singleSimId = Long.parseLong(cmd.getOptionValue("outputSingle"));
		}
		
		if (cmd.hasOption("outputList")) {
			this.outputList = true;
			String[] listIds = cmd.getOptionValues("outputList");
			this.manySimIds = new Long[listIds.length];
			
			for (int i = 0; i < listIds.length; i++) {
				this.manySimIds[i] = Long.parseLong(listIds[i]);	
			}
		}
		
		if (cmd.hasOption("outputRange")) {
			this.outputRange = true;
			String[] rangeIds = cmd.getOptionValues("outputRange");
			int lowerBound = Integer.parseInt(rangeIds[0]);
			int upperBound = Integer.parseInt(rangeIds[1]);
			this.manySimIds = new Long[upperBound-lowerBound + 1];
			int j = 0;
			
			for (int i = lowerBound; i <= upperBound; i++) {
				this.manySimIds[j++] = new Long(i);
			}
		}
		
		if (cmd.hasOption("outputComparisonCharts")) {
			this.outputComparisonCharts = true;
			this.methodComp = cmd.getOptionValue("outputComparisonCharts");
		}
	}
	
	private void buildChartsForSim(Long simId) throws IOException, ClassNotFoundException {
		if (outputSimCharts) {
			logger.info("Building charts for sim " + simId + "...");
			
			try {
				db.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			sim = sto.getSimulationById(simId);
			
			if (exportMode) {
				File exportDir = new File(imagePath + sim.getName());
				
				if (!exportDir.exists())
					exportDir.mkdir();
				else if (!exportDir.isDirectory())
					System.exit(60);
			}
			
			BarChart allocChart = new BarChart(simId, sim, windowSize, "Average Allocation over last 50 rounds", "r", "Alloc", 0.0, 1.0);
			BarChart rTotalChart = new BarChart(simId, sim, windowSize, "Average Total Resources over last 50 rounds", "RTotal", "Total", 0.0, 1.0);
			BarChart pCheatChart = new BarChart(simId, sim, windowSize, "Average Propensity to Cheat over last 50 rounds", "pCheat", "PchBar", 0.0, 1.0);
			
			double utiMax = 
					((Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b"))) >= Double.parseDouble(sim.getParameters().get("c")))
						? Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b")) : Double.parseDouble(sim.getParameters().get("c"));
			BarChart utilityChart = new BarChart(simId, sim, windowSize, "Average Utility over last 50 rounds", "U", "Uti", 0.0, utiMax);
			
			TimeSeriesChart riskTimeChart = new TimeSeriesChart(simId, sim, windowSize, "Agent Perceived Risk", "risk", "RisTime", 0.0, 2.0);
			TimeSeriesChart catchTimeChart = new TimeSeriesChart(simId, sim, windowSize, "Agent Perceived Catch Rate", "catchRate", "CatTime", 0.0, 1.0);
			TimeSeriesChart pCheatTimeChart = new TimeSeriesChart(simId, sim, windowSize, "Agent Propensity to Cheat", "pCheat", "PchTime", 0.0, 1.0);
			TimeSeriesChart satTimeChart = new TimeSeriesChart(simId, sim, windowSize, "Agent Satisfaction", "o", "Sat", 0.0, 1.0);
	
			List<Chart> charts = new ArrayList<Chart>();
			charts.add(allocChart);
			charts.add(rTotalChart);
			charts.add(pCheatChart);
			charts.add(utilityChart);
			charts.add(catchTimeChart);
			charts.add(riskTimeChart);
			charts.add(pCheatTimeChart);
			charts.add(satTimeChart);
	
			final Frame f = new Frame("SpinWorld Chart Plots");
			final Panel p = new Panel(new GridLayout(2, 2));
						
			final JFrame jf = new JFrame("SpinWorld Social Network Visualiser");
	        NetworkGraph ng = new NetworkGraph(sim);
	        							
	        FRLayout<String, String> layout = new FRLayout<String, String>(ng.getGraph());
	        layout.setRepulsionMultiplier(10);
	        layout.setAttractionMultiplier(0.10);
	
	    	VisualizationViewer<String, String> vv = new VisualizationViewer<String, String>(layout, new Dimension(1280, 720));
			vv.setBackground(Color.LIGHT_GRAY);
		    vv.setGraphMouse(new DefaultModalGraphMouse<String, String>());
		    
		    vv.getRenderContext().setVertexFillPaintTransformer(NetworkRenderer.getVertexPaintTransformer());
		    vv.getRenderContext().setVertexShapeTransformer(NetworkRenderer.getVertexLabelTransformer());
		    vv.getRenderContext().setVertexFontTransformer(NetworkRenderer.getVertexFontTransformer());
		    vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
		    vv.getRenderContext().setEdgeFillPaintTransformer(NetworkRenderer.getEdgePaintTransformer());
		    vv.getRenderContext().setEdgeStrokeTransformer(NetworkRenderer.getEdgeStrokeTransformer());
		    vv.getRenderContext().setEdgeFontTransformer(NetworkRenderer.getEdgeFontTransformer());
		    vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<String>());
		    vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		    
			if (!exportMode) {
				f.add(p);
				
				for (Chart chart : charts) {
					p.add(chart.getPanel());
				}
				
				f.pack();
				f.setVisible(true);
							
	    		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				jf.getContentPane().add(vv);
				jf.pack();
				jf.setVisible(true);
			}
						
			for (int i = 1; i <= t; i++) {
				ng.updateGraph(i);
		        vv.setGraphLayout(layout);
		        
				if (exportMode)		
					ChartUtils.saveGraph(vv, imagePath, sim.getName() + "/" + "Net", i);
			}
					
			while (t < sim.getFinishTime() / 2) {
				t++;
				
				for (Chart chart : charts) {
					chart.redraw(t);
				}
										
				ng.updateGraph(t);
		        vv.setGraphLayout(layout);
				
				if (exportMode) {		
					if (t0 == -1)
						t0 = t;
					
					for (Chart chart : charts) {
						ChartUtils.saveChart(chart.getChart(), imagePath,
								sim.getName() + "/" + chart.getShortName(), t, t0);
					}
									
					ChartUtils.saveGraph(vv, imagePath, sim.getName() + "/Net", t);		
				} else {
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			t = 5;
			t0 = -1;
							
			logger.info("Done building charts for sim " + simId + ".");			
		}
	}
	
	private void buildForMethods(List<Long> simIds) {
		if (outputComparisonCharts) {
			if (exportMode) {
				File exportDir = new File(imagePath + "COMPARISON");
				
				if (!exportDir.exists())
					exportDir.mkdir();
				else if (!exportDir.isDirectory())
					System.exit(60);
			}
			
			logger.info("Processing for " + methodComp + " methods...");
			
			List<String> methods = new ArrayList<String>();

			DefaultCategoryDataset utiData = new DefaultCategoryDataset();
			JFreeChart sumUtiChart = ChartFactory.createBarChart(
		                "Total Utility for Experiment: " + methodComp, // Chart title
		                "Method", // Domain axis label
		                "Sum of Utility (x 1,000)", // Range axis label
		                utiData, // Dataset
		                PlotOrientation.VERTICAL,
		                true, // Include legend
		                false, // Tooltips
		                false // URLs
	                );
			
	        // Get a reference to the plot for further customisation...
	        CategoryPlot utiPlot = sumUtiChart.getCategoryPlot();
	        
	        utiPlot.setBackgroundPaint(Color.WHITE);
	        utiPlot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
	        sumUtiChart.getLegend().setPosition(RectangleEdge.RIGHT);

	        // Set the range axis to display integers only...
	        utiPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        utiPlot.getRangeAxis().setAutoRange(true);
			
			DefaultCategoryDataset satData = new DefaultCategoryDataset();
			JFreeChart sumSatChart = ChartFactory.createBarChart(
		                "Total Satisfaction for Experiment: " + methodComp, // Chart title
		                "Method", // Domain axis label
		                "Sum of Satisfaction (x 1,000)", // Range axis label
		                satData, // Dataset
		                PlotOrientation.VERTICAL,
		                true, // Include legend
		                false, // Tooltips
		                false // URLs
	                );
			
	        // Get a reference to the plot for further customisation...
	        CategoryPlot satPlot = sumSatChart.getCategoryPlot();
	        
	        satPlot.setBackgroundPaint(Color.WHITE);
	        satPlot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);
	        sumSatChart.getLegend().setPosition(RectangleEdge.RIGHT);

	        // Set the range axis to display integers only...
	        satPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        satPlot.getRangeAxis().setAutoRange(true);
	        
			String[] keys = new String[] { "c", "nc", "all" };
			Map<String, Map<String, Double>> mapUSums = new HashMap<String, Map<String, Double>>();
			Map<String, Map<String, Double>> mapSatSums = new HashMap<String, Map<String, Double>>();

			for (Long simId : simIds) {
				t = 0;
				sim = sto.getSimulationById(simId);

				methods.add(sim.getName());
				
				Map<String, Double> uSums = new HashMap<String, Double>();
				Map<String, Double> satSums = new HashMap<String, Double>();
				for (String k : keys) {
					uSums.put(k, 0.0);
					satSums.put(k, 0.0);
				}
				
				logger.debug("Processing simulation " + sim.getID());
				while (t < sim.getFinishTime() / 2) {
					t++;
					
					Set<PersistentAgent> pAgents = sim.getAgents();
					for (PersistentAgent a : pAgents) {
						final String name = a.getName();
						boolean compliant = name.startsWith("c");
						TransientAgentState s = a.getState(t);
						
						if (s != null && s.getProperty("U") != null) {
							double u = Double.parseDouble(s.getProperty("U"));

							if (compliant)
								uSums.put("c", uSums.get("c") + (u - uSums.get("c")));
							else
								uSums.put("nc", uSums.get("nc") + (u - uSums.get("nc")));
						}
						
						if (s != null && s.getProperty("o") != null) {
							double o = Double.parseDouble(s.getProperty("o"));

							if (compliant)
								satSums.put("c", satSums.get("c") + (o - satSums.get("c")));
							else
								satSums.put("nc", satSums.get("nc") + (o - satSums.get("nc")));
						}
					}
					
					pAgents = null;
					
					uSums.put("all", uSums.get("c") + uSums.get("nc"));
					satSums.put("all", satSums.get("c") + satSums.get("nc"));
				}
				
				mapUSums.put(sim.getName(), uSums);	
				mapSatSums.put(sim.getName(), satSums);	
			}
			
			for (String k : keys) {
				for (String method : methods) {
					utiData.addValue(mapUSums.get(method).get(k) * 1000, k, method);
					satData.addValue(mapSatSums.get(method).get(k) * 1000, k, method);
				}
			}	
			
			if (exportMode) {
				ChartUtils.saveChart(sumUtiChart, imagePath, "COMPARISON/" + "UTI_" + this.methodComp);
				ChartUtils.saveChart(sumSatChart, imagePath, "COMPARISON/" + "SAT_" + this.methodComp);
			}

			logger.info("Done building charts for " + this.methodComp + " methods.");	
		}
	}
	
	private List<Long> getSimulationList() {
		List<Long> result = null;
		
		if (this.outputSingle) 
			result = Collections.singletonList(this.singleSimId);
		else if (this.outputRange || this.outputList)
			result = Arrays.asList(this.manySimIds);
		else
			result = sto.getSimulations();
		
		return result;
	}
	
	private void finish() {
		db.stop();
	}

}
