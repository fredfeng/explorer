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
		String targetLoc = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/classes";
		String cp = "lib/rt.jar:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/shared/dacapo-9.12/classes:/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/benchmarks/lusearch/jar/lucene-core-2.4.jar";
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
		CallGraph cg = Scene.v().getCallGraph();
		while(qe.hasNext()) {
			SootMethod meth = (SootMethod)qe.next();

			Set<Edge> inSet = getBs(cg.edgesInto(meth));
			Set<Edge> outSet = getBs(cg.edgesOutOf(meth));

			if (inSet.size() > 1
					&& outSet.size() > 1 && !meth.isConstructor()
					&& (meth.isPublic() || meth.isProtected())) {
				for(Edge s : inSet) {
					SootMethod src = (SootMethod)s.getSrc();
					if(src.isJavaLibraryMethod())
						continue;
					
					if(src.equals(main))
						continue;
					
					for(Edge t : outSet) {
						SootMethod tgt = (SootMethod)t.getTgt();
						if(tgt.isJavaLibraryMethod())
							continue;
						String query = main.getSignature() + ".*"
								+ src.getSignature() + ".*"
								+ tgt.getSignature();
						querySet.add(query);
					}
				}
			}
			
		}
		
		int f = 0;
		int cnt = 0;
		for(String q : querySet) {
			String regx = qm.getValidExprBySig(q);
			regx = regx.replaceAll("\\s+", "");
			cnt++;
			boolean res1 = qm.queryRegx(regx);
			System.out.println(q + " Query result:" + res1 + " " + cnt
					+ " out of " + querySet.size() + " false:" + f);
			
			if (!res1) {
				f++;
			}
			
			if(cnt == 200)
				break;
		}
	}
	
	public static Set<Edge> getBs(Iterator it){
	    Set<Edge> result = new HashSet<Edge>();
	    while (it.hasNext()) {
	        result.add((Edge) it.next());
	    }
	    return result;
	}

}
