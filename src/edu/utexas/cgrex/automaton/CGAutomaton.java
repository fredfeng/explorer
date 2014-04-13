package edu.utexas.cgrex.automaton;

import java.util.*;

// cgfsm is the slave fsm
public class CGAutomaton extends Automaton {

	// the first state in return value is the state in refsm
	// the second state in return value is the state in cgfsm
	// i.e.,
	// Map < State, Map < State, Boolean > >
	// | | |
	// masterState slaveState true: create
	// foreach masterState, "annotate" method annotates each slaveState
	// to be true or false, denoting whether we should create the statePair
	// in the intersection machine and whether we should go ahead
	public Map<AutoState, Map<AutoState, Boolean>> annotate(
			Map<AutoState, Set<AutoEdge>> regExprOpts) {
		Map<AutoState, Map<AutoState, Boolean>> opts = new HashMap<AutoState, 
				Map<AutoState, Boolean>>();
		for (AutoState stateInReg : regExprOpts.keySet()) {
			Set<AutoEdge> keyEdges = regExprOpts.get(stateInReg);
			opts.put(stateInReg, annotateOneMasterState(keyEdges));
		}
		return opts;
	}

	// state: each state in cgfsm
	// boolean: true -- this state is OK, and we can create and go ahead
	// false -- the subgraph of this state does not have the key edge
	// Map < State, Boolean >
	// | |
	// slaveState true: create
	protected Map<AutoState, Boolean> annotateOneMasterState(
			Set<AutoEdge> keyEdges) {
		Map<AutoState, Boolean> opts = new HashMap<AutoState, Boolean>();
		for (AutoState s : initStates) {
			annotateOneSlaveStateOneMasterState(s, keyEdges, opts);
		}
		return opts;
	}

	// for each master state in regular expr fsm
	// then for each slave state in call graph fsm
	// annotate the slave state with true/false
	// denoting whether the subgraph of the slave state has at least one key
	// edge
	// that leads to the final state of regular expr fsm
	protected boolean annotateOneSlaveStateOneMasterState(
			AutoState currSlaveState, Set<AutoEdge> keyEdges,
			Map<AutoState, Boolean> opts) {
		// termination conditions
		// 1. if this slave state has been visited, return its boolean value
		// this is just a speedup
		if (opts.containsKey(currSlaveState))
			return opts.get(currSlaveState);
		// 2. if this slave state connects to some other state which will
		// somehow
		// directly or indirectly connect to this slave state, we need to use
		// some method to handle this connected component case
		// lazily, we can set the boolean to be true every time we are trying
		// to annotate a state, in which case it is sound and can remove
		// infinite
		// loop, but this might not be so accurate
		// another better way is to first find the connected components and then
		// handle this outside this method

		// if this slave state has at least one outgoing edge (not cycle)
		// do the recursive case
		// we first check edges
		boolean edge_result = false, state_result = false;
		// might this be not sound? I guess it should be sound
		/*
		 * this way also works but is silly, it is exponential so we change to
		 * the way following this for (Object e :
		 * currSlaveState.outgoingStatesInv()) { Edge eg = (Edge) e; if
		 * (keyEdges.contains(eg)) { edge_result = true; break; } }
		 */
		// this has only constant-complexity
		for (AutoEdge e : keyEdges) {
			if (currSlaveState.getOutgoingStatesInvKeySet().contains(e)) {
				edge_result = true;
				// opts.put(currSlaveState, true);
				break;
			}
		}

		// if we can in advance annotate this currSlaveState to be true
		// we annotate it, which will be good if there is a cycle
		// because when we check the annotations of the neighbors of this state
		// and it happens that the neighbor's neighbors include this state
		// then we can mark that neighbor as true
		// but this does not help if there is a cycle and we cannot annotate
		// true
		// in which case we still need to handle the graph with connected
		// components
		// but generally, we do not need this code and it still works
		if (edge_result)
			opts.put(currSlaveState, true);
		// we check for the neighboring states of currSlaveState
		for (AutoState s : currSlaveState.getOutgoingStatesKeySet()) {
			if (s.equals(currSlaveState))
				continue; // eliminate infinite loop
			state_result = state_result
					|| annotateOneSlaveStateOneMasterState(s, keyEdges, opts);
		}
		// synthesize the result and annotate
		boolean result = edge_result || state_result;
		opts.put(currSlaveState, result);

		return result;
	}
}
