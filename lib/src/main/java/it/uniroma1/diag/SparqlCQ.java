package it.uniroma1.diag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryType;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.ElementGroup;
import org.apache.jena.sparql.syntax.ElementPathBlock;

import it.uniroma1.diag.exceptions.ParserException;

public class SparqlCQ {

	private List<Atom> atoms;

	private List<Node> target;
	
	private boolean isBoolean;
	

	public SparqlCQ(Query q) throws ParserException {
		super();

		List<TriplePath> triples = ((ElementPathBlock) ((ElementGroup) q.getQueryPattern()).getLast()).getPattern()
				.getList();
		initializeAtoms(triples);
		
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
	}

	public SparqlCQ(List<TriplePath> triples, List<Node> target, boolean isBoolean) throws ParserException {
		super();

		initializeAtoms(triples);
		this.target = target;
		this.isBoolean = isBoolean;
	}
	
	private void initializeAtoms(List<TriplePath> triples) throws ParserException {
		atoms = new ArrayList<>();
				
		HashSet<Atom> atomsTest = new HashSet<>(); // avoid duplicate atoms
		for (TriplePath t : triples) {
			Atom a = new Atom(t);
			if(!atomsTest.contains(a)) {
				atomsTest.add(a);
				atoms.add(a);
			}
		}
	}

	
	/**
	 * Number of query atoms.
	 * 
	 * @return
	 */
	public int size() {
		return atoms.size();
	}

	public List<Atom> atoms() {
		return atoms;
	}

	public Set<Node> predicates() {
		Set<Node> preds = new HashSet<>();

		for (Atom a : atoms)
			preds.add(a.getPredicate());

		return preds;
	}

	public List<Node> target() {
		return target;
	}

	/**
	 * Number of free variables.
	 * 
	 * @return
	 */
	public int arity() {
		return target.size();
	}

	public boolean isBoolean() {
		return isBoolean;
	}
	
	
	public String printChunk(List<Integer> c) {
		String chunkStr = "";

		for (int i = 0; i < c.size(); i++) {
			Atom a = atoms.get(c.get(i));
			chunkStr = chunkStr.concat("" + a);

			if (i != c.size() - 1)
				chunkStr = chunkStr.concat(" . ");
		}

		return chunkStr;
	}

	@Override
	public String toString() {
		return atoms.toString();
	}


}
