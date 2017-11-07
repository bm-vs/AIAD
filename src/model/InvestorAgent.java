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
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class InvestorAgent extends Agent {
    private Codec codec;
    private Ontology serviceOntology;
    private ArrayList<Transaction> active;
    private ArrayList<Transaction> closed;
    private float capital;
    private float portfolioValue;
    private double confidence;
    private boolean learn;

    public InvestorAgent(float initialCapital, double confidence, boolean learn) {
        this.capital = initialCapital;
        this.confidence = confidence;
        this.learn = learn;
        active = new ArrayList<>();
        closed = new ArrayList<>();
    }

    public float getCapital() {
        return capital;
    }
    public double getConfidence() { return confidence; }
    public boolean getLearn() { return learn; }
    public float getPortfolioValue() { return portfolioValue; }
    public float getTotalCapital() { return capital + portfolioValue; }

    @Override
    public void setup() {
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
        sd.setType("service-provider");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        // behaviours
        addBehaviour(new InvestorTrade(this));
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class InvestorTrade extends SimpleBehaviour {
        private boolean finished = false;

        public InvestorTrade(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage stockPrices = receive();
            if (stockPrices != null) {
                try {
                    HashMap<String, Float> prices = (HashMap<String, Float>) stockPrices.getContentObject();
                    simpleSell(prices);
                    simpleBuy(prices);
                    updatePorfolioValue(prices);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            block();
        }

        public boolean done() {
            return finished;
        }

        private void simpleSell(HashMap<String, Float> prices) {
            for (Iterator<Transaction> it = active.iterator(); it.hasNext(); ) {
                Transaction t = it.next();
                float currentPrice = prices.get(t.getStock());
                if (currentPrice / t.getBuyPrice() > 1.10 || currentPrice / t.getBuyPrice() < 0.99) {
                    t.setSellPrice(currentPrice);
                    t.closeTransaction();
                    closed.add(t);
                    it.remove();
                    capital += t.getQuantity()*currentPrice;
                }
            }
        }

        private void simpleBuy(HashMap<String, Float> prices) {
            int nStocks = prices.keySet().size();
            float amountPerStock = capital*0.8f/nStocks;
            for (String s: prices.keySet()) {
                float price = prices.get(s);
                int quantity = Math.round(amountPerStock/price);
                Transaction t = new Transaction(s, price, quantity);
                active.add(t);
                capital -= price*quantity;
            }
        }

        private void updatePorfolioValue(HashMap<String, Float> prices) {
            portfolioValue = 0;
            for (Transaction t: active) {
                portfolioValue += prices.get(t.getStock())*t.getQuantity();
            }
        }
    }
}
