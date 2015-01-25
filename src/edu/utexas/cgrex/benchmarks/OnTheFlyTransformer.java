package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

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

/**
 * Build a precise CG on the fly, then perform the queries.
 * @author yufeng
 * 
 */
public class OnTheFlyTransformer extends SceneTransformer {

	public boolean debug = true;

	//CHA.
	QueryManager qm;
	
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub
		
        System.out.println("Running OnTheFlyTransformer------------");
		/* BEGIN: on-the-fly eager CALL graph*/
		long startOTF = System.nanoTime();
		HashMap<String, String> opt2 = new HashMap<String, String>(options);
		opt2.put("enabled", "true");
		opt2.put("verbose", "true");
		opt2.put("field-based", "false");
		opt2.put("on-fly-cg", "true");
		opt2.put("set-impl", "double");
		opt2.put("double-set-old", "hybrid");
		opt2.put("double-set-new", "hybrid");

		SparkOptions opts2 = new SparkOptions(opt2);

		// Build pointer assignment graph
		ContextInsensitiveBuilder b2 = new ContextInsensitiveBuilder();

		final PAG otfPag = b2.setup(opts2);
		b2.build();

		// Build type masks
		otfPag.getTypeManager().makeTypeMask();

		//no need to Propagate. This will run our actual points-to analysis.
//		new OndemandInsensitiveWorklist(otfPag).propagate();
		new PropWorklist(otfPag).propagate();
		AutoPAG otfAutoPAG = new AutoPAG(otfPag);
		otfAutoPAG.build();	
		long endOTF = System.nanoTime();
		StringUtil.reportSec("Building On-the-fly call graph", startOTF, endOTF);

		/* END: on-the-fly eager CALL graph*/

		qm = new QueryManager(otfAutoPAG.getFather()
				.getOnFlyCallGraph().callGraph());

        System.out.println("Reachable Methods: " + qm.getReachableMethods().size());
		
		runBenchmarkWithoutRefine();
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
        System.exit(0);
	}
	
	//get regular expressions from sootOutput/regx.txt
	private void runBenchmark() {
		String regxSource = BenchmarkHarness.queryLoc;
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(regxSource));
			String line;
			while ((line = br.readLine()) != null) {
			   // process the line.
				String regx = qm.getValidExprBySig(line);
				boolean res1 = qm.queryRegx(regx);
				System.out.println(line + ": " + res1);
			}
			br.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
