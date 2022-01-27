package it.uniroma1.diag.examples;

import java.util.List;

import it.uniroma1.diag.Assertion;
import it.uniroma1.diag.Parser;
import it.uniroma1.diag.Rewriter;
import it.uniroma1.diag.SparqlUCQ;
import it.uniroma1.diag.exceptions.ParserException;
import it.uniroma1.diag.exceptions.RewriteException;


public class Main {

	public static void main(String[] args) throws ParserException, RewriteException {

		if (args.length != 2) {
			System.out.println("Was expecting two arguments: mapping and query filepaths.");
			return;
		}

		String mappingPath = args[0];
		String queryPath = args[1];

		// parse mapping
		List<Assertion> mapping = Parser.parseMapping(mappingPath);

		// parse query
		SparqlUCQ query = Parser.parseQuery(queryPath);

		// rewrite query over mapping
		String reformulation = Rewriter.rewriteBoolean(mapping, query);

		if (reformulation != null)
			System.out.println(reformulation);
	}

}