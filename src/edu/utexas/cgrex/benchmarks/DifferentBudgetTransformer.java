package edu.utexas.cgrex.benchmarks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * 
 * @author yufeng
 * 
 */
public class DifferentBudgetTransformer extends SceneTransformer {

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
		int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;

		// ====================eager version (with cache)=================

		PointsToAnalysis pts = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);

		DemandCSPointsTo ptsEager = (DemandCSPointsTo) pts;
		Map<Local, PointsToSet> askEager = new HashMap<Local, PointsToSet>();

		System.out
				.println("---------------Starting eager version--------------");
		long time = 0;
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
							assert (ie.getUseBoxes().get(0).getValue() instanceof Local);
							Local receiver = (Local) ie.getUseBoxes().get(0)
									.getValue();
							Date start = new Date();
							PointsToSet ps = ptsEager.reachingObjects(receiver);
							Date end = new Date();
							if (ps.possibleTypes().size() == 0)
								continue;

							askEager.put(receiver, ps);

							time += (end.getTime() - start.getTime());

						}
					}
				}
			}
		}

		int one = 0;
		int ten = 0;
		int twenty = 0;
		int morethan = 0;

		for (Local receiver : askEager.keySet()) {
			if (askEager.get(receiver).possibleTypes().size() <= 1) {
				one++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 10) {
				ten++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 20) {
				twenty++;
			} else {
				morethan++;
			}
		}
		System.out.println("There are totally " + askEager.size()
				+ " receivers to analyze.");
		System.out.println("There are totally " + one
				+ " receivers with one element");
		System.out.println("There are totally " + ten
				+ " receivers with ten elements");
		System.out.println("There are totally " + twenty
				+ " receivers with twenty elements");
		System.out.println("There are totally " + morethan
				+ " receivers with more than elements");
		System.out.println("Construction time: " + time + " million seconds");
		System.out.println("Eager version DONE!");
		System.out.println("****************************************");

		// ==================== small budget ======================

		// DEFAULT_MAX_PASSES = 100;
		DEFAULT_MAX_TRAVERSAL = 50000;
		pts = DemandCSPointsTo.makeWithBudget(DEFAULT_MAX_TRAVERSAL,
				DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		DemandCSPointsTo ptsSmall = (DemandCSPointsTo) pts;

		Map<Local, PointsToSet> askSmall = new HashMap<Local, PointsToSet>();

		System.out
				.println("---------------Starting small budget version--------------");
		time = 0;

		int less = 0;
		int more = 0;
		int exact = 0;
		int unsound = 0;
		int total = 0;

		// perform pt-set queries for all call sites and record the pt sets.

		for (Local receiver : askEager.keySet()) {
			Date start = new Date();
			PointsToSet ps = ptsSmall.reachingObjects(receiver);
			Date end = new Date();

			time += (end.getTime() - start.getTime());
			askSmall.put(receiver, ps);

			total++;

			if (ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				exact++;
			} else if (ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& !ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				less++;
			} else if (!ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				more++;
			} else {
				unsound++;
			}

		}

		one = 0;
		ten = 0;
		twenty = 0;
		morethan = 0;

		for (Local receiver : askEager.keySet()) {
			if (askEager.get(receiver).possibleTypes().size() <= 1) {
				one++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 10) {
				ten++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 20) {
				twenty++;
			} else {
				morethan++;
			}
		}
		System.out.println("There are totally " + askEager.size()
				+ " receivers to analyze.");
		System.out.println("There are totally " + one
				+ " receivers with one element");
		System.out.println("There are totally " + ten
				+ " receivers with ten elements");
		System.out.println("There are totally " + twenty
				+ " receivers with twenty elements");
		System.out.println("There are totally " + morethan
				+ " receivers with more than elements");

		System.out.println("Construction time: " + time + " million seconds");
		System.out.println("Small budget version DONE!");

		System.out.println("---Summary---");
		System.out.println("Total resolved call sites: " + total);
		System.out.println("Exactly precise than eager version: " + exact);
		System.out.println("Less precise than eager version: " + less);
		System.out.println("More precise than eager version: " + more);
		System.out.println("Unsound results: " + unsound);
		System.out.println("****************************************");

		// ==================== large budget ======================

		DEFAULT_MAX_PASSES = 100;
		DEFAULT_MAX_TRAVERSAL = 10000000;
		pts = DemandCSPointsTo.makeWithBudget(DEFAULT_MAX_TRAVERSAL,
				DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		DemandCSPointsTo ptsLarge = (DemandCSPointsTo) pts;
		Map<Local, PointsToSet> askLarge = new HashMap<Local, PointsToSet>();

		System.out
				.println("----------------Starting large budget version-----------------");
		time = 0;

		less = 0;
		more = 0;
		exact = 0;
		total = 0;
		unsound = 0;

		// perform pt-set queries for all call sites and record the pt sets.

		for (Local receiver : askEager.keySet()) {
			Date start = new Date();
			PointsToSet ps = ptsLarge.reachingObjects(receiver);
			Date end = new Date();
			time += (end.getTime() - start.getTime());
			askLarge.put(receiver, ps);
			total++;

			if (ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				exact++;
			} else if (ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& !ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				less++;
			} else if (!ps.possibleTypes().containsAll(
					ptsEager.reachingObjects(receiver).possibleTypes())
					&& ptsEager.reachingObjects(receiver).possibleTypes()
							.containsAll(ps.possibleTypes())) {
				more++;
			} else {
				unsound++;
			}
		}

		one = 0;
		ten = 0;
		twenty = 0;
		morethan = 0;

		for (Local receiver : askEager.keySet()) {
			if (askEager.get(receiver).possibleTypes().size() <= 1) {
				one++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 10) {
				ten++;
			} else if (askEager.get(receiver).possibleTypes().size() <= 20) {
				twenty++;
			} else {
				morethan++;
			}
		}
		System.out.println("There are totally " + askEager.size()
				+ " receivers to analyze.");
		System.out.println("There are totally " + one
				+ " receivers with one element");
		System.out.println("There are totally " + ten
				+ " receivers with ten elements");
		System.out.println("There are totally " + twenty
				+ " receivers with twenty elements");
		System.out.println("There are totally " + morethan
				+ " receivers with more than elements");
		System.out.println("Construction time: " + time + " million seconds");
		System.out.println("Large budget version DONE!");

		System.out.println("----Summary----");
		System.out.println("Total resolved call sites: " + total);
		System.out.println("Exactly precise than eager version: " + exact);
		System.out.println("Less precise than eager version: " + less);
		System.out.println("More precise than eager version: " + more);
		System.out.println("Unsound results: " + unsound);
		System.out.println("****************************************");

		// ==================== All DONE ==================

		System.out.println("All verification PASSED!");

		// ==================== dump =======================
		StringBuilder str = new StringBuilder();
		// comparing eager version and small budget version
		for (Local receiver : askEager.keySet()) {
			PointsToSet eager = askEager.get(receiver);
			PointsToSet small = askSmall.get(receiver);
			if (small.possibleTypes().size() > eager.possibleTypes().size()) {
				str.append("*****receiver: " + receiver + "\n");
				str.append("*****eager: " + eager.possibleTypes() + "\n");
				str.append("*****small: " + small.possibleTypes() + "\n");
				str.append("----------------------------------------------------\n");
			} else {
				str.append("receiver: " + receiver + "\n");
				str.append("eager: " + eager.possibleTypes() + "\n");
				str.append("small: " + small.possibleTypes() + "\n");
				str.append("----------------------------------------------------\n");
			}
		}
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"output/small"));

			bufw.write(str.toString());
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		str = new StringBuilder();
		// comparing eager version and large budget version
		for (Local receiver : askEager.keySet()) {
			PointsToSet eager = askEager.get(receiver);
			PointsToSet large = askLarge.get(receiver);
			if (large.possibleTypes().size() < eager.possibleTypes().size()) {
				str.append("*****receiver: " + receiver + "\n");
				str.append("*****eager: " + eager.possibleTypes() + "\n");
				str.append("*****large: " + large.possibleTypes() + "\n");
				str.append("----------------------------------------------------\n");
			} else {
				// str.append("receiver: " + receiver + "\n");
				// str.append("eager: " + eager.possibleTypes() + "\n");
				// str.append("large: " + large.possibleTypes() + "\n");
				// str.append("----------------------------------------------------\n");
			}
		}
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"output/large"));

			bufw.write(str.toString());
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		// Congratulations information
		System.out.println("Congratulations! Have a good night~");
		assert (false);
	}
}
