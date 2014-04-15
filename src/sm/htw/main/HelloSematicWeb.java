package sm.htw.main;

import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;
import com.hp.hpl.jena.util.FileManager;

public class HelloSematicWeb {

	private static String defaultNameSpace = "http://semwebprogramming.org/2009/ont/chp2:#";

	private Model friends = null;
	private Model schema = null;
	private InfModel inferredFriends;

	public static void main(String[] args) throws IOException {
		HelloSematicWeb hsw = new HelloSematicWeb();
		System.out.println("Load my FOAF Friends");
		hsw.populateFOAFFriends();
		System.out.println("Add new Friends");
		hsw.populateNewFriends();
		System.out.println("Add the new Ontologies");
		hsw.populateFOAFSchema();
		hsw.populateNewFriendsSchema();
		System.out.println("Define Ontologie relations");
		hsw.createRelationsToOntologies();
		System.out.println("Run a reasoner");
		hsw.bindReasoner();
		System.out.println("Say Hello to Myself");
		hsw.mySelf(hsw.friends);
		System.out.println("Say Hello to my Friends");
		hsw.myFriends(hsw.friends);

	}

	private void populateFOAFFriends() throws IOException {
		friends = ModelFactory.createOntologyModel();
		InputStream inFoafInstance = FileManager.get().open("rdf/data.rdf");
		if (inFoafInstance != null) {
			friends.read(inFoafInstance, defaultNameSpace);
			inFoafInstance.close();
		}

	}

	private void mySelf(Model model) {
		runQuery(" select DISTINCT ?name where { swp2:me foaf:name ?name }",
				model);
	}

	private void runQuery(String queryRequest, Model model) {
		StringBuffer queryStr = new StringBuffer();
		// Estabish Prefix
		queryStr.append("PREFIX swp2" + ": <" + defaultNameSpace + "> ");
		queryStr.append("PREFIX foaf" + ": <" + "http://xmlns.com/foaf/0.1/"
				+ "> ");

		// Now add query
		queryStr.append(queryRequest);
		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query,
				inferredFriends);

		// Run Select
		try {
			ResultSet response = qexec.execSelect();
			while (response.hasNext()) {
				QuerySolution soln = response.nextSolution();
				RDFNode name = soln.get("?name");
				if (name != null)
					System.out.println("Hello to " + name.toString());
				else
					System.out.println("No Friends found!");

			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			qexec.close();
		}

	}

	private void myFriends(Model model) {
		// Hello to just my friends - navigation
		runQuery(" select DISTINCT ?myname ?name where{"
				+ "swp2:me foaf:knows ?friend." + "?friend foaf:name ?name } ",
				model);
	}

	private void populateNewFriends() throws IOException {
		InputStream inFoafInstance = FileManager.get().open(
				"rdf/Individuals.owl");
		friends.read(inFoafInstance, defaultNameSpace);
		inFoafInstance.close();
	}

	private void populateFOAFSchema() {
		schema = ModelFactory.createOntologyModel();
		schema.read("rdf/index.rdf");
		friends.read("rdf/index.rdf");
	}

	private void populateNewFriendsSchema() throws IOException {
		InputStream inFoafInstance = FileManager.get().open(
				"rdf/IndividualsO.owl");
		friends.read(inFoafInstance, defaultNameSpace);
		inFoafInstance.close();
	}

	private void createRelationsToOntologies() {
		// State that :individuals is equivalentClass of foaf:Person
		Resource resource = schema.createResource(defaultNameSpace
				+ "Individual");
		Property prop = schema.createProperty("owl:equivalentClass");
		Resource obj = schema.createResource("foaf:Person");
		schema.add(resource, prop, obj);

		// State that :hasName is equivalentProperty of foaf:name
		resource = schema.createResource(defaultNameSpace + "hasName");
		prop = schema.createProperty("owl:equivalentProperty");
		obj = schema.createResource("foaf:name");
		schema.add(resource, prop, obj);

		// State that :hasFriend is a subproperty of foaf:knows
		resource = schema.createResource(defaultNameSpace + "hasFriend");
		prop = schema.createProperty("rdfs:subPropertyOf");
		obj = schema.createResource("foaf:knows");
		schema.add(resource, prop, obj);

		// State that sem web is the same person as Semantic Web
		resource = schema.createResource("swp2: #me");
		prop = schema.createProperty("owl:sameAs");
		obj = schema.createResource("http://swp2:#Indi_5");
		schema.add(resource, prop, obj);

	}

	private void bindReasoner() {
		Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
		// Reasoner reasoner = PelletReasonerFactory.theInstance().create();
		reasoner = reasoner.bindSchema(schema);
		inferredFriends = ModelFactory.createInfModel(reasoner, friends);
		runJenaRule(inferredFriends);
	}

	private void runJenaRule(Model model) {
		String rules = " [emailChange:"
				+ "(?person foaf:mbox ?email),strConcat(?email,?lit),"
				+ "regex( ?lit, \"(.*@gmail.com)')"
				+ " -> (?person rdf:type> People:GmailPerson)]";
		Reasoner ruleReasoner = new GenericRuleReasoner(Rule.parseRules(rules));
		ruleReasoner = ruleReasoner.bindSchema(schema);
		inferredFriends = ModelFactory.createInfModel(ruleReasoner, model);
	}

}
