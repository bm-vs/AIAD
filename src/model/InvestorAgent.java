package model;

import jade.core.AID;
import jade.content.lang.sl.SLCodec;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import ontology.InvestorInfo;
import ontology.MarketPrices;
import ontology.StockMarketOntology;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.SimpleBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;
import ontology.StockPrice;
import static utils.Settings.*;

import java.io.Serializable;
import java.util.*;

public class InvestorAgent extends ActiveAgent implements Serializable {
    private ArrayList<Integer> skill; // represents the knowledge (0-10) of each sector (0-5)
    private int skillChangePeriod;
    private ArrayList<ArrayList<Integer>> nextSkills;
    private boolean repeat;
    private ArrayList<AID> followers;

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill, int skillChangePeriod) {
        super(id, initialCapital);
        this.skill = skill;
        this.skillChangePeriod = skillChangePeriod;
        this.informer = new AID();
        this.followers = new ArrayList<>();
    }

    public InvestorAgent(String id, float initialCapital, ArrayList<Integer> skill, int skillChangePeriod, ArrayList<ArrayList<Integer>> nextSkills, boolean repeat) {
        super(id, initialCapital);
        this.skill = skill;
        this.skillChangePeriod = skillChangePeriod;
        this.repeat = repeat;
        this.nextSkills = nextSkills;
        this.informer = new AID();
        this.followers = new ArrayList<>();
    }

    public ArrayList<Integer> getSkill() {
        return skill;
    }

    public ArrayList<ArrayList<Integer>> getNextSkills() {
        return nextSkills;
    }

    public int getSkillChangePeriod() {
        return skillChangePeriod;
    }

    public boolean getRepeat() {
        return repeat;
    }

    public void setSkill(ArrayList<Integer> skill) { this.skill = skill; }

    public void setSkill(int index, int skill) {
        this.skill.set(index, skill);
    }

    public void setNextSkills(ArrayList<ArrayList<Integer>> nextSkills) {
        this.nextSkills = nextSkills;
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
        sd.setName(getLocalName() + "-investor");
        sd.setType("investor");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            System.err.println(e.getMessage());
        }

        // Behaviours
        addBehaviour(new InvestorSubscribe(this));
        addBehaviour(new ManageFollowers());
        addBehaviour(new InvestorTrade(this));
        if (skillChangePeriod != STATIC_AGENT) {
            addBehaviour(new InvestorChangeSkill(this));
        }
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
    private class InvestorSubscribe extends SimpleBehaviour {
        private boolean subscribed = false;
        private InvestorAgent agent;

        public InvestorSubscribe(InvestorAgent a) {
            super(a);
            agent = a;
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
                ACLMessage subscribe = new ACLMessage(ACLMessage.SUBSCRIBE);
                subscribe.addReceiver(this.agent.informer);
                subscribe.setLanguage(codec.getName());
                subscribe.setOntology(stockMarketOntology.getName());

                getContentManager().fillContent(subscribe, new InvestorInfo(agent.getId(), agent.getSkill()));
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

    private class InvestorTrade extends CyclicBehaviour {
        InvestorAgent agent;

        public InvestorTrade(InvestorAgent agent) {
            super(agent);
            this.agent = agent;
        }

        public void action() {
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(agent.informer));

            // When new prices are received buy/sell/update portfolio value
            ACLMessage stockPrices = receive(mt);
            if (stockPrices != null) {
                try {
                    MarketPrices marketInfo = (MarketPrices) getContentManager().extractContent(stockPrices);
                    ArrayList<StockPrice> prices = marketInfo.getPrices();
                    if (prices != null) {
                        sendPredictions(marketInfo);
                        moveStock(prices);
                        updatePortfolioValue(prices);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Sends price predictions to followers
        private void sendPredictions(MarketPrices marketInfo) {
            try {
                ACLMessage predictions = new ACLMessage(ACLMessage.INFORM);
                for (AID follower: followers) {
                    predictions.addReceiver(follower);
                }
                predictions.setLanguage(codec.getName());
                predictions.setOntology(stockMarketOntology.getName());
                getContentManager().fillContent(predictions, marketInfo);
                send(predictions);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class InvestorChangeSkill extends TickerBehaviour {
        private InvestorAgent agent;

        public InvestorChangeSkill(InvestorAgent a) {
            super(a, a.getSkillChangePeriod());
            agent = a;
        }

        public void onTick() {
            try {
                // Change current skill
                boolean changed = false;
                ArrayList<Integer> newSkill = new ArrayList<>();
                if (agent.getNextSkills() == null) {
                    Random r = new Random();
                    for (int i = 0; i < agent.getSkill().size(); i++) {
                        newSkill.add(r.nextInt(INVESTOR_MAX_SKILL));
                    }
                    changed = true;
                }
                else if (agent.getNextSkills().size() != 0) {
                    newSkill = agent.getNextSkills().get(0);

                    ArrayList<ArrayList<Integer>> s = new ArrayList<>(agent.getNextSkills().subList(1, agent.getNextSkills().size()));
                    // Add current skill to end to nextSkills list if repeat
                    if (agent.getRepeat()) {
                        s.add(agent.getSkill());
                    }
                    agent.setNextSkills(s);
                    agent.setSkill(newSkill); // Make agent skill the first element of nextSkills
                    changed = true;
                }

                if (changed) {
                    ACLMessage updateInvestor = new ACLMessage(ACLMessage.PROPOSE);
                    updateInvestor.addReceiver(informer);
                    updateInvestor.setLanguage(codec.getName());
                    updateInvestor.setOntology(stockMarketOntology.getName());

                    InvestorInfo investorInfo = new InvestorInfo(agent.getId(), newSkill);
                    getContentManager().fillContent(updateInvestor, investorInfo);
                    agent.send(updateInvestor);

                    // Only accept proposal messages
                    MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchSender(informer));
                    ACLMessage reply = receive(mt);
                    if (reply != null) {
                        agent.setSkill(newSkill);
                        System.out.println(id + " updated ok");
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ManageFollowers extends CyclicBehaviour {
        public void action() {
            // Receive request, subscribe and cancel messages
            MessageTemplate mt = MessageTemplate.or(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)), MessageTemplate.MatchPerformative(ACLMessage.CANCEL));
            ACLMessage msg = receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();

                switch (msg.getPerformative()) {
                    // Respond with current total capital
                    case ACLMessage.REQUEST:
                        reply.setPerformative(ACLMessage.CONFIRM);
                        reply.setContent((new Float(getTotalCapital())).toString());
                        send(reply);
                        break;
                    // Add player to followers
                    case ACLMessage.SUBSCRIBE:
                        addFollower(msg.getSender());
                        reply.setPerformative(ACLMessage.CONFIRM);
                        send(reply);
                        break;
                    // Remove player to followers
                    case ACLMessage.CANCEL:
                        removeFollower(msg.getSender());
                        reply.setPerformative(ACLMessage.CONFIRM);
                        send(reply);
                        break;
                }
            }
        }

        private void addFollower(AID follower){
            if (!followers.contains(follower)) {
                followers.add(follower);
                capital += SUBSCRIBE_TAX;
            }
        }

        private void removeFollower(AID follower){
            if (followers.contains(follower)) {
                followers.remove(follower);
            }
        }
    }
}
