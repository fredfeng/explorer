package test.util;

import java.util.HashSet;
import java.util.Set;

public class refsm extends fsm {
	
	//all states in current fsm.
	protected Set<State> states = new HashSet();
	
	public void addStates(State s) {
		states.add(s);
	}
	
	//dump .dot file of current fsm.
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
				
				///
				b.append(" -> ").append(tgt.id).append(" [label=\"");
				b.append(outEdge.getId());
				b.append("\"]\n");
				///
			}
		}
		b.append("}\n");
		System.out.println(b.toString());
	}
}
