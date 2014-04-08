package test.util;

import java.util.*;

public class cgfsm extends fsm {
	
	// the first state in return value is the state in cgfsm
	// the second state in return value is the state in refsm
	// i.e., 
	// ( masterState[second state], slaveState[first state] ) ----> true / false
	public Map<State, Map<State, Boolean>> annotate(Map<State, Set<Edge>> regExprOpts) {
		Map<State, Map<State, Boolean>> opts = new HashMap<State, Map<State, Boolean>>();
		for (State stateInReg : regExprOpts.keySet()) {
			
		}
		return opts;
	}
	
	// state: each state in cgfsm
	// boolean: true -- this state is OK, false -- the subgraph of this state does not have the key edge
	protected Map<State, Boolean> annotateOneMasterState(Set<Edge> keyEdges) {
		Map<State, Boolean> opts = new HashMap<State, Boolean>();
	}
	
	// for each master state in regular expr fsm
	// then for each slave state in call graph fsm
	// annotate the slave state with true/false
	// denoting whether the subgraph of the slave state has at least one key edge
	// that leads to the final state of regular expr fsm
	protected boolean annotateOneSlaveStateOneMasterState(State currSlaveState, 
			Set<Edge> keyEdges, Map<State, Boolean> opts) {
		// termination conditions
		// 1. if this slave state has been visited, return its boolean value
		if (opts.containsKey(currSlaveState)) 
			return opts.get(currSlaveState);
		// 2. if this slave state has only one (.*) edge, return true if it is fine
		if (currSlaveState.hasOnlyOneDotOutgoingEdge()) {
			if (keyEdges.contains(currSlaveState.getOnlyOneOutgoingEdge())) {
				opts.put(currSlaveState, true);
				return true;
			}
			return false;
		}
		// if this slave state has at least one outgoing edge (not cycle)
		// do the recursive case
		for (Object e : currSlaveState.outgoingStates()) {
			
		}
	}
}
