package spinworld.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

interface Chart {
	
	ChartPanel getPanel();

	JFreeChart getChart();
	
	String getShortName();
		
	void redraw(int t);
	
}
