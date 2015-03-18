package edu.utexas.cgrex.benchmarks;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.CompilationDeathException;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;
import soot.util.queue.QueueReader;
import chord.util.tuple.object.Pair;
import edu.utexas.cgrex.QueryManager;
import edu.utexas.cgrex.analyses.DemandCSPointsTo;
import edu.utexas.cgrex.utils.SCC4Callgraph;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * The harness for dead code detection.
 * @author yufeng
 *
 */
public class SccHarness extends SceneTransformer {

	private static String outLoc = "";

	private double totalTimeOnCha = 0;
	
	private double totalTimeOnCipa = 0;
	
	private double totalTimeOnNoOpt = 0;
	
	private double totalTimeNormal = 0;
	
	private double totalNoCut = 0.0;
    
	private int maxQueries = 100000;
    
	private static double senCg = 0.0;
	private static double senUnit = 0.0;
	private static double expCg = 0.0;
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
		String targetLoc = "", cp = "", targetMain = "org.dacapo.harness.ChordHarness";
		outLoc = prefix + "benchmarks/"; 
//		if (args.length > 0) {
			// run from shell.
//			String benName = args[0];
		String benName = "lusearch";
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
//		}


		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.scc", new SccHarness()));

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
		
		
		QueueReader<MethodOrMethodContext> queue = Scene.v()
				.getReachableMethods().listener();
		PAG spark = (PAG) Scene.v().getPointsToAnalysis();
		
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		PointsToAnalysis pt = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		
		
		Set<Pair<SootMethod, SootMethod>> betterSet = new HashSet<Pair<SootMethod, SootMethod>>();
		int better = 0;
		int totalVirt = 0;
		while (queue.hasNext()) {
			SootMethod meth = (SootMethod) queue.next();
			if (meth.isJavaLibraryMethod())
				continue;
			if (!meth.isConcrete()) {
				continue;
			}
			assert meth.isConcrete() : meth;
			Body body = meth.retrieveActiveBody();
			Chain<Unit> units = body.getUnits();
			Iterator<Unit> uit = units.snapshotIterator();
			while (uit.hasNext()) {
				Stmt stmt = (Stmt) uit.next();
				// check virtual callsites.
				if (stmt.containsInvokeExpr()
						&& (stmt.getInvokeExpr() instanceof InstanceInvokeExpr)) {
					totalVirt++;
					InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt
							.getInvokeExpr();
					Local receiver = (Local) iie.getBase();
					Set<Edge> tgts = new HashSet<Edge>();
					for (Iterator<Edge> it = Scene.v().getCallGraph()
							.edgesOutOf(stmt); it.hasNext();) {
						tgts.add(it.next());
					}
					if (tgts.size() < 2)
						continue;

					Set<Type> sparkTypes = spark.reachingObjects(receiver)
							.possibleTypes();
					Set<Type> ddTypes = pt.reachingObjects(receiver)
							.possibleTypes();
					if (sparkTypes.size() > ddTypes.size()) {
						System.out.println("spark: " + sparkTypes);
						System.out.println("dd: " + ddTypes);
						better++;
						for (Edge tgt : tgts) {
							SootMethod tgtMeth = (SootMethod) tgt.getTgt();
							betterSet.add(new Pair<SootMethod, SootMethod>(
									meth, tgtMeth));
						}
					}
				}
			}

		}
		
//		assert false : totalVirt +  " | " +better;
		// step1: calculate the scc
		SCC4Callgraph sccSet = new SCC4Callgraph(cicg, new HashSet<SootMethod>(
				Scene.v().getEntryPoints()));
		// step2: pick up entry methods of the scc, if any.
		Set<Pair<String, String>> pairs = new HashSet<Pair<String, String>>();
		for (Set<SootMethod> scc : sccSet.getComponents()) {
			if (scc.size() > 10) {
				Set<SootMethod> entries = new HashSet<SootMethod>();
				for (SootMethod entry : scc) {
					for (Iterator<Edge> it = cicg.edgesInto(entry); it
							.hasNext();) {
						Edge inEdge = it.next();
						SootMethod caller = (SootMethod) inEdge.getSrc();
						if (!scc.contains(caller)) {
							entries.add(entry);
							break;
						}
					}
				}
				
				for(Pair<SootMethod, SootMethod> good : betterSet) {
					SootMethod m1 = good.val0;
					SootMethod m2 = good.val1;
					if(scc.contains(m1) && scc.contains(m2)) {
						pairs.add(new Pair<String, String>(m1
								.getSignature(), m2.getSignature()));
					}
				}
//				for (SootMethod src : entries) {
//					for (SootMethod tgt : scc) {
//						if (!entries.contains(tgt))
//							pairs.add(new Pair<String, String>(src
//									.getSignature(), tgt.getSignature()));
//					}
//
//				}
//				System.out.println("good scc:" + scc.size());
//				System.out.println("entries: " + entries.size());
			}
		}
		
		// step3: issue queries in the form of main -> .* -> A .* -> B .* -> A
		Set<String> querySet = new HashSet<String>();
		int appSize = 0;
		for (Pair<String, String> p : pairs) {
			String query = main.getSignature() + ".*" + p.val0 + p.val1;
			querySet.add(query);
		}
		
		int falseCnt = 0;
		int falseCipa = 0;

		int cnt = 0;
		QueryManager qmCha = new QueryManager(SootUtils.getCha(), main);
		// Collections.shuffle(querySet);

		Set<String> outSet = new HashSet<String>();
		System.out.println("querySet: " + querySet.size());
		for (String q : querySet) {
			cnt++;
			String tmp = "<org.dacapo.harness.ChordHarness: void main(java.lang.String[])>.*<org.apache.lucene.search.ConjunctionScorer: boolean doNext()><org.apache.lucene.search.BooleanScorer2: boolean skipTo(int)>";
			String regx = qm.getValidExprBySig(tmp);
//			long startCipa = System.nanoTime();
//			boolean res5 = qm.queryWithoutRefine(regx);
//			long endCipa = System.nanoTime();
//			totalTimeOnCipa += (endCipa - startCipa);
//			
//
//			if (!res5) {
//				falseCipa++;
//				continue;
//			}
			
			long startNormal = System.nanoTime();
			boolean res1 = qm.queryRegx(regx);
			long endNormal = System.nanoTime();
			totalTimeNormal += (endNormal - startNormal);
			
			if (!res1) {
				falseCnt++;
				System.out.println("unreach:" + q);
				outSet.add("unreach:" + q);
			}
			
			if(cnt >= maxQueries)
				break;
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
