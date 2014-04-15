package edu.utexas.cgrex.automaton;

import java.util.*;

import edu.utexas.cgrex.utils.GraphUtil;

// cgfsm is the slave fsm
public class CGAutomaton extends Automaton {

	// this building method fills the SCC-related fields of the states in
	// the automaton so that we can do some SCC analysis
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void buildCGStatesSCC() {

		Set roots = initStates;
		Map nodeToPreds = new HashMap<Object, Set<Object>>();
		Map nodeToSuccs = new HashMap<Object, Set<Object>>();
		for (AutoState s : states) {
			nodeToPreds.put(s, s.getIncomingStatesKeySet());
			nodeToSuccs.put(s, s.getOutgoingStatesKeySet());
		}
		Object sccListTmp = GraphUtil.doAnalysis(roots, nodeToPreds,
				nodeToSuccs);
		List<Set<AutoState>> sccList = (List<Set<AutoState>>) sccListTmp;

		for (Set<AutoState> scc : sccList) {
			Set<AutoEdge> edgesInTheSameSCC = new HashSet<AutoEdge>();
			Set<AutoEdge> outgoingEdgesOfSCC = new HashSet<AutoEdge>();
			Set<AutoState> outgoingStatesOfSCC = new HashSet<AutoState>();
			for (AutoState s : scc) {
				CGAutoState st = (CGAutoState) s;
				// find all the edges in the same SCC
				for (AutoEdge e : st.getOutgoingStatesInvKeySet()) {
					Set<AutoState> lookup = st.outgoingStatesInvLookup(e);
					for (AutoState ss : lookup) {
						if (scc.contains(ss)) {
							edgesInTheSameSCC.add(e);
							break;
						}
					}
				}
				// find all the edges going out of this SCC
				for (AutoEdge e : st.getOutgoingStatesInvKeySet()) {
					Set<AutoState> lookup = st.outgoingStatesInvLookup(e);
					for (AutoState ss : lookup) {
						if (!scc.contains(ss)) {
							outgoingEdgesOfSCC.add(e);
							break;
						}
					}
				}
				// find all the states being neighbors of this SCC
				for (AutoState outState : st.getOutgoingStatesKeySet()) {
					if (!scc.contains(outState))
						outgoingStatesOfSCC.add(outState);
				}
			}
			for (AutoState s : scc) {
				CGAutoState st = (CGAutoState) s;
				st.setBelongsToASCC();
				// scc is statesInTheSameSCC
				st.setStatesInTheSameSCC(scc);
				st.setEdgesInTheSameSCC(edgesInTheSameSCC);
				st.setOutgoingEdgesOfSCC(outgoingEdgesOfSCC);
				st.setOutgoingStatesOfSCC(outgoingStatesOfSCC);
			}
		}

		for (AutoState s : states) {
			CGAutoState st = (CGAutoState) s;
			if (!st.isBelongsToASCC()) {
				// add st itself in the statesInTheSameSCC
				Set<AutoState> statesInTheSameSCC = new HashSet<AutoState>();
				statesInTheSameSCC.add(st);
				st.setStatesInTheSameSCC(statesInTheSameSCC);
				// add the cycle edge of st (if existed) to the
				// edgesInTheSameSCC
				Set<AutoEdge> edgesInTheSameSCC = new HashSet<AutoEdge>();
				AutoEdge ce = st.getCycleEdge();
				if (ce != null)
					edgesInTheSameSCC.add(st.getCycleEdge());
				st.setEdgesInTheSameSCC(edgesInTheSameSCC);
				// remove the cycle edge from the outgoingEdgesOfSCC
				Set<AutoEdge> outgoingEdgesOfSCC = new HashSet<AutoEdge>();
				outgoingEdgesOfSCC.addAll(st.getIncomingStatesInvKeySet());
				AutoEdge cycle = st.getCycleEdge();
				// if (cycle != null)
				outgoingEdgesOfSCC.remove(cycle);
				st.setOutgoingEdgesOfSCC(outgoingEdgesOfSCC);
				// remove st state from the outgoingStatesOfSCC
				Set<AutoState> outgoingStatesOfSCC = new HashSet<AutoState>();
				outgoingStatesOfSCC.addAll(st.getOutgoingStatesKeySet());
				// if (outgoingStatesOfSCC.contains(st))
				outgoingStatesOfSCC.remove(st);
				st.setOutgoingStatesOfSCC(outgoingStatesOfSCC);
			}
		}
	}

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
		buildCGStatesSCC();
		Map<AutoState, Map<AutoState, Boolean>> opts = new HashMap<AutoState, Map<AutoState, Boolean>>();
		for (AutoState stateInReg : regExprOpts.keySet()) {
			Set<AutoEdge> keyEdges = regExprOpts.get(stateInReg);
			System.out.println(stateInReg);
			opts.put(stateInReg, annotateOneMasterState(keyEdges));
		}
		System.out.println("annotations\n" + opts);
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
			// annotateOneSlaveStateOneMasterState(s, keyEdges, opts);
			annotSlaveMasterSCC(s, keyEdges, opts);
		}
		return opts;
	}

	protected boolean annotSlaveMasterSCC(AutoState currSlaveState,
			Set<AutoEdge> keyEdges, Map<AutoState, Boolean> opts) {
		System.out.println("***" + currSlaveState);
		// if currSlaveState has been annotated, return the annotation
		if (opts.containsKey(currSlaveState))
			return opts.get(currSlaveState);

		// 1. annotate according to the outgoingEdgesOfSCC
		boolean outEdgsOfSCCAnnot = false;
		for (AutoEdge e : keyEdges) {
			Set<AutoEdge> out = ((CGAutoState) currSlaveState)
					.getOutgoingEdgesOfSCC();
			if (out != null && out.contains(e)) {
				outEdgsOfSCCAnnot = true;
				break;
			}
		}

		// 2. annotate according to the outgoingStatesOfSCC
		// also recursively update the outgoingStates of this SCC (no cycles)
		boolean outStsOfSCCAnnot = false;

		System.out.println(((CGAutoState) currSlaveState)
				.getOutgoingStatesOfSCC());
		for (AutoState s : ((CGAutoState) currSlaveState)
				.getOutgoingStatesOfSCC()) {
			outStsOfSCCAnnot = annotSlaveMasterSCC(s, keyEdges, opts)
					|| outStsOfSCCAnnot;
		}

		// 3. annotate according to the edgesInTheSameSCC
		boolean edgsInSCCAnnot = false;
		for (AutoEdge e : keyEdges) {
			Set<AutoEdge> in = ((CGAutoState) currSlaveState)
					.getEdgesInTheSameSCC();
			if (in != null && in.contains(e)) {
				edgsInSCCAnnot = true;
				break;
			}
		}

		// 4. get the final annotation and annotate the states in this SCC
		boolean SCCAnnot = outEdgsOfSCCAnnot || outStsOfSCCAnnot
				|| edgsInSCCAnnot;
		for (AutoState s : ((CGAutoState) currSlaveState)
				.getStatesInTheSameSCC()) {
			opts.put(s, SCCAnnot);
		}

		return SCCAnnot;
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
