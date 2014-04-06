package test.util;

import java.util.*;

public class StatePair {
	protected State masterState;
	protected State slaveState;
	protected boolean isInitState = false;
	protected boolean isFinalState = false;
	protected Map<Object, Object> outgoingStatePairs = new HashMap<Object, Object>(); // Map<StatePair, Edge>
	protected Map<Object, Object> outgoingStatePairsInv = new HashMap<Object, Object>(); // Map<Edge, StatePair>
	protected Set<Object> incomingStatePairs = new HashSet<Object>();
	protected Edge incomingEdge;
	
	public StatePair(State masterState, State slaveState, boolean isInitState, boolean isFinalState) {
		this.masterState = masterState;
		this.slaveState = slaveState;
		this.isInitState = isInitState;
		this.isFinalState = isFinalState;
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
	
	public void setIncomingEdge(Edge incomingEdge) {
		this.incomingEdge = incomingEdge;
	}
	
	public boolean addIncomingStates(Object statePair) {
		return incomingStatePairs.add(statePair);
	}
	
	public boolean addOutgoingStatePairs(Object statePair, Object edge) {
		return addToMap(outgoingStatePairs, statePair, edge) | addToMap(outgoingStatePairsInv, edge, statePair);
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
