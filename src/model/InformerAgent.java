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
import model.onto.StockMarketOntology;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import utils.DateNotFoundException;
import utils.StockPrice;

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
    private ArrayList<InvestorAgent.InvestorInfo> investors;

    public InformerAgent(Market market) {
        this.market = market;
        this.currentTime = market.getStartDate();
        this.investors = new ArrayList<>();
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
        public InformerSubscribe(Agent a) {
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
                    InvestorAgent.InvestorInfo investor = (InvestorAgent.InvestorInfo) subscriptions.getContentObject();
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
        public InformerBroadcast(Agent a) {
            super(a, TICK_PERIOD);
        }

        public void onTick() {
            // Sends stock prices to every investor
            boolean dayNotFound = false;
            try {
                ArrayList<StockPrice> prices = market.getPrices(currentTime);

                for (InvestorAgent.InvestorInfo investor : investors) {
                    // Create different predictions to every investor according to their skill level in that sector
                    HashMap<String, StockPrice> investorPrices = new HashMap<>();
                    for (StockPrice price: prices) {
                        int skill = investor.getSkill().get(price.getSector());
                        StockPrice investorPrice = new StockPrice(price.getSymbol(), price.getSector(), price.getCurrPrice(), price.getHourPrice());
                        investorPrice.addError(skill);
                        investorPrices.put(price.getSymbol(), investorPrice);
                    }

                    // Send prices
                    ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                    stockPrices.setContentObject(investorPrices);
                    stockPrices.addReceiver(new AID(investor.getId(), AID.ISLOCALNAME));
                    send(stockPrices);
                }
            }
            catch (Exception e) {
                if (e instanceof DateNotFoundException) {
                    dayNotFound = true;
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

}
