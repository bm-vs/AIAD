package model;

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
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;

import java.io.Serializable;
import java.util.*;

import static utils.Settings.BROADCAST_PERIOD;
import static utils.Settings.PORTFOLIO_SIZE;
import static utils.Settings.TRANSACTION_TAX;

public class PlayerAgent extends Agent implements Serializable {
    private Codec codec;
    private Ontology stockMarketOntology;
    private String id;
    private float capital;
    private float portfolioValue;
    private HashMap<AID, InvestorTrust> investorTrust; // save trust associated with every investor id
    private InvestorAgent investor; // investor the player agent has its money on
    private AID informer;


    public PlayerAgent(String id, float initialCapital) {
      this.id = id;
      this.capital = initialCapital;
      this.investorTrust = new HashMap<>();
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

       // addBehaviour(new ManageFollowing(this));
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
                ACLMessage subscribe = new ACLMessage(ACLMessage.REQUEST);
                subscribe.addReceiver(this.agent.informer);
                subscribe.setLanguage(codec.getName());
                subscribe.setOntology(stockMarketOntology.getName());
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
        private int step;

        public PlayerTrade(PlayerAgent agent) {
            super(agent);
            this.agent = agent;
            prices = new ArrayList<>();
            step = 0;
        }

        public void action() {
            switch (step) {
                case 0:
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(this.agent.informer));

                    // When new prices are received buy/sell/update portfolio value
                    ACLMessage stockPrices = receive(mt);
                    if (stockPrices != null) {
                        try {
                            MarketPrices marketInfo = (MarketPrices) getContentManager().extractContent(stockPrices);
                            ArrayList<StockPrice> prices = marketInfo.getPrices();
                            if (prices != null) {
                                this.prices = prices;
                                // TODO update stock trust
                                step = 1;
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 1:
                    // TODO if subscribed receive prices
                    // TODO buy/sell stock
                    step = 0;
                    break;
            }
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
                    this.agent.investorTrust.put(result[i].getName(), new InvestorTrust());
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

    // TODO - FALTA IMPLEMENTAR - FAZ A GESTÃO DE QUEM ESTÁ A SEGUIR, FAZNEDO PEDIDOS DE NOVAS INFORMAÇÕES DE RATE, OUD E FOLLOW/UNFOLLOW
    // TODO - VERIFICAR EM CADA STEP O QUE PODE EVENTUALMENTE FALTAR
    private  class ManageFollowing extends TickerBehaviour {
        private PlayerAgent agent;
        private int step = 0;
        private int replies = 0;

        public ManageFollowing(PlayerAgent agent){
            super(agent, BROADCAST_PERIOD);
            this.agent = agent;
        }

        public void onTick(){
            switch (step) {
                // Request success rate to all investors
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.REQUEST);

                    for (AID investor : this.agent.investorTrust.keySet()) {
                        cfp.addReceiver(investor);
                    }

                    // TODO - USAR AQUI ONTOLOGIAS??
                    cfp.setContent("rate-request");
                    cfp.setConversationId("rate");

                    cfp.setReplyWith("ratereq" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);

                    // Prepare the template to get confirmations
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("rate"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    step = 1;

                    break;

                // Receive replys with the rate of each investor
                case 1:
                    // Check if investor received the information
                    MessageTemplate mm = MessageTemplate.and(MessageTemplate.MatchConversationId("rate"),
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                    while (replies < investorTrust.size()) {
                        ACLMessage reply = myAgent.receive(mm);

                        // Reply received
                        if (reply != null) {

                            // TODO - GUARDAR AQUI INFORMAÇÃO DE RATE/TRUST RECEBIDA DE CADA INVESTOR


                            replies++;
                        } else {
                            block();
                        }
                    }

                    // TODO - COM CICLO FOR, DETERMINAR AQUI NESTE STEP, APÓS SE RECEBEREM TODOS OS REPLIES, QUAL O INVESTOR A DAR FOLLOW

                    replies = 0;
                    step = 2;

                    break;

                // Send follow request to the most trusty investor
                case 2:
                    ACLMessage cfp3 = new ACLMessage(ACLMessage.REQUEST);

                    // TODO - FAZER AQUI cfp.addreceiver do investor a quem se vai pedir para dar follow



                    cfp3.setContent("follow-request");
                    cfp3.setConversationId("follow");

                    cfp3.setReplyWith("followreq" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp3);

                    // Prepare the template to get confirmations
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("follow-req"),
                            MessageTemplate.MatchInReplyTo(cfp3.getReplyWith()));

                    step = 3;

                    break;

                // Send unfollow request
                case 3:
                    ACLMessage cfp2 = new ACLMessage(ACLMessage.REQUEST);

                    // TODO - DETERMINAR AQUI A QUEM ENVIR UNFOLLOW REQUEST

                    /*for(int i = 0 ; i < following.size() ; i++) {
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
                    }*/

                    cfp2.setContent("unfollow-request");
                    cfp2.setConversationId("unfollow");

                    cfp2.setReplyWith("unfollowreq" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp2);
                    // Prepare the template to get confirmations
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("unfollow-req"),
                            MessageTemplate.MatchInReplyTo(cfp2.getReplyWith()));

                    step = 0;
            }
        }
    }


}