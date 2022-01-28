package it.uniroma1.diag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.uniroma1.diag.exceptions.RewriteException;

public class Resolvent {
	
	private SparqlCQ q;
	
	private Map<Node, List<Integer>> splitMap; // ∀x ∈ Vars(q) lists all indexes i : x ∈ q[C[i]]
	private List<List<Integer>> C; // chunks for this resolvent
	private List<Unifier> U; // chunk resolvents, ∀i : U[i] resolves C[i]

	
	public Resolvent(SparqlCQ q) {
		super();
		this.C = new LinkedList<>();
		this.U = new LinkedList<>();
		this.q = q;
		this.splitMap = new HashMap<>();
	}

	public Resolvent(Resolvent r) {
		this.C = new LinkedList<>(r.C);
		this.U = new LinkedList<>(r.U);
		this.q = r.q;
		this.splitMap = new HashMap<>();
		for (Node n : r.splitMap.keySet()) {
			this.splitMap.put(n, new LinkedList<>(r.splitMap.get(n)));
		}
	}

	
	public void add(List<Integer> c, Unifier u) {
		C.add(c); // updating C
		U.add(u); // updating U

		// updating splitMap
		int j = C.size() - 1; // next available chunk index in C

		List<String> seen = new ArrayList<>(); // prevent repetitions
		for (Integer k : c) { // for all indexes in the chunk
			Atom a = q.atoms().get(k); // get the corresponding query atom
			Node s = a.getSubject(), o = a.getObject();

			// subject
			if (s.isVariable() && !seen.contains(s.getName())) {
				if (splitMap.containsKey(s))
					splitMap.get(s).add(j);
				else {
					List<Integer> J = new ArrayList<>();
					J.add(j);
					splitMap.put(s, J);
				}
				seen.add(s.getName());
			}

			// object if binary predicate
			if (!a.isUnary() && o.isVariable() && !seen.contains(o.getName())) {

				if (splitMap.containsKey(o))
					splitMap.get(o).add(j);
				else {
					List<Integer> J = new ArrayList<>();
					J.add(j);
					splitMap.put(o, J);
				}
				seen.add(o.getName());
			}
		}
	}

	
	public String unfold(List<Assertion> M) throws RewriteException {

		String select = "SELECT "; // SELECT CLAUSE
		for (int i = 0; i < q.arity(); i++) {
			Node x = q.target().get(i);
			
			if(x.isConstant()) {
				select = select.concat(x.getName());
			}
//			else if(x.isVariable()) { // proposal for supporting free variables
//				int j = splitMap.get(x).get(0); // get first chunk index in C where x occurs
//				Unifier u = U.get(j);
//				SparqlCQ h = u.getH();
//				Node u_x = u.get(x);
//	
//				for (Node y : h.target()) {
//					if (u.get(y).equals(u_x)) {
//						String var = y.getName();
//						String alias = "A" + (i + 1);
//						select = select.concat(var + " " + alias);
//						break;
//					}
//				}
//			}

			if (i == q.arity() - 1)
				select = select.concat(" ");
			else
				select = select.concat(", ");
		}
		if (q.arity() == 0) // "ask" sparql query
			select = select.concat("1 ");
		select = select.concat(System.getProperty("line.separator"));

		String from = "FROM "; // FROM CLAUSE
		for (int i = 0; i < C.size(); i++) {
			from = from.concat("(" + M.get(U.get(i).getAssertionNumber()).getBody() + ") V" + (i + 1));
			from = (i == (C.size() - 1)) ? from.concat(" ") 
					: from.concat(", ").concat(System.getProperty("line.separator"));
		}

		// WHERE CLAUSE
		List<String> conds = new ArrayList<>();

		for (int i = 0; i < C.size(); i++) // unification conditions (WHERE)
			conds.addAll(U.get(i).unfold(i + 1));

		for (Node x : splitMap.keySet()) { // frontier equality conditions (WHERE)
			List<Integer> var_cs = splitMap.get(x);

			int i = var_cs.get(0); // i = var_cs[0] : first chunk index where x appears

			for (int k = 1; k < var_cs.size(); k++) {
				int j = var_cs.get(k); // j = var_cs[k] : other chunk index where x appears

				Unifier u_i = U.get(i);
				SparqlCQ h_i = M.get(U.get(i).getAssertionNumber()).getHead();
				Node a = findTwin(u_i.getHeadChunk(), h_i, u_i, u_i.get(x)); // find a ∈ h_i[d_i] : U[i](a) == U[i](x)

				// find b
				Unifier u_j = U.get(j);
				SparqlCQ h_j = M.get(U.get(j).getAssertionNumber()).getHead();
				Node b = findTwin(u_j.getHeadChunk(), h_j, u_j, u_j.get(x)); // find b ∈ h_j[d_j] : U[j](b) == U[j](x)

				conds.add("V" + (i+1) + "." + a.getName() + " = " + "V" + (j+1) + "." + b.getName());
			}
		}

		String where = "WHERE ";
		for (int i = 0; i < conds.size(); i++) {
			where = where.concat(conds.get(i));
			where = (i == (conds.size() - 1)) ? where.concat(" ") : where.concat(" AND ");
		}

		// FINAL OUTPUT
		String rewrite = select.concat(from);
		if (!conds.isEmpty())
			rewrite = rewrite.concat(System.getProperty("line.separator")).concat(where);

		return rewrite;
	}

	private Node findTwin(List<Integer> d, SparqlCQ h, Unifier u, Node u_x) throws RewriteException {
		Set<Node> var_d = new HashSet<>(); // set of all variables in h[d]
		for (Integer i : d) {
			Atom a = h.atoms().get(i);
			if (a.getSubject().isVariable())
				var_d.add(a.getSubject()); // subject
			if (!a.isUnary() && a.getObject().isVariable())
				var_d.add(a.getObject()); // object
		}

		for (Node y : var_d) { // find y ∈ var_d : u(y) == u(x)
			if (u.get(y).equals(u_x))
				return y;
		}

		throw new RewriteException("No variable unifies with a split variable.");
	}

	
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < C.size(); i++) {
			s = s.concat("Query Chunk: q." + C.get(i) + " (" + q.printChunk(C.get(i)) + ");    ");
			s = s.concat(U.get(i) + ".");
			s = s.concat(System.getProperty("line.separator"));
		}
//		s = s.concat("SplitMap: " + splitMap);
		return s;
	}

}
