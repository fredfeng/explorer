package edu.utexas.cgrex.benchmarks;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ObserverHarness extends SceneTransformer {

	static String outLoc = "";
	
    private final Logger logger = LoggerFactory.getLogger(getClass());
	
	private double totalTimeOnCha = 0;
	
	private double totalTimeOnCipa = 0;
	
	private double totalTimeOnNoOpt = 0;
	
	private double totalTimeNormal = 0;
	
	private double totalNoCut = 0.0;
    
	protected int maxQueries = 300;
	
	protected static boolean compareKobj = false;
	
	protected static String benName = "";
	
	protected static long timeout = 7200 * 1000;	
	
	protected static enum RunType {
		NORMAL, CHA, NOCUT, NOOPT;
	}
	
	protected static RunType currType = RunType.NORMAL;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String prefix = "/home/yufeng/research/benchmarks/pjbench-read-only/dacapo/";
		String targetLoc = "", cp = "", targetMain = "org.dacapo.harness.ChordHarness";
		outLoc = prefix + "benchmarks/"; 
		
		if (args.length > 0) {
//			// run from shell.
			benName = args[0];
			currType = RunType.valueOf(args[1]);
			outLoc = outLoc + benName + "/cgoutput-3-18.txt";
			if (benName.equals("fop")) {
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

			} else if (benName.equals("batik")) {
				targetLoc = prefix + "benchmarks/batik/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/batik/jar/batik-all.jar:"
						+ prefix + "benchmarks/batik/jar/crimson-1.1.3.jar:"
						+ prefix + "benchmarks/batik/jar/xml-apis-ext.jar";
			} else if (benName.equals("sunflow")) {
				targetLoc = prefix + "benchmarks/sunflow/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/sunflow/jar/sunflow-0.07.2.jar:"
						+ prefix + "benchmarks/sunflow/jar/janino-2.5.12.jar";
			} else if (benName.equals("weka")) {
				targetLoc = prefix + "benchmarks/weka/classes";
				cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
						+ prefix + "benchmarks/weka/jar/weka.jar";
			} else if (benName.equals("jmeter")) {
			targetLoc = prefix + "benchmarks/jmeter/classes";
			cp = "lib/rt.jar:" + prefix + "shared/dacapo-9.12/classes:"
					+ prefix + "benchmarks/jmeter/jar/ApacheJMeter.jar:"
					+ prefix + "benchmarks/jmeter/jar/ApacheJMeter_core.jar:"
					+ prefix + "benchmarks/jmeter/jar/xstream-1.4.8.jar:"
					+ prefix + "benchmarks/jmeter/jar/jorphan.jar:" + prefix
					+ "benchmarks/jmeter/jar/avalon-framework-4.1.4.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-logging-1.2.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-io-2.4.jar:"
					+ prefix + "benchmarks/jmeter/jar/logkit-2.0.jar:" + prefix
					+ "benchmarks/jmeter/jar/commons-collections-3.2.1.jar:"
					+ prefix + "benchmarks/jmeter/jar/commons-lang3-3.3.2.jar:"
					+ prefix
					+ "benchmarks/jmeter/jar/rsyntaxtextarea-2.5.6.jar:"
					+ prefix + "benchmarks/jmeter/jar/oro-2.0.8.jar:";
			// + prefix + "benchmarks/jmeter/jar/jcharts-0.7.5.jar";
		} else {
				assert benName.equals("pmd") : "unknown benchmark" + benName;
			}
		}

		try {

			PackManager.v().getPack("wjtp")
					.add(new Transform("wjtp.scc", new ObserverHarness()));

			soot.Main.v().run(
					new String[] { "-W", "-process-dir", targetLoc,
							"-allow-phantom-refs",
							"-soot-classpath", cp,
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
		Set<String> queries = SootUtils.genObsQueries(benName, compareKobj);
		int falseCi = 0;
		int falseExp = 0;
		int cnt = 0;
		long start = System.currentTimeMillis();
		long end = start + timeout; // 60 seconds * 1000 ms/sec

		if(currType == RunType.CHA)
			qm = new QueryManager(SootUtils.getCha(), main, "dummy");
		
		for (String partial : queries) {
			String qq = main.getSignature() + ".*" + partial;
			String regx = qm.getValidExprBySig(qq);
			
			logger.info("Query counter: " + cnt);
			if (System.currentTimeMillis() > end) {
				logger.error("Timeout at query " + cnt);
				break;
			}
			
			if(maxQueries == cnt) {
				break;
			}
			
			if (currType == RunType.NORMAL) {
				long startNormal = System.nanoTime();
				boolean res = qm.queryRegx(regx);
				long endNormal = System.nanoTime();
				totalTimeNormal += (endNormal - startNormal);

				long startCipa = System.nanoTime();
				boolean res2 = qm.queryWithoutRefine(regx);
				long endCipa = System.nanoTime();
				totalTimeOnCipa += (endCipa - startCipa);
				if (!res) {
					falseExp++;
					logger.info("refute: " + qq);
				} else {
					logger.info("truth: " + qq);
				}
				if (!res2)
					falseCi++;
			} else if (currType == RunType.CHA) {
				long startCha = System.nanoTime();
				boolean res3 = qm.queryWithoutRefine(regx);
				long endCha = System.nanoTime();
				totalTimeOnCha += (endCha - startCha);
				if (!res3)
					falseCi++;
			} else if (currType == RunType.NOCUT) {

			} else if (currType == RunType.NOOPT) {
				long startNoOpt = System.nanoTime();
				boolean res3 = qm.queryRegxNoLookahead(regx);
				long endNoOpt = System.nanoTime();
				totalTimeOnNoOpt += (endNoOpt - startNoOpt);
				if (!res3)
					falseCi++;
			}
			cnt++;
		}
		// dump info.
		logger.info("----------ObserverExp report-------------------------");
		logger.info("Total queries: " + cnt);
		logger.info("Total refutations(Explorer): " + falseExp);
		logger.info("Total refutations(cipa): " + falseCi);
		logger.info("Total time on Explorer: " + totalTimeNormal / 1e6);
		logger.info("Total time on no Ci: " + totalTimeOnCipa / 1e6);
		logger.info("Total time on no cut: " + totalNoCut / 1e6);
		logger.info("Total time on CHA: " + (totalTimeOnCha / 1e6));
		logger.info("Total time w/o look ahead: " + (totalTimeOnNoOpt / 1e6));
	}

}
