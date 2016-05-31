package spinworld.gui;

import java.awt.Color;
import java.util.Arrays;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.DefaultXYDataset;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class TimeSeriesChart implements Chart {

	final PersistentSimulation sim;
	final int windowSize;

	final DefaultXYDataset data;
	final JFreeChart chart;
	final ChartPanel panel;
	final String property;
	final String shortName;

	TimeSeriesChart(PersistentSimulation sim, int windowSize, String title, 
			String yAxis, String property, String shortName, double lb, double ub) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		this.property = property;
		this.shortName = shortName;
		
		data = new DefaultXYDataset();
		chart = ChartFactory.createXYLineChart(title, yAxis,
				"Timestep", data, PlotOrientation.HORIZONTAL, true, false, false);
		panel = new ChartPanel(chart);

		chart.getXYPlot().setBackgroundPaint(Color.WHITE);
		chart.getXYPlot().getDomainAxis().setRange(lb, ub);
		chart.getXYPlot().setWeight(2);
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
		
		double[][] c = new double[2][length];
		double[][] nc = new double[2][length];
		double[][] mean = new double[2][length];
		Arrays.fill(c[0], 0);
		Arrays.fill(nc[0], 0);
		Arrays.fill(mean[0], 0);

		for (int i = 0; i < length; i++) {
			int t = start + i + 1;
			c[1][i] = t;
			nc[1][i] = t;
			mean[1][i] = t;
			SummaryStatistics statC = new SummaryStatistics();
			SummaryStatistics statNC = new SummaryStatistics();
			
			for (PersistentAgent a : sim.getAgents()) {
				boolean compliant = a.getName().startsWith("c");
				TransientAgentState s = a.getState(t);
				
				if (s != null && s.getProperty(property) != null) {
					double prop = Double.parseDouble(s.getProperty(property));
					
					if (compliant)
						statC.addValue(prop);
					else
						statNC.addValue(prop);
				}
			}
			
			c[0][i] = statC.getMean();
			nc[0][i] = statNC.getMean();
			mean[0][i] = (c[0][i] + nc[0][i])/2;
		}
		
		data.addSeries("Compliant", c);
		data.addSeries("Non compliant", nc);
		data.addSeries("Mean", mean);

		chart.getXYPlot().getRangeAxis()
				.setRange(Math.max(1.0, finish - windowSize + 1), finish);
	}

}
