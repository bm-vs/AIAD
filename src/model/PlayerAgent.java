package model;

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
import sajas.core.behaviours.SimpleBehaviour;
import sajas.domain.DFService;

import java.io.Serializable;
import java.util.*;

public class PlayerAgent extends Agent implements  Serializable{
    private Codec codec;
    private Ontology stockMarketOntology;
    private String id;
    private HashMap<String, InvestorTrust> investorTrust; // save trust associated with every investor id
    private float investedAmount; // amount sent to investor
    private InvestorAgent investor; // investor the player agent has its money on
    private float capital;
    private float portfolioValue;

    public PlayerAgent(String id, float initialCapital) {
      this.id = id;
      this.capital = initialCapital;
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

    public void setInvestorAgent(InvestorAgent investor){
        this.investor = investor;
    }

    public InvestorAgent getInvestorAgent(){
        return  investor;
    }

    @Override
    public void setup(){
        System.out.println("player created");

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
        sd.setName(getLocalName() + "-player");
        sd.setType("player");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd2 = new ServiceDescription();
        sd2.setType("investor");
        template.addServices(sd2);

        /*try {
            DFAgentDescription[] result = DFService.search(this, template);
            System.out.println("Player found the following investor agents:");
            investorAgents = new AID[result.length];
            investorAgentsRatio = new double[result.length];
            for (int i = 0; i < result.length; ++i) {
                investorAgents[i] = result[i].getName();
                investorAgentsRatio[i] = 0;
                System.out.println(investorAgents[i].getName() + "  -  " + result.length);
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }

        if(investorAgents == null || investorAgents.length == 0){
            form.close();
            chart.dispose();
            this.doDelete();
        }

        addBehaviour(new dataReceiver());
        addBehaviour(new SuggestionReceiver());
        addBehaviour(new TickerBehaviour(this, 500) {
            protected void onTick() {

                switch (step) {
                    case 0:
                        // Request success rate to all random investor
                        ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);
                        for(int i = 0 ; i < investorAgents.length ; i++) {
                            cfp.addReceiver(investorAgents[i]);
                        }

                        cfp.setContent("rate-request");
                        cfp.setConversationId("rate-req");

                        cfp.setReplyWith("ratereq" + System.currentTimeMillis()); // Unique value
                        myAgent.send(cfp);
                        // Prepare the template to get confirmations
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("rate-req"),
                                MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                        fpq = new FollowersPQ(investorAgents.length);
                        top = new Follower[2];

                        step = 1;
                        break;
                    case 1:
                        // Check if investor received the information
                        MessageTemplate mm = MessageTemplate.and(MessageTemplate.MatchConversationId("rate-req"),
                                MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                        while (replies < investorAgents.length) {
                            ACLMessage reply = myAgent.receive(mm);
                            if (reply != null) {
                                // Reply received
                                double rate = Double.parseDouble(reply.getContent());

                                for(int i = 0 ; i < investorAgents.length ; i++){
                                    if(investorAgents[i].equals(reply.getSender())){
                                        fpq.add(i, rate);
                                        break;
                                    }
                                }
                                replies++;
                            } else {
                                block();
                            }
                        }
                        Follower[] temp = fpq.getTop2();
                        for(int i = 0 ; i < 2 ; i++){
                            top[i] = temp[i];
                        }
                        replies = 0;
                        step = 2;
                        break;
                    case 2:

                        ACLMessage cfp3 = new ACLMessage(ACLMessage.REQUEST);

                        for(int i = 0 ; i < 2 ; i++){
                            if(top[i] != null && !isFollowing(investorAgents[top[i].getIndex()]) && top[i].getVal() >= trustLimit) {
                                cfp3.addReceiver(investorAgents[top[i].getIndex()]);
                                follow(investorAgents[top[i].getIndex()]);
                            }
                            else if(top[i] == null) break;
                        }

                        cfp3.setContent("follow-request");
                        cfp3.setConversationId("follow-req");

                        cfp3.setReplyWith("followreq" + System.currentTimeMillis()); // Unique value
                        myAgent.send(cfp3);
                        // Prepare the template to get confirmations
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("follow-req"),
                                MessageTemplate.MatchInReplyTo(cfp3.getReplyWith()));

                        step = 3;

                        break;
                    case 3:
                        ACLMessage cfp2 = new ACLMessage(ACLMessage.REQUEST);

                        for(int i = 0 ; i < following.size() ; i++) {
                            boolean found = false;

                            for(int j = 0 ; j < 2 ; j++) {
                                if(top[j] == null) break;

                                if(investorAgents[top[j].getIndex()].equals( following.get(i) )){
                                    found = true;
                                    break;
                                }
                            }
                            if(!found) {
                                cfp2.addReceiver(following.get(i));
                                unfollow(following.get(i));
                            }
                        }

                        cfp2.setContent("unfollow-request");
                        cfp2.setConversationId("unfollow-req");

                        cfp2.setReplyWith("unfollowreq" + System.currentTimeMillis()); // Unique value
                        myAgent.send(cfp2);
                        // Prepare the template to get confirmations
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("unfollow-req"),
                                MessageTemplate.MatchInReplyTo(cfp2.getReplyWith()));

                        System.out.println("Following:");
                        for(int i = 0 ; i< following.size(); i++)
                            System.out.println("\t"+following.get(i).getName());

                        step = 0;

                }
            }
        });*/


        addBehaviour(new receiveReports());

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    //Behaviours
    private class receiveReports extends CyclicBehaviour {

        public void action(){
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchSender(new AID("Informer", AID.ISLOCALNAME)));

            ACLMessage reports = receive(mt);

            if(reports != null){
                updateTrust(reports);

            }
        }

        // TODO - falta implementar
        public void updateTrust(ACLMessage reports){

        }
    }



    /*private class PlayerTrade extends SimpleBehaviour {
        private boolean finished = false;

        public void action(){
            // TODO
            // Receive reports
            // For every report received update trust in each investor

            // Withdraw - send playerId (+capital) (-investedAmount)
            // Invest - send playerId and amount (-capital) (+investedAmount)



        }

        public boolean done(){
            return finished;
        }
    }*/
}
