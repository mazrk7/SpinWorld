package spinworld.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

interface TimeSeriesChart {

	Long getSimId();
	
	ChartPanel getPanel();

	JFreeChart getChart();
	
	void hideLegend(boolean hide);
	
	void redraw(int t);

}
