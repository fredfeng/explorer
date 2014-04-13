package edu.utexas.cgrex.automaton;

import java.util.*;

// for cgfsm, each state corresponds to a single method which is the Id
// for refsm, the Id of a state is meaningless, a state does not necessarily
// correspond to a single method, which depends on the specific regular expr

public abstract class AutoState {
	// Id corresponds to the soot method in the call graph
	// initial and final states' Id are null
	protected Object id;

	protected boolean isInitState = false;

	protected boolean isFinalState = false;

	// Map<State, Set<Edge>>
	protected Map<AutoState, Set<AutoEdge>> outgoingStates = new HashMap<AutoState, Set<AutoEdge>>();

	// Map<Edge, Set<State>>
	protected Map<AutoEdge, Set<AutoState>> outgoingStatesInv = new HashMap<AutoEdge, Set<AutoState>>();

	public AutoState(Object id, boolean isInitState, boolean isFinalState) {
		this.id = id;
		this.isInitState = isInitState;
		this.isFinalState = isFinalState;
	}

	public Object getId() {
		return id;
	}

	public void setInitState() {
		isInitState = true;
	}

	public boolean isInitState() {
		return isInitState;
	}

	public void setFinalState() {
		isFinalState = true;
	}

	public boolean isFinalState() {
		return isFinalState;
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof AutoState)
				&& (id.equals(((AutoState) other).id) ? true : false);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	public abstract Set<AutoState> getIncomingStatesKeySet();
	
	public abstract Set<AutoEdge> getIncomingStatesInvKeySet(); 
	
	//public abstract void setIncomingStates(Map<AutoState, Set<AutoEdge>> incomingStates);
		
	//public abstract void setIncomingStates(Set<AutoState> incomingStates);
	
	//public abstract void setIncomingStatesInv(Map<AutoEdge, Set<AutoState>> incomingStatesInv);
	
	//public abstract void setIncomingStatesInv(AutoEdge incomingEdge);
	
	public abstract Iterator<AutoState> incomingStatesIterator();
	
	public abstract Iterator<AutoEdge> incomingStatesInvIterator();
		
	//public abstract Map<AutoState, Set<AutoEdge>> getIncomingStates();
	
	//public abstract Map<AutoEdge, Set<AutoState>> getIncomingStatesInv();
	
	//public abstract AutoEdge getIncomingEdge();
	
	public abstract Set<AutoEdge> incomingStatesLookup(AutoState state); 

	public abstract Set<AutoState> incomingStatesInvLookup(AutoEdge edge);
	
	public abstract boolean addIncomingStates(AutoState state, AutoEdge edge);
		
	public Set<AutoState> getOutgoingStatesKeySet() {
		return outgoingStates.keySet();
	}

	public Set<AutoEdge> getOutgoingStatesInvKeySet() {
		return outgoingStatesInv.keySet();
	}
	
	public boolean addOutgoingStates(AutoState state, AutoEdge edge) {
		return addToMap(outgoingStates, state, edge)
				| addToInvMap(outgoingStatesInv, edge, state);
	}

	public void setOutgoingStates(Map<AutoState, Set<AutoEdge>> outgoingStates) {
		this.outgoingStates = outgoingStates;
	}

	public void setOutgoingStatesInv(
			Map<AutoEdge, Set<AutoState>> outgoingStatesInv) {
		this.outgoingStatesInv = outgoingStatesInv;
	}

	public Iterator<AutoState> outgoingStatesIterator() {
		return outgoingStates.keySet().iterator();
	}

	public Iterator<AutoEdge> outgoingStatesInvIterator() {
		return outgoingStatesInv.keySet().iterator();
	}

	public Map<AutoState, Set<AutoEdge>> getOutgoingStates() {
		return outgoingStates;
	}

	public Map<AutoEdge, Set<AutoState>> getOutgoingStatesInv() {
		return outgoingStatesInv;
	}

	public Set<AutoEdge> outgoingStatesLookup(AutoState state) {
		return outgoingStates.get(state);
	}

	public Set<AutoState> outgoingStatesInvLookup(AutoEdge edge) {
		return outgoingStatesInv.get(edge);
	}

	public boolean hasOnlyOneOutgoingEdge() {
		return outgoingStatesInv.size() == 1;
	}

	public boolean hasNoOutgoingState() {
		return outgoingStates.isEmpty();
	}

	public AutoEdge getOnlyOneOutgoingEdge() {
		if (outgoingStatesInv.size() != 1)
			return null;
		for (AutoEdge e : outgoingStatesInv.keySet())
			return e;
		return null;
	}

	public boolean hasCycleEdge() {
		for (AutoState s : outgoingStates.keySet()) {
			if (s.equals(this))
				return true;
		}
		return false;
	}

	/** protected methods */
	protected boolean addToMap(Map<AutoState, Set<AutoEdge>> m,
			AutoState state, AutoEdge edge) {
		Set<AutoEdge> edgeList = m.get(state);

		if (edgeList == null) {
			m.put(state, edgeList = new HashSet<AutoEdge>(1));
		}

		return edgeList.add(edge);
	}

	protected boolean addToInvMap(Map<AutoEdge, Set<AutoState>> m,
			AutoEdge edge, AutoState state) {
		Set<AutoState> stateList = m.get(edge);

		if (stateList == null) {
			m.put(edge, stateList = new HashSet<AutoState>(1));
		}

		return stateList.add(state);
	}

	@Override
	public String toString() {
		return id.toString();
	}

}
