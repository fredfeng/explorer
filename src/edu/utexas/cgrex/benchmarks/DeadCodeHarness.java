package edu.utexas.cgrex.benchmarks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

	public static String queryLoc = "";
	
	public static String outLoc = "";

	double totalTimeOnCha = 0;
	
	double totalTimeOnCipa = 0;
	
	double totalTimeOnNoOpt = 0;
	
	double totalTimeNormal = 0;
	
    double totalNoCut = 0.0;
    
    int maxQueries = 100;
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
		String targetLoc = "", cp = "", targetMain = "org.dacapo.harness.ChordHarness";
		outLoc = prefix + "benchmarks/"; 
		if (args.length > 0) {
			// run from shell.
			String benName = args[0];
			outLoc = outLoc + benName + "/cgoutput.txt";
			if (benName.equals("luindex")) {
				targetLoc = prefix + "benchmarks/luindex/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/luindex/jar/lucene-core-2.4.jar:"
						+ prefix
						+ "benchmarks/luindex/jar/lucene-demos-2.4.jar";
			} else if (benName.equals("lusearch")) {
				targetLoc = prefix + "benchmarks/lusearch/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/lusearch/jar/lucene-core-2.4.jar";
			} else if (benName.equals("antlr")) {
				targetLoc = prefix + "benchmarks/antlr/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/antlr/jar/antlr.jar:";
				targetMain = "dacapo.antlr.Main";
			} else if (benName.equals("avrora")) {
				targetLoc = prefix + "benchmarks/avrora/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix
						+ "benchmarks/avrora/jar/avrora-cvs-20091224.jar";
			} else {
				assert benName.equals("pmd") : "unknown benchmark" + benName;
				targetLoc = prefix + "benchmarks/pmd/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/pmd/jar/asm-3.1.jar:" + prefix
						+ "benchmarks/pmd/jar/jaxen-1.1.1.jar:" + prefix
						+ "benchmarks/pmd/jar/pmd-4.2.5.jar:" + prefix
						+ "benchmarks/pmd/jar/junit-3.8.1.jar:" + prefix
						+ "benchmarks/pmd/jar/ant.jar";
			}
		}


		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.dead", new DeadCodeHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-soot-classpath", cp,
							"-main-class", targetMain,
							// "-no-bodies-for-excluded",
							"-p", "cg.spark", "enabled:true",
							"-p", "cg.spark", "simulate-natives:false",

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
		int appSize = 0;
		for (SootMethod meth : SootUtils.getChaReachableMethods()) {
			if(!meth.isJavaLibraryMethod()) 
				appSize++;
			//ignore trivial methods.
			if (meth.isJavaLibraryMethod()
					|| Scene.v().getEntryPoints().contains(meth)
					|| meth.isConstructor()
					|| meth.getName().contains("toString")
					|| meth.getName().contains("<clinit>"))
				continue;
			
			String query = main.getSignature() + ".*" + meth.getSignature();
			querySet.add(query);
		}
		
		int falseCnt = 0;
		int falseCipa = 0;

		int cnt = 0;
		QueryManager qmCha = new QueryManager(SootUtils.getCha(), main);

		Set<String> outSet = new HashSet<String>();
		for (String q : querySet) {
			cnt++;
			long startNormal = System.nanoTime();
			String regx = qm.getValidExprBySig(q);
			boolean res1 = qm.queryRegx(regx);
			long endNormal = System.nanoTime();
			totalTimeNormal += (endNormal - startNormal);
			
			//cipt w/o opt.
			long startNoOpt = System.nanoTime();
			boolean res2 = qm.queryRegxNoLookahead(regx);
			long endNoOpt = System.nanoTime();
			totalTimeOnNoOpt += (endNoOpt - startNoOpt);
			
			long startNoCut = System.nanoTime();
			boolean res4 = qm.queryRegxNoMincut(regx);
			long endNoCut = System.nanoTime();
			totalNoCut += (endNoCut - startNoCut);
			
			long startCipa = System.nanoTime();
			boolean res5 = qm.queryWithoutRefine(regx);
			long endCipa = System.nanoTime();
			totalTimeOnCipa += (endCipa - startCipa);

			long startCha = System.nanoTime();
			String regxCha = qmCha.getValidExprBySig(q);
			boolean res3 = qmCha.queryWithoutRefine(regxCha);
			long endCha = System.nanoTime();
			totalTimeOnCha += (endCha - startCha);
			
			if (!res1) {
				falseCnt++;
				System.out.println("unreach:" + q);
				outSet.add("unreach:" + q);
			} else {
				System.out.println("yesreach:" + q);
				outSet.add("yesreach:" + q);
			}
			
			if(!res5)
				falseCipa++;
				
			if(cnt >= maxQueries)
				break;
		}
		//dump info.
		System.out.println("----------DeadCode report-------------------------");
		System.out.println("Total methods in App: " + appSize);
		System.out.println("Total refutations: " + falseCnt);
		System.out.println("Total refutations(cipa): " + falseCipa);
		System.out.println("Total time on Normal: " + totalTimeNormal/1e6);
		System.out.println("Total time on no Cipa: " + totalTimeOnCipa/1e6);
		System.out.println("Total time on no cut: " + totalNoCut/1e6);
		System.out.println("Total time on CHA: " + (totalTimeOnCha/1e6));
		System.out.println("Total time w/o look ahead: " + (totalTimeOnNoOpt/1e6));
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(outLoc, "UTF-8");
			writer.println("----------DeadCode report-------------------------");
			writer.println("Total refutations: " + falseCnt);
			writer.println("Total refutations(cipa): " + falseCipa);
			writer.println("Total time on Normal: " + totalTimeNormal/1e6);
			writer.println("Total time on no Cipa: " + totalTimeOnCipa/1e6);
			writer.println("Total time on no cut: " + totalNoCut/1e6);
			writer.println("Total methods in App: " + appSize);
			writer.println("Total time on CHA: " + (totalTimeOnCha/1e6));
			writer.println("Total time w/o look ahead: " + (totalTimeOnNoOpt/1e6));
			writer.println("Method detail----");
			for(String out : outSet) {
				writer.println(out);
			}
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
}
