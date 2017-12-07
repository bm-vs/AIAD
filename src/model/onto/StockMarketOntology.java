package model.onto;

import jade.content.Concept;
import jade.content.onto.*;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PrimitiveSchema;

public class StockMarketOntology extends Ontology {
	private static final long serialVersionUID = 1L;
	public static final String ONTOLOGY_NAME = "Stock-market-ontology";

	// Vocabulary
	public static final String INVESTOR_INFO = "InvestorInfo";
	public static final String ID = "id";
	public static final String SKILL = "skill";

	public static final String SUBSCRIBE = "Subscribe";
	public static final String INVESTOR = "investor";

	// Singleton instance of this ontology
	private static Ontology theInstance = new StockMarketOntology();
	
	// Method to access the singleton ontology object
	public static Ontology getInstance() {
		return theInstance;
	}
	
	// Private constructor
	private StockMarketOntology() {
		super(ONTOLOGY_NAME, BasicOntology.getInstance());
		
		try {
			// add all Concept, Predicate and AgentAction
			add(new ConceptSchema(INVESTOR_INFO), InvestorInfo.class);
			add(new AgentActionSchema(SUBSCRIBE), Subscribe.class);

			// structure of the schema for the InvestorInfo concept
			ConceptSchema cs = (ConceptSchema) getSchema(INVESTOR_INFO);
			cs.add(ID, (PrimitiveSchema) getSchema(BasicOntology.STRING));
			cs.add(SKILL, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), 1, ObjectSchema.UNLIMITED);

			// structure of the schema for the Subscribe agent action
			AgentActionSchema as = (AgentActionSchema) getSchema(SUBSCRIBE);
			as.add(INVESTOR, (ConceptSchema) getSchema(INVESTOR_INFO));
		}
		catch(OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
