package edu.utexas.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import test.util.cgfsm;
import test.util.cgfsmId;
import test.util.cgfsmState;
import edu.utexas.RegularPT.RegularPTTransformer;

public class CgFsmConverter {

	Map<SootMethod, cgfsmState> methToStateMap = new HashMap<SootMethod, cgfsmState>();
	
	Map<SootMethod, test.util.Edge> methToEdgeMap = new HashMap<SootMethod, test.util.Edge>();

	cgfsm cgFSM = new cgfsm();

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
		CallGraph cg = Scene.v().getCallGraph();
		
		Iterator<SootMethod> mIt = Scene.v().getMethodNumberer().iterator();
		
		while(mIt.hasNext()) {
			SootMethod meth = mIt.next();
			test.util.Edge inEdge = new test.util.Edge(new cgfsmId(meth.getName()));
			cgfsmState st = new cgfsmState(new cgfsmId("m" + meth.getNumber()), false, true, inEdge);

			methToStateMap.put(meth, st);
			methToEdgeMap.put(meth, inEdge);

			//cgFSM.addStates(st);
		}
		
		//while
		SootMethod mainMeth = Scene.v().getMainMethod();
		//init FSM
		test.util.Edge callEdgeMain = methToEdgeMap.get(mainMeth);
		cgfsmState initState = new cgfsmState(new cgfsmId(-1), false, true, null);
		cgfsmState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);

		cgFSM.addInitState(initState);
		cgFSM.addStates(initState);

		List<SootMethod> worklist = new LinkedList<SootMethod>();
		worklist.add(mainMeth);
		
		Set<cgfsmState> reachableState = new HashSet<cgfsmState>();
		
		while(worklist.size() > 0) {
			SootMethod worker = worklist.remove(0);
			cgfsmState curState = methToStateMap.get(worker);
			reachableState.add(curState);

			//worker.
			Iterator<Edge> eIt =  cg.edgesOutOf(worker);
			while(eIt.hasNext()) {
				Edge e = eIt.next();
				//how about SCC? FIXME!!
				if(e.getTgt().equals(worker)) {//recursive call, add self-loop
					test.util.Edge outEdge = methToEdgeMap.get(worker);
					curState.addOutgoingStates(curState, outEdge);
				} else {
					SootMethod tgtMeth = (SootMethod)e.getTgt();
					worklist.add(tgtMeth);
					test.util.Edge outEdge = methToEdgeMap.get(tgtMeth);
					cgfsmState tgtState = methToStateMap.get(e.getTgt());
					curState.addOutgoingStates(tgtState, outEdge);
				}
			}
	
		}
		
	
		//only add reachable methods.
		for(cgfsmState rs : reachableState) {
			cgFSM.addStates(rs);
			//map reachable states to alphabic number.
		}
			
		cgFSM.dump();
		
	}
	

	
	

}
