package edu.utexas.cgrex.benchmarks;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.CompilationDeathException;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PackManager;
import soot.PointsToAnalysis;
import soot.PrimType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.jimple.CastExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.spark.pag.PAG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.Chain;
import soot.util.queue.QueueReader;
import edu.utexas.cgrex.analyses.DemandCSPointsTo;
import edu.utexas.cgrex.utils.SootUtils;

/**
 * Sanity check for the precision of Manu's pointer analysis.
 * @author yufeng
 *
 */
public class SanityCheckHarness extends SceneTransformer {

	public static String queryLoc = "";
	
	public static String outLoc = "";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
		String targetLoc = "", cp = "", targetMain = "org.dacapo.harness.ChordHarness";
		outLoc = prefix + "benchmarks/";
		// run from shell.
		String benName = "antlr";
		outLoc = outLoc + benName + "/cgoutput.txt";
		if (benName.equals("luindex")) {
			targetLoc = prefix + "benchmarks/luindex/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
					+ prefix + "benchmarks/luindex/jar/lucene-core-2.4.jar:"
					+ prefix + "benchmarks/luindex/jar/lucene-demos-2.4.jar";
		} else if (benName.equals("lusearch")) {
			targetLoc = prefix + "benchmarks/lusearch/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
					+ prefix + "benchmarks/lusearch/jar/lucene-core-2.4.jar";
		} else if (benName.equals("antlr")) {
			targetLoc = prefix + "benchmarks/antlr/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-2006-10-MR2/classes:"
					+ prefix + "benchmarks/antlr/jar/antlr.jar:";
			targetMain = "dacapo.antlr.Main";
		} else if (benName.equals("avrora")) {
			targetLoc = prefix + "benchmarks/avrora/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
					+ prefix + "benchmarks/avrora/jar/avrora-cvs-20091224.jar";
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

		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.dead", new SanityCheckHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs", "-soot-classpath", cp,
							"-main-class", targetMain,
//							 "-no-bodies-for-excluded",
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
//		QueryManager qm = new QueryManager(cicg, main);
		QueueReader<MethodOrMethodContext> queue = Scene.v()
				.getReachableMethods().listener();
		PAG spark = (PAG) Scene.v().getPointsToAnalysis();
//		PointsToAnalysis pt = qm.getDemandPointsTo();
		
		final int DEFAULT_MAX_PASSES = 10;
		final int DEFAULT_MAX_TRAVERSAL = 75000;
		final boolean DEFAULT_LAZY = false;
		
		PointsToAnalysis pt = DemandCSPointsTo.makeWithBudget(
				DEFAULT_MAX_TRAVERSAL, DEFAULT_MAX_PASSES, DEFAULT_LAZY);
		
		int totalCast = 0;
		int castSafeBySpark = 0;
		int castSafeByManu = 0;
		int empty = 0;
		while (queue.hasNext()) {
			SootMethod meth = (SootMethod) queue.next();
			if (meth.isJavaLibraryMethod())
				continue;
			if(!meth.isConcrete()) {
				continue;
			}
			assert meth.isConcrete() : meth;
			Body body = meth.retrieveActiveBody();
			Chain<Unit> units = body.getUnits();
			Iterator<Unit> uit = units.snapshotIterator();
			while (uit.hasNext()) {
				Stmt stmt = (Stmt) uit.next();
				if (stmt instanceof JAssignStmt
						&& ((JAssignStmt) stmt).getRightOp() instanceof CastExpr) {
					CastExpr cast = (CastExpr) ((JAssignStmt) stmt)
							.getRightOp();
					Type castType = cast.getCastType();

					if (cast.getType() instanceof PrimType) {
						System.out.println("Ignore primitive: " + castType);
						continue;
					}
					Local rhs = (Local) cast.getOp();
					Set<Type> sparkTypes = spark.reachingObjects(rhs)
							.possibleTypes();
					Set<Type> ddTypes = pt.reachingObjects(rhs).possibleTypes();
					if (SootUtils.castSafe(castType, sparkTypes))
						castSafeBySpark++;

					if (SootUtils.castSafe(castType, ddTypes)) {
						castSafeByManu++;
						if(ddTypes.size() == 0)
							empty++;
					} else {
						System.out.println("------------------" + stmt);
						System.out.println(ddTypes);
					}
					totalCast++;
				}
			}

		}

		assert false : totalCast + " spark: " + castSafeBySpark + " Manu: "
				+ castSafeByManu + " empty:" + empty;
	}
}