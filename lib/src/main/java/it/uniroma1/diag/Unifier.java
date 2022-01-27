package it.uniroma1.diag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Unifier {
	private Map<Node, Node> s; // Key:Variable, Value:Assignment
	private boolean constantViolation;

	private List<Integer> c;
	private SparqlCQ q;

	private int assertionNumber; // assertion number i wrt mapping M
	private List<Integer> d; // resolution chunk
	private SparqlCQ h;

	
	public Unifier(List<Integer> c, SparqlCQ q, int n, List<Integer> d, SparqlCQ h) {
		super();
		this.s = new HashMap<>();
		this.c = c;
		this.q = q;
		this.assertionNumber = n;
		this.d = d;
		this.h = h;
		this.constantViolation = false;
	}

	
	public int getAssertionNumber() {
		return assertionNumber;
	}

	public List<Integer> getHeadChunk() {
		return d;
	}

	public List<Integer> getQueryChunk() {
		return c;
	}

	public SparqlCQ getHead() {
		return h;
	}

	public SparqlCQ getQuery() {
		return q;
	}

	
	/**
	 * Verify that s(q[c]) = s(h[d])
	 * 
	 * @return
	 */
	public boolean isValid() {
		Set<Atom> s_c = new HashSet<>();
		Set<Atom> s_d = new HashSet<>();

		for (Integer i : c) {
			Atom a = q.atoms().get(i);
			s_c.add(new Atom(a, this));
		}

		for (Integer i : d) {
			Atom a = h.atoms().get(i);
			s_d.add(new Atom(a, this));
		}

		return !constantViolation && s_c.equals(s_d);
	}

	public Unifier add(Node x, Node y) { // s(x) = y
		if (x.isConstant() && y.isConstant()) {
			if (!x.equals(y))
				constantViolation = true;
		} else if (x.isConstant()) {
			add(y, x);
		} else { // x is variable
			if (x.equals(get(x))) // x was never equaled with anything
				s.put(x, y); // equal x with y
			else // x was already equaled with something
				add(get(x), y); // equal s(x) with y
		}
		return this;
	}

	public Node get(Node n) { // get end value of n
		if (!s.containsKey(n))
			return n;
		return get(s.get(n));
	}

	public List<String> unfold(int i) { // unif equalities
		List<String> unif = new ArrayList<>();
		HashMap<Node, Node> map = new HashMap<>();

		for (Node var : h.target()) {
			Node val = get(var);
			if (val.isConstant())
				unif.add("V" + i + "." + var.getName() + " = '" + val.getName() + "'");
			else {
				if (map.containsKey(val))
					// equalities are w.r.t. the first variable with that assigned value
					unif.add("V" + i + "." + var.getName() + " = " + "V" + i + "." + map.get(val).getName());
				else
					map.put(val, var);
			}
		}

		return unif;
	}

	public boolean moreGeneralThan(Unifier mgu) {
		return mgu == null || s.size() < mgu.size();
	}

	private int size() {
		return s.size();
	}

	
	public String printSubstitution() {
		String string = "{";
		Set<Node> keys = s.keySet();
		for (Iterator<Node> it = keys.iterator(); it.hasNext();) {
			Node var = it.next();
			Node val = s.get(var);
			string = string.concat(var + "/" + val);
			string = (it.hasNext()) ? string.concat(", ") : string.concat("");
		}
		string = string.concat("}");

		return string;
	}

	@Override
	public String toString() {
		String s = "";
		s = s.concat("Head Chunk: h" + (assertionNumber + 1) + "." + getHeadChunk() + " {" + h.printChunk(d) + "};");
		s = s.concat(System.getProperty("line.separator")).concat("    ");
		s = s.concat("Unifier: " + printSubstitution());
//		s = s.concat(System.getProperty("line.separator")).concat("    ");
//		s = s.concat("Constant Violation: " + constantViolation);
//		s = s.concat(System.getProperty("line.separator")).concat("    ");
//		s = s.concat("Valid: " + isValid());
		return s;
	}

}
