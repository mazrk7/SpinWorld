package spinworld.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.tc33.jheatchart.HeatChart;

import uk.ac.imperial.presage2.core.db.persistent.PersistentAgent;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;
import uk.ac.imperial.presage2.core.db.persistent.TransientAgentState;

public class UtilityRiskMap implements HeatMap {

	final PersistentSimulation sim;
	final int windowSize;

	final HeatChart map;
	Map<Integer, DescriptiveStatistics> agentUtilities = new HashMap<Integer, DescriptiveStatistics>();
	Map<Integer, DescriptiveStatistics> riskRates = new HashMap<Integer, DescriptiveStatistics>();
	final double[][] data;
	
	UtilityRiskMap(PersistentSimulation sim, int windowSize) {
		super();
		this.sim = sim;
		this.windowSize = windowSize;
		
		data = new double[sim.getAgents().size()][sim.getAgents().size()];

		map = new HeatChart(data);
		
		double xOffset = 0.0;
		double yOffset = 0.0;
		double xInterval = 1.0;
		double yInterval = (1.0/sim.getAgents().size()) * 100;
		 
		map.setXValues(xOffset, xInterval);
		map.setYValues(yOffset, yInterval);
		
		map.setTitle("Utility vs Risk HeatMap");
		map.setXAxisLabel("Particle ID");
		map.setYAxisLabel("Risk Level");
		map.setShowXAxisValues(true);
		map.setShowYAxisValues(true);
		
		map.setHighValueColour(Color.RED);
		map.setLowValueColour(Color.PINK);
		map.setBackgroundColour(Color.WHITE);
	}
	
	@Override
	public HeatChart getChart() {
		return map;
	}
	
	@Override
	public void redraw(int finish) {
		finish = Math.min(finish, sim.getFinishTime() / 2);
		int start = Math.max(finish - windowSize, 0);
		int length = Math.min(windowSize, finish - start);	
		int agentCounter = 0;
				
		if (agentUtilities.size() == 0 && riskRates.size() == 0) {
			for (PersistentAgent a : sim.getAgents()) {				
				DescriptiveStatistics ut = new DescriptiveStatistics(windowSize);
				agentUtilities.put(agentCounter, ut);
				
				DescriptiveStatistics riskR = new DescriptiveStatistics(windowSize);
				riskRates.put(agentCounter, riskR);
				
				for (int i = 0; i < length; i++) {
					int t = start + i + 1;
					TransientAgentState s = a.getState(t);

					if (s != null && s.getProperty("U") != null
							&& s.getProperty("risk") != null) {
						double u = Double.parseDouble(s.getProperty("U"));
						ut.addValue(u);
						
						double rr = Double.parseDouble(s.getProperty("risk"));
						riskR.addValue(rr);
					}
				}
				
				agentCounter++;
			}
		} else {
			for (PersistentAgent a : sim.getAgents()) {
				if (!agentUtilities.containsKey(agentCounter)) {
					agentUtilities.put(agentCounter, new DescriptiveStatistics(windowSize));
				}
				
				if (!riskRates.containsKey(agentCounter)) {
					riskRates.put(agentCounter, new DescriptiveStatistics(windowSize));
				}

				TransientAgentState s = a.getState(finish);
				if (s != null && s.getProperty("U") != null 
						&& s.getProperty("risk") != null) {
					double u = Double.parseDouble(s.getProperty("U"));
					agentUtilities.get(agentCounter).addValue(u);
					
					double rr = Double.parseDouble(s.getProperty("risk"));
					riskRates.get(agentCounter).addValue(rr);
				} else {
					agentUtilities.put(agentCounter, new DescriptiveStatistics(new double[] { 0 }));
					riskRates.put(agentCounter, new DescriptiveStatistics(new double[] { 0 }));
				}
				
				agentCounter++;
			}
		}
		
		List<Integer> agents = new ArrayList<Integer>(agentUtilities.keySet());
		Collections.sort(agents);
		for (Integer key : agents) {
			DescriptiveStatistics ut = agentUtilities.get(key);
			DescriptiveStatistics rr = riskRates.get(key);
			
			int rrIndex = (int) Math.floor(rr.getMean() * sim.getAgents().size());	
			if (rrIndex >= sim.getAgents().size())
				rrIndex = sim.getAgents().size() - 1;
			
			data[rrIndex][key] = ut.getMean();
		}
		
		map.setZValues(data);
	}

}
