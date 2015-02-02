package edu.utexas.cgrex.automaton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
	
	public void clearFinalState() {
		finalStates.clear();
	}

	public void addStates(AutoState state) {
		states.add(state);
		if (state.isInitState)
			initStates.add(state);
		if (state.isFinalState)
			finalStates.add(state);
	}

	public void addEdge(AutoState srcState, AutoState tgtState, AutoEdge edge) {
		srcState.addOutgoingStates(tgtState, edge);
		tgtState.addIncomingStates(srcState, edge);
	}

	public AutoEdge getEdgeBySrc(AutoState src, AutoState sink) {
		AutoEdge e = null;
		for (AutoState s : src.getOutgoingStatesKeySet()) {
			if (s.equals(sink)) {
				e = src.outgoingStatesLookup(s).iterator().next();
				break;
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
			b.append("  ").append("\"" + s.id + "\"");

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
				b.append("  \"initial\" [shape=plaintext,label=\"");
				b.append(s.id);
				b.append("\"];\n");
				b.append("  \"initial\" -> ").append("\"" + s.id + "\"")
						.append("\n");
			}

			for (AutoState tgt : s.getOutgoingStates().keySet()) {
				for (AutoEdge outEdge : s.outgoingStatesLookup(tgt)) {
					b.append("  ").append("\"" + s.id + "\"");

					b.append(" -> ").append("\"" + tgt.id + "\"")
							.append(" [label=\"");
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

	public void dumpFile() {
		StringBuilder b = new StringBuilder("digraph Automaton {\n");
		b.append("  rankdir = LR;\n");
		for (AutoState s : states) {
			b.append("  ").append("\"" + s.id + "\"");

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
				b.append("  \"initial\" [shape=plaintext,label=\"");
				b.append(s.id);
				b.append("\"];\n");
				b.append("  \"initial\" -> ").append("\"" + s.id + "\"")
						.append("\n");
			}

			for (AutoState tgt : s.getOutgoingStates().keySet()) {
				for (AutoEdge outEdge : s.outgoingStatesLookup(tgt)) {
					b.append("  ").append("\"" + s.id + "\"");

					b.append(" -> ").append("\"" + tgt.id + "\"")
							.append(" [label=\"");
					if (outEdge.isDotEdge())
						b.append(".");
					else
						b.append(outEdge.getShortName() + outEdge.getId());
					b.append("\"]\n");
				}

			}
		}
		b.append("}\n");
		// System.out.println(b.toString());

		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"src/inter.dot"));
			bufw.write(b.toString());
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	// validate whether current automaton is well-formed.
	public void validate() {
		Set<AutoState> all = new HashSet<AutoState>();
		all.addAll(this.getStates());
		all.addAll(this.getFinalStates());
		all.addAll(this.getInitStates());
		for (AutoState as : all) {
			assert (as.getId() != null);
			for (Iterator<AutoState> cIt = as.outgoingStatesIterator(); cIt
					.hasNext();) {
				AutoState tgtState = cIt.next();
				assert (tgtState.incomingStatesLookup(as).size() > 0);
			}
		}
	}
		
	//init necessary inverse edges for residual graph.
	public void initResidualEdges() {
		// need to create new edges and nodes.

		for (AutoState s : this.states) {
			List<AutoEdge> tempList = new LinkedList<AutoEdge>(
					s.getOutgoingStatesInvKeySet());
			for (AutoEdge e : tempList) {
				// how about self loop.
				if (e.isInvEdge())
					continue;

				AutoState tgtState = s.outgoingStatesInvLookup(e).iterator()
						.next();
				if (getEdgeBySrc(tgtState, s) != null)
					continue;

				int weight = e.getFlow();
				AutoEdge augEdge = new AutoEdge("inv" + e.getId(), weight,
						weight + "");
				augEdge.setResidual(e.getFlow());
				augEdge.setInvEdge(true);
				this.addEdge(tgtState, s, augEdge);
			}
		}
	}
	
	public void genResidualGraph() {
		for (AutoState as : this.states)
			for (Iterator<AutoEdge> it = as.outgoingStatesInvIterator(); it
					.hasNext();) {
				AutoEdge e = it.next();
				// normal edges.
				if (!e.isInvEdge()) {
					assert (e.getWeight() != 0);
					int newWt = e.getWeight() - e.getFlow();
					// System.out.println("weight::" + e + "==>" + e.getFlow() +
					// "/"
					// + e.getWeight() + '=' + newWt);
					e.setResidual(newWt);
					e.setShortName(e.getWeight() + "");
					assert (newWt >= 0);
					// need to take care of inverse edge.
					assert (as.outgoingStatesInvLookup(e).size() == 1);
					AutoState tgt = as.outgoingStatesInvLookup(e).iterator()
							.next();
					assert (tgt != null);
					AutoEdge revEdge = this.getEdgeBySrc(tgt, as);
					//FIXME: there can be loop between two nodes.
					if(revEdge.isInvEdge())
						revEdge.setResidual(e.getFlow());
				}
			}
	}

}
