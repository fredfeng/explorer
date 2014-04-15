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
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
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
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * Top level class to perform query and call graph refinement.
 * 
 * @author yufeng
 * 
 */

public class QueryManager {

	// default CHA-based call graph.
	CallGraph cg;

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

	public QueryManager() {
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
			System.out.println("********" + uid + " " + meth);

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
		System.out.println("dump regular graph.");
		regAuto.dump();
	}

	private void buildCGAutomaton() {
		// Start from the main entry.
		SootMethod mainMeth = Scene.v().getMainMethod();
		// init FSM
		AutoEdge callEdgeMain = methToEdgeMap.get(mainMeth);

		// 0 is the initial id.
		CGAutoState initState = new CGAutoState(0, false, true);
		CGAutoState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);
		// no incoming edge for initstate.

		cgAuto.addInitState(initState);
		cgAuto.addStates(initState);

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
					curState.addOutgoingStates(curState, outEdge);
				} else {
					SootMethod tgtMeth = (SootMethod) e.getTgt();
//					if (visitedMap.get(tgtMeth))
//						continue;

					worklist.add(tgtMeth);
					AutoEdge outEdge = methToEdgeMap.get(tgtMeth);
					CGAutoState tgtState = methToStateMap.get(tgtMeth);
					curState.addOutgoingStates(tgtState, outEdge);
				}
			}

			// incoming edges
			Iterator<Edge> inIt = cg.edgesInto(worker);
			while (inIt.hasNext()) {
				Edge e = inIt.next();
				// how about SCC? FIXME!!
				if (e.getSrc().equals(worker)) {// recursive call, add self-loop
					AutoEdge inEdge = methToEdgeMap.get(worker);
					curState.addIncomingStates(curState, inEdge);
				} else {
					SootMethod srcMeth = (SootMethod) e.getSrc();
					AutoEdge inEdge = methToEdgeMap.get(srcMeth);
					CGAutoState srcState = methToStateMap.get(srcMeth);
					curState.addIncomingStates(srcState, inEdge);
				}
			}

		}

		// only add reachable methods.
		for (CGAutoState rs : reachableState)
			cgAuto.addStates(rs);

		// dump automaton of the call graph.
		System.out.println("dump Callgraph automaton.....");
		cgAuto.dump();
	}

	private void buildInterAutomaton() {
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();
		System.out.println("dump interset automaton.....");
		interAuto.dump();
	}

	private void grepMinCut() {

	}

	private boolean doPointsToQuery() {
		for (AutoState s : interAuto.getStates()) {
			// local vars
			if (s.getIncomingStatesInvKeySet().size() == 0)
				continue;
			AutoEdge inEdge = s.getIncomingStatesInvKeySet().iterator().next();
			SootMethod callerMeth = uidToMethMap.get(inEdge.getId());

			for (AutoState tgt : s.getOutgoingStatesKeySet()) {
				for (AutoEdge outEdge : s.outgoingStatesLookup(tgt)) {
					// type info.
					SootMethod calleeMeth = uidToMethMap.get(outEdge.getId());
					Type declaredType = calleeMeth.getDeclaringClass().getType();
					Set<Local> varSet = getVarList(callerMeth, calleeMeth);
					System.out.println("******perform points-to query:" + declaredType +"===" + varSet);
				}
			}
		}
		return false;
	}

	private void refineCallgraph() {
		// FIXME.
		cg.removeEdge(null);
	}

	// entry method for the query.
	public boolean doQuery(String regx) {

		buildRegAutomaton(regx);
		buildInterAutomaton();
		doPointsToQuery();

		return false;
	}

	//get a list of vars that can invoke method tgt.
	private Set<Local> getVarList(SootMethod method, SootMethod tgt) {
		Set<Local> varSet = new HashSet<Local>();
		if (!method.isConcrete())
			return varSet;

		Body body = method.retrieveActiveBody();
		System.out.println(body);

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
                		Local var = (Local)ie.getUseBoxes().get(0).getValue();
                		varSet.add(var);
                	}
                }
			}
		}
		return varSet;
	}

}
