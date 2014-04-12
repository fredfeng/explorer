package test.util;

import java.util.*;

public class StatePair extends State {
	protected State masterState;
	protected State slaveState;
	protected Map<Object, Object> incomingStates = new HashMap<Object, Object>(); // Map<StatePair, Set<Edge>>
	protected Map<Object, Object> incomingStatesInv = new HashMap<Object, Object>(); // Map<Edge, Set<StatePair>>
	
	public StatePair(Id id, State masterState, State slaveState, boolean isInitState, boolean isFinalState) {
		super(id, isInitState, isFinalState);
		this.masterState = masterState;
		this.slaveState = slaveState;
	}
	
	public StatePair(Id id, State masterState, State slaveState) {
		super(id, false, false);
		this.masterState = masterState;
		this.slaveState = slaveState;
	}
	
	public boolean buildAsInitial() { return masterState.isInitState && slaveState.isFinalState; }
	public boolean buildAsFinal() { return masterState.isFinalState; }
	
	public State getMasterState() { return masterState; }
	public State getSlaveState() { return slaveState; }
	
	public Set<Object> getIncomingStates() { return incomingStates.keySet(); }
	
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
	
	public boolean addIncomingStatePairs(Object statePair, Object edge) {
		return addToMap(incomingStates, statePair, edge) | addToMap(incomingStatesInv, edge, statePair);
	}
	
	public Set<Object> incomingStatePairsLookup(StatePair key)
	{ return lookup(incomingStates, key); }
	public Set<Object> incomingStatePairsInvLookup(Edge key) 
	{ return lookup(incomingStatesInv, key); }
	
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
