package test.util;

import java.util.*;

public class cgfsmState extends State {
	// the incoming edges correspond to the same method, i.e., this.Id
	// so we do not use map, instead, we just use set
	protected Set<Object> incomingStates = new HashSet<Object>(); // Set<State>
	protected Edge incomingEdge; 
	
	public cgfsmState(Id id, boolean isInitState, boolean isFinalState, Edge incomingEdge) {
		super(id, isInitState, isFinalState);
		this.incomingEdge = incomingEdge;
	}
	
	public void setIncomingEdge(Edge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}
	
	public Edge getIncomingEdge() { return incomingEdge; }
	
	public boolean addIncomingStates(Object state) {
		return incomingStates.add(state);
	}
}
