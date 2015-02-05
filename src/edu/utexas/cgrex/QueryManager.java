package edu.utexas.cgrex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
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
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.Chain;
import soot.util.queue.QueueReader;
import chord.util.tuple.object.Pair;
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

	private int INFINITY = 9999;

	// default CHA-based call graph or on-the-fly.
	CallGraph cg;
	
	// this magic number is used to fix the error by invalid unicode such as
	// \u0022
	private int offset = 100;
	
	private InterAutomaton egAuto;

	private ReachableMethods reachableMethods;

	Map<SootMethod, CGAutoState> methToStateMap = new HashMap<SootMethod, CGAutoState>();
	Map<SootMethod, CGAutoState> methToEagerStateMap = new HashMap<SootMethod, CGAutoState>();
	Map<CGAutoState, SootMethod> eagerStateToMethMap = new HashMap<CGAutoState, SootMethod>();


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
	Map<String, SootMethod> uidToMethMap = new HashMap<String, SootMethod>();
	
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
	private PointsToAnalysis ptsDemand;
	
	/*points-to analysis for demand-driven version*/
	private PointsToAnalysis ptsEager;
	
	private boolean debug = false;
	
	//run eager or not?
	private boolean runEager = false;
	
	//reachable methods
	private Set<SootMethod> reachableMethSet = new HashSet<SootMethod>();

	public QueryManager(CallGraph cg) {
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		
//		ptsEager = DemandCSPointsTo.makeWithBudget(
//				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		
		this.initQM(cg, false);
	}
	
	public QueryManager(CallGraph cg, SootMethod meth) {
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		this.setMainMethod(meth);
		this.initQM(cg, false);
	}
	
	public QueryManager(CallGraph cg, boolean flag, DemandCSPointsTo dcsp) {
		this.initQM(cg, flag);
		ptsDemand = dcsp;
	}
	
	public QueryManager(CallGraph cg, boolean flag,
			DemandCSPointsTo eagerPt, DemandCSPointsTo ddPt) {	
		ptsDemand = ddPt;
//		ptsEager = eagerPt;
		this.initQM(cg, flag);
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
		methToStateMap.put(mainMethod, mst);
		
		while (mIt.hasNext()) {
			SootMethod meth = (SootMethod) mIt.next();
			reachableMethSet.add(meth);

			// map each method to a unicode.
			String uid = "\\u" + String.format("%04x", meth.getNumber() + offset);
			uidToMethMap.put(uid, meth);
			methToUidMap.put(meth, uid);

			CGAutoState st = new CGAutoState(uid, false, true);

			methToStateMap.put(meth, st);
			
			CGAutoState stEager = new CGAutoState(uid, false, true);
			methToEagerStateMap.put(meth, stEager);
			eagerStateToMethMap.put(stEager, meth);
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
		
		if(runEager) {
			long startEager = System.nanoTime();
			buildEagerCGAutomaton();
			long endEager = System.nanoTime();
			StringUtil.reportSec("Time To build Eager CG:", startEager,
					endEager);
		}

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
					AutoEdge outEdge = invkToEdgeMap.get(new Pair<>(srcStmt,
							worker));
					assert outEdge != null : e;
					// need fresh instance for each callsite but share same uid.
					curState.addOutgoingStates(curState, outEdge);
					curState.addIncomingStates(curState, outEdge);
				} else {
					worklist.add(tgtMeth);
					AutoEdge outEdge = invkToEdgeMap.get(new Pair<>(srcStmt,
							tgtMeth));
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
	
	private void buildEagerCGAutomaton() {
		// Start from the main entry.
		SootMethod mainMeth = Scene.v().getMainMethod();
		// init FSM
		AutoEdge callEdgeMain = methToEdgeMap.get(mainMeth);

		// 0 is the initial id.
		CGAutoState initState = new CGAutoState(0, true, true);
		CGAutoState mainState = methToEagerStateMap.get(mainMeth);
		initState.addOutgoingStates(mainState, callEdgeMain);
		mainState.addIncomingStates(initState, callEdgeMain);

		// no incoming edge for initstate.

		cgEagerAuto.addInitState(initState);
		cgEagerAuto.addStates(initState);

		// FIXME: should not use list.
		List<SootMethod> worklist = new LinkedList<SootMethod>();
		worklist.add(mainMeth);

		Set<CGAutoState> reachableState = new HashSet<CGAutoState>();
		
		Set<SootMethod> visited = new HashSet<SootMethod>();

		while (worklist.size() > 0) {
			SootMethod worker = worklist.remove(0);
			CGAutoState curState = methToEagerStateMap.get(worker);
			reachableState.add(curState);
			if(visited.contains(worker))
				continue;
			visited.add(worker);

			// worker. outgoing edges
			Iterator<Edge> outIt = cg.edgesOutOf(worker);
			while (outIt.hasNext()) {
				Edge e = outIt.next();
				//truely edge?
				if(e.isVirtual() && !isValidEdge(cg,e))
					continue;
				// how about SCC? FIXME!!
				if (e.getTgt().equals(worker)) {// recursive call, add self-loop
					AutoEdge outEdge = methToEdgeMap.get(worker);
					// need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(worker.getSignature());
					curState.addOutgoingStates(curState, outEdgeFresh);
					curState.addIncomingStates(curState, outEdgeFresh);

				} else {
					SootMethod tgtMeth = (SootMethod) e.getTgt();
					worklist.add(tgtMeth);

					AutoEdge outEdge = methToEdgeMap.get(tgtMeth);
					// need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(tgtMeth.getSignature());

					CGAutoState tgtState = methToEagerStateMap.get(tgtMeth);
					curState.addOutgoingStates(tgtState, outEdgeFresh);

					// add incoming state.
					tgtState.addIncomingStates(curState, outEdgeFresh);
				}
			}

		}

		// only add reachable methods.
		for (CGAutoState rs : reachableState)
			cgEagerAuto.addStates(rs);

		System.out.println("Total States*******" + cgEagerAuto.getStates().size());

		// dump automaton of the call graph.
		// cgAuto.dump();
		// cgAuto.validate();
	}
	
	private boolean isValidEdge(CallGraph cg, Edge e) {
		SootMethod caller = (SootMethod) e.getSrc();
		if (!caller.isConcrete())
			return true;
		Body body = caller.retrieveActiveBody();
		Chain<Unit> units = body.getUnits();
		Iterator<Unit> uit = units.snapshotIterator();
		while (uit.hasNext()) {
			Stmt stmt = (Stmt) uit.next();
			if (stmt.containsInvokeExpr()) {
				InvokeExpr ie = stmt.getInvokeExpr();
				if ((ie instanceof VirtualInvokeExpr)
						|| (ie instanceof InterfaceInvokeExpr)) {
					Local receiver = (Local) ie.getUseBoxes().get(0).getValue();
					SootMethod callee = ie.getMethod();
					Iterator<Edge> it = cg.edgesOutOf(stmt);
					while (it.hasNext()) {
						Edge tgt = it.next();
						if (e.equals(tgt)) {
							// is this edge exist?
							Set<Type> pTypes = ptsEager.reachingObjects(
									receiver).possibleTypes();

							List<Type> typeSet = SootUtils.compatibleTypeList(
									callee.getDeclaringClass(), callee);
							pTypes.retainAll(typeSet);
							if (pTypes.size() == 0) {
								return false;
							}
							break;
						}
					}
				}
			}
		}

		return true;
	}

	private void createSuperNode(InterAutomaton auto) {
		CGAutoState superFinalSt = new CGAutoState("SuperFinal", false, true);


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
	
	private boolean buildInterAutomaton(CGAutomaton cgAuto, RegAutomaton regAuto) {
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		myoptions.put("two", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		long startInter = System.nanoTime();
		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();
		long endInter = System.nanoTime();
		StringUtil.reportSec("Building InterAuto:", startInter, endInter);

		// interAuto.validate();
		// interAuto.dump();

		// before we do the mincut, we need to exclude some trivial cases
		// such as special invoke, static invoke and certain virtual invoke.
		if (interAuto.getFinalStates().size() == 0) {
			//eager version much also be false in this case.
			return false;
		}
		
		if(debug)
			GraphUtil.checkValidInterAuto(interAuto);
		
		//need to append a super final state, otherwise the result is wrong.
		createSuperNode(interAuto);

		// exhaustive checking.
		long startExhau = System.nanoTime();
		HashMap<AutoEdge, Boolean> edgeMap = new HashMap<AutoEdge, Boolean>();
		//step 1: check all edges
		LinkedList<AutoState> worklist = new LinkedList<AutoState>();
		worklist.addAll(interAuto.getInitStates());
		Set<AutoState> visited = new HashSet<AutoState>();
		while (!worklist.isEmpty()) {
			AutoState worker = worklist.pollFirst();
			if(visited.contains(worker))
				continue;
			
			visited.add(worker);
			worklist.addAll(worker.getOutgoingStatesKeySet());
			if(interAuto.getInitStates().contains(worker))
				continue;
			
			for(AutoEdge e : worker.getOutgoingStatesInvKeySet()) {
				// check each edge.
				Set<AutoState> sts = worker.outgoingStatesInvLookup(e);
				assert sts.size() == 1 : e;
				AutoState tgt = sts.iterator().next();
				if(tgt.isFinalState())
					continue;
				
				SootMethod callee = uidToMethMap.get(((InterAutoEdge) e)
						.getTgtCGAutoStateId());
				assert callee != null : tgt;
				edgeMap.put(e, isValidEdge(e, worker));
			}
		}
		//step 2: perform dfs
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
		
		System.out.println(edgeMap);
		System.out.println(visited);
		System.out.println(interAuto.getFinalStates().iterator().next().getIncomingStatesKeySet());
		visited.retainAll(interAuto.getFinalStates());
		boolean reach = !visited.isEmpty();
		long stopExhau = System.nanoTime();
		StringUtil.reportSec("exhau: " + reach, startExhau, stopExhau);
		
		// Stop conditions:
		// 1. Refute all edges in current cut set;(Yes)
		// 2. Can not find a mincut without infinity anymore.(No)
		long startRefine = System.nanoTime();
		Set<CutEntity> cutset = GraphUtil.minCut(interAuto);
		System.out.println("cutset:" + cutset);
		long endCut = System.nanoTime();

		StringUtil.reportSec("min-cut:", startRefine, endCut);
		
//		for(CutEntity ct : cutset) {
//			assert ct.edge.getWeight() != AutoEdge.INFINITY : ct.edge;
//			assert !ct.edge.isInvEdge();
//			assert ct.getStmt() != null : ct.edge;
//		}

		boolean answer = true;
		// contains infinity edge?
		double ptTime = 0.0;
		while (!hasInfinityEdges(cutset)) {

			boolean refuteAll = true;
			long beginPt = System.nanoTime();
			for (CutEntity e : cutset) {
				if (isValidEdge(e.edge, e.getSrc())) {
					refuteAll = false;
					e.edge.setInfinityWeight();
					break;
				} else { // e is a false positive.
					// remove this edge and refine call graph.
					//Edge callEdge = this.getEdgeFromCallgraph(e);
					// System.out.println("---------Refine call edge: " +
					// callEdge);
					//cg.removeEdge(callEdge);
					// remove this edge from interauto.
					//interAuto.refine(e.edge);
				}
			}
			long endPt = System.nanoTime();
			ptTime = ptTime + ((endPt - beginPt)/1e6);

			// all edges are refute, stop.
			if (refuteAll) {
				answer = false;
				break;
			}
			// modify visited edges and continue.
			cutset = GraphUtil.minCut(interAuto);
		}
		// interAuto.dump();
		long endRefine = System.nanoTime();
		StringUtil.reportInfo("Time on PT: " + ptTime);
		StringUtil.reportSec("min-refute:" + answer, startRefine, endRefine);
//		assert answer == reach;

		return answer;
	}

	private boolean hasInfinityEdges(Set<CutEntity> set) {
		for (CutEntity e : set)
			if (e.edge.getWeight() == INFINITY)
				return true;

		return false;
	}
	
	private boolean isValidEdge(AutoEdge e, AutoState src) {
		
		SootMethod calleeMeth = uidToMethMap.get(((InterAutoEdge) e)
				.getTgtCGAutoStateId());
		
		Set<AutoEdge> inEdges = src.getIncomingStatesInvKeySet();

		assert (calleeMeth != null);
		// main method is always reachable.
		if (calleeMeth.isMain() || calleeMeth.isStatic()
				|| calleeMeth.isPrivate() || calleeMeth.isPhantom())
			return true;

		List<Type> typeSet = SootUtils.compatibleTypeList(
				calleeMeth.getDeclaringClass(), calleeMeth);

		Set<Type> ptTypeSet = new HashSet<Type>();
		Stmt st = e.getSrcStmt();
		// assert st != null : calleeMeth;
		Local l = getVarList(st);
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
//				System.out.println("#######query ctxt:" + ctxt + " var: " + l
//						+ " result:" + types);
				ptTypeSet.addAll(types);
			} else {
				ptTypeSet.addAll(ptsDemand.reachingObjects(l).possibleTypes());
			}
		}

		if (ptTypeSet.size() == 0)
			return false;

		ptTypeSet.retainAll(typeSet);
		return !ptTypeSet.isEmpty();
	}

	// return the edge from soot's call graph
	private Edge getEdgeFromCallgraph(CutEntity cut) {
		SootMethod calleeMeth = uidToMethMap.get(((InterAutoEdge) cut.edge)
				.getTgtCGAutoStateId());
		AutoEdge inEdge = null;
		for (AutoEdge e : cut.state.getIncomingStatesInvKeySet()) {
			if (!e.isInvEdge()) {
				inEdge = e;
				break;
			}
		}
		SootMethod callerMeth = uidToMethMap.get(((InterAutoEdge) inEdge)
				.getTgtCGAutoStateId());
		// System.out.println("look for an edge from " + callerMeth + " to " +
		// calleeMeth);
		for (Iterator<Edge> cIt = cg.edgesOutOf(callerMeth); cIt.hasNext();) {
			Edge outEdge = cIt.next();
			if (outEdge.getTgt().equals(calleeMeth))
				return outEdge;
		}
		// assert(false);
		// System.err.println("CAN not find the right call edge.------------");
		return null;
	}

	// entry method for the query. Only for debug purpose.
	public boolean doQuery() {
		String regx = "";

		// ignore user input, run our own batch test.
		switch (Harness.mode) {
		case 0:// benchmark mode.
			while (true) {
				Scanner in = new Scanner(System.in);
				System.out.println("Please Enter a string:");
				regx = in.nextLine();
				// press "q" to exit the program
				if (regx.equals("q"))
					System.exit(0);
				else {
					System.out.println("You entered string: " + regx);
					regx = this.getValidExprBySig(regx);
					System.out.println("Actual expression......" + regx);
					buildRegAutomaton(regx);
					buildInterAutomaton(cgAuto, regAuto);
				}
			}
		case 1:// interactive mode.
			RegularExpGenerator generator = new RegularExpGenerator(this);
			for (int i = 0; i < Harness.benchmarkSize; i++) {
				regx = generator.genRegx();
				regx = regx.replaceAll("\\s+", "");
				System.out.println("Random regx------" + regx);
				buildRegAutomaton(regx);
				buildInterAutomaton(cgAuto, regAuto);
			}
			break;
		default:
			System.exit(0);
			break;
		}

		return false;
	}

	// interface for query
	public boolean queryRegx(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);
		boolean res = buildInterAutomaton(cgAuto, regAuto);
		return res;
	}
	
	// return the default answer based on interauto w/o refine.
	public boolean defaultAns() {
		return interAuto.getFinalStates().size() > 0;
	}
	
	//Return all m's callers.
//	public Set<String> queryEagerCallers(String callee) {
//		Set<String> callers = new HashSet();
//		
//		SootMethod calleeMeth = Scene.v().getMethod(callee);
//		
//		AutoState calleeSt = methToEagerStateMap.get(calleeMeth);
//		for (AutoState callerSt : calleeSt.getIncomingStatesKeySet()){
//			SootMethod callerMeth = eagerStateToMethMap.get(callerSt);
//			
//			Set<Value> varSet = getVarList(callerMeth, calleeMeth);
//			List<Type> typeSet = SootUtils.compatibleTypeList(
//					calleeMeth.getDeclaringClass(), calleeMeth);
//
//			// to be conservative.
//			if (varSet.size() == 0) {
//				callers.add(callerMeth.getSignature());
//				continue;
//			}
//			
//			Set<Type> ptTypeSet = new HashSet<Type>();
//			for(Value v : varSet) {
//				assert(v instanceof Local);
//				Local l = (Local) v;
//				ptTypeSet.addAll(ptsDemand.reachingObjects(l).possibleTypes());
//			}
//
//	        if(ptTypeSet.size() == 0) {
//				callers.add(callerMeth.getSignature());
//				continue;
//	        }
//
//			ptTypeSet.retainAll(typeSet);
//			if(!ptTypeSet.isEmpty())
//				callers.add(callerMeth.getSignature());
//			
//		}
//		return callers;
//	}
	
	//Return all m's callers.
//	public Set<String> queryCallers(String callee) {
//		Set<String> callers = new HashSet();
//		SootMethod calleeMeth = Scene.v().getMethod(callee);
//		Iterator<Edge> it = cg.edgesInto(calleeMeth);
//		while(it.hasNext()) {
//			Edge callEdge = it.next();
//			SootMethod callerMeth = (SootMethod)callEdge.getSrc();
//			
//			Set<Value> varSet = getVarList(callerMeth, calleeMeth);
//			List<Type> typeSet = SootUtils.compatibleTypeList(
//					calleeMeth.getDeclaringClass(), calleeMeth);
//
//			// to be conservative.
//			if (varSet.size() == 0) {
//				callers.add(callerMeth.getSignature());
//				continue;
//			}
//			
//			Set<Type> ptTypeSet = new HashSet<Type>();
//			for(Value v : varSet) {
//				assert(v instanceof Local);
//				Local l = (Local) v;
//				ptTypeSet.addAll(ptsDemand.reachingObjects(l).possibleTypes());
//			}
//
//	        if(ptTypeSet.size() == 0) {
//				callers.add(callerMeth.getSignature());
//				continue;
//	        }
//
//			ptTypeSet.retainAll(typeSet);
//			if(!ptTypeSet.isEmpty())
//				callers.add(callerMeth.getSignature());
//
//		}
//		return callers;
//	}
	
	public boolean queryRegxEager(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);
		
		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		myoptions.put("two", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		InterAutomaton interAutoEager = new InterAutomaton(myopts, regAuto, cgEagerAuto);
		interAutoEager.build();
		egAuto = interAutoEager;

		// before we do the mincut, we need to exclude some trivial cases
		// such as special invoke, static invoke and certain virtual invoke.
		if (interAutoEager.getFinalStates().size() == 0)
			return false;
		
		if(debug)
			GraphUtil.checkValidInterAuto(interAutoEager);
		
		return true;
	}

	public boolean queryWithoutRefine(String regx) {
		regx = regx.replaceAll("\\s+", "");
		buildRegAutomaton(regx);

		Map<String, Boolean> myoptions = new HashMap<String, Boolean>();
		myoptions.put("annot", true);
		myoptions.put("two", true);
		InterAutoOpts myopts = new InterAutoOpts(myoptions);

		long startInter = System.nanoTime();
		interAuto = new InterAutomaton(myopts, regAuto, cgAuto);
		interAuto.build();
		long endInter = System.nanoTime();
		StringUtil.reportSec("Building InterAuto:", startInter, endInter);

		interAuto.dumpFile();

		// before we do the mincut, we need to exclude some trivial cases
		// such as special invoke, static invoke and certain virtual invoke.
		if (interAuto.getFinalStates().size() == 0)
			return false;
		return true;
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
		return sig;
	}

	// get a list of vars that can invoke method tgt.
	private Local getVarList(Stmt stmt) {

		if (stmt.containsInvokeExpr()) {
			InvokeExpr ie = stmt.getInvokeExpr();
			if ((ie instanceof VirtualInvokeExpr)
					|| (ie instanceof InterfaceInvokeExpr)) {
				int len = ie.getUseBoxes().size();
				assert len > 0;
				Value var = ie.getUseBoxes().get(len - 1).getValue();
				assert var instanceof Local : var + " in: " + ie;
				return (Local) var;
			}
		}
		return null;
	}

}
