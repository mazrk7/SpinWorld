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
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.ui.RectangleEdge;

import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
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
			
			BarChart allocChart = new BarChart(sim, windowSize, "Average Allocation over last 50 rounds", "Alloc", "r", "AllBar", 0.0, 1.0);
			BarChart pCheatChart = new BarChart(sim, windowSize, "Average Propensity to Cheat over last 50 rounds", "PCheat", "pCheat", "PchBar", 0.0, 1.0);
			
			double utiMax = 
					((Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b"))) >= Double.parseDouble(sim.getParameters().get("c")))
						? Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b")) : Double.parseDouble(sim.getParameters().get("c"));
			BarChart utilityChart = new BarChart(sim, windowSize, "Average Utility over last 50 rounds", "Ut.", "U", "UtiBar", 0.0, utiMax);
			
			TimeSeriesChart riskTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Agent Perceived Risk over 50 round window", "Risk", "risk", "RiskTime", 0.0, 2.0);
			TimeSeriesChart catchTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Agent Perceived Catch Rate over 50 round window", "Catch Rate", "catchRate", "CatchTime", 0.0, 1.0);
			TimeSeriesChart pCheatTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Agent Propensity to Cheat over 50 round window", "PCheat", "pCheat", "PchTime", 0.0, 1.0);
			TimeSeriesChart satTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Agent Satisfaction over 50 round window", "Sat.", "o", "SatTime", 0.0, 1.0);
	
			DistributionChart utDistrChart = new DistributionChart(sim, windowSize, "UtiDistr", -0.5, utiMax);
			
			List<Chart> charts = new ArrayList<Chart>();
			charts.add(allocChart);
			charts.add(utDistrChart);
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
	        layout.setAttractionMultiplier(0.25);
	        layout.setAttractionMultiplier(0.25);
	        layout.setRepulsionMultiplier(5);
	        
	    	VisualizationViewer<String, String> vv = new VisualizationViewer<String, String>(layout, new Dimension(1080, 720));
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
				ng.updateGraph(i, layout);
		        vv.setGraphLayout(layout);
		        
				if (exportMode)		
					ChartUtils.saveGraph(vv, imagePath, sim.getName() + "/" + "Net", i);
			}
					
			while (t < sim.getFinishTime() / 2) {
				t++;
				
				for (Chart chart : charts) {
					chart.redraw(t);
				}
										
				ng.updateGraph(t, layout);
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
		                "Sum of Utility", // Range axis label
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
	        
			DefaultCategoryDataset longevityData = new DefaultCategoryDataset();
			JFreeChart longevityChart = ChartFactory.createBarChart(
		                "Sustainability of Networks for Experiment: " + methodComp, // Chart title
		                "Method", // Domain axis label
		                "Average Longevity of Networks (% of Simulation Time)", // Range axis label
		                longevityData, // Dataset
		                PlotOrientation.VERTICAL,
		                false, // Include legend
		                false, // Tooltips
		                false // URLs
	                );
			
	        // Get a reference to the plot for further customisation...
	        CategoryPlot longevityPlot = longevityChart.getCategoryPlot();
	        
	        longevityPlot.setBackgroundPaint(Color.WHITE);
	        longevityPlot.getDomainAxis().setCategoryLabelPositions(CategoryLabelPositions.UP_90);

	        // Set the range axis to display integers only...
	        longevityPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        longevityPlot.getRangeAxis().setAutoRange(true);
			BarRenderer renderer = (BarRenderer) longevityPlot.getRenderer();
			renderer.setItemMargin(0.0);
	        
			DefaultXYDataset satTimeData = new DefaultXYDataset();
			JFreeChart satTimeChart = ChartFactory.createXYLineChart("Avg Agent Satisfaction for Experiment: " + methodComp,
					"Satisfaction Scale", "Timestep", satTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			satTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			satTimeChart.getXYPlot().getDomainAxis().setRange(0, 1);
			satTimeChart.getXYPlot().setWeight(3);
			satTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
	        
			DefaultXYDataset pChTimeData = new DefaultXYDataset();
			JFreeChart pChTimeChart = ChartFactory.createXYLineChart("Avg Agent Propensity to Cheat for Experiment: " + methodComp, 
					"PCheat Scale", "Timestep", pChTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			pChTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			pChTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);
			pChTimeChart.getXYPlot().setWeight(3);
			pChTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			
			DefaultXYDataset riskTimeData = new DefaultXYDataset();
			JFreeChart riskTimeChart = ChartFactory.createXYLineChart("Avg Agent Perceived Risk for Experiment: " + methodComp, 
					"Risk Scale", "Timestep", riskTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			riskTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			riskTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 2.0);
			riskTimeChart.getXYPlot().setWeight(3);
			riskTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			
			DefaultXYDataset catchTimeData = new DefaultXYDataset();
			JFreeChart catchTimeChart = ChartFactory.createXYLineChart("Avg Agent Perceived Catch Rate for Experiment: " + methodComp, 
					"Catch Rate Scale", "Timestep", catchTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			catchTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			catchTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);
			catchTimeChart.getXYPlot().setWeight(3);
			catchTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
	        
			String[] keys = new String[] { "c", "nc", "all" };
			Map<String, Map<String, Double>> mapUSums = new HashMap<String, Map<String, Double>>();
			Map<String, Double> mapLongevity = new HashMap<String, Double>();

			for (Long simId : simIds) {
				t = 0;
				sim = sto.getSimulationById(simId);

				methods.add(sim.getName());
				
				Map<String, Double> uSums = new HashMap<String, Double>();
				for (String k : keys) {
					uSums.put(k, 0.0);
				}
				
				Map<String, Integer> networkLongevity = new HashMap<String, Integer>();

				int length = (int)(sim.getFinishTime()/2);
				
				double[][] satMean = new double[2][length];
				double[][] pchMean = new double[2][length];
				double[][] riskMean = new double[2][length];
				double[][] catchMean = new double[2][length];
				Arrays.fill(satMean[0], 0);
				Arrays.fill(pchMean[0], 0);
				Arrays.fill(riskMean[0], 0);
				Arrays.fill(catchMean[0], 0);
				
				logger.debug("Processing simulation " + sim.getID());
				while (t < length) {
					t++;

					satMean[1][t-1] = t;
					SummaryStatistics satC = new SummaryStatistics();
					SummaryStatistics satNC = new SummaryStatistics();
					
					pchMean[1][t-1] = t;
					SummaryStatistics pchC = new SummaryStatistics();
					SummaryStatistics pchNC = new SummaryStatistics();
					
					riskMean[1][t-1] = t;
					SummaryStatistics riskC = new SummaryStatistics();
					SummaryStatistics riskNC = new SummaryStatistics();
					
					catchMean[1][t-1] = t;
					SummaryStatistics catchC = new SummaryStatistics();
					SummaryStatistics catchNC = new SummaryStatistics();
					
					PersistentEnvironment pEnv = sim.getEnvironment();
					for (String prop : pEnv.getProperties(t).keySet()) {
						if (prop.contains("longevity")) {
							networkLongevity.put(prop.substring(0, 2), 
									Integer.parseInt(pEnv.getProperty(prop, t)));
						}
					}
					
					Set<PersistentAgent> pAgents = sim.getAgents();
					for (PersistentAgent a : pAgents) {
						final String name = a.getName();
						boolean compliant = name.startsWith("c");
						TransientAgentState s = a.getState(t);
						
						if (s != null) {			
							if (s.getProperty("U") != null) {
								double u = Double.parseDouble(s.getProperty("U"));
	
								if (compliant)
									uSums.put("c", uSums.get("c") + u);
								else
									uSums.put("nc", uSums.get("nc") + u);
							}
							
							if (s.getProperty("o") != null) {
								double o = Double.parseDouble(s.getProperty("o"));
	
								if (compliant)
									satC.addValue(o);
								else 								
									satNC.addValue(o);
							}
							
							if (s.getProperty("pCheat") != null) {
								double pc = Double.parseDouble(s.getProperty("pCheat"));
	
								if (compliant)
									pchC.addValue(pc);
								else 								
									pchNC.addValue(pc);
							}
							
							if (s.getProperty("risk") != null) {
								double r = Double.parseDouble(s.getProperty("risk"));
	
								if (compliant)
									riskC.addValue(r);
								else 								
									riskNC.addValue(r);
							}
							
							if (s.getProperty("catchRate") != null) {
								double cr = Double.parseDouble(s.getProperty("catchRate"));
	
								if (compliant)
									catchC.addValue(cr);
								else 								
									catchNC.addValue(cr);
							}
						}
						
						satMean[0][t-1] = (satC.getMean() + satNC.getMean())/2;
						pchMean[0][t-1] = (pchC.getMean() + pchNC.getMean())/2;
						riskMean[0][t-1] = (riskC.getMean() + riskNC.getMean())/2;
						catchMean[0][t-1] = (catchC.getMean() + catchNC.getMean())/2;
					}
										
					pAgents = null;
					
					uSums.put("all", uSums.get("c") + uSums.get("nc"));
					
					satTimeData.addSeries(sim.getName(), satMean);
					pChTimeData.addSeries(sim.getName(), pchMean);
					riskTimeData.addSeries(sim.getName(), riskMean);
					catchTimeData.addSeries(sim.getName(), catchMean);
				}
				
				int sumLongevity = 0;
				for (String k : networkLongevity.keySet())
				{
					sumLongevity += networkLongevity.get(k);
				}
				
				double longevityPerc = ((double)(sumLongevity/networkLongevity.size())/length) * 100;
				mapUSums.put(sim.getName(), uSums);	
				mapLongevity.put(sim.getName(), longevityPerc);	
			}
			
			for (String k : keys) {
				for (String method : methods) {
					utiData.addValue(mapUSums.get(method).get(k), k, method);
				}
			}	
			
			for (String method : methods) {
				longevityData.addValue(mapLongevity.get(method), "Sustainability", method);
			}
			
			if (exportMode) {
				ChartUtils.saveChart(sumUtiChart, imagePath, "COMPARISON/" + "UTI_" + this.methodComp);
				ChartUtils.saveChart(longevityChart, imagePath, "COMPARISON/" + "LONGEVITY_" + this.methodComp);
				ChartUtils.saveChart(satTimeChart, imagePath, "COMPARISON/" + "SAT_TIME_" + this.methodComp);
				ChartUtils.saveChart(pChTimeChart, imagePath, "COMPARISON/" + "PCHEAT_TIME_" + this.methodComp);
				ChartUtils.saveChart(riskTimeChart, imagePath, "COMPARISON/" + "RISK_TIME_" + this.methodComp);
				ChartUtils.saveChart(catchTimeChart, imagePath, "COMPARISON/" + "CATCH_TIME_" + this.methodComp);
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
