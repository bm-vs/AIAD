package model;

import data.Market;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.basic.Action;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import model.onto.InvestorInfo;
import model.onto.MarketPrices;
import model.onto.StockMarketOntology;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import utils.DateNotFoundException;
import model.onto.StockPrice;

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
    private ArrayList<InvestorInfo> investors;

    public InformerAgent(Market market) {
        this.market = market;
        this.currentTime = market.getStartDate();
        this.investors = new ArrayList<>();
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
        public InformerSubscribe(InformerAgent a) {
            super(a);
        }

        public void action() {
            // Only accept subscribe messages
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);

            // Handle subscription of investors
            ACLMessage subscriptions = receive(mt);
            if (subscriptions != null) {
                try {
                    // Save info about every investor
                    InvestorInfo investor = (InvestorInfo) getContentManager().extractContent(subscriptions);
                    if (!investors.contains(investor)) {
                        investors.add(investor);
                        System.out.println(investor + " subscribed");

                        ACLMessage reply = subscriptions.createReply();
                        reply.setPerformative(ACLMessage.AGREE);
                        send(reply);
                    }
                } catch (Exception e) {
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
            // Sends stock prices to every investor
            boolean dayNotFound = false;
            try {
                ArrayList<StockPrice> prices = market.getPrices(currentTime);

                for (InvestorInfo investor : investors) {
                    // Create different predictions to every investor according to their skill level in that sector
                    ArrayList<StockPrice> investorPrices = new ArrayList<>();
                    for (StockPrice price: prices) {
                        int skill = investor.getSkill().get(price.getSector());
                        StockPrice investorPrice = new StockPrice(price.getSymbol(), price.getSector(), price.getCurrPrice(), price.getHourPrice());
                        investorPrice.addError(skill);
                        investorPrices.add(investorPrice);
                    }

                    // Send prices
                    ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                    stockPrices.addReceiver(new AID(investor.getId(), AID.ISLOCALNAME));
                    stockPrices.setLanguage(codec.getName());
                    stockPrices.setOntology(stockMarketOntology.getName());
                    getContentManager().fillContent(stockPrices, new MarketPrices(investorPrices));

                    send(stockPrices);
                }
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
                    InvestorInfo investor = (InvestorInfo) getContentManager().extractContent(updates);
                    for (InvestorInfo inv: investors) {
                        if (inv.getId().equals(investor.getId())) {
                            inv.setSkill(investor.getSkill());
                            System.out.println(inv + " updated");

                            ACLMessage reply = updates.createReply();
                            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            send(reply);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
