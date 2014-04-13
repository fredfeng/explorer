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
	
	
	
	
	
	
	
	public CGAutoState(Object id, boolean isInitState, boolean isFinalState,
			AutoEdge incomingEdge) {
		super(id, isInitState, isFinalState);
		this.incomingEdge = incomingEdge;
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
	
	//@Override
	public AutoEdge getIncomingEdge() {
		return incomingEdge;
	}
	
	/*
	@Override
	public void setIncomingStates(Map<AutoState, Set<AutoEdge>> incomingStates) {
	}
	*/

	//@Override
	public void setIncomingStates(Set<AutoState> incomingStates) {
		this.incomingStates = incomingStates;
	}
	
	/*
	@Override
	public void setIncomingStatesInv(
			Map<AutoEdge, Set<AutoState>> incomingStatesInv) {
	}
	*/

	//@Override
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
	@Override
	public Map<AutoState, Set<AutoEdge>> getIncomingStates() {
		return null;
	}
	*/

	/*
	@Override
	public Map<AutoEdge, Set<AutoState>> getIncomingStatesInv() {
		return null;
	}
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
	
	@Override
	public boolean addIncomingStates(AutoState state, AutoEdge edge) {
		return incomingEdge.equals(edge) ? incomingStates.add(state) : false;
	}

	@Override
	public boolean hasCycleEdge() {
		return incomingStates != null && incomingStates.contains(this);
	}

	public void setIncomingEdge(AutoEdge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}
}
