package it.uniroma1.diag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.gibello.zql.ZQuery;
import org.gibello.zql.ZSelectItem;

import it.uniroma1.diag.exceptions.RewriteException;

public class Rewriter {
	private static int renaming_n;
	private static TreeMap<String, String> renaming_dict;
	private static HashMap<List<Integer>, Set<Unifier>> C_MGUs;
	

	/**
	 * Returns the string representation of the SQL reformulation of a UCQ q against M.
	 * @param M List of mapping assertions
	 * @param q SparqlUCQ instance
	 * @return
	 * @throws RewriteException
	 */
	public static String rewriteBoolean(List<Assertion> M, SparqlUCQ Q) throws RewriteException {
		
		if(!Q.isBoolean()) throw new RewriteException("The provided query is not Boolean.");
		
		Set<String> rewritings = new HashSet<String>();
		for (SparqlCQ q : Q.disjuncts())
			rewritings.add(rewriteBoolean(M, q)); // collect all unf(q,M)
		return concatenateBooleanSqlRewritings(new ArrayList<>(rewritings), Q); // produce unf(Q,M) and return
	}
	
	/**
	 * Returns the string representation of the SQL reformulation of a CQ q against M.
	 * @param M List of mapping assertions
	 * @param q SparqlCQ instance
	 * @return
	 * @throws RewriteException
	 */
	public synchronized static String rewriteBoolean(List<Assertion> M, SparqlCQ q) throws RewriteException {

		if(!q.isBoolean()) throw new RewriteException("The provided query is not Boolean.");
		
		checkArities(M, q);

		renaming_n = 0;
		q = renameVars(q);
		M = renameVars(M);

		checkPredicates(M, q);

		
		C_MGUs = new HashMap<>(); // ∀c ∈ q (chunks q), ∀d ∈ h (chunks h ∈ M), store MGUs u : u(c) = u(d)
		
		for (List<Integer> c : genSubsets(q.size())) { // for all chunks c
			Set<Unifier> c_MGUs = new HashSet<>();
			for (int i = 0; i < M.size(); i++) {
				SparqlCQ h = M.get(i).getHead();
				c_MGUs.addAll(findMGUs(c, q, i, h)); // find all MGUs (chunk resolvent for c)
			}
			C_MGUs.put(c, c_MGUs);
		}

		List<List<List<Integer>>> Cs = genPartitions(q.size()); // chunk subdivisions

		List<Resolvent> R = new ArrayList<>();
		for (List<List<Integer>> C : Cs) // for each chunk subdivision C ∈ Cs
			R.addAll(genResolvents(C, q)); // generate all possible resolvents

		Set<String> R_sql = new HashSet<>(); // set of SQL unfoldings
		for (int i = 0; i < R.size(); i++) {
			Resolvent r = R.get(i);
			String rew_q = r.unfold(M); // unf(q,M,C,U)
			R_sql.add(rew_q);

			System.out.println("\nUnfolding " + (i + 1) + ":");
			System.out.print(r);
			System.out.println("Rewriting:\n" + rew_q);
		}
		System.out.println();

		ArrayList<String> R_sql_list = new ArrayList<String>(R_sql);
		Collections.sort(R_sql_list);
		return concatenateBooleanSqlRewritings(R_sql_list, q); // produce unf(q,M) and return
	}

	/**
	 * Given the set {unf(q,M)} for all disjuncts q of a UCQ Q, produce unf(Q,M), i.e., 
	 * the unfolding of UCQ Q wrt M.
	 * @param rewritings List of unfoldings unf(q,M)
	 * @param q_arity The arity of Q
	 * @return
	 */
	private static String concatenateBooleanSqlRewritings(List<String> rewritings, SparqlUCQ q) {
		
		if (rewritings.size() == 0) return "";

		// SELECT clause
		String select = "SELECT ";
		if (q.arity() == 0) // sparql ask query
			select = select.concat("1");
		else { // sparql boolean select
			for (int i = 0; i < q.arity(); i++) {
				String name = q.target().get(i).getName();
				try {
					Integer.parseInt(name);
				} catch (NumberFormatException e) { // not an Integer
					try {
						Float.parseFloat(name);
					} catch (NumberFormatException e2) { // not a Float
						name = "'"+name+"'";
					}
				}
				select = select.concat(name);
				select = (i == q.arity() - 1) ? select.concat(" ") : select.concat(", ");
			}
		}
		select = select.concat(System.getProperty("line.separator"));

		// FROM clause
		String from = "FROM ".concat(System.getProperty("line.separator")), from_arg = rewritings.get(0).concat(" CQ1 ");
		
		if(rewritings.size() == 1) {
			from_arg = "(" + from_arg + ")";
		}
		else {
			for (int i = 1; i < rewritings.size(); i++)
				from_arg = "(" + from_arg
					+ System.getProperty("line.separator")
					+ "UNION"
					+ System.getProperty("line.separator")
					+ rewritings.get(i) + " CQ"+(i+1) + ")";
		}

		return select.concat(from).concat(from_arg).concat(" UCQ1;");
	}

	/**
	 * Given the set unf(q,M,C,U) for all possible C,U, produce unf(q,M), i.e., 
	 * the set of all unfoldings of q wrt M.
	 * @param rewritings unf(q,M,C,U)
	 * @param q_arity The arity of q
	 * @return
	 */
	private static String concatenateBooleanSqlRewritings(List<String> rewritings, SparqlCQ q) {
		
		if (rewritings.size() == 0) return "";
		
		// SELECT clause
		String select = "SELECT ";
		if (q.arity() == 0) // sparql ask query
			select = select.concat("1");
		else { // sparql boolean select
			for (int i = 0; i < q.arity(); i++) {
				String name = q.target().get(i).getName();
				try {
					Integer.parseInt(name);
				} catch (NumberFormatException e) { // not an Integer
					try {
						Float.parseFloat(name);
					} catch (NumberFormatException e2) { // not a Float
						name = "'"+name+"'";
					}
				}
				select = select.concat(name);
				select = (i == q.arity() - 1) ? select.concat(" ") : select.concat(", ");
			}
		}
		select = select.concat(System.getProperty("line.separator"));
		
		// FROM clause
		String from = "FROM ".concat(System.getProperty("line.separator")), from_arg = rewritings.get(0);
		if(rewritings.size() == 1) from_arg = "(" + from_arg + ")";
		else {
			for (int i = 1; i < rewritings.size(); i++)
				from_arg = "(" + from_arg
					.concat(System.getProperty("line.separator"))
					.concat("UNION")
					.concat(System.getProperty("line.separator"))
					.concat(rewritings.get(i)) + ")";
		}

		return select.concat(from).concat(from_arg);
	}

	
	/**
	 * Lists all possible subsets of the set consisting of the first n integers.
	 * @param n
	 * @return 
	 */
	private static List<List<Integer>> genSubsets(int n) {
		List<List<Integer>> cs = new ArrayList<>();
		for (int i = 0; i < (1 << n); i++) {
			List<Integer> c = new ArrayList<>();
			for (int j = 0; j < n; j++)
				if ((i & (1 << j)) > 0)
					c.add(j);
			if (!c.isEmpty())
				cs.add(c);
		}
		return cs;
	}
	
	private static List<Resolvent> genResolvents(List<List<Integer>> C, SparqlCQ q) {
		List<Resolvent> Rs_C = new ArrayList<>();

		if (C.size() == 0)
			return Rs_C;

		if (C.size() == 1) {
			List<Integer> c = C.remove(0);
			Set<Unifier> us_c = C_MGUs.get(c);
			for (Unifier u_c : us_c) {
				Resolvent r = new Resolvent(q);
				r.add(c, u_c);
				Rs_C.add(r);
			}
			return Rs_C;
		}

		List<Integer> c = C.remove(0);
		List<Resolvent> aux = genResolvents(C, q);

		Set<Unifier> us_c = C_MGUs.get(c);
		for (Resolvent r : aux) {
			for (Unifier u_c : us_c) {
				Resolvent r_copy = new Resolvent(r);
				r_copy.add(c, u_c);
				Rs_C.add(r_copy);
			}
		}

		return Rs_C;
	}

	public static List<List<List<Integer>>> genPartitions(int n) {
		List<Integer> todo = new ArrayList<>();
		for (int i = 0; i < n; i++)
			todo.add(i);
		return auxGenPartitions(todo);
	}

	private static List<List<List<Integer>>> auxGenPartitions(List<Integer> todo) {
		List<List<List<Integer>>> res = new ArrayList<>();

		if (todo.isEmpty())
			return res;

		if (todo.size() == 1) {
			res.add(new ArrayList<>()); // create list B ∈ A
			(res.get(0)).add(new ArrayList<>()); // create list C ∈ B
			res.get(0).get(0).add(todo.remove(0)); // add todo[0] to C
			return res;
		}

		Integer e = todo.remove(todo.size() - 1);
		List<List<List<Integer>>> A = auxGenPartitions(todo);

		for (List<List<Integer>> B : A) {
			for (List<Integer> C : B) {
				List<List<Integer>> B_copy = copy_of(B);
				B_copy.remove(C);
				List<Integer> C_copy = new ArrayList<>(C);
				C_copy.add(e);
				B_copy.add(C_copy);
				res.add(B_copy);
			}
			// single partition
			List<List<Integer>> B_copy = copy_of(B);
			List<Integer> C = new ArrayList<>();
			C.add(e);
			B_copy.add(C);
			res.add(B_copy);
		}

		return res;
	}

	private static List<List<Integer>> copy_of(List<List<Integer>> B) {
		List<List<Integer>> res = new ArrayList<>();
		for (List<Integer> C : B) {
			res.add(new ArrayList<>(C));
		}
		return res;
	}

	
	/**
	 * Check that all predicates always have the same arity.
	 * @param M
	 * @param q
	 * @throws RewriteException
	 */
	private static void checkArities(List<Assertion> M, SparqlCQ q) throws RewriteException {
		Map<Node, Boolean> arityMap = new HashMap<>();
		List<SparqlCQ> L = new LinkedList<>();

		for (Assertion m : M)
			L.add(m.getHead());
		L.add(q);

		for (SparqlCQ l : L) {
			for (Atom a : l.atoms()) {
				if (!arityMap.containsKey(a.getPredicate()))
					arityMap.put(a.getPredicate(), a.isUnary());
				else if (!arityMap.get(a.getPredicate()).equals(a.isUnary()))
					throw new RewriteException("" + a.getPredicate() + " arity is inconsistent.");
			}
		}
	}

	/**
	 * Check that all predicates in q are also in M
	 * 
	 * @param M
	 * @param q
	 * @throws RewriteException
	 */
	private static void checkPredicates(List<Assertion> M, SparqlCQ q) throws RewriteException {
		Set<Node> preds = new HashSet<>();

		// initialize predicates' vocabulary
		for (int i = 0; i < M.size(); i++)
			preds.addAll(M.get(i).getHead().predicates());

		if (!preds.containsAll(q.predicates())) {
			throw new RewriteException("Some predicate in query is not defined in any mapping assertion.");
		}
	}
	
	
	private static List<Assertion> renameVars(List<Assertion> M) throws RewriteException {
		for (int i = 0; i < M.size(); i++) { // rename mapping
			SparqlCQ h = renameVars(M.get(i).getHead()); // head
			ZQuery b = renameVars(M.get(i).getBody()); // body (call after renameVars of the head)
			M.set(i, new Assertion(b, h));
		}
		return M;
	}

	private static SparqlCQ renameVars(SparqlCQ q) throws RewriteException {
		renaming_dict = new TreeMap<>(); // reset dictionary

		for (Atom a : q.atoms()) { // rename query body
			Node s = a.getSubject(); // subject
			Node o = a.getObject(); // object

			if (s.isVariable()) {
				if (!(renaming_dict.containsKey(s.getName())))
					renaming_dict.put(s.getName(), "v".concat("" + (++renaming_n)));
				s.setName(renaming_dict.get(s.getName()));
			}

			if (!a.isUnary() && o.isVariable()) {
				if (!(renaming_dict.containsKey(o.getName())))
					renaming_dict.put(o.getName(), "v".concat("" + (++renaming_n)));
				o.setName(renaming_dict.get(o.getName()));
			}
		}
		
		for (Node n : q.target()) { // rename query head
			if (n.isVariable()) {
				if (!(renaming_dict.containsKey(n.getName())))
					renaming_dict.put(n.getName(), "v".concat("" + (++renaming_n)));
				n.setName(renaming_dict.get(n.getName()));
			}
		}
		
		return q;
	}

	private static ZQuery renameVars(ZQuery q) {
		for (int i = 0; i < q.getSelect().size(); i++) {
			ZSelectItem att = (ZSelectItem) q.getSelect().get(i);
			if (!(renaming_dict.containsKey(att.getAlias())))
				renaming_dict.put(att.getAlias(), "v".concat("" + (++renaming_n)));
			att.setAlias(renaming_dict.get(att.getAlias()));
		}
		return q;
	}

	
	/**
	 * Finds the set U of all unifiers u such that ∀d ∈ h : u(q[c]) = u(h[d])
	 * @param c A chunk in q
	 * @param q A SparqlCQ instance (the user query)
	 * @param n Assertion number of h in M
	 * @param h A SparqlCQ instance (a mapping assertion0s head)
	 * @return
	 */
	private static Set<Unifier> findMGUs(List<Integer> c, SparqlCQ q, int n, SparqlCQ h) {
		Set<Unifier> MGUs_c_h = new HashSet<>();
		
		// get all predicates in chunk c
		Set<Node> predicatesC = new HashSet<>();
		for(Integer i : c) {
			predicatesC.add(q.atoms().get(i).getPredicate());
		}

		// take from h all indexes of atoms whose predicate is in c
		List<Integer> indexesH = new ArrayList<>();
		for(int i=0; i<h.atoms().size(); i++)
			if(predicatesC.contains(h.atoms().get(i).getPredicate()))
				indexesH.add(i);
		
		// for each subset d of indexesH
		for (List<Integer> d_ind : genSubsets(indexesH.size())) {
			List<Integer> d = new ArrayList<>();
			for(Integer i : d_ind) d.add(indexesH.get(i));
			
//			System.out.println("Query chunk: " + c);
//			System.out.println("Head chunk: " + d);
			
			// for all possible substitutions between c and d (partitions of the set of variables)
			List<Atom> atomsC = new ArrayList<>();
			List<Atom> atomsD = new ArrayList<>();
			Set<Node> termsC = new HashSet<>();
			Set<Node> termsD = new HashSet<>();
			List<Node> terms = new ArrayList<>();
			
			for(Integer i : c) {
				Atom a = q.atoms().get(i);
				atomsC.add(a);
				termsC.add(a.getSubject());
				if(!a.isUnary()) termsC.add(a.getObject());
			}
			for(Integer i : d) {
				Atom a = h.atoms().get(i);
				atomsD.add(a);
				termsD.add(a.getSubject());
				if(!a.isUnary()) termsD.add(a.getObject());
			}
			
			terms.addAll(termsC);
			terms.addAll(termsD);
			
			
//			System.out.println(terms);
			
			List<List<List<Integer>>> termsPartitions = genPartitions(terms.size());
			Unifier mgu = null;
			
			// for each substitution between d and c (only one will be the MGU)
			for(List<List<Integer>> termsPartition : termsPartitions) {
//				System.out.println(termsPartition);
				Unifier u = new Unifier(c, q, n, d, h);
				
				// obtain the corresponding substitution
				for(List<Integer> equivalenceGroup : termsPartition) {
//					System.out.println(equivalenceGroup);
					for(int i=1; i<equivalenceGroup.size(); i++) {
//						System.out.println("Entered: " + terms.get(equivalenceGroup.get(i)) + ", " 
//								+ terms.get(equivalenceGroup.get(i-1)));
						u.add(terms.get(equivalenceGroup.get(i)), terms.get(equivalenceGroup.get(i-1))); // add terms equivalence
					}
				}
				
//				System.out.println(u);
				
				// is u a unifier for c-d ?
				if(u.isValid() && u.moreGeneralThan(mgu)) mgu = u;
//				System.out.println();
			}
			
			if(mgu != null) MGUs_c_h.add(mgu);
		}
		
		return MGUs_c_h;
	}

}