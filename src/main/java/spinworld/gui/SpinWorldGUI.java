package spinworld.gui;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.tc33.jheatchart.HeatChart;

import uk.ac.imperial.presage2.core.db.DatabaseModule;
import uk.ac.imperial.presage2.core.db.DatabaseService;
import uk.ac.imperial.presage2.core.db.StorageService;
import uk.ac.imperial.presage2.core.db.persistent.PersistentSimulation;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class SpinWorldGUI {

	final DatabaseService db;
	final StorageService sto;

	PersistentSimulation sim;
	int t = 5;
	int windowSize = 50;
	int t0 = -1;

	boolean exportMode = false;

	final static String imagePath = "/home/markzolotas7/Videos/";

	public static void main(String[] args) throws Exception {
		DatabaseModule module = DatabaseModule.load();
		
		if (module != null) {
			Injector injector = Guice.createInjector(module);
			SpinWorldGUI gui = injector.getInstance(SpinWorldGUI.class);
			
			if (args.length > 1 && Boolean.parseBoolean(args[1]) == true)
				gui.exportMode = true;
			
			gui.init(Integer.parseInt(args[0]));
		}
	}

	@Inject
	public SpinWorldGUI(DatabaseService db, StorageService sto) {
		super();
		this.db = db;
		this.sto = sto;
	}

	void takeScreenshot(JFreeChart chart, String base, int i) {
		if (t0 == -1)
			t0 = i;

		try {
			ChartUtilities.saveChartAsPNG(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"),
					chart, 1280, 720);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void takeScreenshot(HeatChart chart, String base, int i) {
		if (t0 == -1)
			t0 = i;

		try {
			chart.saveToFile(new File(imagePath + base + "" + String.format("%04d", i - t0) + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void init(long simId) {
		try {
			db.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		sim = sto.getSimulationById(simId);
		if (exportMode) {
			File exportDir = new File(imagePath + sim.getName());
			
			if (!exportDir.exists())
				exportDir.mkdir();
			else if (!exportDir.isDirectory())
				System.exit(60);
		}
		
		List<TimeSeriesChart> charts = new ArrayList<TimeSeriesChart>();
		charts.add(new UtilityChart(sim, windowSize));
		charts.add(new SatisfactionChart(sim, windowSize));
		
		List<HeatMap> maps = new ArrayList<HeatMap>();
		maps.add(new UtilityCatchMap(sim, windowSize));
		maps.add(new UtilityRiskMap(sim, windowSize));

		final Frame f = new Frame("SPINWORLD");
		final Panel p = new Panel(new GridLayout(2, 2));
		if (!exportMode) {
			f.add(p);
			
			for (TimeSeriesChart chart : charts) {
				p.add(chart.getPanel());
			}

			f.pack();
			f.setVisible(true);
		}
				
		while (t < sim.getFinishTime() / 2) {
			t++;
			
			for (TimeSeriesChart chart : charts) {
				chart.redraw(t);
			}
			
			for (HeatMap map : maps) {
				map.redraw(t);
			}
			
			if (exportMode) {
				for (TimeSeriesChart chart : charts) {
					takeScreenshot(chart.getChart(),
							sim.getName() + "/" + chart.getClass().getSimpleName().substring(0, 3), t);
				}
				
				for (HeatMap map : maps) {
					takeScreenshot(map.getChart(),
							sim.getName() + "/" + map.getClass().getSimpleName().substring(0, 3) + map.getClass().getSimpleName().substring(7, 10),
							t);
				}
			} else {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		db.stop();
	}

}
