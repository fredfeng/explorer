package test.util;

import java.util.*;

public class refsmState extends State {
	// in the regular expression fsm, it is possible that
	// one state has different multiple incoming edges
	protected Map<Object, Object> incomingStates = new HashMap<Object, Object>(); // Map<State, Set<Edge>>
	protected Map<Object, Object> incomingStatesInv = new HashMap<Object, Object>(); // Map<Edge, Set<State>>

	public refsmState(Id id, boolean isInitState, boolean isFinalState) {
		super(id, isInitState, isFinalState);
	}

	public boolean addIncomingStates(Object state, Object edge) {
		return addToMap(incomingStates, state, edge) | addToMap(incomingStates, edge, state);
	}
	
	/** map copy */
	public void setIncomingStates(Map<Object, Object> incomingStates) { 
		this.incomingStates = incomingStates;
	}
	public void setIncomingStatesInv(Map<Object, Object> incomingStatesInv) {
		this.incomingStatesInv = incomingStatesInv;
	}
	
	public Iterator<Object> incomingStatesIterator() { return incomingStates.keySet().iterator(); }
	public Iterator<Object> incomingStatesInvIterator() { return incomingStatesInv.keySet().iterator(); }
	
	public Set<Object> incomingStates() { return incomingStates.keySet(); }
	public Set<Object> incomingStatesInv() { return incomingStatesInv.keySet(); }
	
	public Set<Object> incomingStatesLookup(State key) 
	{ return lookup(incomingStates, key); }
	public Set<Object> incomingStatesInvLookup(Edge key)
	{ return lookup(incomingStatesInv, key); }
	
	public boolean hasOutgoingDotEdge() {
		for (Object e: outgoingStatesInv.keySet()) {
			Edge eg = (Edge) e;
			if (eg.isDot()) 
				return true;
		}
		return false;
	}
	
	public boolean hasOnlyOneDotOutgoingEdge() {
		if (outgoingStatesInv.keySet().size() != 1) {
			return false;
		}
		for (Object e : outgoingStatesInv.keySet()) {
			Edge eg = (Edge) e;
			if (!eg.isDot()) 
				return false;
		}
		return true;
	}
	
	@Override
	public boolean hasCycleEdge() 
	{ return incomingStates != null && incomingStates.keySet().contains(this); }
}
