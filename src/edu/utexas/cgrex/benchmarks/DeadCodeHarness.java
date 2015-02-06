package edu.utexas.cgrex.benchmarks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class DeadCodeHarness extends SceneTransformer {

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
//		System.out.println("benchmark----------" + targetLoc);
		
//		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
//		String targetLoc = prefix + "benchmarks/antlr/classes";
//		String cp = "lib/rt.jar:"
//				+ prefix + "/shared/dacapo-2006-10-MR2/classes:" + prefix
//				+ "benchmarks/antlr/jar/antlr.jar:";
//		String targetMain = "dacapo.antlr.Main";

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
		CallGraph cicg = Scene.v().getCallGraph();
		SootMethod main = Scene.v().getMainMethod();
		QueryManager qm = new QueryManager(cicg, main);
		Set<String> querySet = new HashSet<String>();

		int java = 0;
		int cc = 0;
		for (SootMethod meth : SootUtils.getChaReachableMethods()) {
			cc++;
			if (meth.isJavaLibraryMethod())
				java++;

			if (meth.isJavaLibraryMethod()
					|| Scene.v().getEntryPoints().contains(meth))
				continue;

			String query = main.getSignature() + ".*" + meth.getSignature();
			querySet.add(query);
		}
//		 assert false : java + " " + cc;

		int falseCnt = 0;
		int cnt = 0;
		for (String q : querySet) {
			cnt++;
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			boolean res1 = qm.queryRegx(regx);
			if (!res1) {
				falseCnt++;
				System.out.println(falseCnt + " || " + cnt + "--****-out of---"
						+ querySet.size());
				System.out.println(" Query result:" + res1);
			}
		}
	}
}
