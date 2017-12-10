package model;

import data.Market;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.InvestorInfo;
import ontology.MarketPrices;
import ontology.StockMarketOntology;
import jade.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import utils.DateNotFoundException;
import ontology.StockPrice;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import static utils.Settings.*;

public class InformerAgent extends Agent {
    private Codec codec;
    private Ontology stockMarketOntology;
    private Market market;
    private Calendar currentTime;
    private HashMap<AID, InvestorInfo> investors;
    private ArrayList<AID> players;

    public InformerAgent(Market market) {
        this.market = market;
        this.currentTime = market.getStartDate();
        this.investors = new HashMap<>();
        this.players = new ArrayList<>();
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
        sd.setName(getLocalName() + "-informer");
        sd.setType("informer");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        // Behaviours
        addBehaviour(new InformerSubscribe(this));
        addBehaviour(new InformerBroadcast(this));
        addBehaviour(new InformerUpdate(this));
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
    private class InformerSubscribe extends CyclicBehaviour {
        private Agent agent;

        public InformerSubscribe(InformerAgent agent) {
            super(agent);
            this.agent = agent;
        }

        public void action() {
            // Only accept subscribe messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);

            // Handle subscriptions
            ACLMessage subscriptions = receive(mt);
            if (subscriptions != null) {
                try {
                    if (subscriptions.getContent().equals(PLAYER_SUBSCRIBE_MSG)) {
                        // Save sender of player subscriptions
                        if (!players.contains(subscriptions.getSender())) {
                            players.add(subscriptions.getSender());
                            System.out.println("Player " + subscriptions.getSender() + " subscribed");

                            ACLMessage reply = subscriptions.createReply();
                            reply.setPerformative(ACLMessage.AGREE);
                            send(reply);
                        }
                    }
                    else {
                        // Save info about every investor
                        InvestorInfo investor = (InvestorInfo) getContentManager().extractContent(subscriptions);
                        if (!investors.containsKey(subscriptions.getSender())) {
                            investors.put(subscriptions.getSender(), investor);
                            System.out.println("Investor " + investor + " subscribed");

                            ACLMessage reply = subscriptions.createReply();
                            reply.setPerformative(ACLMessage.AGREE);
                            send(reply);
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class InformerBroadcast extends TickerBehaviour {
        public InformerBroadcast(InformerAgent a) {
            super(a, BROADCAST_PERIOD);
        }

        public void onTick() {
            // Sends stock prices to every investor and player
            boolean dayNotFound = false;
            try {
                ArrayList<StockPrice> prices = market.getPrices(currentTime);

                // Send to investors
                for (AID investorAID : investors.keySet()) {
                    InvestorInfo investor = investors.get(investorAID);

                    // Create different predictions to every investor according to their skill level in that sector
                    ArrayList<StockPrice> investorPrices = new ArrayList<>();
                    for (StockPrice price: prices) {
                        int skill = investor.getSkill().get(price.getSector());
                        StockPrice investorPrice = new StockPrice(price.getSymbol(), price.getSector(), price.getCurrPrice(), price.getFuturePrice());
                        investorPrice.addError(skill);
                        investorPrices.add(investorPrice);
                    }

                    // Send prices
                    ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                    stockPrices.addReceiver(investorAID);
                    stockPrices.setLanguage(codec.getName());
                    stockPrices.setOntology(stockMarketOntology.getName());
                    getContentManager().fillContent(stockPrices, new MarketPrices(investorPrices));

                    send(stockPrices);
                }

                // Remove future prices (for player agents)
                ArrayList<StockPrice> playerPrices = new ArrayList<>();
                for (StockPrice price: prices) {
                    StockPrice playerPrice = new StockPrice(price.getSymbol(), price.getSector(), price.getCurrPrice(), price.getCurrPrice());
                    playerPrices.add(playerPrice);
                }

                // Send to players
                ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                for (AID playerAID: players) {
                    stockPrices.addReceiver(playerAID);
                }
                stockPrices.setLanguage(codec.getName());
                stockPrices.setOntology(stockMarketOntology.getName());
                getContentManager().fillContent(stockPrices, new MarketPrices(prices));
                send(stockPrices);

            }
            catch (Exception e) {
                if (e instanceof DateNotFoundException) {
                    dayNotFound = true;
                }
                else {
                    e.printStackTrace();
                }
            }

            if (currentTime.get(Calendar.HOUR_OF_DAY) + 1 > CLOSE_TIME) {
                currentTime.add(Calendar.HOUR_OF_DAY, 24-CLOSE_TIME+OPEN_TIME);
                SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
                System.out.print(date.format(currentTime.getTime()));
                if (dayNotFound) {
                    System.out.println(" (closed)");
                }
                else {
                    System.out.println();
                }

            }
            else {
                currentTime.add(Calendar.HOUR_OF_DAY, 1);
            }

        }
    }

    private class InformerUpdate extends CyclicBehaviour {
        public InformerUpdate(InformerAgent a) {
            super(a);
        }

        public void action() {
            // Only accept proposal messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);

            // Handle update of investors
            ACLMessage updates = receive(mt);
            if (updates != null) {
                try {
                    // Update info about matched investor
                    ArrayList<Integer> skill = ((InvestorInfo) getContentManager().extractContent(updates)).getSkill();
                    InvestorInfo investorInfo = investors.get(updates.getSender());
                    investorInfo.setSkill(skill);
                    investors.put(updates.getSender(), investorInfo);

                    System.out.println(investorInfo + " updated");

                    ACLMessage reply = updates.createReply();
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    send(reply);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
