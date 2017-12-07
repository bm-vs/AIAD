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

import static utils.MarketSettings.*;
import static utils.InvestorSettings.*;

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
        sd.setType("investor");
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
            // Subscribe to informer agent to receive prices
            if (!subscribed) {
                try {
                    ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
                    subscribe.setContentObject(new InvestorInfo(agent.getId(), agent.getProfile(), agent.getSkill()));
                    subscribe.addReceiver(new AID("Informer", AID.ISLOCALNAME));
                    send(subscribe);

                    ACLMessage reply = receive();
                    if (reply != null && reply.getPerformative() == ACLMessage.AGREE) {
                        subscribed = true;
                        System.out.println(id + " subscribed ok");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // When new prices are received buy/sell/update portfolio value
            ACLMessage stockPrices = receive();
            if (stockPrices != null && stockPrices.getPerformative() == ACLMessage.INFORM) {
                try {
                    HashMap<String, StockPrice> prices = (HashMap<String, StockPrice>) stockPrices.getContentObject();
                    if (prices != null) {
                        moveStock(prices);
                        updatePortfolioValue(prices);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean done() {
            return finished;
        }

        // Buys/sells stock according to current prices and predicted prices
        private void moveStock(HashMap<String, StockPrice> prices) {
            sellAll(prices);

            // Get top growth stock
            ArrayList<StockPrice> predictedGrowth = new ArrayList<>();
            for (StockPrice price: prices.values()) {
                float estimated = price.getHourPrice()*PROFILE[profile][HOUR_PRICE]+
                                    price.getDayPrice()*PROFILE[profile][DAY_PRICE]+
                                    price.getWeekPrice()*PROFILE[profile][WEEK_PRICE]+
                                    price.getMonthPrice()*PROFILE[profile][MONTH_PRICE];
                price.setEstimatedPrice(estimated);
                predictedGrowth.add(price);
            }
            Collections.sort(predictedGrowth, new StockPrice.StockPriceComparator());
            Collections.reverse(predictedGrowth);

            // Buy stock
            int i = 0;
            float amountPerStock = capital/PORTFOLIO_SIZE;
            for (StockPrice price: predictedGrowth) {
                // Invests an equal amount on the stocks with highest predicted growth
                if (i < PORTFOLIO_SIZE) {
                    int quantity = (int)(amountPerStock/price.getCurrPrice());
                    if (quantity > 0) {
                        Transaction t = new Transaction(price.getSymbol(), price.getCurrPrice(), quantity);
                        active.add(t);
                        capital -= price.getCurrPrice() * quantity + TRANSACTION_TAX;
                    }
                    i++;
                }
            }
        }

        // Liquidates all open positions
        private void sellAll(HashMap<String, StockPrice> prices) {
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
    }

    public class InvestorInfo implements Serializable {

        private String id;
        private int profile;
        private ArrayList<Integer> skill;

        public InvestorInfo(String id, int profile, ArrayList<Integer> skill) {
            this.id = id;
            this.skill = skill;
            this.profile = profile;
        }

        public String getId() {
            return id;
        }

        public int getProfile() {
            return profile;
        }

        public ArrayList<Integer> getSkill() {
            return skill;
        }

        @Override
        public boolean equals(Object obj) {
            return ((InvestorInfo) obj).getId().equals(this.id);
        }

        @Override
        public String toString() {
            String s = id + " - "
                        + profile + " - "
                        + skill.get(TELECOM) + " "
                        + skill.get(FINANCIAL) + " "
                        + skill.get(INDUSTRIAL) + " "
                        + skill.get(ENERGY) + " "
                        + skill.get(HEALTHCARE) + " "
                        + skill.get(TECH) + " ";
            return s;
        }
    }
}
