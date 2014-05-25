package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.test.RegularExpGenerator;
import edu.utexas.cgrex.utils.SootUtils;
import edu.utexas.cgrex.utils.StringUtil;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * 
 * @author yufeng
 * 
 */
public class DaCapoTransformer extends SceneTransformer {
	public boolean debug = true;

	QueryManager qm; // used to generate queries
	public static int numQueries = 1000; // number of queries

	QueryManager qm1; // early-stop
	QueryManager qm2; // regular-stop

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		StringUtil.reportInfo("========== DaCapo Transformer ==========");
		/* BEGIN: CHA-based demand-driven CALL graph */
		long startCHA = System.nanoTime();
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

		final PAG pag = b.setup(opts);
		b.build();

		// Build type masks
		pag.getTypeManager().makeTypeMask();

		long endCHA = System.nanoTime();
		/* END: CHA-based demand-driven CALL graph */

		if (!opts.on_fly_cg() || opts.vta()) {
			CallGraphBuilder cgb = new CallGraphBuilder(pag);
			cgb.build();
		}
		StringUtil.reportSec("Building CHA call graph", startCHA, endCHA);


		/* preparation for demand-driven analysis */
		// propagate to fill the P2Set of each node in the pag
		new PropWorklist(pag).propagate();

		Scene.v().setPointsToAnalysis(pag);

		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;

		DemandCSPointsTo ptsEarly = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		ptsEarly.enableEarlyStop();
		ptsEarly.disableBudget();
		assert (ptsEarly.useEarlyStop());

		DemandCSPointsTo ptsReg = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		ptsReg.disableEarlyStop();
		ptsReg.disableBudget();
		assert (!ptsReg.useEarlyStop());
		
		/* use CHA-based pag to generate queries */

		qm1 = new QueryManager(null, Scene.v().getCallGraph(), false, ptsEarly);
		qm2 = new QueryManager(null, Scene.v().getCallGraph(), false, ptsReg);
		qm = qm1;
		/* finish preparation */
		
		List<String> queries = genQueries();
		/* finish generating queries */

		/* demand-driven analysis */
		runByintervals(queries);
	}

	private void runByintervals(List<String> queries) {

		long time1 = 0;
		long time2 = 0;

		try {

			int i = 1;
			long startDd, endDd;
			for (String regx : queries) {
				System.out.println("-----------------------------------------");
				System.out
						.println("[Analysis] Start the " + i + "-th query...");

				// regular
				startDd = System.nanoTime();
				boolean res2 = qm2.queryRegx(regx);
				endDd = System.nanoTime();
				StringUtil.reportSec("Regular stop: " + i, startDd, endDd);
				time2 += (endDd - startDd);
				System.out.println("Analysis result: " + res2);

				// early stop
				startDd = System.nanoTime();
				boolean res1 = qm1.queryRegx(regx);
				endDd = System.nanoTime();
				StringUtil.reportSec("Early stop: " + i, startDd, endDd);
				time1 += (endDd - startDd);
				System.out.println("Analysis result: " + res1);

				assert (res1 == res2);
				System.out.println("[Analysis] The " + i + "-th query PASSED!");
				System.out.println("-----------------------------------------");

				i++;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}

	// generate a set of regular expressions
	private List<String> genQueries() {
		// picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(qm);
		List<String> queries = new ArrayList<String>();

		int cur = 0;
		// how many queries do we need?
		while (cur < numQueries) {
			String regx = generator.genRegx();
			regx = regx.replaceAll("\\s+", "");

			queries.add(regx);
			cur++;
		}

		return queries;
	}

}
