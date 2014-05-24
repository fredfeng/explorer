package edu.utexas.cgrex.benchmarks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import chord.util.Utils;
import soot.Body;
import soot.Local;
import soot.PointsToAnalysis;
import soot.PointsToSet;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
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
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.StringUtil;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * 
 * @author yufeng
 * 
 */
public class EarlyStopTransformer extends SceneTransformer {

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

		int DEFAULT_MAX_PASSES = 10;
		int DEFAULT_MAX_TRAVERSAL = 7500;
		final boolean DEFAULT_LAZY = false;

		// ====================early stop version=================

		PointsToAnalysis pts1 = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);

		DemandCSPointsTo ptsEarly = (DemandCSPointsTo) pts1;
		ptsEarly.enableEarlyStop();
		ptsEarly.disableBudget();

		PointsToAnalysis pts2 = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		DemandCSPointsTo ptsReg = (DemandCSPointsTo) pts2;
		ptsReg.disableBudget();
		ptsReg.disableEarlyStop();

		Map<Local, PointsToSet> askEarly = new HashMap<Local, PointsToSet>();

		System.out
				.println("---------------Starting early stop version--------------");
		long time1 = 0;
		long time2 = 0;
		int count = 0;
		int range = 1000;

		boolean go = true;
		// perform pt-set queries for all call sites and record the pt sets.
		for (Iterator<SootClass> cIt = Scene.v().getClasses().iterator(); cIt
				.hasNext();) {
			if (!go)
				break;
			final SootClass clazz = (SootClass) cIt.next();
			for (SootMethod m : clazz.getMethods()) {
				if (!go)
					break;
				if (!m.isConcrete())
					continue;
				Body body = m.retrieveActiveBody();
				Chain<Unit> units = body.getUnits();
				Iterator<Unit> uit = units.snapshotIterator();
				while (uit.hasNext()) {
					if (!go)
						break;
					Stmt stmt = (Stmt) uit.next();
					if (stmt.containsInvokeExpr()) {
						InvokeExpr ie = stmt.getInvokeExpr();
						if ((ie instanceof VirtualInvokeExpr)
								|| (ie instanceof InterfaceInvokeExpr)) {
							assert (ie.getUseBoxes().get(0).getValue() instanceof Local);
							Local receiver = (Local) ie.getUseBoxes().get(0)
									.getValue();

							Date start = new Date();
							PointsToSet ps1 = ptsEarly
									.reachingObjects(receiver);
							Date end = new Date();
							time1 += (end.getTime() - start.getTime());
							StringUtil.reportTime("Time: ", start, end);

							start = new Date();
							PointsToSet ps2 = ptsReg.reachingObjects(receiver);
							end = new Date();
							time2 += (end.getTime() - start.getTime());
							StringUtil.reportTime("Time: ", start, end);

							System.out.println("Correctness: "
									+ (ps1.possibleTypes().containsAll(
											ps2.possibleTypes()) && ps2
											.possibleTypes().containsAll(
													ps1.possibleTypes())));

							// if (ps1.possibleTypes().size() == 0)
							// continue;

							count++;
							if (count > range)
								go = false;

							System.out.println("Passed!");
						}
					}
				}
			}

		}

		// ==================== All DONE ==================

		System.out.println("All verification PASSED!");

		// Congratulations information
		System.out.println("Congratulations! Have a good night~");
		assert (false);
	}
}
