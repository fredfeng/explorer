package test.util;

import java.util.*;
import soot.SootMethod;

public class State {
	// Id corresponds to the soot method in the call graph
	// initial and final states' Id are null
	private Id id; 
	private boolean isInitState = false; 
	private boolean isFinalState = false;
	protected Map<Object, Object> outgoingStates; // Map<State, Edge>
	protected Map<Object, Object> outgoingStatesInv; // Map<Edge, State>
	// the incoming edges correspond to the same method, i.e., this.Id
	// so we do not use map, instead, we just use set
	protected Set<Object> incomingStates; // Set<State>
	protected Edge incomingEdge; 
	
	public State(Id id, boolean isInitState, boolean isFinalState) {
		this.id = id;
		this.isInitState = isInitState;
		this.isFinalState = isFinalState;
		this.incomingEdge = new Edge(id);
	}
	
	public void setInitState() { this.isInitState = true; }	
	public void resetInitState() { this.isInitState = false; }
	public void setFinalState() { this.isFinalState = true; }
	public void resetFinalState() { this.isFinalState = false; }
	
	public void setIncomingEdge(Edge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}
	
	public boolean addIncomingStates(Object state) {
		return incomingStates.add(state);
	}
	
	// when setting this outgoingStates
	// first the method M of this state in the original call graph
	// then you can find all the callees of M, say M1, M2, ..., Mn
	// for each Mi, find its representing state Si
	// find its representing edge Ei
	// then use addOutgoingStates(Si, Ei)
	public boolean addOutgoingStates(Object state, Object edge) {
		return addToMap(outgoingStates, state, edge) | addToMap(outgoingStatesInv, edge, state);
	}
	
	/** map copy */
	public void setOutgoingStates(Map<Object, Object> outgoingStates) { 
		this.outgoingStates = outgoingStates;
	}
	public void setOutgoingStatesInv(Map<Object, Object> outgoingStatesInv) {
		this.outgoingStatesInv = outgoingStatesInv;
	}
	
	public Iterator<Object> outgoingStatesIterator() { return outgoingStates.keySet().iterator(); }
	public Iterator<Object> outgoingStatesInvIterator() { return outgoingStatesInv.keySet().iterator(); }
	
	public Set<Object> outgoingStatesSources() { return outgoingStates.keySet(); }
	public Set<Object> outgoingStatesInvSources() { return outgoingStates.keySet(); }
	
	/** protected methods */ 
	protected boolean addToMap(Map<Object, Object> m, Object key, Object value) {
		Object val = m.get(key);
		
		if (val == null) {
			m.put(key, value);
		} else if (value != val) {
			m.put(key, value);
			return false;
		}
		
		return true;
	}
	
	
	
	
	
}
