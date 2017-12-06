package model;

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

import java.io.Serializable;
import java.util.*;

public class PlayerAgent {

    private Codec codec;
    private Ontology serviceOntology;
    private String id;
    private float capital;
    private float portfolioValue;

    public PlayerAgent(String id, float initialCapital)
    {
      this.id = id;
      this.capital = initialCapital;
      this.active = new ArrayList<>();
      this.closed = new ArrayList<>();
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

    public int getProfile() {
        return profile;
    }


}
