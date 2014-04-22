package edu.utexas.cgrex.automaton;

public class InterAutoEdge extends AutoEdge {

	// this is the constructor used to create new InterAutoEdge when building
	// the interAutomaton
	// I just specify the id of this edge and set the isDotEdge to be false
	public InterAutoEdge(Object id) {
		super(id, false);
	}

	// X@Y$Z@W
	// this method returns Z (the Id of the corresponding master RegAutoState of
	// the target of this edge)
	public String getTgtRegStateId() {
		String[] Ids = id.toString().split("$");
		String[] TgtIds = Ids[1].split("@");
		return TgtIds[0];
	}

	// this method returns W (the Id of the corresponding slave CGAutoState of
	// the target of this edge)
	public String getTgtCGAutoStateId() {
		String[] Ids = id.toString().split("@");
		return Ids[2];
	}

	// this method returns X (the Id of the corresponding master RegAutoState of
	// the source of this edge)
	public String getSrcRegAutoStateId() {
		String[] Ids = id.toString().split("@");
		return Ids[0];
	}

	// this method returns Y (The Id of the corresponding master CGAutoState of
	// the source of this edge)
	public String getSrcCGAutoStateId() {
		String[] Ids = id.toString().split("$");
		String[] SrcIds = Ids[0].split("@");
		return SrcIds[1];
	}

}
