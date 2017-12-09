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
import model.onto.InvestorInfo;
import model.onto.MarketPrices;
import model.onto.StockMarketOntology;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import model.onto.StockPrice;
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
    private int skillChangePeriod;
    private ArrayList<ArrayList<Integer>> nextSkills;
    private boolean repeat;
    private AID informer;
    private ArrayList<AID> followers;

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill, int skillChangePeriod) {
        this.id = id;
        this.capital = initialCapital;
        this.skill = skill;
        this.skillChangePeriod = skillChangePeriod;
        this.active = new ArrayList<>();
        this.closed = new ArrayList<>();
        this.informer = new AID();
        this.followers = new ArrayList<>();
    }

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill, int skillChangePeriod, ArrayList<ArrayList<Integer>> nextSkills, boolean repeat) {
        this.id = id;
        this.capital = initialCapital;
        this.skill = skill;
        this.skillChangePeriod = skillChangePeriod;
        this.repeat = repeat;
        this.nextSkills = nextSkills;
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

    public ArrayList<ArrayList<Integer>> getNextSkills() {
        return nextSkills;
    }

    public int getSkillChangePeriod() {
        return skillChangePeriod;
    }

    public boolean getRepeat() {
        return repeat;
    }

    public void setSkill(ArrayList<Integer> skill) { this.skill = skill; }

    public void setSkill(int index, int skill) {
        this.skill.set(index, skill);
    }

    public void setNextSkills(ArrayList<ArrayList<Integer>> nextSkills) {
        this.nextSkills = nextSkills;
    }

    // Updates the sum of values of every stock
    private void updatePortfolioValue(ArrayList<StockPrice> prices) {
        portfolioValue = 0;
        for (Transaction t: active) {
            for (StockPrice stock: prices) {
                if (stock.getSymbol().equals(t.getStock())) {
                    portfolioValue += stock.getCurrPrice()*t.getQuantity();
                    break;
                }
            }
        }
    }

    @Override
    public void setup() {
        // register language and ontology
        codec = new SLCodec(true);
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
        addBehaviour(new ManageFollowers());

        // TODO - TIRAR COMENTÁRIO ASSIM QUE ESTEJA IMPLEMENTADO
        //addBehaviour(new SendInfoFollowers());
        addBehaviour(new InvestorTrade(this));

        if (skillChangePeriod != STATIC_AGENT) {
            addBehaviour(new InvestorChangeSkill(this));
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

    // Behaviours
    private class InvestorSubscribe extends SimpleBehaviour {
        private boolean subscribed = false;
        private InvestorAgent agent;

        public InvestorSubscribe(InvestorAgent a) {
            super(a);
            agent = a;
        }

        public void action() {
            // Find informer agent
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd2 = new ServiceDescription();
            sd2.setType("informer");
            template.addServices(sd2);

            try {
                DFAgentDescription[] result = DFService.search(this.agent, template);
                this.agent.informer = result[0].getName();
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }

            // Subscribe to informer agent to receive prices
            try {
                ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
                subscribe.addReceiver(this.agent.informer);
                subscribe.setLanguage(codec.getName());
                subscribe.setOntology(stockMarketOntology.getName());

                getContentManager().fillContent(subscribe, new InvestorInfo(agent.getId(), agent.getSkill()));
                agent.send(subscribe);

                // Only accept subscribe messages
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchSender(this.agent.informer));
                ACLMessage reply = receive(mt);
                if (reply != null) {
                    subscribed = true;
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
        InvestorAgent agent;

        public InvestorTrade(InvestorAgent agent) {
            super(agent);
            this.agent = agent;
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(this.agent.informer));

            // When new prices are received buy/sell/update portfolio value
            ACLMessage stockPrices = receive(mt);
            if (stockPrices != null) {
                try {
                    MarketPrices marketInfo = (MarketPrices) getContentManager().extractContent(stockPrices);
                    ArrayList<StockPrice> prices = marketInfo.getPrices();
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
        private void moveStock(ArrayList<StockPrice> prices) {
            // Get top growth stock
            ArrayList<StockPrice> predictedGrowth = new ArrayList<>(prices);
            Collections.sort(predictedGrowth, new StockPrice.StockPriceComparator());
            Collections.reverse(predictedGrowth);

            // Get lowest growth stock owned
            ArrayList<StockPrice> currentOwned = new ArrayList<>();
            for (Transaction t: active) {
                for (StockPrice stock: predictedGrowth) {
                    if (t.getStock().equals(stock.getSymbol())) {
                        currentOwned.add(stock);
                    }
                }
            }
            Collections.sort(currentOwned, new StockPrice.StockPriceComparator());

            // Decide stock to buy and sell
            ArrayList<StockPrice> union = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
            union.addAll(currentOwned);
            ArrayList<StockPrice> intersection = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
            intersection.retainAll(currentOwned);

            // Get top growth stock not currently owned
            ArrayList<StockPrice> toBuy = new ArrayList<>(predictedGrowth.subList(0, PORTFOLIO_SIZE));
            toBuy.removeAll(intersection);

            // Get worse growth stock currently owned
            ArrayList<StockPrice> toSell = new ArrayList<>(currentOwned);
            toSell.removeAll(intersection);

            int size = toBuy.size() < toSell.size() ? toBuy.size(): toSell.size();
            // Sell stock owned with lowest growth potential
            for (int i = 0; i < size; i++) {
                sellStock(toSell.get(i));
            }

            // Buy stock with highest growth potential to replace the ones sold
            for (int i = 0; i < size; i++) {
                buyStock(toBuy.get(i), getTotalCapital()/PORTFOLIO_SIZE);
            }

            // Buy stock with biggest growth with the remaining capital
            float amountPerStock = capital/PORTFOLIO_SIZE;
            for (int i = 0; i < PORTFOLIO_SIZE; i++) {
                buyStock(predictedGrowth.get(i), amountPerStock);
            }
        }

        // Buy stock
        private void buyStock(StockPrice stock, float amountPerStock) {
            int quantity = (int)(amountPerStock/stock.getCurrPrice());
            if (quantity > 0) {
                boolean transactionFound = false;
                for (Transaction t: active) {
                    if (t.getStock().equals(stock.getSymbol())) {
                        t.setQuantity(t.getQuantity() + quantity);
                        capital -= stock.getCurrPrice() * quantity;
                        transactionFound = true;
                    }
                }

                if (!transactionFound) {
                    Transaction t = new Transaction(stock.getSymbol(), stock.getCurrPrice(), quantity);
                    active.add(t);
                    capital -= stock.getCurrPrice() * quantity + TRANSACTION_TAX;
                }
            }
        }

        // Sell stock
        private void sellStock(StockPrice stock) {
            for (Iterator<Transaction> it = active.iterator(); it.hasNext(); ) {
                Transaction t = it.next();
                if (t.getStock().equals(stock.getSymbol())) {
                    float currentPrice = stock.getCurrPrice();
                    t.setSellPrice(currentPrice);
                    t.closeTransaction();
                    closed.add(t);
                    it.remove();
                    capital += t.getQuantity()*currentPrice;
                    break;
                }
            }
        }
    }

    private class InvestorChangeSkill extends TickerBehaviour {
        private InvestorAgent agent;

        public InvestorChangeSkill(InvestorAgent a) {
            super(a, a.getSkillChangePeriod());
            agent = a;
        }

        public void onTick() {
            try {
                // Change current skill
                boolean changed = false;
                ArrayList<Integer> newSkill = new ArrayList<>();
                if (agent.getNextSkills() == null) {
                    Random r = new Random();
                    for (int i = 0; i < agent.getSkill().size(); i++) {
                        newSkill.add(r.nextInt(INVESTOR_MAX_SKILL));
                    }
                    changed = true;
                }
                else if (agent.getNextSkills().size() != 0) {
                    newSkill = agent.getNextSkills().get(0);

                    ArrayList<ArrayList<Integer>> s = new ArrayList<>(agent.getNextSkills().subList(1, agent.getNextSkills().size()));
                    // Add current skill to end to nextSkills list if repeat
                    if (agent.getRepeat()) {
                        s.add(agent.getSkill());
                    }
                    agent.setNextSkills(s);
                    agent.setSkill(newSkill); // Make agent skill the first element of nextSkills
                    changed = true;
                }

                if (changed) {
                    ACLMessage updateInvestor = new ACLMessage(ACLMessage.PROPOSE);
                    updateInvestor.addReceiver(new AID("Informer", AID.ISLOCALNAME));
                    updateInvestor.setLanguage(codec.getName());
                    updateInvestor.setOntology(stockMarketOntology.getName());

                    InvestorInfo investorInfo = new InvestorInfo(agent.getId(), newSkill);
                    getContentManager().fillContent(updateInvestor, investorInfo);
                    agent.send(updateInvestor);

                    // Only accept proposal messages
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchSender(new AID("Informer", AID.ISLOCALNAME)));
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        agent.setSkill(newSkill);
                        System.out.println(id + " updated ok");
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ManageFollowers extends CyclicBehaviour {
        public void action() {
            // Receive request, subscribe and cancel messages
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)), MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
            ACLMessage msg = receive(mt);

            if (msg != null) {
                switch (msg.getPerformative()) {
                    // Respond with current total capital
                    case ACLMessage.REQUEST:
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent((new Float(getTotalCapital())).toString());
                        send(reply);
                        break;
                    // TODO respond with ACLMessage.CONFIRM
                    // Add player to followers
                    case ACLMessage.SUBSCRIBE:
                        addFollower(msg.getSender());
                        break;
                    // Remove player to followers
                    case ACLMessage.CANCEL:
                        removeFollower(msg.getSender());
                        break;
                }
            }
        }

        private void addFollower(AID follower){
            if(!followers.contains(follower))
                followers.add(follower);
        }

        private void removeFollower(AID follower){
            if(followers.contains(follower))
                followers.remove(follower);
        }
    }

    // TODO - IMPLEMENTAR - Behaviour para periodicment eenviar informação para os followers deste investor
    private class SendInfoFollowers extends CyclicBehaviour {

        public void action(){

        }
    }

}
