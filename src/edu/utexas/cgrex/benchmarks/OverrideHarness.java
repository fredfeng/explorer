package edu.utexas.cgrex.benchmarks;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.CompilationDeathException;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.queue.QueueReader;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class OverrideHarness extends SceneTransformer{

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
//		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/classes";
//		String cp = "lib/rt.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/jar/lucene-core-2.4.jar";
//		String targetMain = "org.dacapo.harness.ChordHarness";
		
//		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/luindex/classes";
//		String cp = "lib/rt.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/luindex/jar/lucene-core-2.4.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/luindex/jar/lucene-demos-2.4.jar";
//		String targetMain = "org.dacapo.harness.ChordHarness";
		
		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/sunflow/classes";
		String cp = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/sunflow/jar/janino-2.5.12.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/sunflow/jar/sunflow-0.07.2.jar";
		String targetMain = "org.dacapo.harness.ChordHarness";
		
		System.out.println("benchmark----------" + targetLoc);
		
		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.dead", new OverrideHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-soot-classpath", cp,
							"-main-class", targetMain,
							/*
							 * "-no-bodies-for-excluded", "-exclude", "java",
							 * "-exclude", "javax", "-output-format", "none",
							 */
							"-p", "cg.spark", "enabled:true", });

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
		SootMethod main = Scene.v().getMainMethod();
		QueryManager qm = new QueryManager(Scene.v().getCallGraph(), main);
		Set<String> querySet = new HashSet<String>();
		
		QueueReader qe = qm.getReachableMethods().listener();
		while(qe.hasNext()) {
			SootMethod meth = (SootMethod)qe.next();
			SootClass declareClz = meth.getDeclaringClass();
			if(meth.isJavaLibraryMethod())
				continue;
			
			for (SootClass sub : SootUtils.subTypesOf(declareClz)) {
				if (sub.declaresMethod(meth.getSubSignature())
						&& !sub.equals(declareClz)) {
					String query = main.getSignature() + ".*"
							+ meth.getSignature() + ".*"
							+ sub.getMethod(meth.getSubSignature());
					querySet.add(query);
				}
			}
		}
		
		int f = 0;
		int cnt = 0;
		for(String q : querySet) {
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			boolean res1 = qm.queryRegx(regx);
			if(!qm.defaultAns())
				continue;
			
			cnt++;
			System.out.println(q + " Query result:" + res1 + " " + cnt
					+ " out of " + querySet.size() + " false:" + f);
			
			if (!res1) {
				assert false;
				f++;
			}
			
			if(cnt == 20)
				break;
		}
	}
}
