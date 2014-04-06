package test.util;

import java.util.*;

public abstract class fsm {
	protected Set<Object> initStates = new HashSet<Object>();
	protected Set<Object> finalStates = new HashSet<Object>();
	
	public Iterator<Object> initStatesIterator() { return initStates.iterator(); }
	public Iterator<Object> finalStatesIterator() { return finalStates.iterator(); }
	
	public boolean addInitState(Object initState) {
		return initStates.add(initState);
	}
	
	public boolean addFinalState(Object finalState) {
		return finalStates.add(finalState);
	}
	
}
