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
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.tc33.jheatchart.HeatChart;

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
	
	final DatabaseService db;
	final StorageService sto;

	PersistentSimulation sim;
	int t = 5;
	int windowSize = 50;
	int t0 = -1;

	boolean exportMode = false;

	VisualizationViewer<String, String> vv = null;

	final static String imagePath = "/home/markzolotas7/Videos/";

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			SpinWorldGUI gui = injector.getInstance(SpinWorldGUI.class);
			
			if (args.length > 1 && Boolean.parseBoolean(args[1]) == true)
				gui.exportMode = true;
			
			gui.init(Integer.parseInt(args[0]));
		}
	}

	@Inject
	public SpinWorldGUI(DatabaseService db, StorageService sto) {
		super();
		this.db = db;
		this.sto = sto;
	}

	void takeScreenshot(JFreeChart chart, String base, int i) {
		try {
			ChartUtilities.saveChartAsPNG(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"),
					chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void takeScreenshot(HeatChart chart, String base, int i) {
		try {
			chart.saveToFile(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void saveGraph(String base, int i) {	
		VisualizationImageServer<String, String> vis =
			    new VisualizationImageServer<String, String>(vv.getGraphLayout(),
			        vv.getGraphLayout().getSize());

		vis.setBackground(Color.WHITE);
		
        NetworkRenderer nr = new NetworkRenderer(sim);

        vis.getRenderContext().setVertexFillPaintTransformer(nr.getVertexPaint());
        vis.getRenderContext().setVertexShapeTransformer(nr.getVertexLabelTransformer());
        vis.getRenderContext().setVertexFontTransformer(nr.getVertexFont());
        vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
        vis.getRenderContext().setEdgeStrokeTransformer(nr.getEdgeStrokeTransformer());
        vis.getRenderContext().setEdgeFontTransformer(nr.getEdgeFont());
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

	public void init(long simId) {
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		sim = sto.getSimulationById(simId);
		if (exportMode) {
			File exportDir = new File(imagePath + sim.getName());
			
			if (!exportDir.exists())
				exportDir.mkdir();
			else if (!exportDir.isDirectory())
				System.exit(60);
		}
		
		List<TimeSeriesChart> charts = new ArrayList<TimeSeriesChart>();
		charts.add(new UtilityChart(sim, windowSize));
		charts.add(new SatisfactionChart(sim, windowSize));
		
		List<HeatMap> maps = new ArrayList<HeatMap>();
		maps.add(new UtilityCatchMap(sim, windowSize));
		maps.add(new UtilityRiskMap(sim, windowSize));

		final Frame f = new Frame("SpinWorld Time Series Plots");
		final Panel p = new Panel(new GridLayout(2, 2));
		
		final JFrame jf = new JFrame("SpinWorld");
        jf.setTitle("SpinWorld Social Network Visualiser"); //Set the title of our window.
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //Give a close operation.
        NetworkGraph ng = new NetworkGraph(sim);
        							
        FRLayout<String, String> layout = new FRLayout<String, String>(ng.getGraph());
        layout.setSize(new Dimension(1080, 600));
        
		vv = new VisualizationViewer<String, String>(layout, new Dimension(1280, 720));
		vv.setBackground(Color.WHITE);
	    vv.setGraphMouse(new DefaultModalGraphMouse<String, String>());
	    
        NetworkRenderer nr = new NetworkRenderer(sim);

	    vv.getRenderContext().setVertexFillPaintTransformer(nr.getVertexPaint());
	    vv.getRenderContext().setVertexShapeTransformer(nr.getVertexLabelTransformer());
	    vv.getRenderContext().setVertexFontTransformer(nr.getVertexFont());
	    vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
	    vv.getRenderContext().setEdgeStrokeTransformer(nr.getEdgeStrokeTransformer());
	    vv.getRenderContext().setEdgeFontTransformer(nr.getEdgeFont());
	    vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller<String>());
	    vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	    
		if (!exportMode) {
			f.add(p);
			
			for (TimeSeriesChart chart : charts) {
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
	        vv.setGraphLayout(new FRLayout<String, String>(ng.getGraph()));
	        
			if (exportMode)		
				saveGraph(sim.getName() + "/" + "Net", i);
		}
				
		while (t < sim.getFinishTime() / 2) {
			t++;
			
			for (TimeSeriesChart chart : charts) {
				chart.redraw(t);
			}
			
			for (HeatMap map : maps) {
				map.redraw(t);
			}
									
			ng.updateGraph(t);
	        vv.setGraphLayout(new FRLayout<String, String>(ng.getGraph()));
			
			if (exportMode) {		
				if (t0 == -1)
					t0 = t;
				
				for (TimeSeriesChart chart : charts) {
					takeScreenshot(chart.getChart(),
							sim.getName() + "/" + chart.getClass().getSimpleName().substring(0, 3), t);
				}
				
				for (HeatMap map : maps) {
					takeScreenshot(map.getChart(),
							sim.getName() + "/" + map.getClass().getSimpleName().substring(0, 3) + map.getClass().getSimpleName().substring(7, 10),
							t);
				}
								
				saveGraph(sim.getName() + "/" + "Net", t);		
			} else {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		db.stop();
	}

}
