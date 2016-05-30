package spinworld.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

interface Chart {

	Long getSimId();
	
	ChartPanel getPanel();

	JFreeChart getChart();
	
	String getShortName();
	
	void hideLegend(boolean hide);
	
	void redraw(int t);
	
	XYPlot getXYPlot();

}
