package test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootMethod;
import chord.util.graph.IGraph;
import chord.util.graph.MutableGraph;

public class TestSCC {

   /*
    * @param roots A set of nodes designated as roots of the graph.
    *        It may be null, in which case every node in the graph is treated as a root node.
    * @param nodeToPreds A map from each node in the graph to the set of all its immediate predecessor nodes.
    *        It may be null.
    * @param nodeToSuccs A map from each node in the graph to the set of all its immediate successor nodes.
    *        It may be null.
    */
    private void doAnalysis() {
    	Set<Object> roots = new HashSet();
    	//please fill in the map by yourself.
        Map<Object, Set<Object>> nodeToPreds = new HashMap();
        Map<Object, Set<Object>> nodeToSuccs = new HashMap();

		IGraph<Object> graph = new MutableGraph<Object>(roots,
				nodeToPreds, nodeToSuccs);
		//this will give you a list of scc.
		List<Set<Object>> sccList = graph.getTopSortedSCCs();
		int n = sccList.size();
    }

}
