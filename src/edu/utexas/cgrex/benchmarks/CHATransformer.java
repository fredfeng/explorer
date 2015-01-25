package edu.utexas.cgrex.benchmarks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import soot.SceneTransformer;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.SparkOptions;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * our algorithm, which starts with a CHA-based CG.
 * @author yufeng
 * 
 */
public class CHATransformer extends SceneTransformer {

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

		//no need to Propagate. This will run our actual points-to analysis.
		AutoPAG ddAutoPAG = new AutoPAG(pag);
		ddAutoPAG.build();	
		long endCHA = System.nanoTime();
		StringUtil.reportSec("Building CHA call graph", startCHA, endCHA);
		/* END: CHA-based demand-driven CALL graph*/

		qm = new QueryManager(this.buildCallGraph());
		
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
	
	//build a CHA-based call graph 
	public CallGraph buildCallGraph()
	{
		CallGraphBuilder cg = new CallGraphBuilder(DumbPointerAnalysis.v());
		cg.build();
		
		return cg.getCallGraph();
	}
	
}
