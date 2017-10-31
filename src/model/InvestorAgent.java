package model;

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

import java.util.Random;

public class InvestorAgent extends Agent {
    private Codec codec;
    private Ontology serviceOntology;
    private int capital;

    public InvestorAgent(int initialCapital) {
        this.capital = initialCapital;
    }

    public int getCapital() {
        return capital;
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
        addBehaviour(new InvestorBuy(this));
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private class InvestorBuy extends SimpleBehaviour {
        private boolean finished = false;

        public InvestorBuy(Agent a) {
            super(a);
        }

        public void action() {
            Random r = new Random();
            capital += r.nextInt(1000);
        }

        public boolean done() {
            return finished;
        }
    }
}
