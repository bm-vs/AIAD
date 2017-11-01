package model;

import data.Market;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import model.onto.ServiceOntology;
import sajas.core.Agent;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

public class InvestorAgent extends Agent {
    private Codec codec;
    private Ontology serviceOntology;
    private ArrayList<Transaction> active;
    private ArrayList<Transaction> done;
    private double capital;
    private Market market;
    private Calendar currentTime;

    public InvestorAgent(double initialCapital, Market market) {
        this.capital = initialCapital;
        this.market = market;
        this.currentTime = (Calendar) market.getStartDate().clone();
        active = new ArrayList<>();
        done = new ArrayList<>();
    }

    public double getCapital() {
        return capital;
    }

    public Calendar getCurrentTime() {
        return currentTime;
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

        // behaviours
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

    private class InvestorTrade extends SimpleBehaviour {
        private boolean finished = false;

        public InvestorTrade(Agent a) {
            super(a);
        }

        public void action() {
            SimpleDateFormat f = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm");
            try {
                System.out.println(f.format(currentTime.getTime()) + " - " + market.getStocks().get(0).getPrice(currentTime));
            }
            catch (Exception e) {}


            Random r = new Random();
            capital += r.nextInt(1000);


            currentTime.add(Calendar.HOUR_OF_DAY, 1);
        }

        public boolean done() {
            return finished;
        }
    }
}
