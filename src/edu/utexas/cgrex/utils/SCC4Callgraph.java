package edu.utexas.cgrex.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class SCC4Callgraph {
	protected final List<Set<SootMethod>> componentList = new ArrayList<Set<SootMethod>>();

	protected int index = 0;

	protected Map<SootMethod, Integer> indexForNode, lowlinkForNode;

	protected Stack<SootMethod> s;

	protected CallGraph g;

	/**
	 * @param g: a automaton for which we want to compute the strongly connected
	 * components.
	 */
	public SCC4Callgraph(CallGraph g, Set<SootMethod> roots) {
		this.g = g;
		s = new Stack<SootMethod>();
		Set<SootMethod> heads = roots;

		indexForNode = new HashMap<SootMethod, Integer>();
		lowlinkForNode = new HashMap<SootMethod, Integer>();

		for (Iterator<SootMethod> headsIt = heads.iterator(); headsIt.hasNext();) {
			SootMethod head = headsIt.next();
			if (!indexForNode.containsKey(head)) {
				recurse(head);
			}
		}

		// free memory
		indexForNode = null;
		lowlinkForNode = null;
		s = null;
		g = null;
	}

	protected void recurse(SootMethod v) {
		indexForNode.put(v, index);
		lowlinkForNode.put(v, index);
		index++;
		s.push(v);

		Iterator<Edge> it = g.edgesOutOf(v);
		while(it.hasNext()) {
			Edge e = it.next();
			SootMethod succ = (SootMethod)e.getTgt();

			if (!indexForNode.containsKey(succ)) {
				recurse(succ);
				lowlinkForNode.put(
						v,
						Math.min(lowlinkForNode.get(v),
								lowlinkForNode.get(succ)));
			} else if (s.contains(succ)) {
				lowlinkForNode
						.put(v,
								Math.min(lowlinkForNode.get(v),
										indexForNode.get(succ)));
			}
		}
		if (lowlinkForNode.get(v).intValue() == indexForNode.get(v).intValue()) {
			Set<SootMethod> scc = new HashSet<SootMethod>();
			SootMethod v2;
			do {
				v2 = s.pop();
				scc.add(v2);
			} while (v != v2);
			componentList.add(scc);
		}
	}

	/**
	 * @return the list of the strongly-connected components
	 */
	public List<Set<SootMethod>> getComponents() {
		return componentList;
	}
}
