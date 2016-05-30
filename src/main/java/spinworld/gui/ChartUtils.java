package spinworld.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;

import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;

public abstract class ChartUtils {

	public static void saveChart(JFreeChart chart, String imagePath, String base) {
		try {
			ChartUtilities.saveChartAsPNG(new File(imagePath + base + ".png"),
					chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void saveChart(JFreeChart chart, String imagePath, String base, int i, int t0) {
		try {
			ChartUtilities.saveChartAsPNG(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"),
					chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void savePanel(JPanel panel, String imagePath, String base, int i, int t0) {
		panel.setSize(new Dimension(1280, 720));
        
        BufferedImage img = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        panel.paint(img.getGraphics());

        try {     
			// Write image to a png file
			File outputfile = new File(imagePath + base + "" + String.format("%04d", i) + ".png");
			
		    ImageIO.write(img, "png", outputfile);
		} catch (IOException ee) {
			ee.printStackTrace();
		}
	}
	
	public static void saveGraph(VisualizationViewer<String, String> vv, String imagePath, String base, int i) {	
		VisualizationImageServer<String, String> vis =
			    new VisualizationImageServer<String, String>(vv.getGraphLayout(),
			        vv.getGraphLayout().getSize());

		vis.setBackground(Color.LIGHT_GRAY);
		
        vis.getRenderContext().setVertexFillPaintTransformer(NetworkRenderer.getVertexPaintTransformer());
        vis.getRenderContext().setVertexShapeTransformer(NetworkRenderer.getVertexLabelTransformer());
        vis.getRenderContext().setVertexFontTransformer(NetworkRenderer.getVertexFontTransformer());
        vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<String>());
	    vis.getRenderContext().setEdgeDrawPaintTransformer(NetworkRenderer.getEdgePaintTransformer());
        vis.getRenderContext().setEdgeStrokeTransformer(NetworkRenderer.getEdgeStrokeTransformer());
        vis.getRenderContext().setEdgeFontTransformer(NetworkRenderer.getEdgeFontTransformer());
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
