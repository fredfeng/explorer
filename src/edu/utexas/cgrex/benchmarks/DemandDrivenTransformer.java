package edu.utexas.cgrex.benchmarks;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import soot.Body;
import soot.EntryPoints;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.EBBCollapser;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.spark.solver.SCCCollapser;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.utils.SootUtils;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * 
 * @author yufeng
 * 
 */
public class DemandDrivenTransformer extends SceneTransformer {

	public boolean debug = true;

	// CHA.
	QueryManager qm;
	
	CallGraph callgraph;

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		HashMap<String, String> opt = new HashMap<String, String>(options);
		opt.put("enabled", "true");
		opt.put("verbose", "true");
		opt.put("field-based", "false");
		opt.put("on-fly-cg", "false");
		opt.put("set-impl", "double");
		opt.put("double-set-old", "hybrid");
		opt.put("double-set-new", "hybrid");

		SparkOptions opts = new SparkOptions(opt);

		// Build pointer assignment graph
		ContextInsensitiveBuilder b = new ContextInsensitiveBuilder();
		if (opts.pre_jimplify())
			b.preJimplify();
		final PAG pag = b.setup(opts);
		b.build();

		// Build type masks
		pag.getTypeManager().makeTypeMask();

		// Simplify pag
		// We only simplify if on_fly_cg is false. But, if vta is true, it
		// overrides on_fly_cg, so we can still simplify. Something to handle
		// these option interdependencies more cleanly would be nice...
		if ((opts.simplify_sccs() && !opts.on_fly_cg()) || opts.vta()) {
			new SCCCollapser(pag, opts.ignore_types_for_sccs()).collapse();
		}
		if (opts.simplify_offline() && !opts.on_fly_cg()) {
			new EBBCollapser(pag).collapse();
		}
		if (true || opts.simplify_sccs() || opts.vta()
				|| opts.simplify_offline()) {
			pag.cleanUpMerges();
		}

		// Propagate
		new PropWorklist(pag).propagate();

		if (!opts.on_fly_cg() || opts.vta()) {
			CallGraphBuilder cgb = new CallGraphBuilder(pag);
			cgb.build();
		}

		Scene.v().setPointsToAnalysis(pag);

		final int DEFAULT_MAX_PASSES = 10000;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		Date startOnDemand = new Date();
		Scene.v().setPointsToAnalysis(pag);
//		callgraph = pag.getOnFlyCallGraph().callGraph();

		ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		Date endOndemand = new Date();
		System.out
				.println("Initialized on-demand refinement-based context-sensitive analysis"
						+ startOnDemand + endOndemand);

		Set<Local> virtSet = genReceivers();
		// perform pt-set queries.

		assert(false);
//		genCallGraph();
	}
	
	PointsToAnalysis ptsDemand;
	
	public Set<Local> genReceivers() {
		Set<Local> virtSet = new HashSet<Local>();

		for (Iterator<SootClass> cIt = Scene.v().getClasses().iterator(); cIt
				.hasNext();) {
			final SootClass clazz = (SootClass) cIt.next();
			System.out.println("Analyzing...." + clazz);
			for (SootMethod m : clazz.getMethods()) {
				if (!m.isConcrete())
					continue;
				Body body = m.retrieveActiveBody();
				Chain<Unit> units = body.getUnits();
				Iterator<Unit> uit = units.snapshotIterator();
				while (uit.hasNext()) {
					Stmt stmt = (Stmt) uit.next();
					if (stmt.containsInvokeExpr()) {
						InvokeExpr ie = stmt.getInvokeExpr();
						if ((ie instanceof VirtualInvokeExpr)
								|| (ie instanceof InterfaceInvokeExpr)) {
							Local receiver = (Local) ie.getUseBoxes().get(0)
									.getValue();
							PointsToSet ps = ptsDemand.reachingObjects(receiver);
							if (ps.possibleTypes().size() == 0)
								continue;
							virtSet.add(receiver);
						}
					}
				}
			}
		}
		
		return virtSet;
	}
	
	public void genCallGraph() {
		CallGraph cg = Scene.v().getCallGraph();
//		cg = callgraph;
		
		Set<SootMethod> visited = new HashSet<SootMethod>();
		LinkedList<SootMethod> workList = new LinkedList<SootMethod>();
		
		for(SootMethod entry : EntryPoints.v().all()) {
			workList.add(entry);
		}

		while (!workList.isEmpty()) {
			SootMethod head = workList.poll();
			if (visited.contains(head))
				continue;
			visited.add(head);
			Iterator<Edge> outIt = cg.edgesOutOf(head);
			while(outIt.hasNext()) {
				Edge e = outIt.next();
				if(!e.isVirtual() || (e.isVirtual() && isValidEdge(cg,e)))
					workList.add((SootMethod)e.getTgt());
			}
		}
		
		System.out.println("Total methods in CG: " + visited.size());
		System.out.println("Reachable methods in CG: " + Scene.v().getReachableMethods().size());
	}
	
	private boolean isValidEdge(CallGraph cg, Edge e) {
		SootMethod caller = (SootMethod)e.getSrc();
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
					Local receiver = (Local) ie.getUseBoxes().get(0)
							.getValue();
					SootMethod callee = ie.getMethod();
					Iterator<Edge> it = cg.edgesOutOf(stmt);
					while(it.hasNext()) {
						Edge tgt = it.next();
						if( e.equals(tgt) ) {
							System.out.println(e.getTgt() + "~~~~~" + callee);
							//is this edge exist?
							Set<Type> pTypes = ptsDemand.reachingObjects(receiver).possibleTypes();
							List<Type> typeSet = SootUtils.compatibleTypeList(
									callee.getDeclaringClass(), callee);
							pTypes.retainAll(typeSet);
							if(pTypes.size() == 0) {
								System.out.println("REFUTE THIS EDGE--------------------" + e);
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
	
}
