package edu.utexas.cgrex.benchmarks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

	private static String outLoc = "";

	private double totalTimeOnCha = 0;
	
	private double totalTimeOnCipa = 0;
	
	private double totalTimeOnNoOpt = 0;
	
	private double totalTimeNormal = 0;
	
	private double totalNoCut = 0.0;
    
	private int maxQueries = 100;
    
	private static double senCg = 0.0;
	private static double senUnit = 0.0;
	private static double expCg = 0.0;
    
    // trun on crossover?
	private boolean crossOver = false;
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
			outLoc = outLoc + benName + "/cgoutput-3-17.txt";
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
			} else if (benName.equals("chart")) {
				senCg = 3374.0;
				senUnit = 0.52;
				expCg = 46.0;
				targetMain = "dacapo.chart.Main";
				targetLoc = prefix + "benchmarks/chart/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/chart/jar/chart.jar:" + prefix
						+ "benchmarks/chart/jar/lowagie.jar";

			} else if (benName.equals("fop")) {
				senCg = 6688.0;
				senUnit = 0.84;
				expCg = 66.0;
				targetLoc = prefix + "benchmarks/fop/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/fop/jar/fop.jar:" + prefix
						+ "benchmarks/fop/jar/serializer-2.7.0.jar:" + prefix
						+ "benchmarks/fop/jar/avalon-framework-4.2.0.jar:"
						+ prefix + "benchmarks/fop/jar/commons-io-1.3.1.jar:"
						+ prefix
						+ "benchmarks/fop/jar/xmlgraphics-commons-1.3.1.jar:"
						+ prefix
						+ "benchmarks/fop/jar/commons-logging-1.0.4.jar:"
						+ prefix + "benchmarks/fop/jar/xml-apis-ext.jar";

			} else if (benName.equals("bloat")) {
				senCg = 1680.0;
				senUnit = 1.29;
				expCg = 32.0;
				targetLoc = prefix + "benchmarks/bloat/classes";
				targetMain = "dacapo.bloat.Main";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/bloat/jar/bloat.jar";
			} else if (benName.equals("hsqldb")) {
				senCg = 1377.0;
				senUnit = 0.3;
				expCg = 38.0;
				targetMain = "dacapo.hsqldb.Main";
				targetLoc = prefix + "benchmarks/hsqldb/classes";
				cp = "lib/rt.jar:" + prefix
						+ "shared/dacapo-2006-10-MR2/classes:" + prefix
						+ "benchmarks/hsqldb/jar/hsqldb.jar";
			} else if (benName.equals("xalan")) {
				senCg = 409.0;
				senUnit = 0.34;
				expCg = 22.0;
				targetLoc = prefix + "benchmarks/xalan/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/xalan/jar/xalan.jar:" + prefix
						+ "benchmarks/xalan/jar/serializer.jar";
			} else if (benName.equals("batik")) {
				senCg = 3954.0;
				senUnit = 0.7;
				expCg = 68.0;
				targetLoc = prefix + "benchmarks/batik/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/batik/jar/batik-all.jar:"
						+ prefix + "benchmarks/batik/jar/crimson-1.1.3.jar:"
						+ prefix + "benchmarks/batik/jar/xml-apis-ext.jar";
			} else if (benName.equals("sunflow")) {
				senCg = 9663.0;
				senUnit = 1.01;
				expCg = 48.0;
				targetLoc = prefix + "benchmarks/sunflow/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/sunflow/jar/sunflow-0.07.2.jar:"
						+ prefix + "benchmarks/sunflow/jar/janino-2.5.12.jar";
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

		List<String> querySet = new ArrayList<String>();
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
		// Collections.shuffle(querySet);

		Set<String> outSet = new HashSet<String>();
		boolean flag = false;
		System.out.println("querySet: " + querySet.size());
		for (String q : querySet) {
			cnt++;
			long startNormal = System.nanoTime();
			String regx = qm.getValidExprBySig(q);
			boolean res1 = qm.queryRegx(regx);
			long endNormal = System.nanoTime();
			totalTimeNormal += (endNormal - startNormal);
			
			//cipt w/o opt.
			if (!crossOver) {
//				long startNoOpt = System.nanoTime();
//				boolean res2 = qm.queryRegxNoLookahead(regx);
//				long endNoOpt = System.nanoTime();
//				totalTimeOnNoOpt += (endNoOpt - startNoOpt);
//
//				long startNoCut = System.nanoTime();
//				boolean res4 = qm.queryRegxNoMincut(regx);
//				long endNoCut = System.nanoTime();
//				totalNoCut += (endNoCut - startNoCut);

				long startCipa = System.nanoTime();
				boolean res5 = qm.queryWithoutRefine(regx);
				long endCipa = System.nanoTime();
				totalTimeOnCipa += (endCipa - startCipa);

//				long startCha = System.nanoTime();
//				String regxCha = qmCha.getValidExprBySig(q);
//				boolean res3 = qmCha.queryWithoutRefine(regxCha);
//				long endCha = System.nanoTime();
//				totalTimeOnCha += (endCha - startCha);

				if (!res5)
					falseCipa++;

			} else {
				double expTime = totalTimeNormal / 1e9;
				double diff = (expTime + expCg) - (senCg + senUnit * cnt);
				if (diff > 0) {
					System.out.println("Crossing over point: " + cnt + " time:"
							+ expTime);
					flag = true;
					break;
				}
			}
			
			if (!res1) {
				falseCnt++;
				System.out.println("unreach:" + q);
				outSet.add("unreach:" + q);
			} else {
				System.out.println("yesreach:" + q);
				outSet.add("yesreach:" + q);
			}
			
			if(cnt >= maxQueries)
				break;
		}
		if(!flag)
			System.out.println("Crossing over point: N/A");
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
