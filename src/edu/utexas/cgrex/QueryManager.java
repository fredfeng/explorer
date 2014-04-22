package edu.utexas.cgrex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import soot.Body;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
import edu.utexas.cgrex.test.RegularExpGenerator;
import edu.utexas.cgrex.utils.CutEntity;
import edu.utexas.cgrex.utils.GraphUtil;
import edu.utexas.cgrex.utils.SootUtils;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * Top level class to perform query and call graph refinement.
 * 
 * @author yufeng
 * 
 */

public class QueryManager {
	
	AutoPAG autoPAG;

	private int INFINITY = 9999;

	// default CHA-based call graph.
	CallGraph cg;
	
	Map<Value, Edge> valueToCallEdgeMap = new HashMap<Value, Edge>();

	Map<SootMethod, CGAutoState> methToStateMap = new HashMap<SootMethod, CGAutoState>();

	// make sure each method only be visited once.
	Map<SootMethod, Boolean> visitedMap = new HashMap<SootMethod, Boolean>();

	Map<SootMethod, AutoEdge> methToEdgeMap = new HashMap<SootMethod, AutoEdge>();

	// map JSA's automaton to our own regstate.
	Map<State, RegAutoState> jsaToAutostate = new HashMap<State, RegAutoState>();

	// each sootmethod will be represented by the unicode of its number.
	Map<String, SootMethod> uidToMethMap = new HashMap<String, SootMethod>();

	// automaton for call graph.
	CGAutomaton cgAuto;

	// automaton for regular expression.
	RegAutomaton regAuto;

	// automaton for intersect graph.
	InterAutomaton interAuto;

	public QueryManager(AutoPAG autoPAG) {
		this.autoPAG = autoPAG;
		cgAuto = new CGAutomaton();
		regAuto = new RegAutomaton();

		init();
	}

	private void init() {
		cg = Scene.v().getCallGraph();

		Iterator<MethodOrMethodContext> mIt = Scene.v().getReachableMethods()
				.listener();

		while (mIt.hasNext()) {
			SootMethod meth = (SootMethod) mIt.next();

			// map each method to a unicode.
			String uid = "\\u" + String.format("%04d", meth.getNumber());
			uidToMethMap.put(uid, meth);
//			System.out.println("********" + uid + " " + meth);

			AutoEdge inEdge = new AutoEdge(uid);
			inEdge.setShortName(meth.getName());
			CGAutoState st = new CGAutoState(meth.getNumber(), false, true);

			methToStateMap.put(meth, st);
			methToEdgeMap.put(meth, inEdge);
			visitedMap.put(meth, false);
		}
		// only build cgauto once.
		buildCGAutomaton();
	}

	private void buildRegAutomaton(String regx) {
		regx = StringEscapeUtils.unescapeJava(regx);
		// step 1. Constructing a reg without .*
		RegExp r = new RegExp(regx);
		Automaton auto = r.toAutomaton();
		regAuto = new RegAutomaton();
		// Set<RegAutoState> finiteStates = new HashSet<RegAutoState>();
		Set<State> states = auto.getStates();
		int number = 1;

		for (State s : states) {
			// Map<State, Edge>
			// Map<AutoEdge, Set<AutoState>> incomeStates = new
			// HashMap<AutoEdge, Set<AutoState>>();
			RegAutoState mystate = new RegAutoState(number, false, false);

			// mystate.setOutgoingStatesInv(incomeStates);

			// use number to represent state id.
			jsaToAutostate.put(s, mystate);
			number++;
		}

		for (State s : states) {
			RegAutoState fsmState = jsaToAutostate.get(s);
			// Map<State, Edge>
			// Map<AutoState,Set<AutoEdge>> outgoingStates = new
			// HashMap<AutoState,Set<AutoEdge>>();

			if (s.isAccept()) {
				fsmState.setFinalState();
				regAuto.addFinalState(fsmState);
			}

			// System.out.println(auto.toDot());

			if (s.equals(auto.getInitialState())) {
				fsmState.setInitState();
				regAuto.addInitState(fsmState);
			}

			for (Transition t : s.getTransitions()) {
				RegAutoState tgtState = jsaToAutostate.get(t.getDest());
				// Map tgtIncome = tgtState.getOutgoingStatesInv();
				// using edge label as id.
				String unicode = StringUtil.appendChar(t.getMin(),
						new StringBuilder(""));
				String shortName = ".";
				AutoEdge outEdge = new AutoEdge(unicode);

				if (uidToMethMap.get(unicode) != null) {
					shortName = uidToMethMap.get(unicode).getName();
				} else {
					outEdge.setDotEdge();
				}
				outEdge.setShortName(shortName);

				fsmState.addOutgoingStates(tgtState, outEdge);
				tgtState.addIncomingStates(fsmState, outEdge);
			}

			regAuto.addStates(fsmState);
		}
		// dump current result.
//		System.out.println("dump regular graph.");
		regAuto.dump();
	}

	private void buildCGAutomaton() {
		// Start from the main entry.
		SootMethod mainMeth = Scene.v().getMainMethod();
		// init FSM
		AutoEdge callEdgeMain = methToEdgeMap.get(mainMeth);

		// 0 is the initial id.
		CGAutoState initState = new CGAutoState(0, true, true);
		CGAutoState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);
		mainState.addIncomingStates(initState, callEdgeMain);

		// no incoming edge for initstate.

		cgAuto.addInitState(initState);
		cgAuto.addStates(initState);

		//FIXME: should not use list.
		List<SootMethod> worklist = new LinkedList<SootMethod>();
		worklist.add(mainMeth);

		Set<CGAutoState> reachableState = new HashSet<CGAutoState>();

		while (worklist.size() > 0) {
			SootMethod worker = worklist.remove(0);
			visitedMap.put(worker, true);
			CGAutoState curState = methToStateMap.get(worker);
			reachableState.add(curState);

			// worker. outgoing edges
			Iterator<Edge> outIt = cg.edgesOutOf(worker);
			while (outIt.hasNext()) {
				Edge e = outIt.next();
				// how about SCC? FIXME!!
				if (e.getTgt().equals(worker)) {// recursive call, add self-loop
					AutoEdge outEdge = methToEdgeMap.get(worker);
					//need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(worker.getName());

					curState.addOutgoingStates(curState, outEdgeFresh);
					
					curState.addIncomingStates(curState, outEdgeFresh);

				} else {
					SootMethod tgtMeth = (SootMethod) e.getTgt();
					if (!visitedMap.get(tgtMeth))
						worklist.add(tgtMeth);

					AutoEdge outEdge = methToEdgeMap.get(tgtMeth);
					//need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(tgtMeth.getName());

					CGAutoState tgtState = methToStateMap.get(tgtMeth);
					curState.addOutgoingStates(tgtState, outEdgeFresh);
					
					//add incoming state.
					tgtState.addIncomingStates(curState, outEdgeFresh);
				}
			}

		}

		// only add reachable methods.
		for (CGAutoState rs : reachableState)
			cgAuto.addStates(rs);

		// dump automaton of the call graph.
		cgAuto.dump();
		cgAuto.validate();
	}

	private void buildInterAutomaton() {
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();
//		System.out.println("dump interset automaton.....");
		interAuto.validate();
//		interAuto.dump();
		
		//before we do the mincut, we need to exclude some trivial cases
		//such as special invoke, static invoke and certain virtual invoke.
		if(interAuto.getFinalStates().size() == 0) return;
				
		//Stop conditions:
		//1. Refute all edges in current cut set;(Yes)
		//2. Can not find a mincut without infinity anymore.(No)
		Set<CutEntity> cutset = GraphUtil.minCut(interAuto);

		boolean answer = false;
		//contains infinity edge?
		while(!hasInfinityEdges(cutset)) {
	
			answer = false;
			for(CutEntity e : cutset){
				if(doPointsToQuery(e)) {
					System.out.println("--------VERIFY one Edge.");

					answer = true; 
					e.edge.setInfinityWeight();
				} else { //e is a false positive.
					//remove this edge and refine call graph.
					//remove this edge from interauto.
					interAuto.deleteOneEdge(e.state, e.endState, e.edge);
				}
			}
			
			//all edges are refute, stop.
			if(!answer) break;
			//modify visited edges and continue.
			cutset = GraphUtil.minCut(interAuto);
		}
		
		if(answer)
			System.out.println("---------query done, answer is: YES");
		else 
			System.out.println("---------query done, answer is: NO");

//		interAuto.dump();
		
	}
	
	private boolean hasInfinityEdges(Set<CutEntity> set) {
		for(CutEntity e : set)
			if(e.edge.getWeight() == INFINITY) return true;
		
		return false;
	}

	private boolean doPointsToQuery(CutEntity cut) {
		
		// type info.
		SootMethod calleeMeth = uidToMethMap.get(cut.edge.getId());
		if(calleeMeth.isMain()) return true;
		
//		Type declaredType = calleeMeth.getDeclaringClass().getType();

		AutoEdge inEdge = cut.state.getIncomingStatesInvKeySet().iterator().next();
		SootMethod callerMeth = uidToMethMap.get(inEdge.getId());

		List<Value> varSet = getVarList(callerMeth, calleeMeth);
		List<Type> typeSet = SootUtils.compatibleTypeList(
				calleeMeth.getDeclaringClass(), calleeMeth);
		
		//refine call graph with detail info.
		Map<Value, Boolean> detailMap = autoPAG.insensitiveRefine(varSet, typeSet);
		for(Value v : detailMap.keySet()) {
			if(!detailMap.get(v))
				refineCallgraph(v);
		}

		if(varSet.size() == 0) return true;
		
		return autoPAG.insensitiveQuery(varSet, typeSet);
	}

	private void refineCallgraph(Value v) {
		// FIXME.
		Edge e = valueToCallEdgeMap.get(v);
		System.out.println("---------Refine call edge: " + e);
		cg.removeEdge(e);
	}

	// entry method for the query.
	public boolean doQuery(String regx) {
		
		//ignore user input, run our own batch test.
//		int benchmarkSize = 10000;
//		RegularExpGenerator generator = new RegularExpGenerator(methToEdgeMap);
//		for(int i = 0; i < benchmarkSize; i++) {
//			regx = generator.genRegx();		
//			regx = regx.replaceAll("\\s+","");
//			System.out.println("Random regx------" + regx);
//			buildRegAutomaton(regx);
//			buildInterAutomaton();
//		}
		
		//multiple edges:
		regx = "(\u6106|\u5085).*\u0109";
//		regx = "(\u2443|\u6106).*\u6101";
		buildRegAutomaton(regx);
		buildInterAutomaton();
		
//		regx = regx.replaceAll("\\s+","");
//
//		buildRegAutomaton(regx);
//		buildInterAutomaton();

		return false;
	}

	//get a list of vars that can invoke method tgt.
	private List<Value> getVarList(SootMethod method, SootMethod tgt) {
		List<Value> varSet = new LinkedList<Value>();
		if (!method.isConcrete())
			return varSet;

		Body body = method.retrieveActiveBody();

		Chain<Unit> units = body.getUnits();
		Iterator<Unit> uit = units.snapshotIterator();
		while (uit.hasNext()) {
			Stmt stmt = (Stmt) uit.next();

			// invocation statements
			if (stmt.containsInvokeExpr()) {
				InvokeExpr ie = stmt.getInvokeExpr();
                if( (ie instanceof VirtualInvokeExpr) 
                		|| (ie instanceof InterfaceInvokeExpr)){
                	SootMethod callee = ie.getMethod();
                	if(tgt.equals(callee)) {
                		//ie.get
                		Value var = ie.getUseBoxes().get(0).getValue();
                		varSet.add(var);
                		Edge calledge = cg.findEdge(stmt, ie.getMethod());
                		//FIXME: is this correct to map value to its callsite?
                		valueToCallEdgeMap.put(var, calledge);
                	}
                }
			}
		}
		return varSet;
	}

}
