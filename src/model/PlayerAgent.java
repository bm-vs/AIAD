package model;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import model.onto.ServiceOntology;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import utils.StockPrice;

import java.io.Serializable;
import java.util.*;

public class PlayerAgent extends Agent implements  Serializable{
    private Codec codec;
    private Ontology serviceOntology;
    private String id;
    private HashMap<String, InvestorTrust> investorTrust; // save trust associated with every investor id
    private float investedAmount; // amount sent to investor
    private InvestorAgent investor; // investor the player agent has its money on
    private float capital;
    private float portfolioValue;

    public PlayerAgent(String id, float initialCapital) {
      this.id = id;
      this.capital = initialCapital;
    }

    public String getId() {
        return id;
    }

    public float getCapital() {
        return capital;
    }

    public float getPortfolioValue() {
        return portfolioValue;
    }

    public float getTotalCapital() {
        return capital + portfolioValue;
    }

    public void setInvestorAgent(InvestorAgent investor){
        this.investor = investor;
    }

    public InvestorAgent getInvestorAgent(){
        return  investor;
    }

    @Override
    public void setup(){
        // register language and ontology
        codec = new SLCodec();
        serviceOntology = ServiceOntology.getInstance();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(serviceOntology);

        // register provider at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addProtocols(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName() + "-service-provider");
        sd.setType("player");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    //Behaviours
    private class PlayerTrade extends SimpleBehaviour {
        private boolean finished = false;

        public void action(){
            // TODO
            // Receive reports
            // For every report received update trust in each investor

            // Withdraw - send playerId (+capital) (-investedAmount)
            // Invest - send playerId and amount (-capital) (+investedAmount)



        }

        public boolean done(){
            return finished;
        }
    }
}
