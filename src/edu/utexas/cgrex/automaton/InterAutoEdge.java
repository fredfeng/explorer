package edu.utexas.cgrex.automaton;

public class InterAutoEdge extends AutoEdge {

	// this is the constructor used to create new InterAutoEdge when building
	// the interAutomaton
	// I just specify the id of this edge and set the isDotEdge to be false
	public InterAutoEdge(Object id) {
		super(id, false);
	}

}
