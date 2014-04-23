package edu.utexas.cgrex.utils;

import java.util.Collections;
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

public class GraphUtil {
	
	public static Cloner cloner = new Cloner();

		
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
					if (tgtEdge.getResidual() != 0) {
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
	public static Set<CutEntity> minCut(Automaton auto) {

		// reset all flow values to 0
		resetAuto(auto);
		// build the residual graph
		Automaton residual = genResidualGraph(auto);
		
		// we should continue if there is a path to final state in residual
		// graph.
		while (extractPath(residual) != null) {
			// find the augment path until no path can be found.
			Pair<Integer, LinkedList<AutoState>> pair = extractPath(residual);
			int min = pair.val0;

//			residual.dump();
//			System.out.println("--------------" + min + pair.val1);
//			auto.dump();
			AutoState src = pair.val1.getFirst();
			for (int i = 1; i < pair.val1.size(); i++) {
				AutoState tgt = pair.val1.get(i);
				AutoEdge ae = auto.getEdgeBySrc(src, tgt);

				//no such edge in original graph. modify inverse edge.
				if(ae == null) {
					AutoEdge invE = auto.getEdgeBySrc(tgt, src);
//					System.out.println(src + "->" +tgt+ " " + invE.getFlow() + "+" + min + "<=" + invE.getWeight());
					invE.setFlow(invE.getFlow() - min);
					invE.setShortName(invE.getFlow() + "/" + invE.getWeight());
					assert(invE.getFlow() >= 0 && invE.getFlow() <= invE.getWeight());
				} else {
					ae.setFlow(ae.getFlow() + min);
					ae.setShortName(ae.getFlow() + "/" + ae.getWeight());
					assert(ae.getFlow() <= ae.getWeight());
				}

				src = tgt;
			}
						
			residual = genResidualGraph(auto);
		}
		
//		System.out.println("Final result, dump cut set:");
//		residual.dump();
		
		// collect all the vertices(As set S) that are reachable from the
		// initial node
		LinkedList<AutoState> spath = dfs(residual);
		Set<AutoState> orgStates = new HashSet(auto.getStates());
		orgStates.retainAll(spath);
		LinkedList<AutoState> tpath = new LinkedList(auto.getStates());
		tpath.removeAll(orgStates);
		
		// collect all the edges that cross between S and T
		Set<CutEntity> cutset = new HashSet<CutEntity>();
		for(AutoState s : orgStates) {
			//outgoing edges.
			for (Iterator<AutoEdge> cIt = s.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge outEdge = cIt.next();
				AutoState tgt = s.outgoingStatesInvLookup(outEdge).iterator().next();
				if(tpath.contains(tgt) && (outEdge.getWeight()>0)) {
//					System.out.println(s + "->" + tgt + " " + outEdge.getWeight());
					cutset.add(new CutEntity(s,outEdge, tgt));
				}
			}
			
			for (Iterator<AutoEdge> cIt = s.incomingStatesInvIterator(); cIt
					.hasNext();) {
				//incoming edges.
				AutoEdge inEdge = cIt.next();
				AutoState src = s.incomingStatesInvLookup(inEdge).iterator().next();
				if( tpath.contains(src) && (inEdge.getWeight()>0)) {
//					System.out.println(src + "-*>" + s + " " + inEdge.getWeight());
					cutset.add(new CutEntity(src,inEdge, s));
				}
			}
		}
				
		return cutset;

	}
	
	/*reset all flow values to 0 for the new round.*/
	public static void resetAuto(Automaton auto) {
		for(AutoState s : auto.getStates()) {
			//outgoing edges.
			for (Iterator<AutoEdge> cIt = s.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge outEdge = cIt.next();
				outEdge.setFlow(0);
			}
			
			for (Iterator<AutoEdge> cIt = s.incomingStatesInvIterator(); cIt
					.hasNext();) {
				//incoming edges.
				AutoEdge inEdge = cIt.next();
				inEdge.setFlow(0);
			}
		}
	}

	public static Automaton genResidualGraph(Automaton g) {
		Automaton residual = cloner.deepClone(g);
		resetVisited(residual);

		for (AutoState as : residual.getStates())
			for (Iterator<AutoEdge> cIt = as.outgoingStatesInvIterator(); cIt
					.hasNext();) {
				AutoEdge e = cIt.next();
				assert(as.outgoingStatesInvLookup(e).size() == 1);

				assert (e.getWeight() != 0);
				int newWt = e.getWeight() - e.getFlow();
//				System.out.println("weight::" + e + "==>" + e.getFlow() + "/"
//						+ e.getWeight() + '=' + newWt);
				e.setResidual(newWt);
				e.setShortName(e.getWeight() + "");
				assert (newWt >= 0);
				if (e.getFlow() > 0) {
					// need to create new edge for the first time.
					assert(as.outgoingStatesInvLookup(e).size() == 1);
					AutoState tgt = as.outgoingStatesInvLookup(e).iterator()
							.next();
					assert(tgt!=null);
					if (residual.getEdgeBySrc(tgt, as) == null) {
						AutoEdge revEdge = new AutoEdge(cIt.hashCode(),
								e.getFlow(), e.getFlow() + "");
						revEdge.setResidual(e.getFlow());
						tgt.addOutgoingStates(as, revEdge);
						as.addIncomingStates(tgt, revEdge);
					}
				}
			}
		
		residual.validate();
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
			if(edge != null && (edge.getResidual() > 0)) {
				realpath.add(pre);
				cursor = pre;
				if (edge.getResidual() < minWt) minWt = edge.getResidual();
			}
		}

	    Collections.reverse(realpath);
		return new Pair<Integer, LinkedList<AutoState>>(minWt, realpath);
	}
}
