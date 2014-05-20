package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import soot.Scene;
import soot.SceneTransformer;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.SparkOptions;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.utils.StringUtil;
import edu.utexas.spark.ondemand.DemandCSPointsTo;

/**
 * our algorithm, which starts with a CHA-based CG.
 * @author yufeng
 * 
 */
public class IfOnlyIfTransformer extends SceneTransformer {

	public boolean debug = true;

	//CHA.
	QueryManager qm;
	
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		
		/* BEGIN: CHA-based demand-driven CALL graph*/
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
		StringUtil.reportSec("Building CHA call graph", startCHA, endCHA);
		/* END: CHA-based demand-driven CALL graph*/
		
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
//		callgraph = pag.getOnFlyCallGraph().callGraph();

		DemandCSPointsTo ptsDemand = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		Date endOndemand = new Date();
		System.out
				.println("Initialized on-demand refinement-based context-sensitive analysis"
						+ startOnDemand + endOndemand);
		
		qm = new QueryManager(null, Scene.v().getCallGraph());
		
//		runBenchmarkWithoutRefine();
		runBenchmark();
	}
	
	private void runBenchmarkWithoutRefine() {
		String regxSource = BenchmarkHarness.queryLoc;
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(regxSource));
			String line;
			while ((line = br.readLine()) != null) {
			   // process the line.
				String regx = qm.getValidExprBySig(line);
				boolean res1 = qm.queryWithoutRefine(regx);
				System.out.println(line + ": " + res1);
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	//get regular expressions from sootOutput/regx.txt
	private void runBenchmark() {
		String regxSource = IfAndOnlyIfHarness.queryLoc;
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(regxSource));
			String line;
			while ((line = br.readLine()) != null) {
			   // process the line.
				String regx = qm.getValidExprBySig(line);
				boolean res1 = qm.queryRegx(regx);
				boolean res2 = qm.queryRegxEager(regx);
				System.out.println(line + ": " + res1 + res2 );
				assert(res1 == res2);
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("End of Test!");
		assert(false);

	}
	
}
