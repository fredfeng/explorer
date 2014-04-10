package test.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

// refsm is the master fsm
public class refsm extends fsm {
	
	// find() method returns a map that maps each state in refsm with a dot edge
	// to a set of edges that must be followed to reach the one of the final states in refsm
	public Map<State, Set<Edge>> find() {
		Map<State, Set<Edge>> opts = new HashMap<State, Set<Edge>>();
		for (State currState : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge
			if (currState.hasOutgoingDotEdge()) {
				// we do not regard final state as a qualified state
				// and do not create an keyEdges set for the final state
				// but we can change this freely by removing the following two LOC
				if (currState.isFinalState())
					continue;
				
				Set<Edge> keyEdges = new HashSet<Edge>();
				opts.put(currState, keyEdges);
				for (Object e : currState.outgoingStatesInv()) {
					Edge eg = (Edge) e;
					if (!eg.isDot())
						keyEdges.add(eg);
				}
			}
		}
		return opts;
	}
}
