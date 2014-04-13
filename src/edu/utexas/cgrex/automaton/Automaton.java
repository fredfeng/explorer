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
		
	public void dump() {
		StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");
		for (AutoState s : states) {
			b.append("  ").append(s.id);
			
			if (s.isFinalState) {
				b.append(" [shape=doublecircle,label=\"");
				b.append(s.id);
				b.append("\"];\n");
			}else {
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
					if(outEdge.isDotEdge()) 
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
}
