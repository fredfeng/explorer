package edu.utexas.cgrex.utils;

import java.util.Collections;
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

public class GraphUtil {
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
		Stack<AutoState> queue = new Stack<AutoState>();
		LinkedList<AutoState> path = new LinkedList<AutoState>();
		queue.push(init);
		while (!queue.empty()) {
			AutoState cur = queue.pop();
			
			if (!cur.isVisited()) {
//				System.out.println("visiting..." + cur);
				path.add(cur);
				cur.setVisited(true);
				for (Iterator<AutoState> cIt = cur.outgoingStatesIterator(); cIt
						.hasNext();) {
					AutoState tgtState = cIt.next();
					AutoEdge tgtEdge = cur.outgoingStatesLookup(tgtState).iterator().next();
					if (tgtEdge.getWeight() != 0) {
						queue.push(tgtState);
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
	public static void minCut(Automaton auto, AutoState init) {
		// reset all flow values to 0
		// build the residual graph
		Automaton residual = genResidualGraph(auto);
		
		// we should continue if there is a path to final state in residual
		// graph.
		while (extractPath(residual) != null) {
			// find the augment path until no path can be found.
			Pair<Integer, LinkedList<AutoState>> pair = extractPath(residual);
			int min = pair.val0;

			AutoState src = pair.val1.getFirst();
			for (int i = 1; i < pair.val1.size(); i++) {
				AutoState tgt = pair.val1.get(i);
				AutoEdge ae = auto.getEdgeBySrc(src, tgt);

				//no such edge in original graph. modify inverse edge.
				if(ae == null) {
					AutoEdge invE = auto.getEdgeBySrc(tgt, src);
					assert((invE.getFlow() + min) <= invE.getWeight());
					invE.setFlow(invE.getFlow() + min);
					invE.setShortName(invE.getFlow() + "/" + invE.getWeight());
				} else {
					assert((ae.getFlow() + min) <= ae.getWeight());
					ae.setFlow(ae.getFlow() + min);
					ae.setShortName(ae.getFlow() + "/" + ae.getWeight());
				}

				src = tgt;
			}
						
			residual = genResidualGraph(auto);
		}
		
		System.out.println("Final result, dump cut set:");
		residual.dump();
		
		// collect all the vertices(As set S) that are reachable from the
		// initial node
		LinkedList<AutoState> spath = dfs(residual);
		LinkedList<AutoState> tpath = new LinkedList(residual.getStates());
		tpath.removeAll(spath);


		
		System.out.println(tpath);

		// collect all the edges that cross between S and T
		for(AutoState s : tpath) {
			for (Iterator<AutoEdge> cIt = s.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge inEdge = cIt.next();
				AutoState tgt = s.outgoingStatesInvLookup(inEdge).iterator().next();
				if(!tpath.contains(tgt) && (inEdge.getWeight()>0)) 
					System.out.println(s + "->" + tgt + " " + inEdge.getWeight());
			}
		}

	}
	
	public static boolean constainsId(LinkedList<AutoState> list, AutoState tgtState) {
		boolean f = false;
		for(AutoState as : list) {
			if (as.equals(tgtState)) {
				f = true;
				break;
			}
		}
		return f;
	}

	public static Automaton genResidualGraph(Automaton g) {
		Cloner cloner = new Cloner();
		Automaton residual = cloner.deepClone(g);
		resetVisited(residual);

		for (AutoState as : residual.getStates())
			for (Iterator<AutoEdge> cIt = as.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge e = cIt.next();
				int newWt = e.getWeight() - e.getFlow();
//				System.out.println("weight::" + e + "==>" + e.getFlow() + "/"
//						+ e.getWeight() + '=' + newWt);
				assert (newWt >= 0);
				e.setWeight(newWt);
				e.setShortName(e.getWeight() + "");
				if (e.getFlow() != 0) {
					// need to create new edge for the first time.
					AutoState tgt = as.outgoingStatesInvLookup(e).iterator()
							.next();
					if (residual.getEdgeBySrc(tgt, as) == null) {
						AutoEdge revEdge = new AutoEdge(cIt.hashCode(),
								e.getFlow(), e.getFlow() + "");
						tgt.addOutgoingStates(as, revEdge);
					}
				}
			}

		return residual;
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
		
		if(!path.contains(end)) return null;

		while (!path.getLast().equals(end))
			path.pollLast();
		if(path.size() == 0) return null;
		
		LinkedList<AutoState> realpath = new LinkedList<AutoState>();
		AutoState cursor = path.getLast();
		realpath.add(path.getLast());
		
		for (int i = (path.size() - 2) ; i >= 0; i--) {
			AutoState pre = path.get(i);
			AutoEdge edge = auto.getEdgeBySrc(pre, cursor);
			if(edge != null && (edge.getWeight() > 0)) {
				realpath.add(pre);
				cursor = pre;
				if (edge.getWeight() < minWt) minWt = edge.getWeight();
			}
		}

	    Collections.reverse(realpath);
		return new Pair<Integer, LinkedList<AutoState>>(minWt, realpath);
	}
}
