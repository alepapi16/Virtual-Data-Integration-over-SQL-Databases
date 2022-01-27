package it.uniroma1.diag;

import java.util.Objects;

public class Node {

	private org.apache.jena.graph.Node n;
	
	private String name;
	private NodeType type;
	
	
	public Node(org.apache.jena.graph.Node n) {
		this.n = n;
		
		if(n.isVariable()) type = NodeType.VARIABLE;
		else if(n.isLiteral()) type = NodeType.CONSTANT;
		else type = NodeType.PREDICATE;
		
		switch(type) {
		case VARIABLE: name = n.getName(); 
			break;
		case CONSTANT: name =  n.getLiteralLexicalForm();
			break;
		case PREDICATE: name =  n.getLocalName();
			break;
		}
	}

	public Node(String name, NodeType type) {
		super();
		this.name = name;
		this.type = type;
	}
	
	
	public boolean isVariable() {
		return type == NodeType.VARIABLE;
	}
	
	public boolean isConstant() {
		return type == NodeType.CONSTANT;
	}
	
	public boolean isPredicate() {
		return type == NodeType.PREDICATE;
	}
	

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getOriginalName() {
		switch(type) {
			case VARIABLE: return n.getName();
			case CONSTANT: return n.getLiteralLexicalForm();
			case PREDICATE: return n.getLocalName();
			default: return null;
		}
	}
	
	
	@Override
	public int hashCode() {
		return Objects.hash(name,type);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof Node)) return false;
		final Node o = (Node) obj;
		return type.equals(o.type) && name.equals(o.name);
	}
	
	@Override
	public String toString() {
		return getOriginalName();
	}
	
}
