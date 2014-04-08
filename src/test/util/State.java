package test.util;

import java.util.*;

import soot.SootMethod;

// for cgfsm, each state corresponds to a single method which is the Id
// for refsm, the Id of a state is meaningless, a state does not necessarily
// correspond to a single method, which depends on the specific regular expr

public class State {
	// Id corresponds to the soot method in the call graph
	// initial and final states' Id are null
	protected Id id; 
	protected boolean isInitState = false; 
	protected boolean isFinalState = false;
	protected Map<Object, Object> outgoingStates = new HashMap<Object, Object>();; // Map<State, Set<Edge>>
	protected Map<Object, Object> outgoingStatesInv = new HashMap<Object, Object>(); // Map<Edge, Set<State>>
	// the incoming edges correspond to the same method, i.e., this.Id
	// so we do not use map, instead, we just use set
	// protected Set<Object> incomingStates = new HashSet<Object>(); // Set<State>
	// protected Edge incomingEdge; 
	
	public State(Id id, boolean isInitState, boolean isFinalState) {
		this.id = id;
		this.isInitState = isInitState;
		this.isFinalState = isFinalState;
		//this.incomingEdge = new Edge(id);
		//this.incomingEdge = incomingEdge;
	}
	
	public Id getId() { return id; }
	
	public void setInitState() { isInitState = true; }	
	public void resetInitState() { isInitState = false; }
	public boolean isInitState() { return isInitState; }
	public void setFinalState() { isFinalState = true; }
	public void resetFinalState() { isFinalState = false; }
	public boolean isFinalState() { return isFinalState; }
	
	public boolean hasOutgoingDotEdge() {
		for (Object e: outgoingStatesInv.keySet()) {
			Edge eg = (Edge) e;
			if (eg.isDot()) 
				return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof State && id.equals( ((State)other).id ) ? true : false;
	}
	
	@Override
	public int hashCode() { return id.hashCode(); }
	
	/*
	public void setIncomingEdge(Edge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}
	
	public Edge getIncomingEdge() { return incomingEdge; }
	
	public boolean addIncomingStates(Object state) {
		return incomingStates.add(state);
	}
	*/
	
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
	
	public Set<Object> outgoingStates() { return outgoingStates.keySet(); }
	public Set<Object> outgoingStatesInv() { return outgoingStatesInv.keySet(); }
	
	public Map getOutgoingStates() { return outgoingStates; }
	public Map getOutgoingStatesInv() { return outgoingStatesInv; }
	
	public boolean hasNoOutgoingState() { return outgoingStates.isEmpty(); }
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
	public boolean hasOnlyOneOutgoingEdge() 
	{ return outgoingStatesInv.size() == 1; }
	public Edge getOnlyOneOutgoingEdge() {
		if (outgoingStatesInv.size() != 1) 
			return null;
		for (Object e: outgoingStatesInv.keySet())
			return (Edge) e;
		return null;
	}
	
	public Set<Object> outgoingStatesLookup(State key) 
	{ return lookup(outgoingStates, key); }
	public Set<Object> outgoingStatesInvLookup(Edge key)
	{ return lookup(outgoingStatesInv, key); }
	
	
	/** protected methods */ 
	@SuppressWarnings("unchecked")
	public Set<Object> lookup(Map<Object, Object>m , Object key) {
		Object valueList = m.get(key);
		if (valueList == null) {
			return null;
		} else if (valueList instanceof Set) {
			return (Set<Object>)valueList;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean addToMap(Map<Object, Object> m, Object key, Object value) {
		Object valueList = m.get(key);
		
		if (valueList == null) {
			m.put(key, valueList = new HashSet<Object>(1));
		} else if ( !(valueList instanceof Set) ) {
			Object[] ar = (Object[]) valueList;
			HashSet<Object> vl = new HashSet<Object>(ar.length + 1);
			m.put(key, vl);
			for (Object obj : ar)
				vl.add(obj);
			return vl.add(value);
		}
		
		return ( (Set<Object>)valueList ).add(value) ;
	}
	
	@Override
	public String toString() { return id.toString(); }
		
}
