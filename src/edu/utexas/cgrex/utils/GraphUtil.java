package edu.utexas.cgrex.utils;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;
import chord.util.tuple.object.Pair;

import com.rits.cloning.Cloner;

import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.Automaton;
import edu.utexas.cgrex.automaton.InterAutomaton;

public class GraphUtil {

	public static Cloner cloner = new Cloner();
	
	private static boolean debug = false;

	public static Set<AutoState> findRoots(Automaton automaton) {
		Set<AutoState> roots = new HashSet<AutoState>();
		HashSet<AutoState> states = (HashSet<AutoState>) ((HashSet<AutoState>) automaton
				.getStates()).clone();
		AutoState head = states.iterator().hasNext() ? states.iterator().next()
				: null;

		while (head != null) {
			roots.add(head);
			removeSubGraph(head, states);
			head = states.iterator().hasNext() ? states.iterator().next()
					: null;
		}

		return roots;
	}

	public static void removeSubGraph(AutoState start, Set<AutoState> states) {
		LinkedList<AutoState> workList = new LinkedList<AutoState>();
		Set<AutoState> visited = new HashSet<AutoState>();

		workList.add(start);
		AutoState head = null;
		while (!workList.isEmpty()) {
			head = workList.poll();
			if (visited.contains(head))
				continue;

			visited.add(head);
			states.remove(head);
			for (AutoState s : head.getOutgoingStatesKeySet()) {
				workList.add(s);
			}
		}

	}

	public static boolean isReachable(AutoState startSt, AutoState endSt,
			Automaton auto) {

		assert (startSt != null);
		assert (endSt != null);

		Set<AutoState> visited = new HashSet<AutoState>();
		LinkedList<AutoState> workList = new LinkedList<AutoState>();

		workList.add(startSt);
		AutoState head = null;
		while (!workList.isEmpty()) {

			head = workList.poll();

			if (visited.contains(head))
				continue;
			visited.add(head);

			for (AutoState s : head.getOutgoingStatesKeySet()) {
				if (s.equals(endSt)) {
					return true;
				} else {
					workList.add(s);
				}
			}

		}

		return false;
	}
	
	//for all final states, there exists at least one initial state reach it.
	public static void checkValidInterAuto(Automaton auto) {
		for(AutoState finalSt : auto.getFinalStates()) {
			boolean answer = false;
			for(AutoState initSt : auto.getInitStates()) {
				answer = (isReachable(initSt, finalSt, auto) || answer);
			}
			assert(answer);
		}
	}

	public static List<Set<Object>> doAnalysis(Set<Object> roots,
			Map<Object, Set<Object>> nodeToPreds,
			Map<Object, Set<Object>> nodeToSuccs) {
		IGraph<Object> graph = new MutableGraph<Object>(roots, nodeToPreds,
				nodeToSuccs);
		// this will give you a list of scc.
		List<Set<Object>> sccList = graph.getTopSortedSCCs();

		return sccList;
	}

	/**
	 * auto: Graph need to perform DFS init: initial node
	 * 
	 * @return
	 */
	public static LinkedList<AutoState> dfs(Automaton auto) {
		resetVisited(auto);
		AutoState init = auto.getInitStates().iterator().next();
		LinkedList<AutoState> queue = new LinkedList<AutoState>();
		LinkedList<AutoState> path = new LinkedList<AutoState>();
		queue.add(init);
		while (!queue.isEmpty()) {
			AutoState cur = queue.poll();

			if (!cur.isVisited()) {
				// System.out.println("visiting..." + cur);
				path.add(cur);
				cur.setVisited(true);
				for (Iterator<AutoState> cIt = cur.outgoingStatesIterator(); cIt
						.hasNext();) {
					AutoState tgtState = cIt.next();
					AutoEdge tgtEdge = cur.outgoingStatesLookup(tgtState)
							.iterator().next();
					if (tgtEdge.getResidual() != 0) {
						queue.add(tgtState);
					}
				}
			}
		}
		return path;
	}

	/**
	 * Given an automaton, dump out its mincut, which is a set of edge.
	 * Implementation of Ford-Fulkerson algorithm:
	 * http://en.wikipedia.org/wiki/Ford-Fulkerson_algorithm
	 * 
	 * @param auto
	 * @param init
	 */
	public static Set<CutEntity> minCut(Automaton auto) {

		// reset all flow values to 0
		resetAuto(auto);
		// build the residual graph
		auto.initResidualEdges();
		auto.genResidualGraph();
		
		assert(auto.getFinalStates().size() == 1);

		// we should continue if there is a path to final state in residual
		// graph.
		while (extractPath(auto) != null) {
			// find the augment path until no path can be found.
			Pair<Integer, LinkedList<AutoState>> pair = extractPath(auto);
			int min = pair.val0;

			AutoState src = pair.val1.getFirst();
			for (int i = 1; i < pair.val1.size(); i++) {
				AutoState tgt = pair.val1.get(i);
				AutoEdge ae = auto.getEdgeBySrc(src, tgt);

				// no such edge in original graph. modify inverse edge.
				if (ae.isInvEdge()) {
					AutoEdge invE = auto.getEdgeBySrc(tgt, src);
					// System.out.println(src + "->" +tgt+ " " + invE.getFlow()
					// + "+" + min + "<=" + invE.getWeight());
					invE.setFlow(invE.getFlow() - min);
					invE.setShortName(invE.getFlow() + "/" + invE.getWeight());
					assert (invE.getFlow() >= 0 && invE.getFlow() <= invE
							.getWeight());
				} else {
					ae.setFlow(ae.getFlow() + min);
					ae.setShortName(ae.getFlow() + "/" + ae.getWeight());
					assert (ae.getFlow() <= ae.getWeight());
				}

				src = tgt;
			}

			auto.genResidualGraph();
		}

		// collect all the vertices(As set S) that are reachable from the
		// initial node
		LinkedList<AutoState> spath = dfs(auto);
		Set<AutoState> orgStates = new HashSet(auto.getStates());
		orgStates.retainAll(spath);
		LinkedList<AutoState> tpath = new LinkedList(auto.getStates());
		tpath.removeAll(orgStates);
		// collect all the edges that cross between S and T
		Set<CutEntity> cutset = new HashSet<CutEntity>();
		for (AutoState s : orgStates) {
			// outgoing edges.
			for (Iterator<AutoEdge> cIt = s.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge outEdge = cIt.next();
				if(outEdge.isInvEdge()) continue;
				AutoState tgt = s.outgoingStatesInvLookup(outEdge).iterator()
						.next();
				if (tpath.contains(tgt) && (outEdge.getWeight() > 0)) {
					 /*System.out.println(s + "->" + tgt + " " +
					 outEdge.getWeight());*/
					assert(outEdge.isInvEdge() == false);
					cutset.add(new CutEntity(s, outEdge, tgt));
				}
			}

			for (Iterator<AutoEdge> cIt = s.incomingStatesInvIterator(); cIt
					.hasNext();) {
				// incoming edges.
				AutoEdge inEdge = cIt.next();
				if(inEdge.isInvEdge()) continue;

				AutoState src = s.incomingStatesInvLookup(inEdge).iterator()
						.next();
				if (tpath.contains(src) && (inEdge.getWeight() > 0)) {
//					 System.out.println(src + "-*>" + s + " " +
//					 inEdge.getWeight());
					assert(inEdge.isInvEdge() == false);
					cutset.add(new CutEntity(src, inEdge, s));
				}
			}
		}
		
		//make sure this is a valid mincut.
		if(debug)
			assert(isValidCut(auto, cutset));

		return cutset;

	}
	
	//check if it's a valid mincut.
	public static boolean isValidCut(Automaton auto, Set<CutEntity> cutset) {
		AutoState init = auto.getInitStates().iterator().next();
		LinkedList<AutoState> worklist = new LinkedList<AutoState>();
		Set<AutoState> visited = new HashSet<AutoState>();
		Set<AutoEdge> cutEdges = new HashSet<AutoEdge>();
		for(CutEntity cut : cutset) {
			cutEdges.add(cut.edge);
		}
		worklist.add(init);
		while (!worklist.isEmpty()) {
			AutoState cur = worklist.poll();
			
			if(visited.contains(cur))
				continue;
			visited.add(cur);

			for (Iterator<AutoState> cIt = cur.outgoingStatesIterator(); cIt
					.hasNext();) {
				AutoState tgtState = cIt.next();
				AutoEdge tgtEdge = cur.outgoingStatesLookup(tgtState)
						.iterator().next();
				if(tgtEdge.isInvEdge() || cutEdges.contains(tgtEdge)) 
					continue;
				
				worklist.add(tgtState);
			}
		}

		visited.retainAll(auto.getFinalStates());
		return visited.isEmpty();
	}

	/* reset all flow values to 0 for the new round. */
	public static void resetAuto(Automaton auto) {
		for (AutoState s : auto.getStates()) {
			// outgoing edges.
			for (Iterator<AutoEdge> cIt = s.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge outEdge = cIt.next();
				outEdge.setFlow(0);
			}

			for (Iterator<AutoEdge> cIt = s.incomingStatesInvIterator(); cIt
					.hasNext();) {
				// incoming edges.
				AutoEdge inEdge = cIt.next();
				inEdge.setFlow(0);
			}
		}
	}


	public static void resetVisited(Automaton auto) {
		for (AutoState ast : auto.getStates())
			ast.setVisited(false);
	}

	public static Pair<Integer, LinkedList<AutoState>> extractPath(
			Automaton auto) {
		resetVisited(auto);

		LinkedList<AutoState> path = dfs(auto);
		int minWt = 1000;
		AutoState end = auto.getFinalStates().iterator().next();

		if (!path.contains(end))
			return null;

		while (!path.getLast().equals(end))
			path.pollLast();
		if (path.size() == 0)
			return null;

		LinkedList<AutoState> realpath = new LinkedList<AutoState>();
		AutoState cursor = path.getLast();
		realpath.add(path.getLast());

		for (int i = (path.size() - 2); i >= 0; i--) {
			AutoState pre = path.get(i);
			AutoEdge edge = auto.getEdgeBySrc(pre, cursor);
			if (edge != null && (edge.getResidual() > 0)) {
				realpath.add(pre);
				cursor = pre;
				if (edge.getResidual() < minWt)
					minWt = edge.getResidual();
			}
		}

		Collections.reverse(realpath);
		return new Pair<Integer, LinkedList<AutoState>>(minWt, realpath);
	}
}
