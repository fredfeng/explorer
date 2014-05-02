package edu.utexas.cgrex.analyses;

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
import edu.utexas.cgrex.test.RegularExpGenerator;
import edu.utexas.cgrex.utils.StringUtil;

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
		
		otfQm = new QueryManager(otfAutoPAG, otfAutoPAG.getFather()
				.getOnFlyCallGraph().callGraph());
		
//		this.runBenchmark();
		this.checkSoundness();
	}
	
	private void runBenchmark() {
		//picking up samples from CHA-based version.
		RegularExpGenerator generator = new RegularExpGenerator(otfQm);
		int correctQueries = 0;
		double totalCHA = 0.0;
		double totalOTF = 0.0;


		for (int i = 0; i < Harness.benchmarkSize; i++) {
			String regx = generator.genRegx();
			regx = regx.replaceAll("\\s+", "");
//			System.out.println("Random regx------" + regx);
			long startOTF = System.nanoTime();
			boolean res2 = otfQm.queryRegx(regx);
			long endOTF = System.nanoTime();
			totalOTF += ((endOTF - startOTF)/1e6);
			
			long startCHA = System.nanoTime();
			boolean res1 = qm.queryRegx(regx);
			long endCHA = System.nanoTime();
			totalCHA += ((endCHA - startCHA)/1e6);
			
			if(((i+1) % Harness.interval) == 0) {
				StringUtil.reportInfo("Running Time for OTF at iteration " + (i+1) +":" + totalOTF);
				StringUtil.reportInfo("Running Time for CHA at iteration " + (i+1) +":" + totalCHA);
			}

//			System.out.println("CHA------" + res1);
//			System.out.println("OTF------" + res2);
//			System.out.println("--------------------------------------");
			if(res1 == res2) 
				correctQueries++;
			else {
				//assert(res1 == true);//make sure CHA is always sound.
				//record the error.
				StringUtil.reportRefineFail(regx + "---" + res1 + res2);
			}
		}
		
		System.out.println("benchmark result-------" + correctQueries + "/"
				+ Harness.benchmarkSize);
	}
	
	//build a CHA-based call graph 
	public CallGraph buildCallGraph()
	{
		CallGraphBuilder cg = new CallGraphBuilder(DumbPointerAnalysis.v());
		cg.build();
		
		return cg.getCallGraph();
	}
	
	//comparing the points-to set of soot and our version.
	public void checkSoundness() {
		qm.getAutoPAG().dump();
		//for all reachable methods in current project.
		Iterator<MethodOrMethodContext> it = Scene.v().getReachableMethods().listener();
		while(it.hasNext()) {
			SootMethod method = (SootMethod)it.next();
			if (!method.isConcrete())
				continue;

			Body body = method.retrieveActiveBody();

			Chain<Unit> units = body.getUnits();
			Iterator<Unit> uit = units.snapshotIterator();
			while (uit.hasNext()) {
				Stmt stmt = (Stmt) uit.next();

				// invocation statements
				if (stmt.containsInvokeExpr()) {
					InvokeExpr ie = stmt.getInvokeExpr();
	                if( (ie instanceof VirtualInvokeExpr) 
	                		|| (ie instanceof InterfaceInvokeExpr)){
	                	Value var = ie.getUseBoxes().get(0).getValue();
	                	if(var instanceof Local) {
	                		Local l = (Local) var;
	                		List list = new ArrayList();
	                		list.add(l);
	                	//ask for xinyu's points-to set
	                		Set pts = qm.getAutoPAG().insensitiveQueryTest(list);
	                	//ask for soot's point-to set.
	                		Set pts2 = otfQm.getAutoPAG().insensitiveQueryTest(list);
	                		System.out.println("check------------------------ ");
	                		System.out.println("CHA------- " +pts);
	                		System.out.println("OTF------- " + pts2);

	                	}
	                	
	                }
				}
			}
		}
		
	}
}
