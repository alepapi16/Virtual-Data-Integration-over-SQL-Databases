package it.uniroma1.diag;

import org.gibello.zql.ZQuery;

public class Assertion {
	private ZQuery body;
	private SparqlCQ head;
	
	
	public Assertion(ZQuery body, SparqlCQ head) {
		super();
		this.body = body;
		this.head = head;
	}

	
	public ZQuery getBody() {
		return body;
	}

	public SparqlCQ getHead() {
		return head;
	}
	
	
	@Override
	public String toString() {
		return body.toString() + System.getProperty("line.separator") + head.toString();
	}
}
