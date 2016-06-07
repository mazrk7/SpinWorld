package spinworld.gui;

import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleEdge;

import uk.ac.imperial.presage2.core.db.persistent.PersistentEnvironment;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

public class SpiderWebChart implements Chart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultCategoryDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	final RadarPlot plot;
	final String shortName;

	SpiderWebChart(PersistentSimulation sim, int windowSize, String title, 
			String shortName, double utiMin, double utiMax) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		this.shortName = shortName;
		
    	data = new DefaultCategoryDataset();
		plot = new RadarPlot(data);
		plot.setBackgroundPaint(Color.WHITE);
        plot.setAxisTickVisible(true);
        
    	setupPlot(utiMin, utiMax);
        
        plot.setDrawOutOfRangePoints(true);

        Font titleFont = new Font("Arial", Font.BOLD, 20);
		chart = new JFreeChart(title, titleFont, plot, false); 
        LegendTitle legendtitle = new LegendTitle(plot);
        legendtitle.setPosition(RectangleEdge.RIGHT);  
        chart.addSubtitle(legendtitle); 
        Font labelFont = new Font("Arial", Font.BOLD, 16);
        plot.setLabelFont(labelFont);
        
		panel = new ChartPanel(chart);
	}

	@Override
	public ChartPanel getPanel() {
		return panel;
	}

	@Override
	public JFreeChart getChart() {
		return chart;
	}
	
	@Override
	public String getShortName() {
		return this.shortName;
	}
	
	@Override
	public void redraw(int t) {				
		PersistentEnvironment pEnv = sim.getEnvironment();
		for (String prop : pEnv.getProperties(t).keySet()) {
			String net = (prop.substring(0, 3).contains("-")) ? prop.substring(0, 2) : prop.substring(0, 3);
			Double val = Double.parseDouble(pEnv.getProperty(prop, t));
			
			if (prop.contains("longevity")) {
				double perc = (val/(sim.getFinishTime()/2)) * 100;
				data.addValue(perc, net, "Longevity (%)");
			} else if (prop.contains("utility-avg")) {
				data.addValue(val, net, "Utility Avg.");
			} else if (prop.contains("utility-std")) {
				data.addValue(val, net, "Utility Std.");
			} else if (prop.contains("utility-sum")) {
				data.addValue(val, net, "Utility Cumulative Sum");
			} else if (prop.contains("monitoringLevel")) {
				data.addValue(val, net, "Monitoring Level");
			}
		}
	}
	
    private void setupPlot(double utiMin, double utiMax) {
		data.addValue(0.0, "NET_INIT", "Longevity (%)");
		data.addValue(0.0, "NET_INIT", "Utility Avg.");
		data.addValue(0.0, "NET_INIT", "Utility Std.");
		data.addValue(0.0, "NET_INIT", "Utility Cumulative Sum");
		data.addValue(0.0, "NET_INIT", "Monitoring Level");
		
		plot.setMaxValue(0, 100.0);
		plot.setMaxValue(1, utiMax);
		plot.setMaxValue(2, utiMax);
		plot.setMaxValue(3, (double)sim.getFinishTime()/2.0);
		plot.setMaxValue(4, 1.0);

        plot.setOrigin(0, 0d);
        plot.setOrigin(1, utiMin);
        plot.setOrigin(2, 0d);
        plot.setOrigin(3, (double)-sim.getFinishTime()/2.0);
        plot.setOrigin(4, 0d);           
    }

}
