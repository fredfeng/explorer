package edu.utexas.cgrex.automaton;

import java.util.*;

public class InterAutoState extends AutoState {
	protected AutoState masterState;
	protected AutoState slaveState;
	protected Map<AutoState, Set<AutoEdge>> incomingStates = new HashMap<AutoState, Set<AutoEdge>>();
	protected Map<AutoEdge, Set<AutoState>> incomingStatesInv = new HashMap<AutoEdge, Set<AutoState>>();

	public InterAutoState(Object id, AutoState masterState,
			AutoState slaveState, boolean isInitState, boolean isFinalState) {
		super(id, isInitState, isFinalState);
		this.masterState = masterState;
		this.slaveState = slaveState;
	}

	public InterAutoState(Object id, AutoState masterState, AutoState slaveState) {
		super(id, false, false);
		this.masterState = masterState;
		this.slaveState = slaveState;
	}
	
	@Override
	public Set<AutoState> getIncomingStatesKeySet() {
		return incomingStates.keySet();
	}

	@Override
	public Set<AutoEdge> getIncomingStatesInvKeySet() {
		return incomingStatesInv.keySet();
	}
	
	//@Override
	public void setIncomingStates(Map<AutoState, Set<AutoEdge>> incomingStates) {
		this.incomingStates = incomingStates;
	}

	/*
	@Override
	public void setIncomingStates(Set<AutoState> incomingStates) {
	}
	*/
	
	//@Override
	public void setIncomingStatesInv(
			Map<AutoEdge, Set<AutoState>> incomingStatesInv) {
		this.incomingStatesInv = incomingStatesInv;
	}
	
	/*
	@Override
	public void setIncomingStatesInv(AutoEdge incomingEdge) {
	}
	*/

	@Override
	public Iterator<AutoState> incomingStatesIterator() {
		return incomingStates.keySet().iterator();
	}

	@Override
	public Iterator<AutoEdge> incomingStatesInvIterator() {
		return incomingStatesInv.keySet().iterator();
	}

	//@Override
	public Map<AutoState, Set<AutoEdge>> getIncomingStates() {
		return incomingStates;
	}
	
	//@Override
	public Map<AutoEdge, Set<AutoState>> getIncomingStatesInv() {
		return incomingStatesInv;
	}

	/*
	@Override
	public AutoEdge getIncomingEdge() {
		return null;
	}
	*/
	
	@Override
	public Set<AutoEdge> incomingStatesLookup(AutoState state) {
		return incomingStates.get(state);
	}

	@Override
	public Set<AutoState> incomingStatesInvLookup(AutoEdge edge) {
		return incomingStatesInv.get(edge);
	}

	@Override
	public boolean addIncomingStates(AutoState state, AutoEdge edge) {
		return addToMap(incomingStates, state, edge)
				| addToInvMap(incomingStatesInv, edge, state);
	}
	
	public boolean buildAsInitial() {
		return masterState.isInitState && slaveState.isFinalState;
	}

	public boolean buildAsFinal() {
		return masterState.isFinalState;
	}

	public AutoState getMasterState() {
		return masterState;
	}

	public AutoState getSlaveState() {
		return slaveState;
	}
	
	public boolean hasMasterState(AutoState other) {
		return masterState.equals(other);
	}

	public boolean hasSlaveState(AutoState other) {
		return slaveState.equals(other);
	}

	public boolean hasMasterandSlaveState(AutoState master, AutoState slave) {
		return hasMasterState(master) && hasSlaveState(slave);
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof InterAutoState
				&& (masterState.equals(((InterAutoState) other).masterState) && slaveState
						.equals(((InterAutoState) other).slaveState)) ? true
				: false;
	}

	@Override
	public int hashCode() {
		return 37 * masterState.hashCode() + slaveState.hashCode();
	}


}
