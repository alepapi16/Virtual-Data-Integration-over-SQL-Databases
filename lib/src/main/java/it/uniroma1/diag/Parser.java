package it.uniroma1.diag;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryException;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;
import org.gibello.zql.ParseException;
import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;
import org.gibello.zql.ZqlParser;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import it.uniroma1.diag.exceptions.ParserException;

public class Parser {

	public static List<Assertion> parseMapping(String mappingFilePath) throws ParserException {
		List<Assertion> M = new ArrayList<>();

		// 1 - reading file
		String fileText;
		try {
			fileText = Files.asCharSource(new File(mappingFilePath), Charsets.UTF_8).read();
		} catch (IOException e) {
			e.printStackTrace();
			throw new ParserException("Could not read from input file.");
		}

		// 2 - parsing JSON
		JSONArray arr = new JSONArray(fileText);
		String bodyString;
		String headString;
		for (int i=0; i<arr.length(); i++) { // for every JSON entry
			try {
				bodyString = arr.getJSONObject(i).getString("sql").concat(";");
				headString = arr.getJSONObject(i).getString("sparql");
			} catch (JSONException e) {
				e.printStackTrace();
				throw new ParserException("Could not parse JSON syntax.");
			}

			ZQuery body; // ZQL data structure for body SQL query
			Query head; // Jena ARQ data structure for head SPARQL query

			try {
				// 3 - parsing mapping
				ZqlParser zp = new ZqlParser();
				zp.initParser(new ByteArrayInputStream(bodyString.getBytes()));
				org.apache.jena.query.ARQ.init();
				
				body = (ZQuery) zp.readStatement();
				head = QueryFactory.create(headString);
			} catch (ParseException e) {
				e.printStackTrace();
				throw new ParserException("Could not parse SQL body syntax.");
			} catch (QueryException e) {
				e.printStackTrace();
				throw new ParserException("Could not parse SPARQL head syntax.");
			}

			// 4 - validating parsed mapping assertion
			String message = "Parsing assertion "+(i+1)+": ", errorStr;
			@SuppressWarnings("unchecked")
			Vector<ZSelectItem> vars_body = body.getSelect(); // body variable names
			List<String> vars_head = head.getResultVars(); // head variable names

			if (!head.isSelectType()) { errorStr = "head is not a sparql SELECT.";
				throw new ParserException(message.concat(errorStr)); }

			if (head.isQueryResultStar()) { errorStr = "found wildcard ('*') in head.";
				throw new ParserException(message.concat(errorStr)); }

			if (vars_body.get(0).isWildcard()) { errorStr = "found wildcard ('*') in body.";
				throw new ParserException(message.concat(errorStr)); }

			if (vars_body.size() != vars_head.size()) { errorStr = "body and head have different arities.";
				throw new ParserException(message.concat(errorStr)); }

			for (int j=0; j<vars_body.size(); j++) { // check vars_body == vars_head
				if (vars_body.get(j).getAlias() == null) // set alias if missing 
					vars_body.get(j).setAlias(vars_body.get(j).getColumn());

				if (!vars_body.get(j).getAlias().equals(vars_head.get(j))) { 
					errorStr = "variables at index " + (j+1) + " differ.";
					throw new ParserException(message.concat(errorStr)); 
				}
			}

			if (!(head.getQueryPattern() instanceof ElementGroup)) { errorStr = "wrong SPARQL format for WHERE";
				throw new ParserException(message.concat(errorStr)); }

			ElementGroup eg = (ElementGroup) head.getQueryPattern();

			if (!(eg.getElements().size() == 1)) { errorStr = "unexpected size of WHERE pattern in head.";
				throw new ParserException(message.concat(errorStr)); }

			if (!(eg.getLast() instanceof ElementPathBlock)) { errorStr = "unexpected WHERE pattern in head.";
				throw new ParserException(message.concat(errorStr)); }

			// consistency checks
			SparqlCQ h = new SparqlCQ(head);
			for (Atom a : h.atoms()) {

				// head is full CQ
				if (a.getSubject().isVariable() && !(h.target().contains(a.getSubject()))) {
					errorStr = "found subject variable not in target list.";
					throw new ParserException(message.concat(errorStr)); }

				if (!a.isUnary() && a.getObject().isVariable() && !(h.target().contains(a.getObject()))) {
					errorStr = "found object variable not in target list.";
					throw new ParserException(message.concat(errorStr)); }
			}

			// 5 - add assertion to mapping instance
			M.add(new Assertion(body, h));
		}

		return M;
	}

	public static SparqlUCQ parseQuery(String queryFilePath) throws ParserException {
		
		Query q = null; // SPARQL query representation from Jena ARQ 
		
		String file_text;
		String err_msg = "Parsing query: ";

		try {
			file_text = Files.asCharSource(new File(queryFilePath), Charsets.UTF_8).read();
		} catch (IOException e) {
			e.printStackTrace();
			throw new ParserException("Could not read from input file.");
		}

		try {
			q = QueryFactory.create(file_text);
		} catch (QueryException e) {
			e.printStackTrace();
			throw new ParserException(err_msg.concat("wrong SPARQL syntax."));
		}

		// check query is boolean
		if (!(q.isAskType() || q.isSelectType())) 
			throw new ParserException(err_msg.concat("not a SPARQL ASK nor SELECT."));

		// consistency check, should never trigger
		if (!(q.getQueryPattern() instanceof ElementGroup))
			throw new ParserException(err_msg.concat("wrong SPARQL format for WHERE."));

		ElementGroup eg = (ElementGroup) q.getQueryPattern();

		// check query is conjunctive
		if (!(eg.getElements().size() == 1))
			throw new ParserException(err_msg.concat("unexpected size of WHERE pattern in query."));

		// check WHERE tokens are not out of our fragment of interest (only made of RDF triples)
		if (!(eg.getLast() instanceof ElementPathBlock || 
				eg.getLast() instanceof ElementUnion || 
				(eg.getLast() instanceof ElementGroup && 
						((ElementGroup)eg.getLast()).getLast() instanceof ElementPathBlock))) {
			throw new ParserException(err_msg.concat("unexpected WHERE pattern in query."));
		}
		
		return new SparqlUCQ(q);
	}

}
