package edu.utexas.cgrex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import soot.AnySubType;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.queue.QueueReader;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import edu.utexas.cgrex.analyses.CgContext;
import edu.utexas.cgrex.analyses.DemandCSPointsTo;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.CGAutoEdge;
import edu.utexas.cgrex.automaton.CGAutoState;
import edu.utexas.cgrex.automaton.CGAutomaton;
import edu.utexas.cgrex.automaton.InterAutoEdge;
import edu.utexas.cgrex.automaton.InterAutoOpts;
import edu.utexas.cgrex.automaton.InterAutomaton;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
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

	private int INFINITY = 9999;
	
	// default CHA-based call graph or on-the-fly.
	CallGraph cg;
	
	// this magic number is used to fix the error by invalid unicode such as
	// \u0022
	private int offset = 100;
	
	private boolean ignoreAsync = false;
	
	public static long ptTime = 0;
	public static long cutTime = 0;
	
	//want to get a conservative result in the present of reflection?
	public final boolean includeAnyType = false;

	ReachableMethods reachableMethods;

	Map<SootMethod, CGAutoState> methToStateMap = new HashMap<SootMethod, CGAutoState>();

	/**
	 * @return the methToEdgeMap only for the purpose of generating random regx.
	 */
	public Map<SootMethod, AutoEdge> getMethToEdgeMap() {
		return methToEdgeMap;
	}

	Map<SootMethod, AutoEdge> methToEdgeMap = new HashMap<SootMethod, AutoEdge>();
	
	Map<Pair<Stmt, SootMethod>, AutoEdge> invkToEdgeMap = new HashMap<Pair<Stmt, SootMethod>, AutoEdge>();

	// map JSA's automaton to our own regstate.
	Map<State, RegAutoState> jsaToAutostate = new HashMap<State, RegAutoState>();

	// each sootmethod will be represented by the unicode of its number.
	public static Map<String, SootMethod> uidToMethMap = new HashMap<String, SootMethod>();
	
	Map<SootMethod, String> methToUidMap = new HashMap<SootMethod, String>();
	
	SootMethod mainMethod;

	// automaton for call graph.
	CGAutomaton cgAuto;
	
	// automaton for eager call graph.
	CGAutomaton cgEagerAuto;

	// automaton for regular expression.
	RegAutomaton regAuto;

	// automaton for intersect graph.
	InterAutomaton interAuto;
	
	/*points-to analysis for eager version*/
	private DemandCSPointsTo ptsDemand;
	
	private boolean debug = false;
	
	//run eager or not?
	private boolean runEager = false;
	
	//reachable methods
	private Set<SootMethod> reachableMethSet = new HashSet<SootMethod>();

	public QueryManager(CallGraph cg, SootMethod meth) {
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		this.setMainMethod(meth);
		this.initQM(cg, false);
	}
	
	public QueryManager(CallGraph cg, SootMethod meth, boolean flag) {
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		ignoreAsync(flag);
		
		ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		this.setMainMethod(meth);
		this.initQM(cg, false);
	}
	
	public QueryManager(SootMethod main, Set<SootMethod> setM,
			Set<Trio<SootMethod, Stmt, SootMethod>> edgeSet, boolean flag) {
		mainMethod = main;
		cgAuto = new CGAutomaton();
		regAuto = new RegAutomaton();
		ignoreAsync(flag);
		
		System.out.println("init reachables: " + setM.size());
		
		String mainId = "\\u" + String.format("%04x", mainMethod.getNumber() + offset);
		uidToMethMap.put(mainId, mainMethod);
		methToUidMap.put(mainMethod, mainId);
		CGAutoState mst = new CGAutoState(mainId, false, true);
		mst.setDesc(mainMethod.getName() + "|"
				+ mainMethod.getDeclaringClass().getName());
		methToStateMap.put(mainMethod, mst);
		
		for (SootMethod meth : setM) {
			reachableMethSet.add(meth);
			// map each method to a unicode.
			String uid = "\\u"
					+ String.format("%04x", meth.getNumber() + offset);
			uidToMethMap.put(uid, meth);
			methToUidMap.put(meth, uid);

			CGAutoState st = new CGAutoState(uid, false, true);
			st.setDesc(meth.getName() + "|"
					+ meth.getDeclaringClass().getName());

			methToStateMap.put(meth, st);
		}
		
		for(Trio<SootMethod,Stmt,SootMethod> trio : edgeSet) {
			SootMethod tgtMethod = trio.val2;
			if(!reachableMethSet.contains(tgtMethod))
				continue;

			Stmt st = trio.val1;
			String uid = methToUidMap.get(tgtMethod);
			assert uid != null : "tgt method: " + tgtMethod;
			CGAutoEdge inEdge = new CGAutoEdge(uid, st);
			inEdge.setShortName(st != null ? st.toString() : "null");
			invkToEdgeMap
					.put(new Pair<Stmt, SootMethod>(st, tgtMethod), inEdge);
		}
		
		// only build cgauto once.
		long startDd = System.nanoTime();
		buildCGAutomaton(edgeSet);
		long endDd = System.nanoTime();
		StringUtil.reportSec("Time To build Demand CG:", startDd, endDd);
	}
	
	public void initQM(CallGraph cg, boolean flag) {
		
		runEager = flag;
		
		cgAuto = new CGAutomaton();
		if(runEager)
			cgEagerAuto = new CGAutomaton();

		regAuto = new RegAutomaton();
		this.cg = cg;

		init();
	}
	
	public PointsToAnalysis getDemandPointsTo() {
		return ptsDemand;
	}
	
	public ReachableMethods getReachableMethods() {
		if (reachableMethods == null) {
			reachableMethods = new ReachableMethods(cg,
					new ArrayList<MethodOrMethodContext>(Scene.v().getEntryPoints()));
			assert !Scene.v().getEntryPoints().isEmpty() : "No entry points.";
		}
		reachableMethods.update();
		return reachableMethods;
	}

	private void init() {

		Iterator<MethodOrMethodContext> mIt = this.getReachableMethods()
				.listener();
		// this magic number is used to fix the error by invalid unicode such as
		// \u0022
		System.out.println("init reachables: "
				+ this.getReachableMethods().size());
		// this magic number is used to fix the error by invalid unicode such as
		// \u0022
		
		String mainId = "\\u" + String.format("%04x", mainMethod.getNumber() + offset);
		uidToMethMap.put(mainId, mainMethod);
		methToUidMap.put(mainMethod, mainId);
		CGAutoState mst = new CGAutoState(mainId, false, true);
		mst.setDesc(mainMethod.getName() + "|"
				+ mainMethod.getDeclaringClass().getName());
		methToStateMap.put(mainMethod, mst);
		
		while (mIt.hasNext()) {
			SootMethod meth = (SootMethod) mIt.next();
			reachableMethSet.add(meth);

			// map each method to a unicode.
			String uid = "\\u" + String.format("%04x", meth.getNumber() + offset);
			uidToMethMap.put(uid, meth);
			methToUidMap.put(meth, uid);

			CGAutoState st = new CGAutoState(uid, false, true);
			st.setDesc(meth.getName() + "|"
					+ meth.getDeclaringClass().getName());

			methToStateMap.put(meth, st);
		}
		
		QueueReader<Edge> qr = cg.listener();
		while(qr.hasNext()) {
			Edge callEdge = qr.next();
			SootMethod tgtMethod = (SootMethod)callEdge.getTgt();
			if(!reachableMethSet.contains(tgtMethod))
				continue;

			Stmt st = callEdge.srcStmt();
			String uid = methToUidMap.get(tgtMethod);
			assert uid != null : "tgt method: " + tgtMethod;
			CGAutoEdge inEdge = new CGAutoEdge(uid, st);
			inEdge.setShortName(st != null ? st.toString() : "null");
			invkToEdgeMap
					.put(new Pair<Stmt, SootMethod>(st, tgtMethod), inEdge);
		}
		
		// only build cgauto once.
		long startDd = System.nanoTime();
		buildCGAutomaton();
		long endDd = System.nanoTime();
		StringUtil.reportSec("Time To build Demand CG:", startDd, endDd);
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
			if (s.isAccept()) {
				fsmState.setFinalState();
				regAuto.addFinalState(fsmState);
			}

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
				String shortName = "undefined";
				AutoEdge outEdge = new AutoEdge(unicode);

				if (uidToMethMap.get(unicode) != null) {
					shortName = uidToMethMap.get(unicode).getName();
				} else if(t.getDest().equals(s)){
					outEdge.setDotEdge();
					shortName = ".";
				} 
				
				outEdge.setShortName(shortName);
				fsmState.addOutgoingStates(tgtState, outEdge);
				tgtState.addIncomingStates(fsmState, outEdge);
			}

			regAuto.addStates(fsmState);
		}
		// dump current result.
		// System.out.println("dump regular graph.");
		// regAuto.dump();
	}
	
	public void setMainMethod(SootMethod meth) {
		mainMethod = meth;
	}
	
	/* Build cg automaton from chord's call graph.*/
	private void buildCGAutomaton(
			Set<Trio<SootMethod, Stmt, SootMethod>> edgeSet) {
		// Start from the main entry.
		assert mainMethod != null;
		SootMethod mainMeth = mainMethod;
		// init FSM
		String mainId = methToUidMap.get(mainMeth);
		assert mainId != null : "empty main id.";
		CGAutoEdge callEdgeMain = new CGAutoEdge(mainId, null);
		callEdgeMain.setShortName("init");

		// 0 is the initial id.
		CGAutoState initState = new CGAutoState(0, true, true);
		initState.setDesc("init");
		CGAutoState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);
		mainState.addIncomingStates(initState, callEdgeMain);

		// no incoming edge for initstate.
		cgAuto.addInitState(initState);
		cgAuto.addStates(initState);

		Set<CGAutoState> reachableState = new HashSet<CGAutoState>();

		for (Trio<SootMethod, Stmt, SootMethod> trio : edgeSet) {
			SootMethod worker = trio.val0;
			CGAutoState curState = methToStateMap.get(worker);
			reachableState.add(curState);
			if (ignoreAsync && SootUtils.asyncClass(worker)) {
				continue;
			}

			Stmt srcStmt = trio.val1;
			SootMethod tgtMeth = trio.val2;
			CGAutoState tgtState = methToStateMap.get(tgtMeth);
			reachableState.add(tgtState);

			// how about SCC? FIXME!!
			if (tgtMeth.equals(worker)) {// recursive call, add self-loop
				AutoEdge outEdge = invkToEdgeMap
						.get(new Pair<Stmt, SootMethod>(srcStmt, worker));
				// need fresh instance for each callsite but share same uid.
				curState.addOutgoingStates(curState, outEdge);
				curState.addIncomingStates(curState, outEdge);
			} else {
				AutoEdge outEdge = invkToEdgeMap
						.get(new Pair<Stmt, SootMethod>(srcStmt, tgtMeth));
				// need fresh instance for each callsite but share same uid.
				curState.addOutgoingStates(tgtState, outEdge);
				// add incoming state.
				assert outEdge != null : outEdge;
				assert curState != null : curState;
				tgtState.addIncomingStates(curState, outEdge);
			}
		}
		
		//prune all states that can not be reached from main.
		LinkedList<AutoState> worklist = new LinkedList<AutoState>();
		Set<AutoState> visited = new HashSet<AutoState>();
		worklist.add(mainState);
		while(!worklist.isEmpty()) {
			AutoState worker = worklist.poll();
			if(visited.contains(worker))
				continue;
			
			visited.add(worker);
			worklist.addAll(worker.getOutgoingStatesKeySet());
		}
		
		// only add reachable methods.
		for (AutoState rs : visited)
			cgAuto.addStates(rs);

		// dump automaton of the call graph.
		// cgAuto.validate();
		// cgAuto.dump();
	}
	
	private void buildCGAutomaton() {
		// Start from the main entry.
		assert mainMethod != null;
		SootMethod mainMeth = mainMethod;
		// init FSM
		String mainId = methToUidMap.get(mainMeth);
		assert mainId != null : "empty main id.";
		CGAutoEdge callEdgeMain = new CGAutoEdge(mainId, null);
		callEdgeMain.setShortName("init");

		// 0 is the initial id.
		CGAutoState initState = new CGAutoState(0, true, true);
		initState.setDesc("init");
		CGAutoState mainState = methToStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);
		mainState.addIncomingStates(initState, callEdgeMain);

		// no incoming edge for initstate.

		cgAuto.addInitState(initState);
		cgAuto.addStates(initState);

		// FIXME: should not use list.
		List<SootMethod> worklist = new LinkedList<SootMethod>();
		worklist.add(mainMeth);

		Set<CGAutoState> reachableState = new HashSet<CGAutoState>();
		Set<SootMethod> visited = new HashSet<SootMethod>();


		while (worklist.size() > 0) {
			SootMethod worker = worklist.remove(0);
			if(visited.contains(worker))
				continue;
			visited.add(worker);
			if (ignoreAsync && SootUtils.asyncClass(worker)) {
				continue;
			}
			CGAutoState curState = methToStateMap.get(worker);
			reachableState.add(curState);
			
			// worker. outgoing edges
			Iterator<Edge> outIt = cg.edgesOutOf(worker);
			while (outIt.hasNext()) {
				Edge e = outIt.next();
				Stmt srcStmt = e.srcStmt();
				SootMethod tgtMeth = (SootMethod) e.getTgt();
				if(!reachableMethSet.contains(tgtMeth)) {
					System.err.println("unreachable target method: " + tgtMeth.getSignature());
					assert !Scene.v().containsMethod(tgtMeth.getSignature()) : tgtMeth;
					continue;
				}
				// how about SCC? FIXME!!
				if (tgtMeth.equals(worker)) {// recursive call, add self-loop
					AutoEdge outEdge = invkToEdgeMap
							.get(new Pair<Stmt, SootMethod>(srcStmt, worker));
					assert outEdge != null : e;
					// need fresh instance for each callsite but share same uid.
					curState.addOutgoingStates(curState, outEdge);
					curState.addIncomingStates(curState, outEdge);
				} else {
					worklist.add(tgtMeth);
					AutoEdge outEdge = invkToEdgeMap
							.get(new Pair<Stmt, SootMethod>(srcStmt, tgtMeth));
					assert outEdge != null : e;
					// need fresh instance for each callsite but share same uid.
					CGAutoState tgtState = methToStateMap.get(tgtMeth);
					curState.addOutgoingStates(tgtState, outEdge);
					// add incoming state.
					assert outEdge != null : outEdge;
					assert curState != null : curState;
					tgtState.addIncomingStates(curState, outEdge);
				}
			}

		}

		// only add reachable methods.
		for (CGAutoState rs : reachableState)
			cgAuto.addStates(rs);

		// dump automaton of the call graph.
		// cgAuto.validate();
		// cgAuto.dump();
	}
	
	public boolean isReachable(String m) {
		if(!Scene.v().containsMethod(m))
			return false;
		
		SootMethod meth = Scene.v().getMethod(m);
		return this.reachableMethSet.contains(meth);
	}
	
	private void createSuperNode(InterAutomaton auto) {
		CGAutoState superFinalSt = new CGAutoState("SuperFinal", false, true);
		superFinalSt.setDesc("Final");

		for(AutoState autoSt : auto.getFinalStates()) {
			AutoEdge superEdge = new AutoEdge(autoSt.getId() + "@superFinal");
			superEdge.setInfinityWeight();
			
			autoSt.addOutgoingStates(superFinalSt, superEdge);

			// add incoming state.
			superFinalSt.addIncomingStates(autoSt, superEdge);
			autoSt.resetFinalState();
		}
		auto.clearFinalState();
		auto.addFinalState(superFinalSt);
		auto.addStates(superFinalSt);
	}
	
	private boolean buildInterAutomaton(CGAutomaton cgAuto,
			RegAutomaton regAuto, boolean annot, boolean stepTwo, boolean exhaust) {
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", annot);
		myoptions.put("two", stepTwo);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();

		// interAuto.validate();
		// interAuto.dumpFile();

		// before we do the mincut, we need to exclude some trivial cases
		// such as special invoke, static invoke and certain virtual invoke.
		if (interAuto.getFinalStates().size() == 0) {
			// eager version much also be false in this case.
			return false;
		}

		if (debug)
			GraphUtil.checkValidInterAuto(interAuto);

		// need to append a super final state, otherwise the result is wrong.
		createSuperNode(interAuto);

		boolean answer;
		// exhaustive checking.
		if (exhaust) {
			answer = checkExhaust();
		} else {
			answer = checkByMincut();
		}
		return answer;
	}
	
	// checking validity using mincut.
	private boolean checkByMincut() {
		ptTime = 0;
		cutTime = 0;
		// Stop conditions:
		// 1. Refute all edges in current cut set;(Yes)
		// 2. Can not find a mincut without infinity anymore.(No)
		Set<CutEntity> cutset = GraphUtil.minCut(interAuto);
		// System.out.println("cutset:" + cutset);

		boolean answer = true;
		// contains infinity edge?
		while (!hasInfinityEdges(cutset)) {
			boolean refuteAll = true;
			for (CutEntity e : cutset) {
				//Heuristic: Too many edges from libs.
//				if (e.getStmt() != null
//						&& e.getStmt().getInvokeExpr().getMethod()
//								.isJavaLibraryMethod() && cutset.size() > 20)
//					return true;
				
				if (isValidEdge(e.edge, e.getSrc())) {
					refuteAll = false;
					e.edge.setInfinityWeight();
					break;
				} else {
					// TODO:e is a false positive.
				}
			}
			// all edges are refute, stop.
			if (refuteAll) {
				answer = false;
				break;
			}
			// modify visited edges and continue.
			cutset = GraphUtil.minCut(interAuto);
			// System.out.println("cutset:" + cutset);
		}
		StringUtil.reportDiff("Time on PT: ", ptTime);
		StringUtil.reportDiff("Cut time:" + answer, cutTime);
		return answer;
	}

	// checking every edge along the path eagerly.
	private boolean checkExhaust() {
		long startExhau = System.nanoTime();
		HashMap<AutoEdge, Boolean> edgeMap = new HashMap<AutoEdge, Boolean>();
		// step 1: check all edges
		LinkedList<AutoState> worklist = new LinkedList<AutoState>();
		worklist.addAll(interAuto.getInitStates());
		Set<AutoState> visited = new HashSet<AutoState>();
		while (!worklist.isEmpty()) {
			AutoState worker = worklist.pollFirst();
			if (visited.contains(worker))
				continue;

			visited.add(worker);
			worklist.addAll(worker.getOutgoingStatesKeySet());
			if (interAuto.getInitStates().contains(worker))
				continue;

			for (AutoEdge e : worker.getOutgoingStatesInvKeySet()) {
				// check each edge.
				Set<AutoState> sts = worker.outgoingStatesInvLookup(e);
				assert sts.size() == 1 : e;
				AutoState tgt = sts.iterator().next();
				if (tgt.isFinalState())
					continue;

				SootMethod callee = uidToMethMap.get(((InterAutoEdge) e)
						.getTgtCGAutoStateId());
				assert callee != null : tgt;
				edgeMap.put(e, isValidEdge(e, worker));
			}
		}
		// step 2: perform dfs
		worklist.clear();
		worklist.addAll(interAuto.getInitStates());
		visited.clear();
		while (!worklist.isEmpty()) {
			AutoState worker = worklist.pollFirst();
			if (visited.contains(worker))
				continue;

			visited.add(worker);
			for (AutoEdge e : worker.getOutgoingStatesInvKeySet()) {
				Set<AutoState> sts = worker.outgoingStatesInvLookup(e);
				assert sts.size() == 1 : e;
				AutoState tgt = sts.iterator().next();
				if (!edgeMap.containsKey(e) || edgeMap.get(e)) {
					worklist.add(tgt);
				}
			}
		}

		visited.retainAll(interAuto.getFinalStates());
		boolean answer = !visited.isEmpty();
		long stopExhau = System.nanoTime();
		StringUtil.reportSec("exhau: " + answer, startExhau, stopExhau);
		return answer;
	}

	private boolean hasInfinityEdges(Set<CutEntity> set) {
		for (CutEntity e : set)
			if (e.edge.getWeight() == INFINITY)
				return true;

		return false;
	}
	
	private boolean isValidEdge(AutoEdge e, AutoState src) {
		long start = System.nanoTime();
		Stmt st = e.getSrcStmt();
		if (st == null)
			return true;
		SootMethod calleeMeth = uidToMethMap.get(((InterAutoEdge) e)
				.getTgtCGAutoStateId());
		SootClass calleeClz = calleeMeth.getDeclaringClass();
		Set<AutoEdge> inEdges = src.getIncomingStatesInvKeySet();
		assert (calleeMeth != null);
		// main method is always reachable.
		if (calleeMeth.isMain() || calleeMeth.isStatic()
				|| calleeMeth.isPrivate() || calleeMeth.isPhantom())
			return true;

		List<Type> typeSet = SootUtils
				.compatibleTypeList(calleeClz, calleeMeth);
		if (st.getInvokeExpr() instanceof SpecialInvokeExpr) {
			// handle super.foo();
			for (SootClass sub : SootUtils.subTypesOf(calleeClz)) {
				typeSet.add(sub.getType());
			}
		}

		Set<Type> ptTypeSet = new HashSet<Type>();
		assert st != null : calleeMeth;
		Local l = getReceiver(st);
		// get the context of l. This could be optimized later.
		for (AutoEdge in : inEdges) {
			if (in.isInvEdge())
				continue;
			Stmt stmt = in.getSrcStmt();

			if (stmt != null
					&& stmt.containsInvokeExpr()
					&& ((stmt.getInvokeExpr() instanceof VirtualInvokeExpr) || (stmt
							.getInvokeExpr() instanceof InterfaceInvokeExpr))) {
				CgContext ctxt = new CgContext(stmt.getInvokeExpr());
				Set<Type> types = ptsDemand.reachingObjects(ctxt, l)
						.possibleTypes();
				// System.out.println("stmt: " + st + " ctxt: " + ctxt + " var:"
				// + l + " types:" + types);
				ptTypeSet.addAll(types);
			} else {
				// Since we limit k=1, if the context is static, we ignore
				ptTypeSet.addAll(ptsDemand.reachingObjects(l).possibleTypes());
			}
		}

		long end = System.nanoTime();
		ptTime = ptTime + (end - start);

		if (ptTypeSet.size() == 0)
			return false;

		// super.<init> always true;
		if (calleeMeth.isConstructor())
			return true;

		if (includeAnyType && hasAnyType(ptTypeSet))
			return true;

		ptTypeSet.retainAll(typeSet);
		return !ptTypeSet.isEmpty();
	}
	
	private boolean hasAnyType(Set<Type> types) {
		for (Type t : types) {
			if (t instanceof AnySubType) {
				AnySubType any = (AnySubType) t;
				Type base = any.getBase();
				if (base.toString().equals("java.lang.Object"))
					return true;
			}
		}
		return false;
	}

	// interface for query
	public boolean queryRegx(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);
		boolean res = buildInterAutomaton(cgAuto, regAuto, true, true, false);
		return res;
	}
	
	public boolean queryRegxNoLookahead(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);
		boolean res = buildInterAutomaton(cgAuto, regAuto, false, false, false);
		return res;
	}
	
	public boolean queryRegxNoMincut(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);
		boolean res = buildInterAutomaton(cgAuto, regAuto, true, true, true);
		return res;
	}
	
	// return the default answer based on interauto w/o refine.
	public boolean defaultAns() {
		return interAuto.getFinalStates().size() > 0;
	}
	
	public boolean queryWithoutRefine(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);

		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		myoptions.put("two", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();
		return !interAuto.getFinalStates().isEmpty();
	}
	
	// return a valid regular expression based on method's signature.
	public String getValidExprBySig(String sig) {
		Pattern pattern = Pattern.compile("<[^\\s]*:\\s[^:]*>");

		Matcher matcher = pattern.matcher(sig);
		while (matcher.find()) {
		    String subSig = matcher.group(0);
            if(Scene.v().containsMethod(subSig)) {
                SootMethod meth = Scene.v().getMethod(subSig);

                String uid = "\\u" + String.format("%04x", meth.getNumber() + offset);
                //assert(uidToMethMap.get(uid)!=null);
                sig = sig.replace(matcher.group(0), uid);
            } else {
                String unknown = "\\uffff";
                sig = sig.replace(matcher.group(0), unknown);
            }
		}
		sig = sig.replaceAll("\\s+", "");
		return sig;
	}

	// get a list of vars that can invoke method tgt.
	private Local getReceiver(Stmt stmt) {
		if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			if ((ie instanceof InstanceInvokeExpr)) {
                InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
		        Local receiver = (Local) iie.getBase();
		        return receiver;
			}
		}
		return null;
	}
	
	public CGAutomaton getCGAuto() {
		return this.cgAuto;
	}

	public InterAutomaton getInterAuto() {
		return this.interAuto;
	}
	
	//Should not consider async edges in performance exp.
	public void ignoreAsync(boolean flag) {
		ignoreAsync = flag;
	}

}
