package sm.htw.main;

import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.FileManager;

public class HelloSematicWeb {

	private static String defaultNameSpace = "http://semwebprogramming.org/2009/ont/chp2:#";

	private Model friends = null;
	private Model schema = null;

	public static void main(String[] args) throws IOException {
		HelloSematicWeb hsw = new HelloSematicWeb();
		System.out.println("Load my FOAF Friends");
		hsw.populateFOAFFriends();
		System.out.println("Add new Friends");
		hsw.populateNewFriends();
		System.out.println("Add the new Ontologies");
		hsw.populateFOAFSchema();
		hsw.populateNewFriendsSchema();
		System.out.println("Say Hello to Myself");
		hsw.mySelf(hsw.friends);
		System.out.println("Say Hello to my FOAF Friends");
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
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

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
				"rdf/IndividualsOntologies.owl");
		friends.read(inFoafInstance, defaultNameSpace);
		inFoafInstance.close();
	}

}
