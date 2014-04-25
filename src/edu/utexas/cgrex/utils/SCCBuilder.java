package edu.utexas.cgrex.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.DirectedGraph;

public class SCCBuilder {
	  protected final List<List<Object>> componentList = new ArrayList<List<Object>>();

	  protected int index = 0;

	  protected Map<Object,Integer> indexForNode, lowlinkForNode;

	  protected Stack<Object> s;

	  protected CallGraph g;
	    
	  /**
	   *  @param g a graph for which we want to compute the strongly
	   *           connected components. 
	   *  @see DirectedGraph
	   */
	  public SCCBuilder(CallGraph g, Set roots)
	  {
	    this.g = g;
	    s = new Stack<Object>();
	    Set<Object> heads = roots;

	    indexForNode = new HashMap<Object, Integer>();
	    lowlinkForNode = new HashMap<Object, Integer>();

	    for(Iterator<Object> headsIt = heads.iterator(); headsIt.hasNext(); ) {
	      Object head = headsIt.next();
	      if(!indexForNode.containsKey(head)) {
	        recurse((SootMethod)head);
	      }
	    }

	    //free memory
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
			
		for (Iterator<Edge> cIt = g.edgesOutOf(v); cIt.hasNext();) {
		  SootMethod succ = (SootMethod)cIt.next().getTgt();

	      if(!indexForNode.containsKey(succ)) {
	        recurse(succ);
	        lowlinkForNode.put(v, Math.min(lowlinkForNode.get(v), lowlinkForNode.get(succ)));
	      } else if(s.contains(succ)) {
	        lowlinkForNode.put(v, Math.min(lowlinkForNode.get(v), indexForNode.get(succ)));
	      }			
	    }
	    if(lowlinkForNode.get(v).intValue() == indexForNode.get(v).intValue()) {
	      List<Object> scc = new ArrayList<Object>();
	      Object v2;
	      do {
	        v2 = s.pop();
	        scc.add(v2);
	      }while(v!=v2);			
	      componentList.add(scc);
	    }
	  }

	  /**
	   *   @return the list of the strongly-connected components
	   */
	  public List<List<Object>> getComponents()
	  {
	    return componentList;
	  }
}
