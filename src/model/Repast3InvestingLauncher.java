package model;

import java.util.ArrayList;
import java.util.List;

import jade.core.Profile;
import jade.core.ProfileImpl;
import sajas.core.Runtime;
import sajas.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import sajas.sim.repast3.Repast3Launcher;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;

public class Repast3InvestingLauncher extends Repast3Launcher {
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	private List<InvestorAgent> investors;
	private OpenSequenceGraph plot;

	private int nInvestors = 5;
	private int initialCapital = 10000;

	public Repast3InvestingLauncher() {
		super();
	}

	public int getnInvestors() {
		return nInvestors;
	}

	public void setnInvestors(int n) {
		this.nInvestors = n;
	}

	public int getInitialCapital() {
		return initialCapital;
	}

	public void setInitialCapital(int n) {
		this.initialCapital = n;
	}

	@Override
	public String[] getInitParam() {
		return new String[] {"nInvestors", "initialCapital"};
	}

	@Override
	public String getName() {
		return "Investing -- SAJaS Repast3 Test";
	}

	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);
		agentContainer = mainContainer;
		
		launchAgents();
	}
	
	private void launchAgents() {
		investors = new ArrayList<InvestorAgent>();
		
		try {
			// create investors
			for (int i = 0; i < nInvestors; i++) {
				InvestorAgent agent = new InvestorAgent(initialCapital);
				agentContainer.acceptNewAgent("Investor" + i, agent).start();
				investors.add(agent);
			}
		}
		catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void begin() {
		super.begin();
		buildAndScheduleDisplay();
	}

	private void buildAndScheduleDisplay() {
		// graph
		if (plot != null) {
			plot.dispose();
		}
		plot = new OpenSequenceGraph("Investment success", this);
		plot.setAxisTitles("time", "total capital");

		for (int i = 0; i < investors.size(); i++) {
			InvestorAgent agent = investors.get(i);
			plot.addSequence("Investor " + i, new Sequence() {
				public double getSValue() {
					return agent.getCapital();
				}
			});
			plot.display();
		}

		getSchedule().scheduleActionAtInterval(100, plot, "step", Schedule.LAST);
	}


	/**
	 * Launching Repast3
	 * @param args
	 */
	public static void main(String[] args) {
		SimInit init = new SimInit();
		init.setNumRuns(1);
		init.loadModel(new Repast3InvestingLauncher(), null, false);
	}

}
