package edu.utexas.cgrex.automaton;

import java.util.*;

public class CGAutoState extends AutoState {
	// the incoming edges correspond to the same method, i.e., this.Id
	// so we do not use map, instead, we just use set
	protected Set<AutoState> incomingStates = new HashSet<AutoState>();

	protected AutoEdge incomingEdge;

	/* fields to handle SCC when annotating */
	protected boolean belongsToASCC = false;

	// only when belongsToASCC == true will we update the following sets?
	protected Set<AutoState> statesInTheSameSCC;

	protected Set<AutoEdge> edgesInTheSameSCC;

	protected Set<AutoState> outgoingStatesOfSCC;

	protected Set<AutoEdge> outgoingEdgesOfSCC;

	public void setBelongsToASCC() {
		belongsToASCC = true;
	}

	public boolean isBelongsToASCC() {
		return belongsToASCC;
	}

	/** setters */
	public void setStatesInTheSameSCC(Set<AutoState> statesInTheSameSCC) {
		this.statesInTheSameSCC = statesInTheSameSCC;
	}

	public void setEdgesInTheSameSCC(Set<AutoEdge> edgesInTheSameSCC) {
		this.edgesInTheSameSCC = edgesInTheSameSCC;
	}

	public void setOutgoingStatesOfSCC(Set<AutoState> outgoingStatesOfSCC) {
		this.outgoingStatesOfSCC = outgoingStatesOfSCC;
	}

	public void setOutgoingEdgesOfSCC(Set<AutoEdge> outgoingEdgesOfSCC) {
		this.outgoingEdgesOfSCC = outgoingEdgesOfSCC;
	}

	/** getters */
	public Set<AutoState> getStatesInTheSameSCC() {
		return statesInTheSameSCC;
	}

	public Set<AutoEdge> getEdgesInTheSameSCC() {
		return edgesInTheSameSCC;
	}

	public Set<AutoState> getOutgoingStatesOfSCC() {
		return outgoingStatesOfSCC;
	}

	public Set<AutoEdge> getOutgoingEdgesOfSCC() {
		return outgoingEdgesOfSCC;
	}

	/** non-SCC related */
	public CGAutoState(Object id, boolean isInitState, boolean isFinalState) {
		super(id, isInitState, isFinalState);
		this.incomingEdge = null;
	}

	@Override
	public Set<AutoState> getIncomingStatesKeySet() {
		return incomingStates;
	}

	@Override
	public Set<AutoEdge> getIncomingStatesInvKeySet() {
		Set<AutoEdge> inv = new HashSet<AutoEdge>();
		inv.add(incomingEdge);
		return inv;
	}

	// @Override
	public AutoEdge getIncomingEdge() {
		return incomingEdge;
	}

	/*
	 * @Override public void setIncomingStates(Map<AutoState, Set<AutoEdge>>
	 * incomingStates) { }
	 */

	// @Override
	public void setIncomingStates(Set<AutoState> incomingStates) {
		this.incomingStates = incomingStates;
	}

	/*
	 * @Override public void setIncomingStatesInv( Map<AutoEdge, Set<AutoState>>
	 * incomingStatesInv) { }
	 */

	// @Override
	public void setIncomingStatesInv(AutoEdge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}

	@Override
	public Iterator<AutoState> incomingStatesIterator() {
		return incomingStates.iterator();
	}

	@Override
	public Iterator<AutoEdge> incomingStatesInvIterator() {
		Set<AutoEdge> inv = new HashSet<AutoEdge>();
		inv.add(incomingEdge);
		return inv.iterator();
	}

	/*
	 * @Override public Map<AutoState, Set<AutoEdge>> getIncomingStates() {
	 * return null; }
	 */

	/*
	 * @Override public Map<AutoEdge, Set<AutoState>> getIncomingStatesInv() {
	 * return null; }
	 */

	@Override
	public Set<AutoEdge> incomingStatesLookup(AutoState state) {
		Set<AutoEdge> in = new HashSet<AutoEdge>();
		in.add(incomingEdge);
		return in;
	}

	@Override
	public Set<AutoState> incomingStatesInvLookup(AutoEdge edge) {
		return incomingStates;
	}

	// this ensures that you must add the correct edge
	@Override
	public boolean addIncomingStates(AutoState state, AutoEdge edge) {
		if (edge.equals(incomingEdge)) {
			incomingEdge = edge;
			return incomingStates.add(state);
		} else {
			return false;
		}
		// return incomingEdge.equals(edge) ? incomingStates.add(state) : false;
	}

	// the following four delete methods can update the related SCC at the same
	// time, so you do not need to worry about SCC too much
	@Override
	public boolean deleteOneIncomingState(AutoState state) {
		boolean succ = incomingStates.remove(state); // try removing
		if (incomingStates.isEmpty())
			incomingEdge = null; // if no incoming state, set edge to be null

		return succ;
	}

	@Override
	public boolean deleteOneIncomingEdge(AutoEdge edge) {
		if (!edge.equals(incomingEdge))
			return false;
		incomingEdge = null;
		incomingStates.clear();
		return true;
	}

	@Override
	public boolean deleteOneIncomingEdge(AutoState state, AutoEdge edge) {
		if (!edge.equals(incomingEdge))
			return false;
		boolean succ = incomingStates.remove(state);
		if (incomingStates.isEmpty())
			incomingEdge = null;
		return succ;
	}

	@Override
	public boolean isIsolated() {
		return incomingStates.isEmpty() && incomingEdge == null
				&& outgoingStates.isEmpty() && outgoingStatesInv.isEmpty();
	}

	@Override
	public boolean deleteOneOutgoingState(AutoState state) {
		Set<AutoEdge> edgeList = new HashSet<AutoEdge>();
		// maintain a shallow copy of the edges to be removed
		if (outgoingStates.containsKey(state))
			edgeList.addAll(outgoingStates.get(state));

		// take care of the outgoing maps
		boolean succ = super.deleteOneOutgoingState(state);
		if (!succ) // if not successful, meaning no such state to remove
			return false;

		// then take care of the related SCCs
		for (AutoEdge edge : edgeList)
			succ = succ | deleteFromSCC(state, edge);

		return succ;
	}

	@Override
	public boolean deleteOneOutgoingEdge(AutoEdge edge) {
		Set<AutoState> stateList = new HashSet<AutoState>();
		// maintain a shallow copy of the states to be removed
		if (outgoingStatesInv.containsKey(edge))
			stateList.addAll(outgoingStatesInv.get(edge));

		// take care of the outgoing maps
		boolean succ = super.deleteOneOutgoingEdge(edge);
		super.deleteOneOutgoingEdge(edge);
		if (!succ)
			return false;

		// then take care of the related SCCs
		for (AutoState state : stateList)
			succ = succ | deleteFromSCC(state, edge);

		return succ;
	}

	@Override
	public boolean hasCycleEdge() {
		return incomingStates != null && incomingStates.contains(this);
	}

	public void setIncomingEdge(AutoEdge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}

	protected boolean deleteFromSCC(AutoState state, AutoEdge edge) {
		return statesInTheSameSCC.remove(state)
				| edgesInTheSameSCC.remove(edge)
				| outgoingStatesOfSCC.remove(state)
				| outgoingEdgesOfSCC.remove(edge);
	}
}
