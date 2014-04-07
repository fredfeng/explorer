package test.util;

import java.util.*;

public abstract class fsm {
	protected Set<Object> initStates = new HashSet<Object>();
	protected Set<Object> finalStates = new HashSet<Object>();
	
	//all states in current fsm.
	protected Set<State> states = new HashSet();
	
	public Iterator<Object> initStatesIterator() { return initStates.iterator(); }
	public Iterator<Object> finalStatesIterator() { return finalStates.iterator(); }
	
	public boolean addInitState(Object initState) {
		return initStates.add(initState);
	}
	
	public boolean addFinalState(Object finalState) {
		return finalStates.add(finalState);
	}
	
	public void addStates(State s) {
		states.add(s);
	}
	
	public void dump() {
		StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");
		for (State s : states) {
			b.append("  ").append(s.id);
			if (s.isFinalState)
				b.append(" [shape=doublecircle,label=\"\"];\n");
			else
				b.append(" [shape=circle,label=\"\"];\n");
			if (s.isInitState) {
				b.append("  initial [shape=plaintext,label=\"\"];\n");
				b.append("  initial -> ").append(s.id).append("\n");
			}
			for (Object t : s.getOutgoingStates().keySet()) {
				assert(t instanceof State);

				State tgt = (State) t;
				Set edgeSet = (HashSet)s.getOutgoingStates().get(tgt);
				Edge outEdge = (Edge)((Set)s.getOutgoingStates().get(tgt)).iterator().next();
				b.append("  ").append(s.id);
				
				b.append(" -> ").append(tgt.id).append(" [label=\"");
				if(outEdge.isDot()) 
					b.append(".");
				else
					b.append(outEdge.getId());
				b.append("\"]\n");
			}
		}
		b.append("}\n");
		System.out.println(b.toString());
	}
}
