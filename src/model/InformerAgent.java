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
import java.util.Random;

import static utils.InvestorSettings.INVESTOR_MAX_SKILL;

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
            // Handle subscription of investors
            ACLMessage subscriptions = receive();
            if (subscriptions != null && subscriptions.getPerformative() == ACLMessage.SUBSCRIBE) {
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
                }
                catch (Exception e) {
                }
            }

            // Update current date
            if (ticks == ticksPerHour) {
                // Sends stock prices to every investor
                try {
                    ArrayList<StockPrice> prices = market.getPrices(currentTime);

                    for (InvestorAgent.InvestorInfo investor : investors) {
                        // Create different predictions to every investor according to their skill level in that sector
                        HashMap<String, StockPrice> investorPrices = new HashMap<>();
                        for (StockPrice price: prices) {
                            int skill = investor.getSkill().get(price.getSector());
                            StockPrice investorPrice = new StockPrice(price.getSymbol(), price.getSector(), price.getCurrPrice(), errorPrice(price.getHourPrice(), skill), errorPrice(price.getDayPrice(), skill), errorPrice(price.getWeekPrice(), skill), errorPrice(price.getMonthPrice(), skill));
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
                }

                currentTime.add(Calendar.HOUR_OF_DAY, 1);
                ticks = 0;
            }
            else {
                ticks++;
            }

        }

        // Introduces error into prices according to skill
        // The higher the skill the more accurate the return value is
        private float errorPrice(float price, int skill) {
            // Deciding if the value is over or under the real one
            Random r = new Random();
            int sign = r.nextInt(2);
            if (sign == 0) {
                sign = -1;
            }

            // Error is x% of price with x being inversely proportional to skill
            float error = (INVESTOR_MAX_SKILL - skill + 1) * sign * 2 / 100f;
            return price+price*error;
        }

        public boolean done() {
            return finished;
        }
    }

}
