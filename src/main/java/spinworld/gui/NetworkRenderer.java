package spinworld.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.BasicStroke;

import org.apache.commons.collections15.Transformer;

public abstract class NetworkRenderer {
	
	public static Transformer<String, Paint> getVertexPaintTransformer() {
	    // Setup up a new vertex to paint transformer
	    Transformer<String, Paint> vertexPaint = new Transformer<String, Paint>() {
	    	public Paint transform(String i) {
	    		return Color.RED;
	    	}
	    };
	    
	    return vertexPaint;
	}
	
	public static Transformer<String, Font> getVertexFontTransformer() {
	    Transformer<String, Font> vertexFont = new Transformer<String, Font>() {
	    	public Font transform(String i) {
	    		return new Font("Arial", Font.BOLD, 12);
	    	}
	    };
	    
	    return vertexFont;
	}
	
	public static Transformer<String, Shape> getVertexLabelTransformer() {
	    Transformer<String, Shape> vertexLabelTransformer = new Transformer<String, Shape>() {
	    	public Shape transform(String i) {
			    double width = i.length() * 10.0;
			    return new Ellipse2D.Double(-(width/2), -12.5, width, 25);
	    	}
	    };
	    
	    return vertexLabelTransformer;
	}
	
	public static Transformer<String, Paint> getEdgePaintTransformer() {
	    // Setup up a new vertex to paint transformer
	    Transformer<String, Paint> edgePaint = new Transformer<String, Paint>() {
	    	public Paint transform(String i) {
	    		return Color.GREEN;
	    	}
	    };
	    
	    return edgePaint;
	}
	
	public static Transformer<String, Stroke> getEdgeStrokeTransformer() {
	    final Stroke edgeStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
	    		BasicStroke.JOIN_MITER, 10.0f);
	    
	    Transformer<String, Stroke> edgeStrokeTransformer = new Transformer<String, Stroke>() {
		    public Stroke transform(String s) {
		    	return edgeStroke;
		    }
	    };
	    
	    return edgeStrokeTransformer;
	} 
	
	public static Transformer<String, Font> getEdgeFontTransformer() {
	    Transformer<String, Font> edgeFont = new Transformer<String, Font>() {
	    	public Font transform(String i) {
	    		return new Font("Arial", Font.ITALIC, 10);
	    	}
	    };
	    
	    return edgeFont;
	}
	
}
