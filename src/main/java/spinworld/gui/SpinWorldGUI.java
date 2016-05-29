package spinworld.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
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
	int mapWindowSize = 5;
	int t0 = -1;
	
	boolean exportMode = false;
	boolean outputSimCharts = false;
	boolean outputSingle = false;
	Long singleSimId;
	
	VisualizationViewer<String, String> vv = null;

	final static String imagePath = "/home/markzolotas7/Videos/";

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			SpinWorldGUI gui = injector.getInstance(SpinWorldGUI.class);
			
			try {		
				gui.init(args);
				
				List<Long> simIds = gui.getSimulationList();
				int endTime = gui.getEndTime(simIds);
						
				for (Long simId : simIds) {
					gui.buildChartsForSim(simId);
				}
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
		options.addOption("outputSimCharts", false, "Output a set of charts for each simulation ? (default:false)");
		options.addOption("outputSingle", true, "Output a set of charts for a single simulation ? (default:false)");
		
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
	}
	
	private void buildChartsForSim(Long simId) throws IOException, ClassNotFoundException {
		logger.info("Building charts for sim " + simId + "...");
		
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		sim = sto.getSimulationById(simId);
		String choiceMethod = sim.getName();
		
		if (exportMode) {
			File exportDir = new File(imagePath + sim.getName());
			
			if (!exportDir.exists())
				exportDir.mkdir();
			else if (!exportDir.isDirectory())
				System.exit(60);
		}
		
		List<TimeSeriesChart> charts = new ArrayList<TimeSeriesChart>();
		charts.add(new AllocationChart(simId, sim, windowSize));
		charts.add(new PCheatChart(simId, sim, windowSize));
		charts.add(new SatisfactionChart(simId, sim, windowSize));
		
		final Frame f = new Frame("SpinWorld Time Series Plots");
		final Panel p = new Panel(new GridLayout(2, 2));
		
		HeatMapChart map = new UtilityHeatMap(sim, mapWindowSize);
		
		final JFrame hmf = new JFrame("SpinWorld Heat Map Plots");
		final JPanel jp = new JPanel(new GridLayout(2, 2));
		
		final JFrame jf = new JFrame("SpinWorld Social Network Visualiser");
        NetworkGraph ng = new NetworkGraph(sim);
        							
        FRLayout<String, String> layout = new FRLayout<String, String>(ng.getGraph());
        layout.setRepulsionMultiplier(10);
        layout.setAttractionMultiplier(0.10);

		vv = new VisualizationViewer<String, String>(layout, new Dimension(1280, 720));
		vv.setBackground(Color.LIGHT_GRAY);
	    vv.setGraphMouse(new DefaultModalGraphMouse<String, String>());
	    
        NetworkRenderer nr = new NetworkRenderer(sim);

	    vv.getRenderContext().setVertexFillPaintTransformer(nr.getVertexPaintTransformer());
	    vv.getRenderContext().setVertexShapeTransformer(nr.getVertexLabelTransformer());
	    vv.getRenderContext().setVertexFontTransformer(nr.getVertexFontTransformer());
	    vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
	    vv.getRenderContext().setEdgeFillPaintTransformer(nr.getEdgePaintTransformer());
	    vv.getRenderContext().setEdgeStrokeTransformer(nr.getEdgeStrokeTransformer());
	    vv.getRenderContext().setEdgeFontTransformer(nr.getEdgeFontTransformer());
	    vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<String>());
	    vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	    
		if (!exportMode) {
			f.add(p);
			
			for (TimeSeriesChart chart : charts) {
				p.add(chart.getPanel());
			}

			f.pack();
			f.setVisible(true);
			
			jp.add(map.getPanel());
			hmf.add(jp);
			
			hmf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        hmf.setSize(1280, 720);
			hmf.pack();
			hmf.setVisible(true);
						
    		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jf.getContentPane().add(vv);
			jf.pack();
			jf.setVisible(true);
		}
		
		for (int i = 1; i <= t; i++) {
			ng.updateGraph(i);
	        vv.setGraphLayout(layout);
	        
			if (exportMode)		
				saveGraph(sim.getName() + "/" + "Net", i);
		}
				
		while (t < sim.getFinishTime() / 2) {
			t++;
			
			for (TimeSeriesChart chart : charts) {
				chart.redraw(t);
			}
			
			map.redraw(t);
									
			ng.updateGraph(t);
	        vv.setGraphLayout(layout);
			
			if (exportMode) {		
				if (t0 == -1)
					t0 = t;
				
				for (TimeSeriesChart chart : charts) {
					takeScreenshot(chart.getChart(),
							sim.getName() + "/" + chart.getClass().getSimpleName().substring(0, 3), t);
				}
									
				savePaint(map.getPanel(),
							sim.getName() + "/" + map.getClass().getSimpleName().substring(0, 3), t);
								
				saveGraph(sim.getName() + "/" + "Net", t);		
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
	
	private List<Long> getSimulationList() {
		List<Long> result = null;
		
		if (this.outputSingle) 
			result = Collections.singletonList(this.singleSimId);
		else
			result = sto.getSimulations();
		
		return result;
	}
	
	private void finish() {
		db.stop();
	}
	
	private int getEndTime(List<Long> simIds) {
		int endTime = 0;
		
		for (Long simId : simIds) {
			int currTime = sto.getSimulationById(simId).getCurrentTime();
			if (currTime > endTime) {
				endTime = currTime;
			}
		}
		
		return endTime;
	}
	
	void takeScreenshot(JFreeChart chart, String base, int i) {
		try {
			ChartUtilities.saveChartAsPNG(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"),
					chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void savePaint(JPanel panel, String base, int i) {
		panel.setSize(new Dimension(1280, 720));
        
        BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        panel.paint(img.getGraphics());

        try {     
			// Write image to a png file
			File outputfile = new File(imagePath + base + "" + String.format("%04d", i) + ".png");
			
		    ImageIO.write(img, "png", outputfile);
        } catch(IOException e)
        {
			e.printStackTrace();
        }
	}
	
	void saveGraph(String base, int i) {	
		VisualizationImageServer<String, String> vis =
			    new VisualizationImageServer<String, String>(vv.getGraphLayout(),
			        vv.getGraphLayout().getSize());

		vis.setBackground(Color.LIGHT_GRAY);
		
        NetworkRenderer nr = new NetworkRenderer(sim);

        vis.getRenderContext().setVertexFillPaintTransformer(nr.getVertexPaintTransformer());
        vis.getRenderContext().setVertexShapeTransformer(nr.getVertexLabelTransformer());
        vis.getRenderContext().setVertexFontTransformer(nr.getVertexFontTransformer());
        vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
	    vis.getRenderContext().setEdgeDrawPaintTransformer(nr.getEdgePaintTransformer());
        vis.getRenderContext().setEdgeStrokeTransformer(nr.getEdgeStrokeTransformer());
        vis.getRenderContext().setEdgeFontTransformer(nr.getEdgeFontTransformer());
        vis.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<String>());
        vis.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		
		// Create the buffered image
		BufferedImage image = (BufferedImage) vis.getImage(
		    new Point2D.Double(vv.getGraphLayout().getSize().getWidth() / 2,
		    vv.getGraphLayout().getSize().getHeight() / 2),
		    new Dimension(vv.getGraphLayout().getSize()));

		try {
			// Write image to a png file
			File outputfile = new File(imagePath + base + "" + String.format("%04d", i) + ".png");
			
		    ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
