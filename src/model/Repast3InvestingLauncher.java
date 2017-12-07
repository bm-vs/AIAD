package model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

import static utils.Settings.*;

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
	private boolean detailedInfo = false;
	private String customInvestors = "test1";

	public Repast3InvestingLauncher() {
		super();
		market = new Market(OPEN_TIME, CLOSE_TIME);
	}

	public int getNInvestors() {
		return nInvestors;
	}

	public float getInitialCapital() {
		return initialCapital;
	}

	public boolean getDetailedInfo() {
	    return detailedInfo;
	}

	public String getCustomInvestors() { return customInvestors; }

	public void setNInvestors(int n) {
		nInvestors = n;
	}

	public void setInitialCapital(float n) {
		initialCapital = n;
	}

	public void setDetailedInfo(boolean b) {
	    detailedInfo = b;
	}

	public void setCustomInvestors(String s) { customInvestors = s; }

	@Override
	public String[] getInitParam() {
		return new String[] {"nInvestors", "initialCapital", "detailedInfo", "customInvestors"};
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
            boolean custom = readCustomFile();
            if (custom) {
                System.out.println("Custom Investors");
                for (InvestorAgent agent: investors) {
                    agentContainer.acceptNewAgent(agent.getId(), agent).start();
                }
            }
			if (!custom) {
                System.out.println("Random Investors");
				Random r = new Random();
				for (int i = 0; i < nInvestors; i++) {
					String id = "Investor" + i;
					ArrayList<Integer> skill = new ArrayList<>();
					for (int j = 0; j < nSectors; j++) {
						skill.add(r.nextInt(INVESTOR_MAX_SKILL));
					}

					InvestorAgent agent = new InvestorAgent(id, initialCapital, skill);
					agentContainer.acceptNewAgent(id, agent).start();
					investors.add(agent);
				}
			}

			// Create informer agent
			informer = new InformerAgent(market);
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
			plot.addSequence(agent.getId() + "(T)", new Sequence() {
				public double getSValue() {
					return agent.getTotalCapital();
				}
			});
			if (detailedInfo) {
				plot.addSequence(agent.getId() + "(C)", new Sequence() {
					public double getSValue() {
						return agent.getCapital();
					}
				});
				plot.addSequence(agent.getId() + "(P)", new Sequence() {
					public double getSValue() {
						return agent.getPortfolioValue();
					}
				});
			}
			plot.display();
		}

		getSchedule().scheduleActionAtInterval(100, plot, "step", Schedule.LAST);
	}

	// Reads investor type file using customInvestors
	private boolean readCustomFile() {
        try {
            ArrayList<InvestorAgent> custom = new ArrayList<>();

            File file = new File("test/" + customInvestors + ".txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] info = line.split("-");
                String id = info[0];
                float capital = Float.parseFloat(info[1]);
                String[] skillString = info[2].split(";");

				int nSectors = market.getNSectors();
				ArrayList<Integer> skill = new ArrayList<>();
                for (String s: skillString) {
                	skill.add(Integer.parseInt(s));
				}

                custom.add(new InvestorAgent(id, capital, skill));
            }
            fileReader.close();
            investors = custom;
        }
        catch (Exception e) {
            return false;
        }

	    return true;
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
