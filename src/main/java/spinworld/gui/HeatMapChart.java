package spinworld.gui;

import spinworld.gui.heatmap.HeatMap;

public interface HeatMapChart {

	HeatMap getPanel();

	void redraw(int t);
	
}
