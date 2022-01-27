package it.uniroma1.diag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;
import org.apache.jena.sparql.syntax.ElementUnion;

import it.uniroma1.diag.exceptions.ParserException;

public class SparqlUCQ {

	private List<SparqlCQ> cqs;
	
	private List<Node> target;
	
	private boolean isBoolean;

	
	public SparqlUCQ(Query q) throws ParserException {
		super();
		
		target = new LinkedList<>();
		isBoolean = true;
		if (q.queryType() == QueryType.SELECT) {
			for(int i=0; i<q.getProjectVars().size(); i++) {
				Var v = q.getProjectVars().get(i);
				
				if(v.getName().startsWith(".")) { // ARQ format for constants
					String literal = q.toString() // the literal shall be taken from query string directly
							.split(System.getProperty("line.separator"))[0] // only pick select clause
							.split(" ")[i+2]; // and get the i-th return value
					literal = literal.replaceAll("[^a-zA-Z0-9]", ""); // remove non alphanumeric characters
					target.add(new Node(literal, NodeType.CONSTANT));
				}
				else {
					target.add(new Node(v.asNode()));
					isBoolean = false;
				}
			}
		}
		
		cqs = new ArrayList<SparqlCQ>();
		Element base = ((ElementGroup) q.getQueryPattern()).getLast();
		
		if(base instanceof ElementUnion) { // UCQ
			List<Element> disjuncts = ((ElementUnion) base).getElements();
			for(Element e : disjuncts) {
				ElementGroup eg = (ElementGroup) e;
				List<TriplePath> triples = ((ElementPathBlock) eg.getLast()).getPattern().getList();
				cqs.add(new SparqlCQ(triples, target, isBoolean));
			}
		}
		else if (base instanceof ElementGroup) { // CQ (ElementGroup)
			List<TriplePath> triples = ((ElementPathBlock)((ElementGroup)base).getLast()).getPattern().getList();
			cqs.add(new SparqlCQ(triples, target, isBoolean));
		}
		else { // CQ (ElementPathBlock)
			cqs.add(new SparqlCQ(q));
		}
	}

	
	public int size() {
		return cqs.size();
	}

	public List<Node> target() {
		return target;
	}

	public int arity() {
		return target.size();
	}
	
	public List<SparqlCQ> disjuncts() {
		return cqs;
	}
	
	public boolean isBoolean() {
		return isBoolean;
	}
	
}
