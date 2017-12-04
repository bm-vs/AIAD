package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import data.Market;
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
import utils.MarketSettings;

import static utils.InvestorSettings.INVESTOR_MAX_SKILL;
import static utils.InvestorSettings.N_PROFILES;

public class Repast3InvestingLauncher extends Repast3Launcher {
	private ContainerController mainContainer;
	private ContainerController agentContainer;
	private OpenSequenceGraph plot;

	// Data
	private Market market;
	private InformerAgent informer;
	private List<InvestorAgent> investors;

	// Simulation paramaters
	private int nInvestors = 1;
	private float initialCapital = 10000;
	private int ticksPerHour = 10;
	private boolean detailedInfo = true;

	public Repast3InvestingLauncher() {
		super();
		market = new Market(MarketSettings.OPEN_TIME, MarketSettings.CLOSE_TIME);
	}

	public int getNInvestors() {
		return nInvestors;
	}

	public float getInitialCapital() {
		return initialCapital;
	}

	public int getTicksPerHour() {
	    return ticksPerHour;
	}

	public boolean getDetailedInfo() {
	    return detailedInfo;
	}

	public void setNInvestors(int n) {
		nInvestors = n;
	}

	public void setInitialCapital(float n) {
		initialCapital = n;
	}

	public void setTicksPerHour(int n) {
        ticksPerHour = n;
    }

	public void setDetailedInfo(boolean b) {
	    detailedInfo = b;
	}

	@Override
	public String[] getInitParam() {
		return new String[] {"nInvestors", "initialCapital", "ticksPerHour", "detailedInfo"};
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
		investors = new ArrayList<>();
		int nSectors = market.getNSectors();
		
		try {
			// Create investors
            Random r = new Random();
			for (int i = 0; i < nInvestors; i++) {
				String id = "Investor" + i;
				ArrayList<Integer> skill = new ArrayList<>();
				for (int j = 0; j < nSectors; j++) {
				    skill.add(i*5);
					//skill.add(r.nextInt(INVESTOR_MAX_SKILL));
                }

				InvestorAgent agent = new InvestorAgent(id, initialCapital, skill, r.nextInt(N_PROFILES));
				agentContainer.acceptNewAgent(id, agent).start();
				investors.add(agent);
			}

			// Create informer agent
			informer = new InformerAgent(market, ticksPerHour);
			agentContainer.acceptNewAgent("Informer", informer).start();
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
		plot.setAxisTitles("time", "capital");

		for (int i = 0; i < investors.size(); i++) {
			InvestorAgent agent = investors.get(i);
			plot.addSequence("Total " + i, new Sequence() {
				public double getSValue() {
					return agent.getTotalCapital();
				}
			});
			if (detailedInfo) {
				plot.addSequence("Cash " + i, new Sequence() {
					public double getSValue() {
						return agent.getCapital();
					}
				});
				plot.addSequence("Portfolio " + i, new Sequence() {
					public double getSValue() {
						return agent.getPortfolioValue();
					}
				});
			}
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
