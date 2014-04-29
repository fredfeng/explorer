package edu.utexas.cgrex.analyses;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import soot.G;
import soot.Scene;
import soot.SceneTransformer;
import soot.SourceLocator;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.SparkOptions;
import edu.utexas.cgrex.Harness;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.test.RegularExpGenerator;

/**
 * Transformer for interactive debugging.
 * 
 * @author yufeng
 * 
 */
public class TestTransformer extends SceneTransformer {

	public boolean debug = true;

	//CHA.
	QueryManager qm;
	
	//on-the-fly.
	QueryManager otfQm;
	
	protected void internalTransform(String phaseName,
			@SuppressWarnings("rawtypes") Map options) {
		// TODO Auto-generated method stub

		/* BEGIN: CHA-based demand-driven CALL graph*/
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
		/* END: CHA-based demand-driven CALL graph*/
		qm = new QueryManager(ddAutoPAG, this.buildCallGraph());
		qm.doQuery();
		
	}
	
	private void runBenchmark() {
		//picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(qm);
		int correctQueries = 0;
		for (int i = 0; i < Harness.benchmarkSize; i++) {
			String regx = generator.genRegx();
			regx = regx.replaceAll("\\s+", "");
			System.out.println("Random regx------" + regx);
			boolean res1 = qm.queryRegx(regx);
			boolean res2 = otfQm.queryRegx(regx);
			System.out.println("CHA------" + res1);
			System.out.println("OTF------" + res2);
			System.out.println("--------------------------------------");
			if(res1 == res2) correctQueries++;
		}
		
		System.out.println("benchmark result-------" + correctQueries + "/" + Harness.benchmarkSize);
	}
	
	//build a CHA-based call graph 
	public CallGraph buildCallGraph()
	{
		CallGraphBuilder cg = new CallGraphBuilder(DumbPointerAnalysis.v());
		cg.build();
		return cg.getCallGraph();
	}
}
