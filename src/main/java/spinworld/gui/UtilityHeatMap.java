package spinworld.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import spinworld.gui.heatmap.Gradient;
import spinworld.gui.heatmap.HeatMap;
import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class UtilityHeatMap implements HeatMapChart {

	final PersistentSimulation sim;
	final int windowSize;

	final HeatMap panel;
	Map<String, DescriptiveStatistics> agentUtilities = new HashMap<String, DescriptiveStatistics>();
	Map<String, DescriptiveStatistics> riskRates = new HashMap<String, DescriptiveStatistics>();
	Map<String, DescriptiveStatistics> catchRates = new HashMap<String, DescriptiveStatistics>();
	final double[][] data;
	final boolean useGraphicsYAxis = true;
	
	UtilityHeatMap(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		
		data = new double[101][101];

        panel = new HeatMap(data, useGraphicsYAxis, Gradient.GRADIENT_BLUE_TO_RED);
		
        panel.setDrawLegend(true);

        panel.setTitle("Utility HeatMap");
        panel.setDrawTitle(true);

        panel.setXAxisTitle("Risk Rate (%)");
        panel.setDrawXAxisTitle(true);

        panel.setYAxisTitle("Catch Rate (%)");
        panel.setDrawYAxisTitle(true);

        panel.setCoordinateBounds(0, 100, 0, 100);

        panel.setDrawXTicks(true);
        panel.setDrawYTicks(true);      
	}
	
	@Override
	public HeatMap getPanel() {
		return panel;
	}
	
	@Override
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);	
		
		if (agentUtilities.size() == 0) {
			for (PersistentAgent a : sim.getAgents()) {				
				DescriptiveStatistics ut = new DescriptiveStatistics(windowSize);
				DescriptiveStatistics cr = new DescriptiveStatistics(windowSize);
				DescriptiveStatistics rr = new DescriptiveStatistics(windowSize);
				agentUtilities.put(a.getName(), ut);
				catchRates.put(a.getName(), cr);
				riskRates.put(a.getName(), rr);
		
				for (int i = 0; i < length; i++) {
					int t = start + i + 1;
					TransientAgentState s = a.getState(t);

					if (s != null && s.getProperty("U") != null
							&& s.getProperty("catchRate") != null
							&& s.getProperty("risk") != null) {
						double u = Double.parseDouble(s.getProperty("U"));
						ut.addValue(u);
						
						double catchR = Double.parseDouble(s.getProperty("catchRate"));
						cr.addValue(catchR);
						
						double riskR = Double.parseDouble(s.getProperty("risk"));
						rr.addValue(riskR);
					}
				}
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				if (!agentUtilities.containsKey(a.getName())) {
					agentUtilities.put(a.getName(), new DescriptiveStatistics(windowSize));
				}
				
				if (!catchRates.containsKey(a.getName())) {
					catchRates.put(a.getName(), new DescriptiveStatistics(windowSize));
				}
				
				if (!riskRates.containsKey(a.getName())) {
					riskRates.put(a.getName(), new DescriptiveStatistics(windowSize));
				}

				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty("U") != null 
						&& s.getProperty("catchRate") != null
						&& s.getProperty("risk") != null) {
					double u = Double.parseDouble(s.getProperty("U"));
					agentUtilities.get(a.getName()).addValue(u);
					
					double cr = Double.parseDouble(s.getProperty("catchRate"));
					catchRates.get(a.getName()).addValue(cr);
					
					double rr = Double.parseDouble(s.getProperty("risk"));
					riskRates.get(a.getName()).addValue(rr);
				} else {
					agentUtilities.put(a.getName(), new DescriptiveStatistics(new double[] { 0 }));
					catchRates.put(a.getName(), new DescriptiveStatistics(new double[] { 0 }));
					riskRates.put(a.getName(), new DescriptiveStatistics(new double[] { 0 }));
				}	
			}
		}
		
		List<String> agents = new ArrayList<String>(agentUtilities.keySet());
		Collections.sort(agents);
		for (String key : agents) {
			DescriptiveStatistics ut = agentUtilities.get(key);
			DescriptiveStatistics cr = catchRates.get(key);
			DescriptiveStatistics rr = riskRates.get(key);
			
			int crIndex = (int) Math.floor(cr.getMean() * 100);		
			int rrIndex = (int) Math.floor(rr.getMean() * 100);	

			data[rrIndex][crIndex] = ut.getSum();
		}
		
		panel.updateData(data, useGraphicsYAxis);
	}

}
