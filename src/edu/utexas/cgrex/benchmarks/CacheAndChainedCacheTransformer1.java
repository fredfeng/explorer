package edu.utexas.cgrex.benchmarks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
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
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.solver.EBBCollapser;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.spark.solver.SCCCollapser;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * 
 * @author yufeng
 * 
 */
public class CacheAndChainedCacheTransformer1 extends SceneTransformer {

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

		// ====================eager version (with cache)=================
		final int DEFAULT_MAX_PASSES = 1000000;
		final int DEFAULT_MAX_TRAVERSAL = 7500000;
		final boolean DEFAULT_LAZY = false;
		Scene.v().setPointsToAnalysis(pag);

		PointsToAnalysis ptsEager = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		DemandCSPointsTo dcs = (DemandCSPointsTo) ptsEager;
		assert (dcs.usesCache());
		assert (!dcs.useChainedCache());

		List<Local> virtSet = new ArrayList<Local>();

		System.out.println("Starting eager version...");
		// perform pt-set queries for all call sites and record the pt sets.
		for (Iterator<SootClass> cIt = Scene.v().getClasses().iterator(); cIt
				.hasNext();) {
			final SootClass clazz = (SootClass) cIt.next();
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
							PointsToSet ps = ptsEager.reachingObjects(receiver);

							if (ps.possibleTypes().size() == 0)
								continue;
							virtSet.add(receiver);
							Set<Type> s = new HashSet<Type>();
							s.addAll(ps.possibleTypes());

							assert (s.size() > 0);
							assert (s.size() == ps.possibleTypes().size());
							assert (pag.findLocalVarNode(receiver) != null);
						}
					}
				}
			}
		}
		System.out.println("Eager version DONE!");
		Scene.v().setPointsToAnalysis(pag);

		// ====================ondemand (no chained cache)=================

		PointsToAnalysis ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);

		int trialNum = virtSet.size();

		dcs = (DemandCSPointsTo) ptsDemand;
		assert (dcs.usesCache());
		assert (!dcs.useChainedCache());

		System.out
				.println("Starting demand-driven analysis (no chained cache)....");

		int count = 1;
		System.out.println("Verifying pointer analysis results....");
		for (int i = 0; i < trialNum; i++) {
			Local ran = virtSet.get(i);
			System.out.println("This is the " + count++
					+ "-th regular verification (no chained cache)...");
			assert (ptsDemand.reachingObjects(ran).possibleTypes()
					.containsAll(ptsEager.reachingObjects(ran).possibleTypes()));
			assert (ptsEager.reachingObjects(ran).possibleTypes()
					.containsAll(ptsDemand.reachingObjects(ran).possibleTypes()));
			System.out.println("PASSED!");
		}
		System.out
				.println("Hitting cache " + dcs.getHittingCache() + " times.");
		System.out.println("Regular verification all PASSED!");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		count = 1;
		System.out.println("Verifying cache results....");
		Map<Local, PointsToSet> cache = dcs.getCache();
		for (Local ran : cache.keySet()) {
			System.out.println("This is the " + count++
					+ "-th cache verification (no chained cache)...");
			assert (cache.get(ran).possibleTypes().containsAll(ptsEager
					.reachingObjects(ran).possibleTypes()));
			assert (ptsEager.reachingObjects(ran).possibleTypes()
					.containsAll(cache.get(ran).possibleTypes()));
			System.out.println("PASSED!");

		}
		System.out.println("Cache size: " + cache.size());
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Cache verification all PASSED!");

		System.out.println("Demand-driven analysis (no chained cache) DONE!");

		// ====================ondemand (with chained cache)=================

		ptsDemand = DemandCSPointsTo.makeWithBudget(DEFAULT_MAX_TRAVERSAL,
				DEFAULT_MAX_PASSES, DEFAULT_LAZY);

		trialNum = virtSet.size();

		dcs = (DemandCSPointsTo) ptsDemand;
		dcs.enableChainedCache();
		dcs.disableCache();
		assert (!dcs.usesCache());
		assert (dcs.useChainedCache());

		System.out
				.println("Starting demand-driven analysis (with chained cache)....");

		count = 1;
		System.out.println("Verifying pointer analysis results....");
		for (int i = 0; i < trialNum; i++) {
			Local ran = virtSet.get(i);
			System.out.println("This is the " + count++
					+ "-th regular verification (with chained cache)...");
			assert (ptsDemand.reachingObjects(ran).possibleTypes()
					.containsAll(ptsEager.reachingObjects(ran).possibleTypes()));
			assert (ptsEager.reachingObjects(ran).possibleTypes()
					.containsAll(ptsDemand.reachingObjects(ran).possibleTypes()));
			System.out.println("PASSED!");
		}
		System.out.println("Hitting chained cache "
				+ dcs.getHittingChainedCache() + " times.");
		System.out.println("Regular verification all PASSED!");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		count = 1;
		System.out.println("Verifying chained cache results....");
		Map<VarNode, PointsToSet> chainedCache = dcs.getChainedCache();
		for (VarNode var : chainedCache.keySet()) {
			if (chainedCache.containsKey(var)) {
				System.out
						.println("This is the "
								+ count++
								+ "-th chained cache verification (with chained cache)...");
				assert (chainedCache.get(var).possibleTypes()
						.containsAll(((DemandCSPointsTo) ptsEager)
								.computeRefinedReachingObjects(var)
								.possibleTypes()));
				assert ((((DemandCSPointsTo) ptsEager)
						.computeRefinedReachingObjects(var)).possibleTypes()
						.containsAll(chainedCache.get(var).possibleTypes()));
				System.out.println("PASSED!");
			}
		}
		System.out.println("Chained cache size: " + chainedCache.size());
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Chained chained cache verification all PASSED!");

		System.out.println("Demand-driven analysis (with chained cache) DONE!");

		System.out.println("All verification PASSED!");

		System.out.println("Congratulations! Have a good day~");
	}
}
