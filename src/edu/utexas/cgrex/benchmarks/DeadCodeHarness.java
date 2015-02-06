package edu.utexas.cgrex.benchmarks;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.G;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Transform;
import soot.jimple.ReachingTypeDumper;
import soot.jimple.spark.builder.ContextInsensitiveBuilder;
import soot.jimple.spark.geom.geomPA.GeomPointsTo;
import soot.jimple.spark.ondemand.DemandCSPointsTo;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.PAG2HTML;
import soot.jimple.spark.pag.PAGDumper;
import soot.jimple.spark.solver.EBBCollapser;
import soot.jimple.spark.solver.PropAlias;
import soot.jimple.spark.solver.PropCycle;
import soot.jimple.spark.solver.PropIter;
import soot.jimple.spark.solver.PropMerge;
import soot.jimple.spark.solver.PropWorklist;
import soot.jimple.spark.solver.Propagator;
import soot.jimple.spark.solver.SCCCollapser;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.SparkOptions;
import soot.util.queue.QueueReader;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class DeadCodeHarness extends SceneTransformer{

	public static int benchmarkSize = 10;
	
	//we will collect the running time at each interval.
	public static int interval = 5;

	// 0: interactive mode; 1: benchmark mode
	public static int mode = 1;
	
	public static String queryLoc = "";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/classes";
		String cp = "lib/rt.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/jar/lucene-core-2.4.jar";
		String targetMain = "org.dacapo.harness.ChordHarness";
		System.out.println("benchmark----------" + targetLoc);
		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.dead", new DeadCodeHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-soot-classpath", cp,
							"-main-class", targetMain,
							// "-no-bodies-for-excluded",
							/*
							 * "-no-bodies-for-excluded", "-exclude", "java",
							 * "-exclude", "javax", "-output-format", "none",
							 */
							"-p", "cg.spark", "enabled:true",

					});

		} catch (CompilationDeathException e) {
			e.printStackTrace();
			if (e.getStatus() != CompilationDeathException.COMPILATION_SUCCEEDED)
				throw e;
			else
				return;
		}

	}

	@Override
	protected void internalTransform(String phaseName,
			Map<String, String> options) {
		// TODO Auto-generated method stub
		CallGraph cha = SootUtils.getCHA();
		SootMethod main = Scene.v().getMainMethod();
		QueryManager qm = new QueryManager(cha, main);
		Set<String> querySet = new HashSet<String>();

		QueueReader<MethodOrMethodContext> qe = qm.getReachableMethods()
				.listener();
		while (qe.hasNext()) {
			SootMethod meth = (SootMethod) qe.next();

			if (meth.isJavaLibraryMethod()
					|| Scene.v().getEntryPoints().contains(meth))
				continue;

			String query = main.getSignature() + ".*" + meth.getSignature();
			querySet.add(query);
		}
		
		int falseCnt = 0;
		int cnt = 0;
		for (String q : querySet) {
			cnt++; 
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			boolean res1 = qm.queryRegx(regx);
			if (!res1) {
				falseCnt++;
				System.out.println(falseCnt + " || " + cnt +  "--****-out of---" + querySet.size());
				System.out.println(q + " Query result:" + res1);
			}
		}
	}
}
