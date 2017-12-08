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
	private OpenSequenceGraph investorPlot;
	private OpenSequenceGraph playerPlot;

	// Data
	private Market market;
	private InformerAgent informer;
	private ArrayList<InvestorAgent> investors;
	private ArrayList<PlayerAgent> players;

	// Simulation paramaters
	private int nInvestors = 1;
	private float initialInvestorCapital = 10000;
	private int nPlayers = 1;
	private float initialPlayerCapital = 1000;
    private int dynamicInvestors = 0;
	private boolean detailedInfo = false;
	private String customInvestors = "";

	public Repast3InvestingLauncher() {
		super();
		market = new Market(OPEN_TIME, CLOSE_TIME);
	}

	public int getNInvestors() {
		return nInvestors;
	}

    public int getnPlayers() {
        return nPlayers;
    }

    public float getInitialInvestorCapital() {
		return initialInvestorCapital;
	}

	public float getInitialPlayerCapital() {
		return initialPlayerCapital;
	}

	public int getDynamicInvestors() { return dynamicInvestors; }

	public boolean getDetailedInfo() {
	    return detailedInfo;
	}

	public String getCustomInvestors() { return customInvestors; }

	public void setNInvestors(int n) {
		nInvestors = n;
	}

    public void setnPlayers(int nPlayers) {
        this.nPlayers = nPlayers;
    }

    public void setInitialInvestorCapital(float n) {
		initialInvestorCapital = n;
	}

	public void setInitialPlayerCapital(float initialPlayerCapital) {
		this.initialPlayerCapital = initialPlayerCapital;
	}

    public void setDynamicInvestors(int dynamicInvestors) {
        this.dynamicInvestors = dynamicInvestors;
    }

    public void setDetailedInfo(boolean b) {
	    detailedInfo = b;
	}

	public void setCustomInvestors(String s) { customInvestors = s; }

	@Override
	public String[] getInitParam() {
		return new String[] {"nInvestors", "initialInvestorCapital", "nPlayers", "initialPlayerCapital", "dynamicInvestors", "detailedInfo", "customInvestors"};
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
		players = new ArrayList<>();
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

					InvestorAgent agent = new InvestorAgent(id, initialInvestorCapital, skill, dynamicInvestors);
					agentContainer.acceptNewAgent(id, agent).start();
					investors.add(agent);
				}
			}

			// Create players
            System.out.println("Random Players");
			for (int i = 0; i < nPlayers; i++) {
				String id = "Player" + i;
				PlayerAgent agent = new PlayerAgent(id, initialPlayerCapital);
				agentContainer.acceptNewAgent(id, agent).start();
				players.add(agent);
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
		// Investor Graph
		if (investorPlot != null) {
            investorPlot.dispose();
		}
        investorPlot = new OpenSequenceGraph("Investor profit", this);
        investorPlot.setAxisTitles("time", "capital");

		for (int i = 0; i < investors.size(); i++) {
			InvestorAgent agent = investors.get(i);
            investorPlot.addSequence(agent.getId() + "(T)", new Sequence() {
				public double getSValue() {
					return agent.getTotalCapital();
				}
			});
			if (detailedInfo) {
                investorPlot.addSequence(agent.getId() + "(C)", new Sequence() {
					public double getSValue() {
						return agent.getCapital();
					}
				});
                investorPlot.addSequence(agent.getId() + "(P)", new Sequence() {
					public double getSValue() {
						return agent.getPortfolioValue();
					}
				});
			}
            investorPlot.display();
		}

		getSchedule().scheduleActionAtInterval(100, investorPlot, "step", Schedule.LAST);


		// Player Graph
        if (playerPlot != null) {
            playerPlot.dispose();
        }
        playerPlot = new OpenSequenceGraph("Player profit", this);
        playerPlot.setAxisTitles("time", "capital");

        for (int i = 0; i < players.size(); i++) {
            PlayerAgent agent = players.get(i);
            playerPlot.addSequence(agent.getId() + "(T)", new Sequence() {
                public double getSValue() {
                    return agent.getTotalCapital();
                }
            });
            playerPlot.display();
        }

        getSchedule().scheduleActionAtInterval(100, playerPlot, "step", Schedule.LAST);


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

				int dynamic = Integer.parseInt(info[3]);

                custom.add(new InvestorAgent(id, capital, skill, dynamic));
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
