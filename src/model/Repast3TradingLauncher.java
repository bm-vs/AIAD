package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jade.core.AID;
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
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Network2DDisplay;
import uchicago.src.sim.gui.OvalNetworkItem;
import uchicago.src.sim.network.DefaultDrawableNode;

public class Repast3TradingLauncher extends Repast3Launcher {
    private static final int N_AGENTS = 1;


    private static final boolean SEPARATE_CONTAINERS = false;
    private ContainerController mainContainer;
    private ContainerController agentContainer;

    @Override
    public String getName() {
        return "Trade Hero -- SAJaS Repast3 Test";
    }

    @Override
    public String[] getInitParam() {
        return new String[0];
    }

    @Override
    protected void launchJADE() {
        Runtime rt = Runtime.instance();
        Profile p1 = new ProfileImpl();
        mainContainer = rt.createMainContainer(p1);

        if(SEPARATE_CONTAINERS) {
            Profile p2 = new ProfileImpl();
            agentContainer = rt.createAgentContainer(p2);
        } else {
            agentContainer = mainContainer;
        }

        launchAgents();
    }

    private void launchAgents() {
        try {
            for (int i = 0; i < N_AGENTS; i++) {
                InvestorAgent inv = new InvestorAgent();
                agentContainer.acceptNewAgent("Investor" + i, inv).start();
            }
        }
        catch (StaleProxyException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        SimInit init = new SimInit();
        init.setNumRuns(1);   // works only in batch mode
        init.loadModel(new Repast3TradingLauncher(), null, true);
    }
}
