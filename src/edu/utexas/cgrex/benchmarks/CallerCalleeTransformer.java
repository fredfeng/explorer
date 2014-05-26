package edu.utexas.cgrex.benchmarks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.test.RegularExpGenerator;
import edu.utexas.cgrex.utils.StringUtil;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * our algorithm, which starts with a CHA-based CG.
 * 
 * @author yufeng
 * 
 */
public class CallerCalleeTransformer extends SceneTransformer {

	public boolean debug = true;
	
	public int benSize = 1000;


	// CHA.
	QueryManager qm;

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		StringUtil.reportInfo("Caller-Callee Transformer----------");
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

		// Propagate
		new PropWorklist(pag).propagate();

		if (!opts.on_fly_cg() || opts.vta()) {
			CallGraphBuilder cgb = new CallGraphBuilder(pag);
			cgb.build();
		}
		StringUtil.reportSec("Building CHA call graph", startCHA, endCHA);

		Scene.v().setPointsToAnalysis(pag);

		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;

		DemandCSPointsTo ptsEager = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		ptsEager.enableEarlyStop();
		ptsEager.disableBudget();

		DemandCSPointsTo ptsDd = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		ptsDd.enableEarlyStop();
		ptsDd.disableBudget();
		
		qm = new QueryManager(null, Scene.v().getCallGraph(), true, ptsEager,
				ptsDd);

		runByintervals();
	}

	private void runByintervals() {
		
		RegularExpGenerator generator = new RegularExpGenerator(qm);
		
		int cur = 0;
		//how many queries do we need?
		while (cur < benSize) {
			String callee = generator.genCallee();
			
			long startEa = System.nanoTime();
			Set<String> eaCallers = qm.queryCallers(callee);
			long endEa = System.nanoTime();
			StringUtil.reportSec("Query Time: " + cur, startEa, endEa);
			
			long startDd = System.nanoTime();
			Set<String> callers = qm.queryCallers(callee);
			long endDd = System.nanoTime();

			StringUtil.reportSec("Query Time: " + cur, startDd, endDd);
			
			assert(callers.containsAll(eaCallers) && eaCallers.containsAll(callers));

			StringUtil.reportInfo("Callers of " + callee + ": " + callers);
			


			cur++;
		}
		System.exit(0);
	}
}
