package edu.utexas.cgrex.benchmarks;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.G;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.ondemand.DemandCSPointsTo;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.PAGDumper;
import soot.jimple.spark.solver.EBBCollapser;
import soot.jimple.spark.solver.PropAlias;
import soot.jimple.spark.solver.PropCycle;
import soot.jimple.spark.solver.PropIter;
import soot.jimple.spark.solver.PropMerge;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.spark.solver.Propagator;
import soot.jimple.spark.solver.SCCCollapser;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.StringUtil;

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

		final int DEFAULT_MAX_PASSES = 1000000;
		final int DEFAULT_MAX_TRAVERSAL = 7500000;
		final boolean DEFAULT_LAZY = true;
		Date startOnDemand = new Date();
		Scene.v().setPointsToAnalysis(pag);

		PointsToAnalysis onDemandAnalysis = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		Date endOndemand = new Date();
		System.out
				.println("Initialized on-demand refinement-based context-sensitive analysis"
						+ startOnDemand + endOndemand);
		Scene.v().setPointsToAnalysis(onDemandAnalysis);

		PointsToAnalysis pts = Scene.v().getPointsToAnalysis();

		// perform pt-set queries.
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
							PointsToSet ps = pts.reachingObjects(receiver);
							if (ps.possibleTypes().size() == 0)
								continue;
							System.out.println("Virtual call------" + ie);
							System.out.println("Points-to set------"
									+ ps.possibleTypes());
							System.out.println("===========================");
						}
					}
				}
			}
		}

	}

}
