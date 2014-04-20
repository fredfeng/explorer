package edu.utexas.cgrex.automaton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// refsm is the master fsm
public class RegAutomaton extends Automaton {

	// find() method returns a map that maps each state in refsm with a dot edge
	// to a set of edges that must be followed to reach the one of the final
	// state in refsm
	public Map<AutoState, Set<AutoEdge>> find() {
		Map<AutoState, Set<AutoEdge>> opts = new HashMap<AutoState, Set<AutoEdge>>();
		for (AutoState s : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge
			RegAutoState currState = (RegAutoState) s;
			
			/*
			// we can replace this by hasCycleEdge() if the only possible cycle
			// is dot edge
			if (currState.hasCycleEdge()) {
				// we do not regard final state as a qualified state
				// and do not create an keyEdges set for the final state
				// but we can change this freely by removing the following two
				// LOC
				if (currState.isFinalState())
					continue;

				Set<AutoEdge> keyEdges = new HashSet<AutoEdge>();
				opts.put(currState, keyEdges);

				for (AutoEdge e : currState.getOutgoingStatesInvKeySet()) {
					AutoEdge eg = (AutoEdge) e;
					if (!eg.isDotEdge())
						keyEdges.add(eg);
					else if (currState.outgoingStatesInvLookup(eg).equals(
							currState)) {
						keyEdges.clear();
						break;
					}
				}
			}
			*/
			if (currState.isFinalState())
				continue;
			
			Set<AutoEdge> keyEdges = new HashSet<AutoEdge>();
			opts.put(currState, keyEdges);

			for (AutoEdge e : currState.getOutgoingStatesInvKeySet()) {
				AutoEdge eg = (AutoEdge) e;
				if (!eg.isDotEdge())
					keyEdges.add(eg);
				else {
					Set<AutoState> out = currState.outgoingStatesInvLookup(eg);
					for (AutoState os : out) {
						if (!os.equals(currState)) {
//							System.out.println("***" + os + " " + currState);
							keyEdges.clear();
							break;
						}
					}
				}
			}
			
		}
		System.out.println(opts);
		return opts;
	}
}
