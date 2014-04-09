package test.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// refsm is the master fsm
public class refsm extends fsm {

	// all states in current fsm.

	public Map<State, Set<Edge>> find() {
		Map<State, Set<Edge>> opts = new HashMap<State, Set<Edge>>();
		for (State currState : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge
			if (currState.hasOutgoingDotEdge()) {
				if (currState.isFinalState())
					continue;
				opts.put(currState, new HashSet<Edge>());
				for (Object e : currState.outgoingStatesInv()) {
					Edge eg = (Edge) e;
					if (!eg.isDot())
						opts.get(currState).add(eg);
				}
			}
		}
		return opts;
	}
}
