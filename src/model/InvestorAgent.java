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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static utils.MarketSettings.*;

public class InvestorAgent extends Agent implements Serializable {
    private Codec codec;
    private Ontology serviceOntology;
    private ArrayList<Transaction> active;
    private ArrayList<Transaction> closed;
    private String id;
    private float capital;
    private float portfolioValue;
    private ArrayList<Integer> skill; // represents the knowledge (0-10) of each sector (0-5)
    private int profile;

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill, int profile) {
        this.id = id;
        this.capital = initialCapital;
        this.skill = skill;
        this.profile = profile;
        this.active = new ArrayList<>();
        this.closed = new ArrayList<>();
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

    public ArrayList<Integer> getSkill() {
        return skill;
    }

    public int getProfile() {
        return profile;
    }

    // Updates the sum of values of every stock
    private void updatePortfolioValue(HashMap<String, StockPrice> prices) {
        portfolioValue = 0;
        for (Transaction t: active) {
            portfolioValue += prices.get(t.getStock()).getCurrPrice()*t.getQuantity();
        }
    }


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

        // Behaviours
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


    // Behaviours
    private class InvestorTrade extends SimpleBehaviour {
        private boolean finished = false;
        private boolean subscribed = false;
        private InvestorAgent agent;

        public InvestorTrade(InvestorAgent a) {
            super(a);
            agent = a;
        }

        public void action() {
            if (!subscribed) {
                try {
                    ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
                    subscribe.setContentObject(new InvestorInfo(agent.getId(), agent.getSkill()));
                    subscribe.addReceiver(new AID("Informer", AID.ISLOCALNAME));
                    send(subscribe);

                    ACLMessage reply = receive();
                    if (reply != null && reply.getPerformative() == ACLMessage.AGREE) {
                        subscribed = true;
                        //System.out.println(id + " subscribed ok");
                    }
                    block();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            ACLMessage stockPrices = receive();
            if (stockPrices != null) {
                try {
                    HashMap<String, StockPrice> prices = (HashMap<String, StockPrice>) stockPrices.getContentObject();
                    simpleSell(prices);
                    simpleBuy(prices);
                    updatePortfolioValue(prices);
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

        private void simpleSell(HashMap<String, StockPrice> prices) {
            for (Iterator<Transaction> it = active.iterator(); it.hasNext(); ) {
                Transaction t = it.next();
                float currentPrice = prices.get(t.getStock()).getCurrPrice();

                t.setSellPrice(currentPrice);
                t.closeTransaction();
                closed.add(t);
                it.remove();
                capital += t.getQuantity()*currentPrice;
            }
        }

        private void simpleBuy(HashMap<String, StockPrice> prices) {
            float total = 0;
            HashMap<String, Float> growth = new HashMap<>();
            for (String s : prices.keySet()) {
                StockPrice p = prices.get(s);
                float g = (p.getDayPrice()-p.getCurrPrice())/p.getCurrPrice();
                growth.put(s, g);
                if (g > 0) {
                    total += g;
                }
            }

            for (String s: prices.keySet()) {
                float g = growth.get(s);
                if (g > 0) {
                    float amountPerStock = capital*g/total;
                    float price = prices.get(s).getCurrPrice();
                    int quantity = Math.round(amountPerStock / price);
                    if (quantity > 0) {
                        Transaction t = new Transaction(s, price, quantity);
                        active.add(t);
                        capital -= price * quantity;
                    }
                }
            }
        }
    }

    public class InvestorInfo implements Serializable {

        private String id;
        private ArrayList<Integer> skill;

        public InvestorInfo(String id, ArrayList<Integer> skill) {
            this.id = id;
            this.skill = skill;
        }

        public String getId() {
            return id;
        }

        public ArrayList<Integer> getSkill() {
            return skill;
        }

        @Override
        public String toString() {
            String s = id + "\n";

            s += "telecom: " + skill.get(TELECOM) + "\n";
            s += "financial: " + skill.get(FINANCIAL) + "\n";
            s += "industrial: " + skill.get(INDUSTRIAL) + "\n";
            s += "energy: " + skill.get(ENERGY) + "\n";
            s += "healthcare: " + skill.get(HEALTHCARE) + "\n";
            s += "tech: " + skill.get(TECH) + "\n";

            return s;
        }
    }
}
