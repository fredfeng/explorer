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
import soot.EntryPoints;
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
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.util.Chain;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.automaton.AutoEdge;
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

	AutoPAG autoPAG;

	private int INFINITY = 9999;

	// default CHA-based call graph or on-the-fly.
	CallGraph cg;

	private ReachableMethods reachableMethods;

	Map<SootMethod, CGAutoState> methToStateMap = new HashMap<SootMethod, CGAutoState>();

	// make sure each method only be visited once.
	Map<SootMethod, Boolean> visitedMap = new HashMap<SootMethod, Boolean>();

	/**
	 * @return the methToEdgeMap only for the purpose of generating random regx.
	 */
	public Map<SootMethod, AutoEdge> getMethToEdgeMap() {
		return methToEdgeMap;
	}

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

	public QueryManager(AutoPAG autoPAG, CallGraph cg) {
		this.autoPAG = autoPAG;
		cgAuto = new CGAutomaton();
		regAuto = new RegAutomaton();
		this.cg = cg;

		init();
	}

	public AutoPAG getAutoPAG() {
		return this.autoPAG;
	}

	public ReachableMethods getReachableMethods() {
		if (reachableMethods == null) {
			reachableMethods = new ReachableMethods(cg,
					new ArrayList<MethodOrMethodContext>(EntryPoints.v().all()));
		}
		reachableMethods.update();
		// System.out.println("Reachable methods--------" +
		// reachableMethods.size());
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
		int offset = 100;
		while (mIt.hasNext()) {
			SootMethod meth = (SootMethod) mIt.next();

			// map each method to a unicode.
			String uid = "\\u" + String.format("%04x", meth.getNumber() + offset);
			uidToMethMap.put(uid, meth);

			AutoEdge inEdge = new AutoEdge(uid);
			inEdge.setShortName(meth.getSignature());
			CGAutoState st = new CGAutoState(uid, false, true);

			methToStateMap.put(meth, st);
			methToEdgeMap.put(meth, inEdge);
			visitedMap.put(meth, false);
		}
		// only build cgauto once.
		buildCGAutomaton();
	}

	private void buildRegAutomaton(String regx) {
        //System.out.println("Regx:------" + regx);
		regx = StringEscapeUtils.unescapeJava(regx);
		// step 1. Constructing a reg without .*
		RegExp r = new RegExp(regx);
		Automaton auto = r.toAutomaton();
        //System.out.println(auto.toDot());
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
				String shortName = "xxxxx";
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
//		System.out.println("dump regular graph.");
		// regAuto.dump();
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

		// FIXME: should not use list.
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
					// need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(worker.getSignature());

					curState.addOutgoingStates(curState, outEdgeFresh);

					curState.addIncomingStates(curState, outEdgeFresh);

				} else {
					SootMethod tgtMeth = (SootMethod) e.getTgt();
					if (!visitedMap.get(tgtMeth))
						worklist.add(tgtMeth);

					AutoEdge outEdge = methToEdgeMap.get(tgtMeth);
					// need fresh instance for each callsite but share same uid.
					AutoEdge outEdgeFresh = new AutoEdge(outEdge.getId());
					outEdgeFresh.setShortName(tgtMeth.getSignature());

					CGAutoState tgtState = methToStateMap.get(tgtMeth);
					curState.addOutgoingStates(tgtState, outEdgeFresh);

					// add incoming state.
					tgtState.addIncomingStates(curState, outEdgeFresh);
				}
			}

		}

		// only add reachable methods.
		for (CGAutoState rs : reachableState)
			cgAuto.addStates(rs);

		System.out.println("Total States*******" + cgAuto.getStates().size());

		// dump automaton of the call graph.
		// cgAuto.dump();
		// cgAuto.validate();
	}

	private boolean buildInterAutomaton() {
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
		if (interAuto.getFinalStates().size() == 0)
			return false;

		// Stop conditions:
		// 1. Refute all edges in current cut set;(Yes)
		// 2. Can not find a mincut without infinity anymore.(No)
		long startRefine = System.nanoTime();
		Set<CutEntity> cutset = GraphUtil.minCut(interAuto);

		boolean answer = false;
		// contains infinity edge?
		while (!hasInfinityEdges(cutset)) {

			answer = false;
			for (CutEntity e : cutset) {
				if (doPointsToQuery(e)) {
					answer = true;
					e.edge.setInfinityWeight();
				} else { // e is a false positive.
					// remove this edge and refine call graph.
					Edge callEdge = this.getEdgeFromCallgraph(e);
					// System.out.println("---------Refine call edge: " +
					// callEdge);
					cg.removeEdge(callEdge);
					// remove this edge from interauto.
					interAuto.refine(e.edge);
				}
			}

			// all edges are refute, stop.
			if (!answer)
				break;
			// modify visited edges and continue.
			cutset = GraphUtil.minCut(interAuto);
		}
		// interAuto.dump();
		long endRefine = System.nanoTime();
		StringUtil.reportSec("Building refine:", startRefine, endRefine);

		return answer;
	}

	private boolean hasInfinityEdges(Set<CutEntity> set) {
		for (CutEntity e : set)
			if (e.edge.getWeight() == INFINITY)
				return true;

		return false;
	}

	private boolean doPointsToQuery(CutEntity cut) {

		// type info.
		SootMethod calleeMeth = uidToMethMap.get(((InterAutoEdge) cut.edge)
				.getTgtCGAutoStateId());

		assert (calleeMeth != null);
		// main method is always reachable.
		if (calleeMeth.isMain() || calleeMeth.isStatic()
				|| calleeMeth.isPrivate())
			return true;

		AutoEdge inEdge = null;
		for (AutoEdge e : cut.state.getIncomingStatesInvKeySet()) {
			if (!e.isInvEdge()) {
				inEdge = e;
				break;
			}
		}
		SootMethod callerMeth = uidToMethMap.get(((InterAutoEdge) inEdge)
				.getTgtCGAutoStateId());

		List<Value> varSet = getVarList(callerMeth, calleeMeth);
		List<Type> typeSet = SootUtils.compatibleTypeList(
				calleeMeth.getDeclaringClass(), calleeMeth);

		// to be conservative.
		if (varSet.size() == 0)
			return true;

		return autoPAG.insensitiveQuery(varSet, typeSet);
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
					buildInterAutomaton();
				}
			}
		case 1:// interactive mode.
			RegularExpGenerator generator = new RegularExpGenerator(this);
			for (int i = 0; i < Harness.benchmarkSize; i++) {
				regx = generator.genRegx();
				regx = regx.replaceAll("\\s+", "");
				System.out.println("Random regx------" + regx);
				buildRegAutomaton(regx);
				buildInterAutomaton();
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
		boolean res = buildInterAutomaton();
		return res;
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

	private int miss = 0;

	// return a valid regular expression based on method's signature.
	public String getValidExprBySig(String sig) {
		Pattern pattern = Pattern.compile("<[^\\s]*:\\s[^:]*>");

		Matcher matcher = pattern.matcher(sig);
		boolean flag = true;
		while (matcher.find()) {
		    String subSig = matcher.group(0);
            if(Scene.v().containsMethod(subSig)) {
                SootMethod meth = Scene.v().getMethod(subSig);
                if(!this.reachableMethods.contains(meth) && flag) { 
                    flag = false;
                    miss++;
                }

                int offset = 100;
                String uid = "\\u" + String.format("%04x", meth.getNumber() + offset);
                //assert(uidToMethMap.get(uid)!=null);
                sig = sig.replace(matcher.group(0), uid);
            } else {
                String unknown = "\\uffff";
                sig = sig.replace(matcher.group(0), unknown);
            }
		}
		System.out.println("dump miss----------" + miss);
		return sig;
	}

	// get a list of vars that can invoke method tgt.
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
				if ((ie instanceof VirtualInvokeExpr)
						|| (ie instanceof InterfaceInvokeExpr)) {
					SootMethod callee = ie.getMethod();
					if (SootUtils.compatibleWith(tgt, callee)) {
						// ie.get
						Value var = ie.getUseBoxes().get(0).getValue();
						varSet.add(var);
					}
				}
			}
		}
		return varSet;
	}

}
