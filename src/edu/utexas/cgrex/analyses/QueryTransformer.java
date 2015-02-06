package edu.utexas.cgrex.analyses;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;
import soot.util.queue.QueueReader;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * Entry point of the whole analysis.
 * 
 * @author yufeng
 * 
 */
public class QueryTransformer extends SceneTransformer {

	public boolean debug = true;

	//CHA.
	QueryManager qm;
	
	//on-the-fly.
	QueryManager otfQm;
	
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		qm = new QueryManager(SootUtils.getCHA(), Scene.v().getMainMethod());
		this.runBenchmark();
	}
	
	private void runBenchmark() {
//		dumpMethods();
		// picking up samples from CHA-based version.
//		String regx = generator.genRegx();
		//false;
//		String regx = ".*<Test1: void main2()>.*<A: void bar()>";
//		String regx = ".*<Test1: void main1()>.*<B: void bar()>";

		//true;
//		String regx = ".*<Test1: void main2()>.*<B: void bar()>";
		String regx = ".*<Test1: void main1()>.*<A: void bar()>";
//		regx = "<Test2: void main(java.lang.String[])>.*<A: void goo()>";
//		
		regx = qm.getValidExprBySig(regx);
		
//		regx = regx.replaceAll("\\s+", "");
		System.out.println("Random regx------" + regx);
		boolean res1 = qm.queryRegx(regx);

		System.out.println("Query result:" + res1);
	}
}
