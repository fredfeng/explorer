package edu.utexas.cgrex.automaton;

public class SrcTgtEdgeWrapper {

	InterAutoState srcState;

	InterAutoState tgtState;

	InterAutoEdge edge;

	public SrcTgtEdgeWrapper(InterAutoState srcState, InterAutoState tgtState,
			InterAutoEdge edge) {
		this.srcState = srcState;
		this.tgtState = tgtState;
		this.edge = edge;
	}
	
	public InterAutoState getSrcState() {
		return srcState;
	}
	
	public InterAutoState getTgtState() {
		return tgtState;
	}
	
	public InterAutoEdge getEdge() {
		return edge;
	}

}
