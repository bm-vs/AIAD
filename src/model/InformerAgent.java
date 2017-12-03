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
import model.onto.ServiceOntology;
import sajas.core.AID;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;
import utils.StockPrice;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class InformerAgent extends Agent {
    private Codec codec;
    private Ontology serviceOntology;
    private Market market;
    private Calendar currentTime;
    private int ticksPerHour;
    private ArrayList<InvestorAgent.InvestorInfo> investors;

    public InformerAgent(Market market, int ticksPerHour) {
        this.market = market;
        this.currentTime = market.getStartDate();
        this.ticksPerHour = ticksPerHour;
        this.investors = new ArrayList<>();
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

    private class InformerBroadcast extends SimpleBehaviour {
        private boolean finished = false;
        private int ticks;

        public InformerBroadcast(Agent a) {
            super(a);
            ticks = 0;
        }

        public void action() {
            ACLMessage subscriptions = receive();
            if (subscriptions != null && subscriptions.getPerformative() == ACLMessage.SUBSCRIBE) {
                try {
                    InvestorAgent.InvestorInfo investor = (InvestorAgent.InvestorInfo) subscriptions.getContentObject();
                    investors.add(investor);
                    //System.out.println(investor.getId() + " subscribed");
                    //System.out.println(investors);

                    ACLMessage reply = subscriptions.createReply();
                    reply.setPerformative(ACLMessage.AGREE);
                    send(reply);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Update current date
            if (ticks == ticksPerHour) {
                // Sends stock prices to every investor
                try {
                    ArrayList<StockPrice> prices = market.getPrices(currentTime);

                    for (InvestorAgent.InvestorInfo investor : investors) {
                        HashMap<String, StockPrice> investorPrices = new HashMap<>();
                        for (StockPrice price: prices) {
                            // TODO: for each investor change future prices to reflect their skill
                            investorPrices.put(price.getSymbol(), price);
                        }
                        ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                        stockPrices.setContentObject(investorPrices);
                        stockPrices.addReceiver(new AID(investor.getId(), AID.ISLOCALNAME));
                        send(stockPrices);
                    }
                }
                catch (Exception e) {
                }

                currentTime.add(Calendar.HOUR_OF_DAY, 1);
                ticks = 0;
            }
            else {
                ticks++;
            }

        }

        public boolean done() {
            return finished;
        }
    }

}
