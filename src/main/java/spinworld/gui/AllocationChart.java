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

public class AllocationChart implements TimeSeriesChart {

	final PersistentSimulation sim;
	final int windowSize;

	final Long simId;
	final DefaultCategoryDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	Map<String, DescriptiveStatistics> agentAllocations = new HashMap<String, DescriptiveStatistics>();

	AllocationChart(Long simId, PersistentSimulation sim, int windowSize) {
		super();
		this.simId = simId;
		this.sim = sim;
		this.windowSize = windowSize;

		data = new DefaultCategoryDataset();
		chart = ChartFactory.createBarChart("Average Allocation over last 50 rounds", "", "", data,
				PlotOrientation.VERTICAL, false, false, false);
		panel = new ChartPanel(chart);

		chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		chart.getCategoryPlot().getRangeAxis().setRange(0, 1);
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
	public void hideLegend(boolean hide) {
		this.chart.getLegend().setVisible(!hide);
	}

	@Override
	public Long getSimId() {
		return this.simId;
	}

	@Override
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		if (agentAllocations.size() == 0) {
			for (PersistentAgent a : sim.getAgents()) {
				DescriptiveStatistics alloc = new DescriptiveStatistics(windowSize);
				agentAllocations.put(a.getName(), alloc);
				for (int i = 0; i < length; i++) {
					int t = start + i + 1;
					TransientAgentState s = a.getState(t);

					if (s != null && s.getProperty("r") != null) {
						double all = Double.parseDouble(s.getProperty("r"));
						alloc.addValue(all);
					}
				}
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				if (!agentAllocations.containsKey(a.getName())) {
					agentAllocations.put(a.getName(), new DescriptiveStatistics(windowSize));
				}

				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty("r") != null) {
					double all = Double.parseDouble(s.getProperty("r"));
					agentAllocations.get(a.getName()).addValue(all);
				} else {
					agentAllocations.put(a.getName(), new DescriptiveStatistics(new double[] { 0 }));
				}
			}
		}

		List<String> agents = new ArrayList<String>(agentAllocations.keySet());
		Collections.sort(agents);
		for (String key : agents) {
			DescriptiveStatistics e = agentAllocations.get(key);
			data.addValue(e.getMean(), key.substring(0, 1), key);
		}
	}

}
