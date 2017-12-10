package model;

import data.Stock;
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
import model.onto.MarketPrices;
import model.onto.StockMarketOntology;

import model.onto.StockPrice;
import sajas.core.Agent;
import sajas.core.behaviours.*;
import sajas.domain.DFService;

import java.io.Serializable;
import java.util.*;

import static utils.Settings.*;

public class PlayerAgent extends ActiveAgent implements Serializable {
    private HashMap<AID, InvestorTrust> investors; // save trust associated with every investor id
    private InvestorTrust followed; // investor the player agent is following

    public PlayerAgent(String id, float initialCapital) {
        super(id, initialCapital);
        this.investors = new HashMap<>();
        this.followed = null;
    }

    public void setFollowed(InvestorTrust investor){
        this.followed = investor;
    }

    public InvestorTrust getFollowed(){
        return  followed;
    }

    @Override
    public void setup(){
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
        sd.setName(getLocalName() + "-player");
        sd.setType("player");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        addBehaviour(new PlayerSubscribe(this));
        addBehaviour(new PlayerTrade(this));
        addBehaviour(new SearchInvestorAgents(this));
        addBehaviour(new ManageFollowing(this));
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
    private class PlayerSubscribe extends SimpleBehaviour {
        private boolean subscribed = false;
        private PlayerAgent agent;

        public PlayerSubscribe(PlayerAgent agent) {
            super(agent);
            this.agent = agent;
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
                subscribe.setContent(PLAYER_SUBSCRIBE_MSG);
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

    private class PlayerTrade extends CyclicBehaviour {
        PlayerAgent agent;
        ArrayList<StockPrice> prices;

        public PlayerTrade(PlayerAgent agent) {
            super(agent);
            this.agent = agent;
            prices = new ArrayList<>();
        }

        public void action() {
            // Add sequential behaviour
            SequentialBehaviour seq = new SequentialBehaviour();
            addBehaviour(seq);

            // Receive current prices from informer agent
            seq.addSubBehaviour(
                new SimpleBehaviour() {
                    boolean received = false;

                    @Override
                    public void action() {
                        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(agent.informer));
                        ACLMessage stockPrices = receive(mt);
                        if (stockPrices != null) {
                            try {
                                MarketPrices marketInfo = (MarketPrices) getContentManager().extractContent(stockPrices);
                                ArrayList<StockPrice> currentPrices = marketInfo.getPrices();
                                if (prices != null) {
                                    prices = currentPrices;
                                    // TODO update stock trust
                                    received = true;
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public boolean done() {
                        return received;
                    }
                }
            );

            // Receive price predictions from the subscribed investor
            seq.addSubBehaviour(
                new SimpleBehaviour() {
                    boolean received = false;

                    @Override
                    public void action() {
                        // Receive prices from followed
                        if (followed != null) {
                            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(followed.getInvestor()));
                            ACLMessage stockPredictions = receive(mt);
                            if (stockPredictions != null) {
                                try {
                                    MarketPrices marketInfo = (MarketPrices) getContentManager().extractContent(stockPredictions);
                                    ArrayList<StockPrice> futurePrices = marketInfo.getPrices();
                                    if (futurePrices != null) {
                                        for (StockPrice future : futurePrices) {
                                            for (StockPrice current : prices) {
                                                if (future.getSymbol().equals(current.getSymbol())) {
                                                    current.setFuturePrice(future.getFuturePrice());
                                                    break;
                                                }
                                            }
                                        }

                                        received = true;
                                    }
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            received = true;
                        }
                    }

                    @Override
                    public boolean done() {
                        return received;
                    }
                }
            );

            // Buy/sell stock
            seq.addSubBehaviour(
                new OneShotBehaviour() {
                    @Override
                    public void action() {
                        if (prices != null) {
                            moveStock(prices);
                            updatePortfolioValue(prices);
                        }
                    }
                }
            );
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
                    this.agent.investors.put(result[i].getName(), new InvestorTrust(result[i].getName()));
                }
                foundInvestors = true;
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

        @Override
        public boolean done() {
            return foundInvestors;
        }
    }

    private class ManageFollowing extends TickerBehaviour {
        private PlayerAgent agent;
        private ACLMessage requestResults;
        private ACLMessage unfollow;
        private ACLMessage follow;
        private InvestorTrust bestInvestor;

        public ManageFollowing(PlayerAgent agent) {
            super(agent, REQUEST_PERIOD);
            this.agent = agent;
        }

        public void onTick() {
            // Add sequential behaviour
            SequentialBehaviour seq = new SequentialBehaviour();
            addBehaviour(seq);

            // Request success rate to all investors
            seq.addSubBehaviour(
                new OneShotBehaviour() {
                    @Override
                    public void action() {
                        requestResults = new ACLMessage(ACLMessage.REQUEST);
                        for (AID investor: agent.investors.keySet()) {
                            requestResults.addReceiver(investor);
                        }
                        requestResults.setLanguage(codec.getName());
                        requestResults.setOntology(stockMarketOntology.getName());
                        requestResults.setReplyWith(getLocalName() + hashCode() + System.currentTimeMillis());
                        send(requestResults);
                    }
                }
            );

            // Add parallel behaviour to receive info from every investor
            ParallelBehaviour par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
            seq.addSubBehaviour(par);

            for (AID investor: this.agent.investors.keySet()) {
                par.addSubBehaviour(
                    new SimpleBehaviour() {
                        boolean done = false;

                        @Override
                        public void action() {
                            MessageTemplate mt = MessageTemplate.and(
                                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                    MessageTemplate.MatchInReplyTo(requestResults.getReplyWith()));

                            // Update investors trust
                            ACLMessage investorCapital = receive(mt);
                            if (investorCapital != null) {
                                InvestorTrust trust = investors.get(investor);
                                trust.addPastCapital(Float.parseFloat(investorCapital.getContent()));
                                investors.put(investor, trust);
                                done = true;
                            }
                        }

                        @Override
                        public boolean done() {
                            return done;
                        }
                    }
                );

            }

            // Determine follow/unfollow of investor
            seq.addSubBehaviour(
                new OneShotBehaviour() {
                    @Override
                    public void action() {
                        bestInvestor = followed;
                        float maxTrust = followed == null ? -1 : followed.getTrust();

                        // Determine best investor
                        for (InvestorTrust investor : investors.values()) {
                            // TODO check if its worth it to change investor
                            if (investor.getTrust() > maxTrust && capital >= SUBSCRIBE_TAX) {
                                bestInvestor = investor;
                                maxTrust = investor.getTrust();
                            }
                        }
                    }
                }
            );

            // Send unfollow
            seq.addSubBehaviour(
                new OneShotBehaviour() {
                    @Override
                    public void action() {
                        if (!bestInvestor.equals(followed) && followed != null) {
                            unfollow = new ACLMessage(ACLMessage.CANCEL);
                            unfollow.addReceiver(followed.getInvestor());
                            unfollow.setLanguage(codec.getName());
                            unfollow.setOntology(stockMarketOntology.getName());
                            unfollow.setReplyWith(getLocalName() + hashCode() + System.currentTimeMillis());
                            send(unfollow);
                        }
                    }
                }
            );

            // Wait for unfollow confirmation
            seq.addSubBehaviour(
                new SimpleBehaviour() {
                    @Override
                    public void action() {
                        if (unfollow != null) {
                            MessageTemplate mt = MessageTemplate.and(
                                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                    MessageTemplate.MatchInReplyTo(unfollow.getReplyWith()));

                            ACLMessage unfollowSuccess = receive(mt);
                            if (unfollowSuccess != null) {
                                System.out.println("Unfollowed " + followed.getInvestor());
                                followed = null;
                            }
                        }
                    }

                    @Override
                    public boolean done() {
                        return followed == null;
                    }
                }
            );

            // Follow best investor
            seq.addSubBehaviour(
                new OneShotBehaviour() {
                    @Override
                    public void action() {
                        if (followed == null) {
                            follow = new ACLMessage(ACLMessage.SUBSCRIBE);
                            follow.addReceiver(bestInvestor.getInvestor());
                            follow.setLanguage(codec.getName());
                            follow.setOntology(stockMarketOntology.getName());
                            follow.setReplyWith(getLocalName() + hashCode() + System.currentTimeMillis());
                            send(follow);
                        }
                    }
                }
            );

            // Wait for follow confirmation
            seq.addSubBehaviour(
                new SimpleBehaviour() {
                    @Override
                    public void action() {
                        if (follow != null) {
                            MessageTemplate mt = MessageTemplate.and(
                                    MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                                    MessageTemplate.MatchInReplyTo(follow.getReplyWith()));

                            ACLMessage followSuccess = receive(mt);
                            if (followSuccess != null) {
                                followed = bestInvestor;
                                capital -= SUBSCRIBE_TAX;
                                System.out.println("Followed " + followed.getInvestor());
                            }
                        }
                    }

                    @Override
                    public boolean done() {
                        return followed != null;
                    }
                }
            );
        }
    }
}