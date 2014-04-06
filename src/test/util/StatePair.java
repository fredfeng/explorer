package test.util;

import java.util.*;

public class StatePair {
	protected State masterState;
	protected State slaveState;
	protected boolean isInitState = false;
	protected boolean isFinalState = false;
	protected Map<Object, Object> outgoingStatePairs = new HashMap<Object, Object>(); // Map<StatePair, Set<Edge>>
	protected Map<Object, Object> outgoingStatePairsInv = new HashMap<Object, Object>(); // Map<Edge, Set<StatePair>>
	protected Map<Object, Object> incomingStatePairs = new HashMap<Object, Object>(); // Map<StatePair, Set<Edge>>
	protected Map<Object, Object> incomingStatePairsInv = new HashMap<Object, Object>(); // Map<Edge, Set<StatePair>>
	
	public StatePair(State masterState, State slaveState, boolean isInitState, boolean isFinalState) {
		this.masterState = masterState;
		this.slaveState = slaveState;
		this.isInitState = isInitState;
		this.isFinalState = isFinalState;
	}
	
	public StatePair(State masterState, State slaveState) {
		this.masterState = masterState;
		this.slaveState = slaveState;
	}
	
	public void setInitState() { isInitState = true; }	
	public void resetInitState() { isInitState = false; }
	public boolean isInitState() { return isInitState; }
	public void setFinalState() { isFinalState = true; }
	public void resetFinalState() { isFinalState = false; }
	public boolean isFinalState() { return isFinalState; }
	
	public State getMasterState() { return masterState; }
	public State getSlaveState() { return slaveState; }
	
	@Override
	public boolean equals(Object other) {
		return other instanceof StatePair 
				&& ( masterState.equals( ((StatePair)other ).masterState) 
					 && slaveState.equals( ((StatePair)other).slaveState) 
				   ) ? true : false;
	}
	
	@Override
	public int hashCode() 
	{ return 37 * masterState.hashCode() + slaveState.hashCode(); }
	
	public boolean hasMasterState(State other) { return masterState.equals(other); }
	public boolean hasSlaveState(State other) { return slaveState.equals(other); }
	public boolean hasMasterandSlaveState(State master, State slave) {
		return hasMasterState(master) && hasSlaveState(slave);
	}
	
	public boolean addOutgoingStatePairs(Object statePair, Object edge) {
		return addToMap(outgoingStatePairs, statePair, edge) | addToMap(outgoingStatePairsInv, edge, statePair);
	}
	
	public boolean addIncomingStatePairs(Object statePair, Object edge) {
		return addToMap(incomingStatePairs, statePair, edge) | addToMap(incomingStatePairsInv, edge, statePair);
	}
	
	/** map copy */
	public void setOutgoingStates(Map<Object, Object> outgoingStatePairs) { 
		this.outgoingStatePairs = outgoingStatePairs;
	}
	public void setOutgoingStatesInv(Map<Object, Object> outgoingStatePairsInv) {
		this.outgoingStatePairsInv = outgoingStatePairsInv;
	}
	
	public Iterator<Object> outgoingStatesIterator() { return outgoingStatePairs.keySet().iterator(); }
	public Iterator<Object> outgoingStatesInvIterator() { return outgoingStatePairsInv.keySet().iterator(); }
	
	public Set<Object> outgoingStates() { return outgoingStatePairs.keySet(); }
	public Set<Object> outgoingStatesInv() { return outgoingStatePairs.keySet(); }
	
	public Set<Object> outgoingStatePairsLookup(StatePair key) 
	{ return lookup(outgoingStatePairs, key); }
	public Set<Object> outgoingStatePairsInvLookup(Edge key)
	{ return lookup(outgoingStatePairsInv, key); }
	public Set<Object> incomingStatePairsLookup(StatePair key)
	{ return lookup(incomingStatePairs, key); }
	public Set<Object> incomingStatePairsInvLookup(Edge key) 
	{ return lookup(incomingStatePairsInv, key); }
	
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
	
	
}
