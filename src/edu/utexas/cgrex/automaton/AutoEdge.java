package edu.utexas.cgrex.automaton;

public class AutoEdge {
	// id for Edge class is a
	// SootMethod for call graph finite state machine
	// String for regular expression finite state machine
	protected Object id;

	protected boolean isDotEdge = false;
	
	//name displayed in the graph.
	protected String shortName = "";

	public AutoEdge(Object id) {
		this.id = id;
		this.isDotEdge = false;
		shortName = (String) id;
	}
	
	public AutoEdge(Object id, boolean isDotEdge) {
		this.id = id;
		this.isDotEdge = isDotEdge;
	}

	public Object getId() {
		return id;
	}

	public boolean isDotEdge() {
		return isDotEdge;
	}
	
	public String getShortName() { 
		return shortName;
	}
	
	public void setShortName(String name) { 
		shortName = name; 
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof AutoEdge)
				&& (id.equals(((AutoEdge) other).getId()) ? true : false);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
