package edu.utexas.cgrex.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;

import soot.CompilationDeathException;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import edu.utexas.RegularPT.RegularPTTransformer;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;

public class CgFsmConverter {

	CallGraph cg;

	Map<SootMethod, CGAutoState> methToStateMap = new HashMap<SootMethod, CGAutoState>();
	
	Map<SootMethod, AutoEdge> methToEdgeMap = new HashMap<SootMethod, AutoEdge>();
	
	//map JSA's automaton to our own regstate.
	Map<State, RegAutoState> jsaToAutostate = new HashMap<State,RegAutoState>();

	//each sootmethod will be represented by the unicode of its number.
	Map<String, SootMethod> uidToMethMap = new HashMap<String, SootMethod>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Convert cg to fsm...");
		
		  String targetLoc = "benchmarks/CFLexamples/bin/";

			try {

				StringBuilder options = new StringBuilder();			
				PackManager.v().getPack("wjtp")
						.add(new Transform("wjtp.regularPT", new RegularPTTransformer()));

				soot.Main.v().run(new String[] {
						"-W",
						"-process-dir", targetLoc,
						"-src-prec", "java",
						"-allow-phantom-refs",
						"-no-bodies-for-excluded",
						"-exclude", "java",
						"-exclude", "javax",
						"-output-format", "none",
						"-p", "jb", "use-original-names:true",
						//"-p", "cg.cha", "on",
						"-p", "cg.spark", "on",
						"-debug"} );
				
				new CgFsmConverter().generateFSM();

			} catch (CompilationDeathException e) {
				e.printStackTrace();
				if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
					throw e;
				else
					return;
			}
	}
	
	
	//method's encoding rule will be "m" plus its number, e.g. m10, m11, etc.
	
	public void generateFSM() {
		CGAutomaton cgAuto = new CGAutomaton();	

		CallGraph cg = Scene.v().getCallGraph();
		
		Iterator<MethodOrMethodContext> mIt = Scene.v().getReachableMethods().listener();

		while (mIt.hasNext()) {
			SootMethod meth = (SootMethod) mIt.next();

			// map each method to a unicode.
			String uid = "\\u" + String.format("%04d", meth.getNumber());
			uidToMethMap.put(uid, meth);

			AutoEdge inEdge = new AutoEdge(meth.getName());
			CGAutoState st = new CGAutoState("m" + meth.getNumber(),false, true);
			methToStateMap.put(meth, st);
			methToEdgeMap.put(meth, inEdge);
		}
		
		//while
		SootMethod mainMeth = Scene.v().getMainMethod();
		//init FSM
		AutoEdge callEdgeMain = methToEdgeMap.get(mainMeth);
		CGAutoState initState = new CGAutoState(-1, false, true);
		CGAutoState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);

		cgAuto.addInitState(initState);
		cgAuto.addStates(initState);

		List<SootMethod> worklist = new LinkedList<SootMethod>();
		worklist.add(mainMeth);
		
		Set<CGAutoState> reachableState = new HashSet<CGAutoState>();
		
		while(worklist.size() > 0) {
			SootMethod worker = worklist.remove(0);
			CGAutoState curState = methToStateMap.get(worker);
			reachableState.add(curState);

			//worker.
			Iterator<Edge> eIt =  cg.edgesOutOf(worker);
			while(eIt.hasNext()) {
				Edge e = eIt.next();
				//how about SCC? FIXME!!
				if(e.getTgt().equals(worker)) {//recursive call, add self-loop
					AutoEdge outEdge = methToEdgeMap.get(worker);
					curState.addOutgoingStates(curState, outEdge);
				} else {
					SootMethod tgtMeth = (SootMethod)e.getTgt();
					worklist.add(tgtMeth);
					AutoEdge outEdge = methToEdgeMap.get(tgtMeth);
					CGAutoState tgtState = methToStateMap.get(e.getTgt());
					curState.addOutgoingStates(tgtState, outEdge);
				}
			}
	
		}
	
		//only add reachable methods.
		for(CGAutoState rs : reachableState)
			cgAuto.addStates(rs);
		
		//dump automaton of the call graph.
		cgAuto.dump();
		
		//now i am gonna to write regx based previous mapping.
		String query = "(\u6106|\u6099).*\u1150";
		RegExp r = new RegExp(query);
		System.out.println(r.toAutomaton().toDot());
		
		new REtoFSMConverter().doConvert(query, uidToMethMap);
		
	}
	

	
	

}
