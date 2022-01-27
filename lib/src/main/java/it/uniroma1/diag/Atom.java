package it.uniroma1.diag;

import java.util.Objects;

import org.apache.jena.sparql.core.TriplePath;

import it.uniroma1.diag.exceptions.ParserException;

/**
 * Wrapper for TriplePath
 * 
 * @author Alessio Papi <papi.1761063@studenti.uniroma1.it>
 */
public class Atom {

	private Node s;
	private Node p;
	private Node o;
	private boolean isUnary = false;

	
	public Atom(TriplePath t) throws ParserException {
		s  = new Node(t.getSubject());
		
		checkPredicate(t.getPredicate());
		if (t.getPredicate().getLocalName().equals("type")) isUnary = true;
		
		if(isUnary) {
			checkPredicate(t.getObject());
			p = new Node(t.getObject());
			o = null;
		}
		else {
			p = new Node(t.getPredicate());
			o = new Node(t.getObject());
		}
		
		
			
	}
	
	public Atom(Atom a, Unifier u) {
		this.isUnary = a.isUnary;
		this.s = u.get(a.getSubject());
		this.p = a.getPredicate();
		if(!isUnary) this.o = u.get(a.getObject());
		else this.o = null;
	}

	private void checkPredicate(org.apache.jena.graph.Node p) throws ParserException {
		if(p.isVariable() || p.isLiteral()) throw new ParserException("Found wrong predicate format.");
	}

	
	public Node getSubject() {
		return s;
	}

	public Node getPredicate() {
		return p;
	}
	
	public Node getObject() {
		return o;
	}

	public boolean isUnary() {
		return isUnary;
	}

	
	@Override
	public String toString() {
		if(isUnary) return (""+p).concat("("+s+")");
		return (""+p).concat("("+s+","+o+")");
	}

	@Override
	public int hashCode() {
		return Objects.hash(s,p,o);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof Atom)) return false;
		final Atom e = (Atom) obj;
		return isUnary == e.isUnary 
				&& s.equals(e.s)
				&& p.equals(e.p)
				&& o.equals(e.o);
	}
	
}
