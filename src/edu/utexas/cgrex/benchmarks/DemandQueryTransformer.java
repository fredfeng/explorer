package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.StringUtil;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * our algorithm, which starts with a CHA-based CG.
 * 
 * @author yufeng
 * 
 */
public class DemandQueryTransformer extends SceneTransformer {

	public boolean debug = true;

	// CHA.
	QueryManager qm1;
	QueryManager qm2;

	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		StringUtil.reportInfo("DemandQuery Transformer----------");
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

		qm1 = new QueryManager(null, Scene.v().getCallGraph(), false, ptsEarly);
		qm2 = new QueryManager(null, Scene.v().getCallGraph(), false, ptsReg);

		runByintervals();
	}

	private void runByintervals() {
		String regxSource = DemandQueryHarness.queryLoc;

		long time1 = 0;
		long time2 = 0;
		int range = 20;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(regxSource));
			String line;
			int i = 1;

			while ((line = br.readLine()) != null) {
				// process the line.
				String regx = qm1.getValidExprBySig(line);

				// early stop
				long startDd = System.nanoTime();
				boolean res1 = qm1.queryRegx(regx);
				long endDd = System.nanoTime();
				StringUtil.reportSec("Early Stop: " + i, startDd, endDd);
				time1 += (endDd - startDd);
				System.out.println("Result: " + res1);

				// regular
				startDd = System.nanoTime();
				boolean res2 = qm2.queryRegx(regx);
				endDd = System.nanoTime();
				StringUtil.reportSec("Regular : " + i, startDd, endDd);
				time2 += (endDd - startDd);
				System.out.println("Result: " + res2);

				assert (res1 == res2);

				i++;
				// if (i >= range)
				// break;
			}

			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
}
