package spinworld.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class BarChart implements Chart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultCategoryDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	Map<String, DescriptiveStatistics> agentData = new HashMap<String, DescriptiveStatistics>();
	final String property;
	final String shortName;
	
	BarChart(PersistentSimulation sim, int windowSize, String title, 
			String yAxis, String property, String shortName, double lb, double ub) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		this.property = property;
		this.shortName = shortName;

		data = new DefaultCategoryDataset();
		chart = ChartFactory.createBarChart(title, yAxis, "Particles", data,
				PlotOrientation.VERTICAL, false, false, false);
		panel = new ChartPanel(chart);

		chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		chart.getCategoryPlot().getRangeAxis().setRange(lb, ub);
		BarRenderer renderer = (BarRenderer) chart.getCategoryPlot().getRenderer();
		renderer.setItemMargin(-.5);
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
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		if (agentData.size() == 0) {
			for (PersistentAgent a : sim.getAgents()) {
				DescriptiveStatistics props = new DescriptiveStatistics(windowSize);
				agentData.put(a.getName(), props);
				for (int i = 0; i < length; i++) {
					int t = start + i + 1;
					TransientAgentState s = a.getState(t);

					if (s != null && s.getProperty(this.property) != null) {
						double p = Double.parseDouble(s.getProperty(this.property));
						props.addValue(p);
					}
				}
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				if (!agentData.containsKey(a.getName())) {
					agentData.put(a.getName(), new DescriptiveStatistics(windowSize));
				}

				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty(this.property) != null) {
					double p = Double.parseDouble(s.getProperty(this.property));
					agentData.get(a.getName()).addValue(p);
				} else {
					agentData.put(a.getName(), new DescriptiveStatistics(new double[] { 0 }));
				}
			}
		}

		List<String> agents = new ArrayList<String>(agentData.keySet());
		Collections.sort(agents);
		for (String key : agents) {
			DescriptiveStatistics e = agentData.get(key);
			data.addValue(e.getMean(), key.substring(0, 1), key);
		}
	}

}
