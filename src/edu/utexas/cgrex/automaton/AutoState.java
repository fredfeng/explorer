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

	private boolean visited = false;

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
	
	/**
	 * @return the visited
	 */
	public boolean isVisited() {
		return visited;
	}

	/**
	 * @param visited the visited to set
	 */
	public void setVisited(boolean visited) {
		this.visited = visited;
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

//	@Override
//	public int hashCode() {
//		return super.hashCode();
//	}

	public abstract Set<AutoState> getIncomingStatesKeySet();

	public abstract Set<AutoEdge> getIncomingStatesInvKeySet();

	// public abstract void setIncomingStates(Map<AutoState, Set<AutoEdge>>
	// incomingStates);

	// public abstract void setIncomingStates(Set<AutoState> incomingStates);

	// public abstract void setIncomingStatesInv(Map<AutoEdge, Set<AutoState>>
	// incomingStatesInv);

	// public abstract void setIncomingStatesInv(AutoEdge incomingEdge);

	public abstract Iterator<AutoState> incomingStatesIterator();

	public abstract Iterator<AutoEdge> incomingStatesInvIterator();

	// public abstract Map<AutoState, Set<AutoEdge>> getIncomingStates();

	// public abstract Map<AutoEdge, Set<AutoState>> getIncomingStatesInv();

	// public abstract AutoEdge getIncomingEdge();

	public abstract Set<AutoEdge> incomingStatesLookup(AutoState state);

	public abstract Set<AutoState> incomingStatesInvLookup(AutoEdge edge);

	public abstract boolean addIncomingStates(AutoState state, AutoEdge edge);

	public abstract boolean deleteOneIncomingState(AutoState state);

	public abstract boolean deleteOneIncomingEdge(AutoEdge edge);
	
	public abstract boolean deleteOneIncomingEdge(AutoState state, AutoEdge edge);
	
	public abstract boolean isIsolated(); // self-cycle is not isolated
	
	// just specify the state
	public boolean deleteOneOutgoingState(AutoState state) {
		Set<AutoEdge> edgeList = new HashSet<AutoEdge>();
		if (outgoingStates.containsKey(state))
			edgeList.addAll(outgoingStates.get(state));
		else
			return false;
		boolean succ = false;
		for (AutoEdge edge : edgeList) {
			succ = succ | deleteFromMap(outgoingStates, state, edge)
					| deleteFromMapInv(outgoingStatesInv, state, edge);
		}
		return succ;
	}

	// just specify the edge
	public boolean deleteOneOutgoingEdge(AutoEdge edge) {
		Set<AutoState> stateList = new HashSet<AutoState>();
		if (outgoingStatesInv.containsKey(edge))
			stateList.addAll(outgoingStatesInv.get(edge));
		else
			return false;
		boolean succ = false;
		for (AutoState state : stateList) {
			succ = succ | deleteFromMap(outgoingStates, state, edge)
					| deleteFromMapInv(outgoingStatesInv, state, edge);
		}
		return succ;
	}

	// specify both the end state and the edge
	public boolean deleteOneOutgoingEdge(AutoState state, AutoEdge edge) {
		return deleteFromMap(outgoingStates, state, edge)
				| deleteFromMapInv(outgoingStatesInv, state, edge);
	}

	public Set<AutoState> getOutgoingStatesKeySet() {
		return outgoingStates.keySet();
	}

	public Set<AutoEdge> getOutgoingStatesInvKeySet() {
		return outgoingStatesInv.keySet();
	}

	public boolean addOutgoingStates(AutoState state, AutoEdge edge) {
		assert(state.getId() != null);
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

	// get the cycle edge of this state
	// if existed, return the cycle edge, else return null
	public AutoEdge getCycleEdge() {
		for (AutoEdge e : outgoingStatesInv.keySet()) {
			if (outgoingStatesInvLookup(e).equals(this))
				return e;
		}
		return null;
	}

	public Set<AutoEdge> getOutgoingStatesInvKeySetExceptCycleEdge() {
		Set<AutoEdge> clone = new HashSet<AutoEdge>();
		clone.addAll(outgoingStatesInv.keySet());
		AutoEdge cycle = getCycleEdge();
		if (cycle != null)
			clone.remove(cycle);
		return clone;
	}

	public boolean hasCycleEdge() {
		for (AutoState s : outgoingStates.keySet()) {
			if (s.equals(this))
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return id.toString();
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

	// delete: this --(edge)-- state from map
	// if existed and deleted, return true, else return false
	protected boolean deleteFromMap(Map<AutoState, Set<AutoEdge>> m,
			AutoState state, AutoEdge edge) {
		Set<AutoEdge> edgeList = m.get(state);

		if (edgeList == null) {
			return false; // this edge does not exist
		} else {
			if (!edgeList.remove(edge)) // try deleting this edge
				return false; // this edge does not exist

			if (edgeList.isEmpty())
				m.remove(state); // remove the item from the map
		}
		return true;
	}

	// delete: this --(state)-- edge from mapinv
	// if existed and deleted, return true, else return false
	protected boolean deleteFromMapInv(Map<AutoEdge, Set<AutoState>> m,
			AutoState state, AutoEdge edge) {
		Set<AutoState> stateList = m.get(edge);

		if (stateList == null) {
			return false; // this state does not exist
		} else {
			if (!stateList.remove(state)) // try deleting this state
				return false; // this state does not exist
			if (stateList.isEmpty())
				m.remove(edge); // remove the item from the invmap
		}

		return true;
	}

}
