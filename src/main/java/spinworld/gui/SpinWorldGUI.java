package spinworld.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
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
				// List<Chart> charts = gui.buildForMethods(simIds);
				// gui.combineMethodCharts(charts);
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
			
			TimeSeriesChart riskTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Moving Avg. Perceived Risk", "Risk", "risk", "RiskTime", 0.0, 1.0);
			TimeSeriesChart catchTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Moving Avg. Perceived Catch Rate", "Catch Rate", "catchRate", "CatchTime", 0.0, 1.0);
			TimeSeriesChart pCheatTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Moving Avg. Propensity to Cheat", "PCheat", "pCheat", "PchTime", 0.0, 1.0);
			TimeSeriesChart satTimeChart = new TimeSeriesChart(sim, windowSize, 
					"Moving Avg. Satisfaction", "Satisfaction", "o", "SatTime", 0.0, 1.0);
				
			List<Chart> timeCharts = new ArrayList<Chart>();
			timeCharts.add(satTimeChart);
			timeCharts.add(catchTimeChart);
			timeCharts.add(pCheatTimeChart);
			timeCharts.add(riskTimeChart);
			
			BarChart allocChart = new BarChart(sim, windowSize, "Allocated Resource Bar Plot", "Allocation", "r", "AllBar", 0.0, 1.0);
			BarChart pCheatChart = new BarChart(sim, windowSize, "Propensity to Cheat Bar Plot", "PCheat", "pCheat", "PchBar", 0.0, 1.0);
			
			double utiMax = 
					((Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b"))) >= Double.parseDouble(sim.getParameters().get("c")))
						? Double.parseDouble(sim.getParameters().get("a")) + Double.parseDouble(sim.getParameters().get("b")) : Double.parseDouble(sim.getParameters().get("c"));
			BarChart utilityChart = new BarChart(sim, windowSize, "Utility Bar Plot", "Utility", "U", "UtiBar", -utiMax, utiMax);
			DistributionChart utDistrChart = new DistributionChart(sim, windowSize, "UtiDistr", -utiMax, utiMax);

			List<Chart> otherCharts = new ArrayList<Chart>();
			otherCharts.add(allocChart);
			otherCharts.add(pCheatChart);
			otherCharts.add(utilityChart);
			otherCharts.add(utDistrChart);
			
			SpiderWebChart spiderChart = new SpiderWebChart(sim, windowSize, 
					"Radar Chart of Networks", "SpiPlot", -utiMax, utiMax);
	
			final Frame fTime = new Frame("Time Series Plots");
			final Panel pTime = new Panel(new GridLayout(2, 2));
			
			final Frame fOther = new Frame("Bar & Distribution Plots");
			final Panel pOther = new Panel(new GridLayout(2, 2));
			
			final Frame fSpider = new Frame("Spider Web Plots");
			final Panel pSpider = new Panel(new GridLayout(1, 1));
						
			final JFrame jf = new JFrame("Social Network Visualiser");
	        NetworkGraph ng = new NetworkGraph(sim);
	        							
	        FRLayout<String, String> layout = new FRLayout<String, String>(ng.getGraph());
	        layout.setAttractionMultiplier(0.25);
	        layout.setAttractionMultiplier(0.25);
	        layout.setRepulsionMultiplier(5);
	        
	    	VisualizationViewer<String, String> vv = new VisualizationViewer<String, String>(layout, new Dimension(1080, 720));
			vv.setBackground(Color.WHITE);
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
				fTime.add(pTime);
				
				for (Chart chart : timeCharts) {
					pTime.add(chart.getPanel());
				}
				
				fTime.pack();
				fTime.setVisible(true);
				
				fOther.add(pOther);

				for (Chart chart : otherCharts) {
					pOther.add(chart.getPanel());
				}
				
				fOther.pack();
				fOther.setVisible(true);
				
				fSpider.add(pSpider);
				pSpider.add(spiderChart.getPanel());
				fSpider.pack();
				fSpider.setVisible(true);
	
	    		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				jf.getContentPane().add(vv);
				jf.pack();
				jf.setVisible(true);
			}
						
			for (int i = 1; i <= t; i++) {
				
				spiderChart.redraw(i);

				ng.updateGraph(i, layout);
		        vv.setGraphLayout(layout);
		        
				if (exportMode)	{	
					ChartUtils.saveChart(spiderChart.getChart(), imagePath,
							sim.getName() + "/" + spiderChart.getShortName(), t, t0);
					
					ChartUtils.saveGraph(vv, imagePath, sim.getName() + "/" + "Net", i);
				}
			}
					
			while (t < sim.getFinishTime() / 2) {
				t++;
				
				for (Chart chart : timeCharts) {
					chart.redraw(t);
				}
				
				for (Chart chart : otherCharts) {
					chart.redraw(t);
				}
				
				spiderChart.redraw(t);
										
				ng.updateGraph(t, layout);
		        vv.setGraphLayout(layout);
				
				if (exportMode) {		
					if (t0 == -1)
						t0 = t;
					
					for (Chart chart : timeCharts) {
						ChartUtils.saveChart(chart.getChart(), imagePath,
								sim.getName() + "/" + chart.getShortName(), t, t0);
					}
					
					for (Chart chart : otherCharts) {
						ChartUtils.saveChart(chart.getChart(), imagePath,
								sim.getName() + "/" + chart.getShortName(), t, t0);
					}
					
					ChartUtils.saveChart(spiderChart.getChart(), imagePath,
							sim.getName() + "/" + spiderChart.getShortName(), t, t0);
														
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
	
	private void/*List<Charts>*/ buildForMethods(List<Long> simIds) {
		if (outputComparisonCharts) {
			if (exportMode) {
				File exportDir = new File(imagePath + "COMPARISON");
				
				if (!exportDir.exists())
					exportDir.mkdir();
				else if (!exportDir.isDirectory())
					System.exit(60);
			}
			
			logger.info("Processing for " + methodComp + " methods...");
			
			DefaultCategoryDataset utiData = new DefaultCategoryDataset();
			JFreeChart sumUtiChart = ChartFactory.createBarChart(
		                "Total Utility Generated in Experiment: " + methodComp, // Chart title
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
	        sumUtiChart.getLegend().setPosition(RectangleEdge.RIGHT);

	        // Set the range axis to display integers only...
	        utiPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        utiPlot.getRangeAxis().setAutoRange(true);
	        
			DefaultCategoryDataset longevityData = new DefaultCategoryDataset();
			JFreeChart longevityChart = ChartFactory.createBarChart(
		                "Sustainability of Networks in Experiment: " + methodComp, // Chart title
		                "Method", // Domain axis label
		                "Avg. Longevity of Networks (% of Overall Simulation Time)", // Range axis label
		                longevityData, // Dataset
		                PlotOrientation.HORIZONTAL,
		                false, // Include legend
		                false, // Tooltips
		                false // URLs
	                );
			
	        // Get a reference to the plot for further customisation...
	        CategoryPlot longevityPlot = longevityChart.getCategoryPlot();
	        
	        longevityPlot.setBackgroundPaint(Color.WHITE);
	        
	        // Set the range axis to display integers only...
	        longevityPlot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
	        longevityPlot.getRangeAxis().setRange(0, 100);

	        
			DefaultXYDataset satTimeData = new DefaultXYDataset();
			JFreeChart satTimeChart = ChartFactory.createXYLineChart("Avg. Agent Satisfaction in Experiment: " + methodComp,
					"Satisfaction", "Timestep", satTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			satTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			satTimeChart.getXYPlot().getDomainAxis().setRange(0, 1);
			satTimeChart.getXYPlot().setWeight(3);
			satTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			satTimeChart.getLegend().setPosition(RectangleEdge.RIGHT);

			DefaultXYDataset pChTimeData = new DefaultXYDataset();
			JFreeChart pChTimeChart = ChartFactory.createXYLineChart("Avg. Agent Propensity to Cheat in Experiment: " + methodComp, 
					"PCheat", "Timestep", pChTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			pChTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			pChTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);
			pChTimeChart.getXYPlot().setWeight(3);
			pChTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			pChTimeChart.getLegend().setPosition(RectangleEdge.RIGHT);

			DefaultXYDataset riskTimeData = new DefaultXYDataset();
			JFreeChart riskTimeChart = ChartFactory.createXYLineChart("Avg. Agent Perceived Risk in Experiment: " + methodComp, 
					"Risk", "Timestep", riskTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			riskTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			riskTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);
			riskTimeChart.getXYPlot().setWeight(3);
			riskTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			riskTimeChart.getLegend().setPosition(RectangleEdge.RIGHT);

			DefaultXYDataset catchTimeData = new DefaultXYDataset();
			JFreeChart catchTimeChart = ChartFactory.createXYLineChart("Avg. Agent Perceived Catch Rate for Experiment: " + methodComp, 
					"Catch Rate", "Timestep", catchTimeData, PlotOrientation.HORIZONTAL, true, false, false);

			catchTimeChart.getXYPlot().setBackgroundPaint(Color.WHITE);
			catchTimeChart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);
			catchTimeChart.getXYPlot().setWeight(3);
			catchTimeChart.getXYPlot().getRangeAxis().setAutoRange(true);
			catchTimeChart.getLegend().setPosition(RectangleEdge.RIGHT);

	        Font titleFont = new Font("Arial", Font.BOLD, 20);
	        Font labelFont = new Font("Arial", Font.BOLD, 16);

			DefaultCategoryDataset spiderWebData = new DefaultCategoryDataset();
			RadarPlot radarPlot = new RadarPlot(spiderWebData);
			radarPlot.setBackgroundPaint(Color.WHITE);
			radarPlot.setAxisTickVisible(true);	        	        
			radarPlot.setDrawOutOfRangePoints(true);
			JFreeChart radarChart = new JFreeChart("Radar Chart of Network Properties for Experiment: " + methodComp,
					titleFont, radarPlot, false); 
	        LegendTitle legendtitle = new LegendTitle(radarPlot); 
	        legendtitle.setPosition(RectangleEdge.RIGHT);  
	        radarChart.addSubtitle(legendtitle); 
	        radarPlot.setLabelFont(labelFont);
	        
			DefaultCategoryDataset bestWebData = new DefaultCategoryDataset();
			RadarPlot bestRadarPlot = new RadarPlot(bestWebData);
			bestRadarPlot.setBackgroundPaint(Color.WHITE);
			bestRadarPlot.setAxisTickVisible(true);	        	        
			bestRadarPlot.setDrawOutOfRangePoints(true);
			JFreeChart bestRadarChart = new JFreeChart("Radar Chart of Most Sustainable Network Properties for Experiment: " + methodComp,
					titleFont, bestRadarPlot, false); 
	        LegendTitle bestWebLegendtitle = new LegendTitle(bestRadarPlot); 
	        bestWebLegendtitle.setPosition(RectangleEdge.RIGHT);  
	        bestRadarChart.addSubtitle(legendtitle); 
	        bestRadarPlot.setLabelFont(labelFont);
	        
			String[] keys = new String[] { "c", "nc", "all" };

			for (Long simId : simIds) {
				t = 0;
				sim = sto.getSimulationById(simId);

				String method = sim.getName();
				
				Map<String, Double> uSums = new HashMap<String, Double>();
				for (String k : keys) {
					uSums.put(k, 0.0);
				}
				
				SummaryStatistics networkLongevity = new SummaryStatistics();
				SummaryStatistics networkSumUt = new SummaryStatistics();
				SummaryStatistics networkAvgUt = new SummaryStatistics();
				SummaryStatistics networkStdUt = new SummaryStatistics();
				SummaryStatistics networkMonitoring = new SummaryStatistics();

				int length = (int)(sim.getFinishTime()/2);
				
				int totalNumNetworks = 0;
				
				String bestNet = "";
				double bestLongevity = 0.0;
				int bestTime = 0;
				
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
					SummaryStatistics sat = new SummaryStatistics();
					
					pchMean[1][t-1] = t;
					SummaryStatistics pch = new SummaryStatistics();
					
					riskMean[1][t-1] = t;
					SummaryStatistics risk = new SummaryStatistics();
					
					catchMean[1][t-1] = t;
					SummaryStatistics catchR = new SummaryStatistics();
					
					for (String prop : sim.getEnvironment().getProperties(t).keySet()) {
						double val = Double.parseDouble(sim.getEnvironment().getProperty(prop, t));
						
						if (t == length || sim.getEnvironment().getProperty(prop, t + 1) == null) {
							totalNumNetworks++;
							
							if (prop.contains("longevity")) {
								networkLongevity.addValue(val);
								
								if (val > bestLongevity) {
									bestLongevity = val;
									bestNet = prop.substring(0, prop.indexOf("-"));
									bestTime = t;
								}
							}
							else if (prop.contains("utility-avg"))
								networkAvgUt.addValue(val);
							else if (prop.contains("utility-std"))
								networkStdUt.addValue(val);
							else if (prop.contains("utility-sum"))
								networkSumUt.addValue(val);
							else if (prop.contains("monitoringLevel"))
								networkMonitoring.addValue(val);	
						}
					}
					
					for (PersistentAgent a : sim.getAgents()) {
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
								sat.addValue(o);
							}
							
							if (s.getProperty("pCheat") != null) {
								double pc = Double.parseDouble(s.getProperty("pCheat"));
								pch.addValue(pc);
							}
							
							if (s.getProperty("risk") != null) {
								double r = Double.parseDouble(s.getProperty("risk"));
								risk.addValue(r);
							}
							
							if (s.getProperty("catchRate") != null) {
								double cr = Double.parseDouble(s.getProperty("catchRate"));
								catchR.addValue(cr);
							}
						}
						
						satMean[0][t-1] = sat.getMean();
						pchMean[0][t-1] = pch.getMean();
						riskMean[0][t-1] = risk.getMean();
						catchMean[0][t-1] = catchR.getMean();
					}
															
					uSums.put("all", uSums.get("c") + uSums.get("nc"));
					
					satTimeData.addSeries(sim.getName(), satMean);
					pChTimeData.addSeries(sim.getName(), pchMean);
					riskTimeData.addSeries(sim.getName(), riskMean);
					catchTimeData.addSeries(sim.getName(), catchMean);
				}
				
				for (String k : keys) {
					utiData.addValue(uSums.get(k), k, method);
				}
				
				double longPerc = (networkLongevity.getMean()/length) * 100;
				longevityData.addValue(longPerc, "Sustainability", method);
				
				spiderWebData.addValue(longPerc, method, "Longevity (%)");
				spiderWebData.addValue(networkSumUt.getMean(), method, "Ut. Sum");
				spiderWebData.addValue(networkAvgUt.getMean(), method, "Ut. Avg.");
				spiderWebData.addValue(networkStdUt.getMean(), method, "Ut. Std.");
				spiderWebData.addValue(networkMonitoring.getMean(), method, "Monitoring Frequency");
				spiderWebData.addValue((double)(totalNumNetworks)/5.0, method, "Total No. of Networks Formed");
				
				bestWebData.addValue((bestLongevity/length) * 100, method, "Longevity (%)");
				bestWebData.addValue(Double.parseDouble(sim.getEnvironment()
						.getProperty(bestNet + "-utility-sum", bestTime)), 
						method, "Ut. Sum");
				bestWebData.addValue(Double.parseDouble(sim.getEnvironment()
						.getProperty(bestNet + "-utility-avg", bestTime)), 
						method, "Ut. Avg");
				bestWebData.addValue(Double.parseDouble(sim.getEnvironment()
						.getProperty(bestNet + "-utility-std", bestTime)), 
						method, "Ut. Std");
				bestWebData.addValue(Double.parseDouble(sim.getEnvironment()
						.getProperty(bestNet + "-monitoringLevel", bestTime)), 
						method, "Monitoring Frequency");
			}	
			
			if (exportMode) {
				ChartUtils.saveChart(sumUtiChart, imagePath, "COMPARISON/" + "UTI_" + this.methodComp);
				ChartUtils.saveChart(longevityChart, imagePath, "COMPARISON/" + "LONGEVITY_" + this.methodComp);
				ChartUtils.saveChart(satTimeChart, imagePath, "COMPARISON/" + "SAT_TIME_" + this.methodComp);
				ChartUtils.saveChart(pChTimeChart, imagePath, "COMPARISON/" + "PCHEAT_TIME_" + this.methodComp);
				ChartUtils.saveChart(riskTimeChart, imagePath, "COMPARISON/" + "RISK_TIME_" + this.methodComp);
				ChartUtils.saveChart(catchTimeChart, imagePath, "COMPARISON/" + "CATCH_TIME_" + this.methodComp);
				ChartUtils.saveChart(radarChart, imagePath, "COMPARISON/" + "SPIDER_WEB_" + this.methodComp);
				ChartUtils.saveChart(bestRadarChart, imagePath, "COMPARISON/" + "BEST_WEB_" + this.methodComp);
			}
			
			/*List<JFreeChart> charts = new ArrayList<JFreeChart>();
			charts.add(allocChart);
			charts.add(pCheatChart);
			charts.add(utilityChart);
			charts.add(utDistrChart);*/

			logger.info("Done building charts for " + this.methodComp + " methods.");	
		}
	}
	
	/**
	 * Take the map from {choiceMethod:{chartType:chart}} and build one chart for each chartType
	 * Each chart has on it an avg line for each choiceMethod
	 * 
	 * @param methodCharts
	 * @param endTime
	 */
	/* private void combineMethodCharts(List<Chart> methodCharts) {
		logger.info("Combining data from method types...");
		HashMap<String,XYDataset> outputData = new HashMap<String,XYDataset>();
		for (Entry<OwnChoiceMethod, HashMap<String, Chart>> methodEntry : methodCharts.entrySet()) {
			OwnChoiceMethod method = methodEntry.getKey();
			HashMap<String,Chart> chartMap = methodEntry.getValue();
			logger.debug("Getting data from method \"" + method + "\"...");
			for (Entry<String,Chart> chartEntry : chartMap.entrySet()) {
				String chartType = chartEntry.getKey();
				Chart chart = chartEntry.getValue();
				if (!outputData.containsKey(chartType)) {
					outputData.put(chartType, new XYSeriesCollection());
				}
				// get avg series from chart to put into output dataset
				XYPlot xyPlot = chart.getChart().getXYPlot();
				XYSeries series = null;
				// sanity check
				if (xyPlot.getDatasetCount()!=1) {
					try {
						series = (XYSeries) ((XYSeriesCollection)xyPlot.getDataset(1)).getSeries(0).clone();
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
					}
				}
				if (series!=null) {
					logger.debug("Got data from method \"" + method + "\".");
					series.setKey(method.toString());
					((XYSeriesCollection)outputData.get(chartType)).addSeries(series);
				}
			}
		}
		logger.info("Got all method data. Drawing charts...");
		LinkedHashSet<Chart> finalCharts = new LinkedHashSet<Chart>();
		for (Entry<String,XYDataset> dataEntry : outputData.entrySet()) {
			String chartType = dataEntry.getKey();
			XYDataset dataset = dataEntry.getValue();
			logger.debug("Drawing " + chartType);
			logger.debug("Drawing comparison of " + chartType + " chart...");
			Chart chart = new CombinedTimeSeriesChart(chartType, dataset, endTime);
			finalCharts.add(chart);
			if (chart!=null && outputComparisonCharts) {
				ChartUtils.saveChart(chart.getChart(), imagePath, "_comparison", chartType);
			}
		}
		
		if (!headlessMode && outputComparisonCharts) {
			Frame frame = new Frame("Comparison Results");
			Panel panel = new Panel(new GridLayout(0,2));
			frame.add(panel);
			for (Chart chart : finalCharts) {
				panel.add(chart.getPanel());
			}
			panel.add(globalLengthBAW.getPanel());
			frame.pack();
			frame.setVisible(true);
			ChartUtils.savePanel(panel, imagePath, "_", "comparison"); // won't draw if not visible...
		}
		
		logger.info("Done drawing comparison charts.");
	} */
	
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
