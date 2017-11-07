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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class InformerAgent extends Agent {
    private Codec codec;
    private Ontology serviceOntology;
    private Market market;
    private Calendar currentTime;
    private int nInvestors;
    private int ticksPerHour;

    public InformerAgent(Market market, int nInvestors, int ticksPerHour) {
        this.market = market;
        this.currentTime = market.getStartDate();
        this.nInvestors = nInvestors;
        this.ticksPerHour = ticksPerHour;
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
            // Update current date
            if (ticks == ticksPerHour) {

                // Sends stock prices to every investor
                try {
                    HashMap<String, Float> prices = market.getPrices(currentTime);
                    for (int i = 0; i < nInvestors; i++) {
                        ACLMessage stockPrices = new ACLMessage(ACLMessage.INFORM);
                        stockPrices.setContentObject(prices);
                        stockPrices.addReceiver(new AID("Investor" + i, AID.ISLOCALNAME));
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
