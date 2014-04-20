package edu.utexas.cgrex.automaton;

import java.util.*;

public abstract class Automaton {

	protected Set<AutoState> initStates = new HashSet<AutoState>();

	protected Set<AutoState> finalStates = new HashSet<AutoState>();

	protected Set<AutoState> states = new HashSet<AutoState>();

	public Set<AutoState> getInitStates() {
		return initStates;
	}

	public Set<AutoState> getFinalStates() {
		return finalStates;
	}

	public Set<AutoState> getStates() {
		return states;
	}

	public Iterator<AutoState> initStatesIterator() {
		return initStates.iterator();
	}

	public Iterator<AutoState> finalStatesIterator() {
		return finalStates.iterator();
	}

	public Iterator<AutoState> statesIterator() {
		return states.iterator();
	}

	public boolean addInitState(AutoState initState) {
		return initStates.add(initState);
	}

	public boolean addFinalState(AutoState finalState) {
		return finalStates.add(finalState);
	}

	public void addStates(AutoState state) {
		states.add(state);
	}
	
	public AutoEdge getEdgeBySrc(AutoState src, AutoState sink) {
		AutoEdge e = null;
		for (AutoState as : getStates()) {
			if(!as.equals(src)) continue;
			
			for (Iterator<AutoState> cIt = as.outgoingStatesIterator(); cIt
					.hasNext();) {
				AutoState tgtState = cIt.next();
				if(!tgtState.equals(sink)) continue;
				Set<AutoEdge> out = as.outgoingStatesLookup(tgtState);
				e = out.iterator().next();
				return e;
			}
		}
	
		return e;
	}
	
	// 1. delete the specified state from the states field in Automaton
	// so that you can never find that state in the states of the Automaton
	// 2. delete the edges like other --> state so that others can never find
	// that state by the outgoingStates of the Automaton
	// 3. delete the edges like state --> other so that others can never find
	// that state by the incomingStates of the Automaton
	public boolean deleteOneState(AutoState state) {
		boolean succ = states.remove(state);
		CGAutoState cSt = (CGAutoState) state;
		for (AutoState s : cSt.getIncomingStatesKeySet()) {
			succ = succ | s.deleteOneOutgoingState(state);
		}
		for (AutoState s : cSt.getOutgoingStatesKeySet()) {
			succ = succ | s.deleteOneIncomingState(state);
		}

		return succ;
	}

	// delete the edge in the form like : startSt --(edge)-- endSt
	public boolean deleteOneEdge(AutoState startSt, AutoState endSt,
			AutoEdge edge) {
		return startSt.deleteOneOutgoingEdge(endSt, edge)
				| endSt.deleteOneIncomingEdge(startSt, edge);
	}

	public void dump() {
		StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");
		for (AutoState s : states) {
			b.append("  ").append(s.id);

			if (s.isFinalState) {
				b.append(" [shape=doublecircle,label=\"");
				b.append(s.id);
				b.append("\"];\n");
			} else {
				b.append(" [shape=circle,label=\"");
				b.append(s.id);
				b.append("\"];\n");
			}

			if (s.isInitState) {
				b.append("  initial [shape=plaintext,label=\"");
				b.append(s.id);
				b.append("\"];\n");
				b.append("  initial -> ").append(s.id).append("\n");
			}

			for (AutoState tgt : s.getOutgoingStates().keySet()) {
				for (AutoEdge outEdge : s.outgoingStatesLookup(tgt)) {
					b.append("  ").append(s.id);

					b.append(" -> ").append(tgt.id).append(" [label=\"");
					if (outEdge.isDotEdge())
						b.append(".");
					else
						b.append(outEdge.getShortName());
					b.append("\"]\n");
				}

			}
		}
		b.append("}\n");
		System.out.println(b.toString());
	}
	
	//validate whether current automaton is well-formed.
	public void validate() {
		Set<AutoState> all = new HashSet();
		all.addAll(this.getStates());
		all.addAll(this.getFinalStates());
		all.addAll(this.getInitStates());
		for (AutoState as : all) {
			assert(as.getId() != null);
			for (Iterator<AutoState> cIt = as.outgoingStatesIterator(); cIt
					.hasNext();) {
				AutoState tgtState = cIt.next();
				assert (tgtState.incomingStatesLookup(as).size() > 0);
			}
		}
	}
	
}
