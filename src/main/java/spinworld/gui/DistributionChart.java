package spinworld.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class DistributionChart implements Chart {

	final PersistentSimulation sim;
	final int windowSize;
	
	final DefaultXYDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	final String shortName;

	DistributionChart(PersistentSimulation sim, int windowSize, String shortName, double lb, double ub) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		this.shortName = shortName;
		
		data = new DefaultXYDataset();
		chart = ChartFactory.createScatterPlot(
				"Utility Distribution per Time Step", "Ut.", "Compliant rounds", data,
				PlotOrientation.HORIZONTAL, true, false, false);
		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setRange(lb, ub);
		chart.getXYPlot().getRangeAxis()
				.setRange(0, windowSize + 1);
		
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
		return shortName;
	}

	@Override
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);

		List<Pair<Integer, Double>> c = new ArrayList<Pair<Integer, Double>>();
		List<Pair<Integer, Double>> nc = new ArrayList<Pair<Integer, Double>>();
		for (PersistentAgent a : sim.getAgents()) {
			int compliantRounds = 0;
			SummaryStatistics utility = new SummaryStatistics();
			boolean compliant = a.getName().startsWith("c");
			
			for (int i = 0; i < length; i++) {
				int t = start + i + 1;
				TransientAgentState s = a.getState(t);
				
				if (s != null && s.getProperty("U") != null) {
					try {
						double u = Double.parseDouble(s.getProperty("U"));
						utility.addValue(u);
					} catch (NumberFormatException e) {
						continue;
					}
					
					try {
						double g = Double.parseDouble(s.getProperty("g"));
						double p = Double.parseDouble(s.getProperty("p"));
						
						if (Math.abs(g - p) <= 1E-4)
							compliantRounds++;
					} catch (NumberFormatException e) {
						continue;
					}
				}
			}
			
			if (compliant)
				c.add(Pair.of(compliantRounds, utility.getMean()));
			else
				nc.add(Pair.of(compliantRounds, utility.getMean()));
		}
		
		double[][] cArr = new double[2][c.size()];
		for (int i = 0; i < c.size(); i++) {
			Pair<Integer, Double> point = c.get(i);
			cArr[1][i] = point.getLeft();
			cArr[0][i] = point.getRight();
		}
		
		double[][] ncArr = new double[2][nc.size()];
		for (int i = 0; i < nc.size(); i++) {
			Pair<Integer, Double> point = nc.get(i);
			ncArr[1][i] = point.getLeft();
			ncArr[0][i] = point.getRight();
		}
		
		data.addSeries("Compliant", cArr);
		data.addSeries("Non compliant", ncArr);
	}
}
