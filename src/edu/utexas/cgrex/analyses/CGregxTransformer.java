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
 * Entry point of the whole analysis.
 * 
 * @author yufeng
 * 
 */
public class CGregxTransformer extends SceneTransformer {

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
		
		/* BEGIN: on-the-fly eager CALL graph*/
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
		AutoPAG otfAutoPAG = new AutoPAG(otfPag);
		otfAutoPAG.build();	
		/* END: on-the-fly eager CALL graph*/

		qm = new QueryManager(ddAutoPAG, this.buildCallGraph());
		
		otfQm = new QueryManager(otfAutoPAG, otfAutoPAG.getFather()
				.getOnFlyCallGraph().callGraph());
		
		this.runBenchmark();
	}
	
	private void runBenchmark() {
		//picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(otfQm);
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
