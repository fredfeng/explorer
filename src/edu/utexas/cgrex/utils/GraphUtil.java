package edu.utexas.cgrex.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;

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
}
