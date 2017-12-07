package model;

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
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import utils.StockPrice;
import static utils.Settings.*;

import java.io.Serializable;
import java.util.*;

public class InvestorAgent extends Agent implements Serializable {
    private Codec codec;
    private Ontology stockMarketOntology;
    private ArrayList<Transaction> active;
    private ArrayList<Transaction> closed;
    private String id;
    private float capital;
    private float portfolioValue;
    private ArrayList<Integer> skill; // represents the knowledge (0-10) of each sector (0-5)
    private int profile;

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill) {
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
        stockMarketOntology = StockMarketOntology.getInstance();
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(stockMarketOntology);

        // register provider at DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addProtocols(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
        ServiceDescription sd = new ServiceDescription();
        sd.setName(getLocalName() + "-investor");
        sd.setType("investor");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        // Behaviours
        addBehaviour(new InvestorSubscribe(this));
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
    private class InvestorSubscribe extends SimpleBehaviour {
        private boolean subscribed = false;
        private InvestorAgent agent;

        public InvestorSubscribe(InvestorAgent a) {
            super(a);
            agent = a;
        }

        public void action() {
            // Subscribe to informer agent to receive prices
            try {
                ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
                subscribe.setContentObject(new InvestorInfo(agent.getId(), agent.getSkill()));
                subscribe.addReceiver(new AID("Informer", AID.ISLOCALNAME));
                send(subscribe);

                // Only accept subscribe messages
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchSender(new AID("Informer", AID.ISLOCALNAME)));
                ACLMessage reply = receive(mt);
                if (reply != null) {
                    subscribed = true;
                    System.out.println(id + " subscribed ok");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return subscribed;
        }
    }

    private class InvestorTrade extends CyclicBehaviour {
        public InvestorTrade(InvestorAgent a) {
            super(a);
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(new AID("Informer", AID.ISLOCALNAME)));

            // When new prices are received buy/sell/update portfolio value
            ACLMessage stockPrices = receive(mt);
            if (stockPrices != null) {
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

        // Buys/sells stock according to current prices and predicted prices
        private void moveStock(HashMap<String, StockPrice> prices) {
            sellAll(prices);

            // Get top growth stock
            ArrayList<StockPrice> predictedGrowth = new ArrayList<>();
            for (StockPrice price: prices.values()) {
                float estimated = price.getHourPrice();
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
        public boolean equals(Object obj) {
            return ((InvestorInfo) obj).getId().equals(this.id);
        }

        @Override
        public String toString() {
            String s = id + " - "
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
