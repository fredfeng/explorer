package edu.utexas.cgrex.benchmarks;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.pointer.DumbPointerAnalysis;
import soot.options.SparkOptions;
import soot.util.Chain;
import edu.utexas.cgrex.Harness;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.AutoPAG;
import edu.utexas.cgrex.test.RegularExpGenerator;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * Generate n numbers of valid regular expressions from CHA-based automaton.
 * valid means the answer for the query should be YES.
 * @author yufeng
 * 
 */
public class QueryGenTransformer extends SceneTransformer {

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

		qm = new QueryManager(ddAutoPAG, this.buildCallGraph());
		
		genQueries();
	}
	
	//generate a set of regular expressions
	private void genQueries() {
		//picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(qm);
		StringBuilder sb = new StringBuilder("");
		
		int cur = 0;
		//how many queries do we need?
		while (cur < GenRegxHarness.benchmarkSize) {
			String regx = generator.genRegx();
			regx = regx.replaceAll("\\s+", "");
			
			boolean res = qm.queryRegx(regx);
			
			//only need valid regx.
			if(res) {
				String tgt = generator.getSigRegx();
				System.out.println("regx----" + res + regx);
				System.out.println("regx-***--" + tgt);
				String newRegx = qm.getValidExprBySig(tgt);
				assert(newRegx.equals(regx));
				sb.append(tgt).append("\n");
				//dump the regx to files.
				cur++;
			}
		}
		
		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/regx.txt"));
			bufw.write(sb.toString());
			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
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
