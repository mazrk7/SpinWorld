package spinworld.gui;

import org.tc33.jheatchart.HeatChart;

public interface HeatMap {

	HeatChart getChart();

	void redraw(int t);
}
