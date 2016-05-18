package spinworld.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.BasicStroke;

import org.apache.commons.collections15.Transformer;

import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

public class NetworkRenderer {

	final PersistentSimulation sim;
	
	public NetworkRenderer(PersistentSimulation sim) {
		this.sim = sim;
	}
	
	public Transformer<String, Paint> getVertexPaint() {
	    // Setup up a new vertex to paint transformer
	    Transformer<String, Paint> vertexPaint = new Transformer<String, Paint>() {
	    	public Paint transform(String i) {
	    		return Color.GREEN;
	    	}
	    };
	    
	    return vertexPaint;
	}
	
	public Transformer<String, Font> getVertexFont() {
	    Transformer<String, Font> vertexFont = new Transformer<String, Font>() {
	    	public Font transform(String i) {
	    		return new Font("Verdana", Font.BOLD, 12);
	    	}
	    };
	    
	    return vertexFont;
	}
	
	public Transformer<String, Shape> getVertexLabelTransformer() {
	    Transformer<String, Shape> vertexLabelTransformer = new Transformer<String, Shape>() {
	    	public Shape transform(String i) {
			    double width = i.length() * 10.0;
			    Shape circle = new java.awt.geom.Ellipse2D.Double(-(width/2), -12.5, width, 25);
	    		return circle;
	    	}
	    };
	    
	    return vertexLabelTransformer;
	}
	
	public Transformer<String, Stroke> getEdgeStrokeTransformer() {
		float dash[] = {15.0f};
	    final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
	    		BasicStroke.JOIN_MITER, 15.0f, dash, 0.0f);
	    
	    Transformer<String, Stroke> edgeStrokeTransformer = new Transformer<String, Stroke>() {
		    public Stroke transform(String s) {
		    	return edgeStroke;
		    }
	    };
	    
	    return edgeStrokeTransformer;
	} 
	
	public Transformer<String, Font> getEdgeFont() {
	    Transformer<String, Font> edgeFont = new Transformer<String, Font>() {
	    	public Font transform(String i) {
	    		return new Font("Verdana", Font.ITALIC, 10);
	    	}
	    };
	    
	    return edgeFont;
	}
	
}
