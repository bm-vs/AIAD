package model;

import jade.core.AID;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.onto.StockMarketOntology;

import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;

import java.io.Serializable;
import java.util.*;

public class PlayerAgent extends Agent implements  Serializable{
    private Codec codec;
    private Ontology stockMarketOntology;
    private String id;
    private float capital;
    private float portfolioValue;
    private HashMap<AID, InvestorTrust> investorTrust; // save trust associated with every investor id
    private InvestorAgent investor; // investor the player agent has its money on


    public PlayerAgent(String id, float initialCapital) {
      this.id = id;
      this.capital = initialCapital;
      this.investorTrust = new HashMap<>();
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
        System.out.println("Player created");

        // register language and ontology
        codec = new SLCodec();
        stockMarketOntology = StockMarketOntology.getInstance();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(stockMarketOntology);

        // register provider at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addProtocols(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName() + "-player");
        sd.setType("player");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        addBehaviour(new SearchInvestorAgents(this));
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class SearchInvestorAgents extends SimpleBehaviour {
        private boolean foundInvestors = false;
        private PlayerAgent agent;

        public SearchInvestorAgents(PlayerAgent agent) {
            super(agent);
            this.agent = agent;
        }

        public void action() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd2 = new ServiceDescription();
            sd2.setType("investor");
            template.addServices(sd2);

            try {
                DFAgentDescription[] result = DFService.search(this.agent, template);

                for (int i = 0; i < result.length; i++) {
                    this.agent.investorTrust.put(result[i].getName(), new InvestorTrust());

                    // TODO - IMPLEMENTAR
                    //investorAgents[i] = result[i].getName();
                    //investorAgentsRatio[i] = 0;
                    //System.out.println(investorAgents[i].getName() + "  -  " + result.length);
                }

                foundInvestors = true;

                System.out.println("Found "+this.agent.investorTrust.size()+"investor agents.");
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return foundInvestors;
        }
    }

    //Behaviours
    private class ReportsReveiver extends CyclicBehaviour {

        public void action(){
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchSender(new AID("Informer", AID.ISLOCALNAME)));

            ACLMessage reports = myAgent.receive(mt);

            if(reports != null){
                updateTrust(reports);

            }
        }

        // TODO - falta implementar
        public void updateTrust(ACLMessage reports){

        }
    }



    /*private class PlayerTrade extends SimpleBehaviour {
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
    }*/
}
